package com.nighthawkapps.wallet.android.ui.setup

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.exception.InitializerException
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.SERVER_HOST
import com.nighthawkapps.wallet.android.ext.SERVER_PORT
import com.nighthawkapps.wallet.android.ext.getInt
import com.nighthawkapps.wallet.android.ext.getString
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
     * Throw an exception if the seed phrase is bad.
     */
    fun validatePhrase(seedPhrase: String) {
        mnemonics.validate(seedPhrase.toCharArray())
    }

    fun loadBirthdayHeight(): Int? {
        return lockBox[Const.Backup.BIRTHDAY_HEIGHT]
    }

    suspend fun newWallet(): Initializer {
        twig("Initializing new wallet")
        with(mnemonics) {
            storeWallet(nextMnemonic(nextEntropy()), loadNearestBirthday())
        }
        return openStoredWallet()
    }

    suspend fun importWallet(seedPhrase: String, birthdayHeight: Int): Initializer {
        twig("Importing wallet. Requested birthday: $birthdayHeight")
        storeWallet(seedPhrase.toCharArray(), loadNearestBirthday(birthdayHeight))
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

    private suspend fun loadConfig(): Initializer.Config {
        val vk = lockBox.getCharsUtf8(Const.Backup.VIEWING_KEY)?.let { String(it) }
            ?: onMissingViewingKey()
        return Initializer.Config { config ->
            val birthdayHeight = loadBirthdayHeight()
                ?: onMissingBirthday()
            val host = getString(SERVER_HOST, Const.Default.Server.HOST)
            val port = getInt(SERVER_PORT, Const.Default.Server.PORT)

            config.importWallet(vk, birthdayHeight, host, port)
        }
    }

    private fun onMissingBirthday(): Int {
        return loadNearestBirthday()?.height ?: throw InitializerException.MissingBirthdayException
    }

    private suspend fun onMissingViewingKey(): String {
        // potentially log this scenario and add any logic that might help troubleshoot issues

        // this can happen across upgrades from wallets that only relied on the seed
        // the code here allows those wallets to migrate by using the only thing they have stored
        // (the seed) to create and store the viewing key for future use.
        try {
            val seed = lockBox.getBytes(Const.Backup.SEED)!!
            return DerivationTool.deriveViewingKeys(seed)[0].also {
                storeViewingKey(it)
            }
        } catch (t: Throwable) {
            throw InitializerException.MissingViewingKeyException
        }
    }

    private fun loadNearestBirthday(birthdayHeight: Int? = null) =
        WalletBirthdayTool.loadNearest(NighthawkWalletApp.instance, birthdayHeight)

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
        birthday: WalletBirthdayTool.WalletBirthday
    ) {
        check(!lockBox.getBoolean(Const.Backup.HAS_SEED)) {
            "Error! Cannot store a seed when one already exists! This would overwrite the" +
                    " existing seed and could lead to a loss of funds if the user has no backup!"
        }

        storeBirthday(birthday)

        mnemonics.toSeed(seedPhraseChars).let { bip39Seed ->
            DerivationTool.deriveViewingKeys(bip39Seed)[0].let { viewingKey ->
                storeSeedPhrase(seedPhraseChars)
                storeSeed(bip39Seed)
                storeViewingKey(viewingKey)
            }
        }
    }

    private suspend fun storeBirthday(birthday: WalletBirthdayTool.WalletBirthday) = withContext(
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

    private suspend fun storeViewingKey(vk: String) = withContext(Dispatchers.IO) {
        twig("storeViewingKey vk: ${vk.length}")
        lockBox[Const.Backup.VIEWING_KEY] = vk
    }
}