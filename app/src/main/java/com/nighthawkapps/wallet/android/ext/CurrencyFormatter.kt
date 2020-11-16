package com.nighthawkapps.wallet.android.ext

import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZec
import cash.z.ecc.android.sdk.ext.toZec
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object ConversionsUniform {
    var ONE_ZEC_IN_ZATOSHI = BigDecimal(ZcashSdk.ZATOSHI_PER_ZEC, MathContext.DECIMAL128)
    var ZEC_FORMATTER = (NumberFormat.getNumberInstance(Locale("en", "UK")) as DecimalFormat).apply {
        applyPattern("###.##")
        roundingMode = RoundingMode.DOWN
        maximumFractionDigits = 6
        minimumFractionDigits = 0
        minimumIntegerDigits = 1
    }
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
