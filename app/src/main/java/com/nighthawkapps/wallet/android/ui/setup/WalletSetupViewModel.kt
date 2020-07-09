package com.nighthawkapps.wallet.android.ui.setup

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Initializer.DefaultBirthdayStore
import cash.z.ecc.android.sdk.Initializer.DefaultBirthdayStore.Companion.ImportedWalletBirthdayStore
import cash.z.ecc.android.sdk.Initializer.DefaultBirthdayStore.Companion.NewWalletBirthdayStore
import cash.z.ecc.android.sdk.ext.twig
import com.nighthawkapps.wallet.android.NighthawkWalletApp

import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.kotlin.mnemonic.Mnemonics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WalletSetupViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var mnemonics: Mnemonics

    @Inject
    lateinit var lockBox: LockBox

    enum class WalletSetupState {
        UNKNOWN, SEED_WITH_BACKUP, SEED_WITHOUT_BACKUP, NO_SEED
    }

    fun checkSeed(): Flow<WalletSetupState> = flow {
        when {
            lockBox.getBoolean(LockBoxKey.HAS_BACKUP) -> emit(WalletSetupState.SEED_WITH_BACKUP)
            lockBox.getBoolean(LockBoxKey.HAS_SEED) -> emit(WalletSetupState.SEED_WITHOUT_BACKUP)
            else -> emit(WalletSetupState.NO_SEED)
        }
    }

    /**
     * Re-open an existing wallet. This is the most common use case, where a user has previously
     * created or imported their seed and is returning to the wallet. In other words, this is the
     * non-FTUE case.
     */
    fun openWallet(): Initializer {
        twig("Opening existing wallet")
        return NighthawkWalletApp.component.initializerSubcomponent()
            .create(DefaultBirthdayStore(NighthawkWalletApp.instance)).run {
                initializer().open(birthdayStore().getBirthday())
            }
    }

    suspend fun newWallet(): Initializer {
        twig("Initializing new wallet")
        return NighthawkWalletApp.component.initializerSubcomponent()
            .create(NewWalletBirthdayStore(NighthawkWalletApp.instance)).run {
                initializer().apply {
                    new(createWallet(), birthdayStore().getBirthday())
                }
            }
    }

    suspend fun importWallet(seedPhrase: String, birthdayHeight: Int): Initializer {
        twig("Importing wallet. Requested birthday: $birthdayHeight")
        return NighthawkWalletApp.component.initializerSubcomponent()
            .create(ImportedWalletBirthdayStore(NighthawkWalletApp.instance, birthdayHeight)).run {
                initializer().apply {
                    import(importWallet(seedPhrase.toCharArray()), birthdayStore().getBirthday())
                }
            }
    }

    /**
     * Take all the steps necessary to create a new wallet and measure how long it takes.
     *
     * @param feedback the object used for measurement.
     */
    private suspend fun createWallet(): ByteArray = withContext(Dispatchers.IO) {
        check(!lockBox.getBoolean(LockBoxKey.HAS_SEED)) {
            "Error! Cannot create a seed when one already exists! This would overwrite the" +
                    " existing seed and could lead to a loss of funds if the user has no backup!"
        }

        mnemonics.run {
            nextEntropy().let { entropy ->
                nextMnemonic(entropy).let { seedPhrase ->
                    toSeed(seedPhrase).let { bip39Seed ->
                        lockBox.setCharsUtf8(LockBoxKey.SEED_PHRASE, seedPhrase)
                        lockBox.setBoolean(LockBoxKey.HAS_SEED_PHRASE, true)
                        lockBox.setBytes(LockBoxKey.SEED, bip39Seed)
                        lockBox.setBoolean(LockBoxKey.HAS_SEED, true)
                        bip39Seed
                    }
                }
            }
        }
    }

    suspend fun loadBirthdayHeight(): Int = withContext(Dispatchers.IO) {
        DefaultBirthdayStore(NighthawkWalletApp.instance).getBirthday().height
    }

    /**
     * Take all the steps necessary to import a wallet and measure how long it takes.
     *
     * @param feedback the object used for measurement.
     */
    private suspend fun importWallet(
        seedPhrase: CharArray
    ): ByteArray = withContext(Dispatchers.IO) {
        check(!lockBox.getBoolean(LockBoxKey.HAS_SEED)) {
            "Error! Cannot import a seed when one already exists! This would overwrite the" +
                    " existing seed and could lead to a loss of funds if the user has no backup!"
        }
        mnemonics.run {
            toSeed(seedPhrase).let { bip39Seed ->
                lockBox.setCharsUtf8(LockBoxKey.SEED_PHRASE, seedPhrase)
                lockBox.setBoolean(LockBoxKey.HAS_SEED_PHRASE, true)
                lockBox.setBytes(LockBoxKey.SEED, bip39Seed)
                lockBox.setBoolean(LockBoxKey.HAS_SEED, true)
                bip39Seed
            }
        }
    }

    /**
     * Throw an exception if the seed phrase is bad.
     */
    fun validatePhrase(seedPhrase: String) {
        mnemonics.validate(seedPhrase.toCharArray())
    }

    object LockBoxKey {
        const val SEED = "cash.z.ecc.android.SEED"
        const val SEED_PHRASE = "cash.z.ecc.android.SEED_PHRASE"
        const val HAS_SEED = "cash.z.ecc.android.HAS_SEED"
        const val HAS_SEED_PHRASE = "cash.z.ecc.android.HAS_SEED_PHRASE"
        const val HAS_BACKUP = "cash.z.ecc.android.HAS_BACKUP"
    }
}