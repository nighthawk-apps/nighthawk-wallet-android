package com.nighthawkapps.wallet.android.ui.setup

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.exception.InitializerException
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import cash.z.ecc.android.sdk.type.UnifiedViewingKey
import cash.z.ecc.android.sdk.type.WalletBirthday
import cash.z.ecc.android.sdk.type.ZcashNetwork
import com.nighthawkapps.wallet.android.BuildConfig
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.Const.HOST_PORT
import com.nighthawkapps.wallet.android.ext.Const.HOST_SERVER
import com.nighthawkapps.wallet.android.ext.failWith
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.kotlin.mnemonic.Mnemonics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class WalletSetupViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var mnemonics: Mnemonics

    @Inject
    lateinit var lockBox: LockBox

    @Inject
    @Named(Const.Name.APP_PREFS)
    lateinit var prefs: LockBox

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
     * A hook to run logic that needs to execute before sync starts. This is a good place for
     * "first run" logic, which typically involves fixing known issues from previous versions or
     * migrating data.
     */
    suspend fun onPrepareSync(synchronizer: Synchronizer) {
        // call prepare here so that we can rewind if we need to
        synchronizer.prepare()
        val existingVersion = lockBox.get<Int?>(Const.App.LAST_VERSION) ?: -1
        // if this is the first run of this version
        if (existingVersion < BuildConfig.VERSION_CODE) {
            twig("Detected the first run of app version ${BuildConfig.VERSION_CODE}, executing first run logic.")
            synchronizer.quickRewind()
            lockBox[Const.App.LAST_VERSION] = BuildConfig.VERSION_CODE
            twig("First run logic complete. Saving version ${BuildConfig.VERSION_CODE}.")
        } else {
            twig("App Version ${BuildConfig.VERSION_CODE} has run before on this device. Skipping first run logic!")
        }
    }

    /**
     * Throw an exception if the seed phrase is bad.
     */
    fun validatePhrase(seedPhrase: String) {
        mnemonics.validate(seedPhrase.toCharArray())
    }

    fun loadBirthdayHeight(): Int? {
        return lockBox[Const.Backup.BIRTHDAY_HEIGHT]
    }

//    suspend fun onRestore() {
//        val vk = lockBox.getCharsUtf8(Const.Backup.VIEWING_KEY)?.let { String(it) } ?: onMissingViewingKey()
//        val birthdayHeight = loadBirthdayHeight() ?: 0
//        importWallet(vk, birthdayHeight)
//    }

    suspend fun newWallet(): Initializer {
        val network = NighthawkWalletApp.instance.defaultNetwork
        twig("Initializing new ${network.networkName} wallet")
        with(mnemonics) {
            storeWallet(nextMnemonic(nextEntropy()), network, loadNearestBirthday(network))
        }
        return openStoredWallet()
    }

    suspend fun importWallet(seedPhrase: String, birthdayHeight: Int): Initializer {
        val network = NighthawkWalletApp.instance.defaultNetwork
        twig("Importing ${network.networkName} wallet. Requested birthday: $birthdayHeight")
        storeWallet(seedPhrase.toCharArray(), network, loadNearestBirthday(network, birthdayHeight))
        return openStoredWallet()
    }

    /**
     * Re-open an existing wallet. This is the most common use case, where a user has previously
     * created or imported their seed and is returning to the wallet. In other words, this is the
     * non-FTUE case.
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
        val host = prefs[HOST_SERVER] ?: Const.Default.Server.HOST
        val port = prefs[HOST_PORT] ?: Const.Default.Server.PORT

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

    private fun onMissingBirthday(network: ZcashNetwork): Int = failWith(InitializerException.MissingBirthdayException) {
        twig("Warning: Birthday was missing. We will fall back to sapling activation. This may be worth messaging to the user.")
        loadNearestBirthday(network, network.saplingActivationHeight).height
    }

    private fun loadNearestBirthday(network: ZcashNetwork, birthdayHeight: Int? = null) =
        WalletBirthdayTool.loadNearest(NighthawkWalletApp.instance, network, birthdayHeight)

    private suspend fun onMissingViewingKey(network: ZcashNetwork): UnifiedViewingKey {
        twig("Viewing key was missing attempting migration")
        var migrationViewingKey: UnifiedViewingKey? = null
        try {
            val seed = lockBox.getBytes(Const.Backup.SEED)!!
            migrationViewingKey = DerivationTool.deriveUnifiedViewingKeys(seed, network)[0]
            twig("Successfully recreated Unified Viewing Key from seed!")
        } catch (t: Throwable) {
            twig("Failed to migrate viewing key. This is an non-recoverable error.")
        }

        // this will happen during rare upgrade scenarios when the user migrates from a seed-only wallet to this vk-based version
        // or during more common scenarios where the user migrates from a vk only wallet to a unified vk wallet
        if (migrationViewingKey != null) {
            // at this point we store the migrated key and future launches will work as expected, without this migration step
            storeUnifiedViewingKey(migrationViewingKey)
            return migrationViewingKey
        } else {
            throw InitializerException.MissingViewingKeyException
        }
    }

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
        birthday: WalletBirthday
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

    private suspend fun storeBirthday(birthday: WalletBirthday) = withContext(
        Dispatchers.IO
    ) {
        twig("Storing birthday ${birthday.height} with and key ${Const.Backup.BIRTHDAY_HEIGHT}")
        lockBox[Const.Backup.BIRTHDAY_HEIGHT] = birthday.height
    }

    private suspend fun storeSeedPhrase(seedPhrase: CharArray) = withContext(Dispatchers.IO) {
        twig("Storing seedphrase: ${seedPhrase.size}")
        lockBox[Const.Backup.SEED_PHRASE] = seedPhrase
        lockBox[Const.Backup.HAS_SEED_PHRASE] = true
    }

    private suspend fun storeSeed(bip39Seed: ByteArray) = withContext(Dispatchers.IO) {
        twig("Storing seed: ${bip39Seed.size}")
        lockBox.setBytes(Const.Backup.SEED, bip39Seed)
        lockBox[Const.Backup.HAS_SEED] = true
    }

    private suspend fun storeUnifiedViewingKey(vk: UnifiedViewingKey) = withContext(Dispatchers.IO) {
        twig("storeViewingKey vk: ${vk.extfvk.length}")
        lockBox[Const.Backup.VIEWING_KEY] = vk.extfvk
        lockBox[Const.Backup.PUBLIC_KEY] = vk.extpub
    }
}
