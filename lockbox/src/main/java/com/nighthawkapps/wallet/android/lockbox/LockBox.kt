package com.nighthawkapps.wallet.android.lockbox

import android.content.Context
import cash.z.android.plugin.LockBoxPlugin
import de.adorsys.android.securestoragelibrary.SecurePreferences
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject

class LockBox @Inject constructor(private val appContext: Context) : LockBoxPlugin {

    private val maxLength: Int = 50

    override fun setBoolean(key: String, value: Boolean) {
        setChunkedString(key, value.toString())
    }

    override fun getBoolean(key: String): Boolean {
        return getChunkedString(key)?.toBoolean() ?: false
    }

    override fun setBytes(key: String, value: ByteArray) {
        // using hex here because this library doesn't really work well for byte arrays
        // but hopefully we can code to arrays and then change the underlying library, later
        setChunkedString(key, value.toHex())
    }

    override fun getBytes(key: String): ByteArray? {
        return getChunkedString(key)?.fromHex()
    }

    override fun setCharsUtf8(key: String, value: CharArray) {
        // Using string here because this library doesn't work well for char arrays
        // but hopefully we can code to arrays and then change the underlying library, later
        setChunkedString(key, String(value))
    }

    override fun getCharsUtf8(key: String): CharArray? {
        return getChunkedString(key)?.toCharArray()
    }

    fun delete(key: String) {
        return SecurePreferences.removeValue(appContext, key)
    }
    fun clear() {
        SecurePreferences.clearAllValues(appContext)
    }

    inline operator fun <reified T> set(key: String, value: T) {
        when (T::class) {
            Boolean::class -> setBoolean(key, value as Boolean)
            ByteArray::class -> setBytes(key, value as ByteArray)
            CharArray::class -> setCharsUtf8(key, value as CharArray)
            Double::class, Float::class, Integer::class, Long::class, String::class -> setChunkedString(key, value.toString())
            else -> throw UnsupportedOperationException("Lockbox does not yet support setting ${T::class.java.simpleName} objects but it can easily be added.")
        }
    }

    inline operator fun <reified T> get(key: String): T? = when (T::class) {
        Boolean::class -> getBoolean(key)
        ByteArray::class -> getBytes(key)
        CharArray::class -> getCharsUtf8(key)
        Double::class -> getChunkedString(key)?.let { it.toDoubleOrNull() }
        Float::class -> getChunkedString(key)?.let { it.toFloatOrNull() }
        Integer::class -> getChunkedString(key)?.let { it.toIntOrNull() }
        Long::class -> getChunkedString(key)?.let { it.toLongOrNull() }
        String::class -> getChunkedString(key)
        else -> throw UnsupportedOperationException("Lockbox does not yet support getting ${T::class.simpleName} objects but it can easily be added")
    } as T

    /**
     * Splits a string value into smaller pieces so as not to exceed the limit on the length of
     * String that can be stored.
     */
    fun setChunkedString(key: String, value: String) {
        if (value.length > maxLength) {
            SecurePreferences.setValue(appContext, key, value.chunked(maxLength))
        } else {
            SecurePreferences.setValue(appContext, key, value)
        }
    }

    /**
     * Returns a string value from storage by first fetching the key, directly. If that is missing,
     * it checks for a chunked version of the key. If that exists, it will be merged and returned.
     * If not, then null will be returned.
     *
     * @return the key if found and null otherwise.
     */
    fun getChunkedString(key: String): String? {
        return SecurePreferences.getStringValue(appContext, key, null)
            ?: SecurePreferences.getStringListValue(appContext, key, listOf()).let { result ->
                if (result.size == 0) null else result.joinToString("")
            }
    }


    //
    // Extensions (TODO: find library that works better with arrays of bytes and chars)
    //

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this)
            sb.append(String.format("%02x", b))
        return sb.toString()
    }

    private fun String.fromHex(): ByteArray {
        val len = length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] =
                ((Character.digit(this[i], 16) shl 4) + Character.digit(this[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun CharArray.toBytes(): ByteArray {
        val byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(this))
        return Arrays.copyOf(byteBuffer.array(), byteBuffer.limit());
    }

    private fun ByteArray.fromBytes(): CharArray {
        val charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(this))
        return Arrays.copyOf(charBuffer.array(), charBuffer.limit())
    }
}


