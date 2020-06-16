package com.nighthawkapps.wallet.android.lockbox

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LockBoxText {

    private lateinit var appContext: Context
    private lateinit var lockBox: LockBoxProvider

    @Before
    fun start() {
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
        lockBox = LockBox(appContext)
    }

    @Test
    fun testSeed_store() {
        val testMessage = "Some Bytes To Test"
        val testBytes = testMessage.toByteArray()
        lockBox.setBytes("seed", testBytes)
        assertEquals(testMessage, String(lockBox.getBytes("seed")!!))
    }

    @Test
    fun testSeed_storeNegatives() {
        val testBytes = byteArrayOf(0x00, 0x00, -0x0F, -0x0B)
        lockBox.setBytes("seed", testBytes)
        assertTrue(testBytes.contentEquals(lockBox.getBytes("seed")!!))
    }

    @Test
    fun testSeed_storeLeadingZeros() {
        val testBytes = byteArrayOf(0x00, 0x00, 0x0F, 0x0B)
        lockBox.setBytes("seed", testBytes)
        assertTrue(testBytes.contentEquals(lockBox.getBytes("seed")!!))
    }

    @Test
    fun testPrivateKey_retrieve() {
        val testMessage = "Some Bytes To Test"
        lockBox.setCharsUtf8("spendingKey", testMessage.toCharArray())
        assertEquals(testMessage, String(lockBox.getCharsUtf8("spendingKey")!!))
    }
}
