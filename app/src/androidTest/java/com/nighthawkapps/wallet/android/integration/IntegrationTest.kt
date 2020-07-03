package com.nighthawkapps.wallet.android.integration

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.Initializer
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.kotlin.mnemonic.Mnemonics
import okio.Buffer
import okio.GzipSink
import okio.Okio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntegrationTest {

    private lateinit var appContext: Context
    private val mnemonics = Mnemonics()
    private val phrase =
        "human pulse approve subway climb stairs mind gentle raccoon warfare fog roast sponsor" +
                " under absorb spirit hurdle animal original honey owner upper empower describe"

    @Before
    fun start() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun testSeed_generation() {
        val seed = mnemonics.toSeed(phrase.toCharArray())
        assertEquals(
            "Generated incorrect BIP-39 seed!",
            "f4e3d38d9c244da7d0407e19a93c80429614ee82dcf62c141235751c9f1228905d12a1f275f" +
                    "5c22f6fb7fcd9e0a97f1676e0eec53fdeeeafe8ce8aa39639b9fe",
            seed.toHex()
        )
    }

    @Test
    fun testSeed_storage() {
        val seed = mnemonics.toSeed(phrase.toCharArray())
        val lb = LockBox(appContext)
        lb.setBytes("seed", seed)
        assertTrue(seed.contentEquals(lb.getBytes("seed")!!))
    }

    @Test
    fun testPhrase_storage() {
        val lb = LockBox(appContext)
        val phraseChars = phrase.toCharArray()
        lb.setCharsUtf8("phrase", phraseChars)
        assertTrue(phraseChars.contentEquals(lb.getCharsUtf8("phrase")!!))
    }

    @Test
    fun testPhrase_maxLengthStorage() {
        val lb = LockBox(appContext)
        // find and expose the max length
        var acceptedSize = 256
        while (acceptedSize > 0) {
            try {
                lb.setCharsUtf8("temp", nextString(acceptedSize).toCharArray())
                break
            } catch (t: Throwable) {
            }
            acceptedSize--
        }

        // 215 (max length of each word is 8)
        val maxSeedPhraseLength = 8 * 24 + 23
        assertTrue(
            "LockBox does not support the maximum length seed phrase." +
                    " Expected: $maxSeedPhraseLength but was: $acceptedSize",
            acceptedSize > maxSeedPhraseLength
        )
    }

    @Test
    fun testAddress() {
        val seed = mnemonics.toSeed(phrase.toCharArray())
        val initializer = Initializer(appContext).apply {
            new(
                seed,
                Initializer.DefaultBirthdayStore(appContext).newWalletBirthday,
                overwrite = true
            )
        }
        assertEquals(
            "Generated incorrect z-address!",
            "zs1gn2ah0zqhsxnrqwuvwmgxpl5h3ha033qexhsz8tems53fw877f4gug353eefd6z8z3n4zxty65c",
            initializer.rustBackend.getAddress()
        )
        initializer.clear()
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this)
            sb.append(String.format("%02x", b))
        return sb.toString()
    }

    fun String.gzip(): ByteArray {
        val result = Buffer()
        val sink = Okio.buffer(GzipSink(result))
        sink.use {
            sink.write(toByteArray())
        }
        return result.readByteArray()
    }

    fun nextString(length: Int): String {
        val allowedChars = "ACGT"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}