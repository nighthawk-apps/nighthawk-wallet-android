package com.nighthawkapps.wallet.android.ui.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.exception.InitializerException
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.UnifiedViewingKey
import cash.z.ecc.android.sdk.type.ZcashNetwork
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.Const.HOST_PORT
import com.nighthawkapps.wallet.android.ext.Const.HOST_SERVER
import com.nighthawkapps.wallet.android.ext.failWith
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.kotlin.mnemonic.Mnemonics
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WalletSetupViewModel @Inject constructor() : ViewModel() {

    private val mnemonics by lazy { Mnemonics() }

    private val lockBox by lazy { LockBox(NighthawkWalletApp.instance) }

    enum class WalletSetupState {
        SEED_WITH_BACKUP, SEED_WITHOUT_BACKUP, NO_SEED
    }

    fun checkSeed(): Flow<WalletSetupState> = flow {
        when {
            lockBox.getBoolean(Const.Backup.HAS_BACKUP) -> emit(WalletSetupState.SEED_WITH_BACKUP)
            lockBox.getBoolean(Const.Backup.HAS_SEED) -> emit(WalletSetupState.SEED_WITHOUT_BACKUP)
            else -> emit(WalletSetupState.NO_SEED)
        }
    }

    /**
     * Throw an exception if the seed phrase is bad.
     */
    fun validatePhrase(seedPhrase: String) {
        mnemonics.validate(seedPhrase.toCharArray())
    }

    fun loadBirthdayHeight(): BlockHeight? {
        val h: Int? = lockBox[Const.Backup.BIRTHDAY_HEIGHT]
        twig("Loaded birthday with key ${Const.Backup.BIRTHDAY_HEIGHT} and found $h")
        h?.let {
            return BlockHeight.new(NighthawkWalletApp.instance.defaultNetwork, it.toLong())
        }
        return null
    }

    suspend fun newWallet(): Initializer {
        val network = NighthawkWalletApp.instance.defaultNetwork
        twig("Initializing new ${network.networkName} wallet")
        with(mnemonics) {
            storeWallet(nextMnemonic(nextEntropy()), network, loadNearestBirthday(network))
        }
        return openStoredWallet()
    }

    suspend fun importWallet(seedPhrase: String, birthdayHeight: BlockHeight?): Initializer {
        val network = NighthawkWalletApp.instance.defaultNetwork
        twig("Importing ${network.networkName} wallet. Requested birthday: $birthdayHeight")
        storeWallet(seedPhrase.toCharArray(), network, birthdayHeight ?: loadNearestBirthday(network))
        return openStoredWallet()
    }

    /**
     * Re-open an existing wallet. This is the most common use case, where a user has previously
     * created or imported their seed and is returning to the wallet.
     */
    suspend fun openStoredWallet(): Initializer {
        val config = loadConfig()
        return NighthawkWalletApp.component.initializerSubcomponent().create(config).initializer()
    }

    /**
     * Build a config object by loading in the viewingKey, birthday and server info which is already
     * known by this point.
     */
    private suspend fun loadConfig(): Initializer.Config {
        twig("Loading config variables")
        var overwriteVks = false
        val network = NighthawkWalletApp.instance.defaultNetwork
        val vk = loadUnifiedViewingKey() ?: onMissingViewingKey(network).also { overwriteVks = true }
        val birthdayHeight = loadBirthdayHeight() ?: onMissingBirthday(network)
        // TDDO: verify that we're using the same values that would get set by the user in settings
        val host = lockBox[HOST_SERVER] ?: Const.Default.Server.HOST
        val port = lockBox[HOST_PORT] ?: Const.Default.Server.PORT

        twig("Done loading config variables")
        return Initializer.Config {
            it.importWallet(vk, birthdayHeight, network, host, port)
            it.setOverwriteKeys(overwriteVks)
        }
    }

    private fun loadUnifiedViewingKey(): UnifiedViewingKey? {
        val extfvk = lockBox.getCharsUtf8(Const.Backup.VIEWING_KEY)
        val extpub = lockBox.getCharsUtf8(Const.Backup.PUBLIC_KEY)
        return when {
            extfvk == null || extpub == null -> {
                if (extfvk == null) {
                    twig("Warning: Shielded key was missing")
                }
                if (extpub == null) {
                    twig("Warning: Transparent key was missing")
                }
                null
            }
            else -> UnifiedViewingKey(extfvk = String(extfvk), extpub = String(extpub))
        }
    }

    private suspend fun onMissingViewingKey(network: ZcashNetwork): UnifiedViewingKey {
        twig("Recover VK: Viewing key was missing")
        // add some temporary logic to help us troubleshoot this problem.
        NighthawkWalletApp.instance.getSharedPreferences("SecurePreferences", Context.MODE_PRIVATE)
            .all.map { it.key }.joinToString().let { keyNames ->
                "${Const.Backup.VIEWING_KEY}, ${Const.Backup.PUBLIC_KEY}".let { missingKeys ->
                    var recoveryViewingKey: UnifiedViewingKey? = null
                    var ableToLoadSeed = false
                    try {
                        val seed = lockBox.getBytes(Const.Backup.SEED)!!
                        ableToLoadSeed = true
                        twig("Recover UVK: Seed found")
                        recoveryViewingKey = DerivationTool.deriveUnifiedViewingKeys(seed, network)[0]
                        twig("Recover UVK: successfully derived UVK from seed")
                    } catch (t: Throwable) {
                        twig("Failed while trying to recover UVK due to: $t")
                    }

                    // this will happen during rare upgrade scenarios when the user migrates from a seed-only wallet to this vk-based version
                    // or during more common scenarios where the user migrates from a vk only wallet to a unified vk wallet
                    if (recoveryViewingKey != null) {
                        storeUnifiedViewingKey(recoveryViewingKey)
                        return recoveryViewingKey
                    }
                    throw InitializerException.MissingViewingKeyException
                }
            }
    }

    private suspend fun onMissingBirthday(network: ZcashNetwork): BlockHeight = failWith(InitializerException.MissingBirthdayException) {
        twig("Recover Birthday: falling back to sapling birthday")
        loadNearestBirthday(network)
    }

    private suspend fun loadNearestBirthday(network: ZcashNetwork) =
        BlockHeight.ofLatestCheckpoint(
            NighthawkWalletApp.instance,
            network
        )

    //
    // Storage Helpers
    //

    /**
     * Entry point for all storage. Takes a seed phrase and stores all the parts so that we can
     * selectively use them, the next time the app is opened. Although we store everything, we
     * primarily only work with the viewing key and spending key. The seed is only accessed when
     * presenting backup information to the user.
     */
    private suspend fun storeWallet(
        seedPhraseChars: CharArray,
        network: ZcashNetwork,
        birthday: BlockHeight
    ) {
        check(!lockBox.getBoolean(Const.Backup.HAS_SEED)) {
            "Error! Cannot store a seed when one already exists! This would overwrite the" +
                " existing seed and could lead to a loss of funds if the user has no backup!"
        }

        storeBirthday(birthday)

        mnemonics.toSeed(seedPhraseChars).let { bip39Seed ->
            DerivationTool.deriveUnifiedViewingKeys(bip39Seed, network)[0].let { viewingKey ->
                storeSeedPhrase(seedPhraseChars)
                storeSeed(bip39Seed)
                storeUnifiedViewingKey(viewingKey)
            }
        }
    }

    private suspend fun storeBirthday(birthday: BlockHeight) = withContext(IO) {
        twig("Storing birthday ${birthday.value} with and key ${Const.Backup.BIRTHDAY_HEIGHT}")
        lockBox[Const.Backup.BIRTHDAY_HEIGHT] = birthday.value
    }

    private suspend fun storeSeedPhrase(seedPhrase: CharArray) = withContext(IO) {
        twig("Storing seedphrase: ${seedPhrase.size}")
        lockBox[Const.Backup.SEED_PHRASE] = seedPhrase
        lockBox[Const.Backup.HAS_SEED_PHRASE] = true
    }

    private suspend fun storeSeed(bip39Seed: ByteArray) = withContext(IO) {
        twig("Storing seed: ${bip39Seed.size}")
        lockBox.setBytes(Const.Backup.SEED, bip39Seed)
        lockBox[Const.Backup.HAS_SEED] = true
    }

    private suspend fun storeUnifiedViewingKey(vk: UnifiedViewingKey) = withContext(IO) {
        twig("storeViewingKey vk: ${vk.extfvk.length}")
        lockBox[Const.Backup.VIEWING_KEY] = vk.extfvk
        lockBox[Const.Backup.PUBLIC_KEY] = vk.extpub
    }
}
