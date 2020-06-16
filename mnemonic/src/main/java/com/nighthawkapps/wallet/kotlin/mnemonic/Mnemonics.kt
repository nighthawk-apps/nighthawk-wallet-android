package com.nighthawkapps.wallet.kotlin.mnemonic

import cash.z.android.plugin.MnemonicPlugin
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.Mnemonics.MnemonicCode
import cash.z.ecc.android.bip39.Mnemonics.WordCount
import cash.z.ecc.android.bip39.toEntropy
import cash.z.ecc.android.bip39.toSeed
import java.util.*
import javax.inject.Inject

class Mnemonics @Inject constructor() : MnemonicPlugin {
    override fun fullWordList(languageCode: String) = Mnemonics.getCachedWords(Locale.ENGLISH.language)
    override fun nextEntropy(): ByteArray = WordCount.COUNT_24.toEntropy()
    override fun nextMnemonic(): CharArray = MnemonicCode(WordCount.COUNT_24).chars
    override fun nextMnemonic(entropy: ByteArray): CharArray = MnemonicCode(entropy).chars
    override fun nextMnemonicList(): List<CharArray> = MnemonicCode(WordCount.COUNT_24).words
    override fun nextMnemonicList(entropy: ByteArray): List<CharArray> = MnemonicCode(entropy).words
    override fun toSeed(mnemonic: CharArray): ByteArray = MnemonicCode(mnemonic).toSeed()
    override fun toWordList(mnemonic: CharArray): List<CharArray> = MnemonicCode(mnemonic).words

    fun validate(mnemonic: CharArray) {
        MnemonicCode(mnemonic).validate()
    }
}