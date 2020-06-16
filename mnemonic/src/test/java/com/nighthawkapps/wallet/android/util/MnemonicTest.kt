package com.nighthawkapps.wallet.android.util

import cash.z.android.plugin.MnemonicPlugin
import com.nighthawkapps.wallet.kotlin.mnemonic.Mnemonics
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.Math.max
import java.util.*


class MnemonicTest {

    lateinit var mnemonics: MnemonicPlugin

    @Before
    fun start() {
        mnemonics = Mnemonics()
    }

    @Test
    fun testSeed_fromMnemonic() {
        val seed = mnemonics.run {
            toSeed(nextMnemonic())
        }
        assertEquals(64, seed.size)
    }

    @Test
    fun testMnemonic_create() {
        val words = String(mnemonics.nextMnemonic()).split(' ')
        assertEquals(24, words.size)
        validate(words)
    }

    @Test
    fun testMnemonic_createList() {
        val words = mnemonics.nextMnemonicList()
        assertEquals(24, words.size)
        validate(words.map { String(it) })
    }

    @Test
    fun testMnemonic_toList() {
        val words = mnemonics.run {
            toWordList(nextMnemonic())
        }
        assertEquals(24, words.size)
        validate(words.map { String(it) })
    }

    @Test
    fun testMnemonic_longestWord() {
        var max = 0
        val englishWordList = mnemonics.fullWordList(Locale.ENGLISH.language)
        repeat(2048) {
            max = max(max, englishWordList[it].length)
        }
        assertEquals(8, max)
    }

    private fun validate(words: List<String>) {
        val englishWordList = mnemonics.fullWordList(Locale.ENGLISH.language)
        // return or crash!
        words.forEach { word ->
            var i = 0
            while (true) {
                if (englishWordList[i++] == word) {
                    println(word)
                    break
                }

            }
        }
    }
}

private fun CharSequence.toWords(): List<CharSequence> {
    return mutableListOf<CharSequence>().let { result ->
        var index = 0
        repeat(length) {
            if (this[it] == ' ') {
                result.add(subSequence(index, it))
                index = it + 1
            }
        }
        result.add(subSequence(index, length))
        result
    }
}
