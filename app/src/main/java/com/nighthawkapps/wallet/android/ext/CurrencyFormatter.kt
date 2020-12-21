package com.nighthawkapps.wallet.android.ext

import cash.z.ecc.android.sdk.ext.Conversions
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.ext.toZec
import com.nighthawkapps.wallet.android.ext.ConversionsUniform.FULL_FORMATTER
import com.nighthawkapps.wallet.android.ext.ConversionsUniform.LONG_SCALE
import com.nighthawkapps.wallet.android.ext.ConversionsUniform.SHORT_FORMATTER
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object ConversionsUniform {
    var ONE_ZEC_IN_ZATOSHI = BigDecimal(ZcashSdk.ZATOSHI_PER_ZEC, MathContext.DECIMAL128)
    val LONG_SCALE = 8
    val SHORT_SCALE = 8
    val SHORT_FORMATTER = from(SHORT_SCALE)
    val FULL_FORMATTER = from(LONG_SCALE)
    val roundingMode = RoundingMode.HALF_EVEN
    var ZEC_FORMATTER = (NumberFormat.getNumberInstance(Locale("en", "UK")) as DecimalFormat).apply {
        applyPattern("###.##")
        roundingMode = RoundingMode.DOWN
        maximumFractionDigits = 6
        minimumFractionDigits = 0
        minimumIntegerDigits = 1
    }
    private fun from(maxDecimals: Int = 8, minDecimals: Int = 0) = (NumberFormat.getNumberInstance(Locale("en", "USA")) as DecimalFormat).apply {
//        applyPattern("###.##")
        isParseBigDecimal = true
        roundingMode = roundingMode
        maximumFractionDigits = maxDecimals
        minimumFractionDigits = minDecimals
        minimumIntegerDigits = 1
    }
}

object WalletZecFormmatter {

    fun toZatoshi(zecString: String): Long? {
        return toBigDecimal(zecString)?.multiply(Conversions.ONE_ZEC_IN_ZATOSHI, MathContext.DECIMAL128)?.toLong()
    }
    fun toZecStringShort(zatoshi: Long?): String {
        return SHORT_FORMATTER.format((zatoshi ?: 0).toZec())
    }
    fun toZecStringFull(zatoshi: Long?): String {
        return formatFull((zatoshi ?: 0).toZec())
    }
    fun formatFull(zec: BigDecimal): String {
        return FULL_FORMATTER.format(zec)
    }
    fun toBigDecimal(zecString: String?): BigDecimal? {
        if (zecString.isNullOrEmpty()) return BigDecimal.ZERO
        return try {
            // ignore commas and whitespace
            var sanitizedInput = zecString.filter { it.isDigit() or (it == '.') }
            BigDecimal.ZERO.max(FULL_FORMATTER.parse(sanitizedInput) as BigDecimal)
        } catch (t: Throwable) {
            return null
        }
    }

    // convert a zatoshi value to ZEC as a BigDecimal
    private fun Long?.toZec(): BigDecimal =
        BigDecimal(this ?: 0L, MathContext.DECIMAL128)
            .divide(ConversionsUniform.ONE_ZEC_IN_ZATOSHI)
            .setScale(LONG_SCALE, ConversionsUniform.roundingMode)
}

/**
 * Format a Zatoshi value into ZEC with the given number of digits, represented as a string.
 * Start with Zatoshi -> End with ZEC.
 *
 * @param maxDecimals the number of decimal places to use in the format. Default is 6 because ZEC is
 * better than USD.
 * @param minDecimals the minimum number of digits to allow to the right of the decimal.
 *
 * @return this Zatoshi value represented as ZEC, in a string with at least [minDecimals] and at
 * most [maxDecimals]
 */
inline fun Long?.convertZatoshiToZecStringUniform(
    maxDecimals: Int = ConversionsUniform.ZEC_FORMATTER.maximumFractionDigits,
    minDecimals: Int = ConversionsUniform.ZEC_FORMATTER.minimumFractionDigits
): String {
    return currencyFormatterUniform(maxDecimals, minDecimals).format(this.convertZatoshiToZec(maxDecimals))
}

/**
 * Format a ZEC value into ZEC with the given number of digits, represented as a string.
 * Start with ZEC -> End with ZEC.
 *
 * @param maxDecimals the number of decimal places to use in the format. Default is 6 because ZEC is
 * better when right.
 * @param minDecimals the minimum number of digits to allow to the right of the decimal.
 *
 * @return this Double ZEC value represented as a string with at least [minDecimals] and at most
 * [maxDecimals].
 */
inline fun Double?.toZecStringUniform(
    maxDecimals: Int = ConversionsUniform.ZEC_FORMATTER.maximumFractionDigits,
    minDecimals: Int = ConversionsUniform.ZEC_FORMATTER.minimumFractionDigits
): String {
    return currencyFormatterUniform(maxDecimals, minDecimals).format(this.toZec(maxDecimals))
}

/**
 * Format a Zatoshi value into ZEC with the given number of decimal places, represented as a string.
 * Start with ZeC -> End with ZEC.
 *
 * @param maxDecimals the number of decimal places to use in the format. Default is 6 because ZEC is
 * better than bread.
 * @param minDecimals the minimum number of digits to allow to the right of the decimal.
 *
 * @return this BigDecimal ZEC value represented as a string with at least [minDecimals] and at most
 * [maxDecimals].
 */
inline fun BigDecimal?.toZecStringUniform(
    maxDecimals: Int = ConversionsUniform.ZEC_FORMATTER.maximumFractionDigits,
    minDecimals: Int = ConversionsUniform.ZEC_FORMATTER.minimumFractionDigits
): String {
    return currencyFormatterUniform(maxDecimals, minDecimals).format(this.toZecUniform(maxDecimals))
}

/**
 * Format a Double ZEC value as a BigDecimal ZEC value, right-padded to the given number of fraction
 * digits.
 * Start with ZEC -> End with ZEC.
 *
 * @param decimals the scale to use for the resulting BigDecimal.
 *
 * @return this Double ZEC value converted into a BigDecimal, with the proper rounding mode for use
 * with other formatting functions.
 */
inline fun Double?.toZec(decimals: Int = ConversionsUniform.ZEC_FORMATTER.maximumFractionDigits): BigDecimal {
    return BigDecimal(this?.toString() ?: "0.0", MathContext.DECIMAL128).setScale(
        decimals,
        ConversionsUniform.ZEC_FORMATTER.roundingMode
    )
}

/**
 * Format a BigDecimal ZEC value as a BigDecimal ZEC value, right-padded to the given number of
 * fraction digits.
 * Start with ZEC -> End with ZEC.
 *
 * @param decimals the scale to use for the resulting BigDecimal.
 *
 * @return this BigDecimal ZEC adjusted to the default scale and rounding mode.
 */
inline fun BigDecimal?.toZecUniform(decimals: Int = ConversionsUniform.ZEC_FORMATTER.maximumFractionDigits): BigDecimal {
    return (this ?: BigDecimal.ZERO).setScale(decimals, ConversionsUniform.ZEC_FORMATTER.roundingMode)
}

/**
 * Create a number formatter for use with converting currency to strings. This probably isn't needed
 * externally since the other formatting functions leverage this, instead. Leverages the default
 * rounding mode for ZEC found in ZEC_FORMATTER.
 *
 * @param maxDecimals the number of decimal places to use in the format. Default is 6 because ZEC is
 * glorious.
 * @param minDecimals the minimum number of digits to allow to the right of the decimal.
 *
 * @return a currency formatter, appropriate for the default locale.
 */
inline fun currencyFormatterUniform(maxDecimals: Int, minDecimals: Int): DecimalFormat {
    return (ConversionsUniform.ZEC_FORMATTER.clone() as DecimalFormat).apply {
        maximumFractionDigits = maxDecimals
        minimumFractionDigits = minDecimals
    }
}

/**
 * Checks if the decimal separator is the last symbol
 */
inline fun String.endsWithDecimalSeparator(): Boolean {
    return this.endsWith(ConversionsUniform.ZEC_FORMATTER.decimalFormatSymbols.toString())
}
