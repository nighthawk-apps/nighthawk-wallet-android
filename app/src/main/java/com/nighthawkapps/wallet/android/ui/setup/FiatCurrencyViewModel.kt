package com.nighthawkapps.wallet.android.ui.setup

import androidx.lifecycle.ViewModel
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.lockbox.LockBox
import javax.inject.Inject

class FiatCurrencyViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var lockBox: LockBox

    fun updateLocalCurrency(fiatCurrency: FiatCurrency) {
        lockBox[Const.AppConstants.KEY_LOCAL_CURRENCY] = fiatCurrency.currencyName
    }

    fun localCurrencySelected(): FiatCurrency {
        val currencyName: String? = lockBox[Const.AppConstants.KEY_LOCAL_CURRENCY]
        return FiatCurrency.getFiatCurrencyByName(currencyName ?: "")
    }

    enum class FiatCurrency(val currencyName: String, val currencyText: String, val serverUrl: String) {
        USD("USD", "United States Dollar", "usd"),
        EUR("EUR", "Euro", "eur"),
        INR("INR", "Indian Rupee", "inr"),
        JPY("JPY", "Japanese Yen", "jpy"),
        GBP("GBP", "British Pound", "gbp"),
        CAD("CAD", "Canadian Dollar", "cad"),
        AUD("AUD", "Australian Dollar", "aud"),
        HKD("HKD", "Hong Kong Dollar", "hkd"),
        SGD("SGD", "Singapore Dollar", "sgd"),
        CHF("CHF", "Swiss Franc", "chf"),
        CNY("CNY", "Chinese Yuan", "cny");

        companion object {
            fun getFiatCurrencyByName(currencyName: String): FiatCurrency {
                return when (currencyName.lowercase()) {
                    USD.currencyName.lowercase() -> USD
                    EUR.currencyName.lowercase() -> EUR
                    INR.currencyName.lowercase() -> INR
                    JPY.currencyName.lowercase() -> JPY
                    GBP.currencyName.lowercase() -> GBP
                    CAD.currencyName.lowercase() -> CAD
                    AUD.currencyName.lowercase() -> AUD
                    HKD.currencyName.lowercase() -> HKD
                    SGD.currencyName.lowercase() -> SGD
                    CHF.currencyName.lowercase() -> CHF
                    CNY.currencyName.lowercase() -> CNY
                    else -> USD
                }
            }
            fun getFiatCurrencyByMarket(marketName: String): FiatCurrency {
                return when (marketName.lowercase()) {
                    USD.serverUrl.lowercase() -> USD
                    EUR.serverUrl.lowercase() -> EUR
                    INR.serverUrl.lowercase() -> INR
                    JPY.serverUrl.lowercase() -> JPY
                    GBP.serverUrl.lowercase() -> GBP
                    CAD.serverUrl.lowercase() -> CAD
                    AUD.serverUrl.lowercase() -> AUD
                    HKD.serverUrl.lowercase() -> HKD
                    SGD.serverUrl.lowercase() -> SGD
                    CHF.serverUrl.lowercase() -> CHF
                    CNY.serverUrl.lowercase() -> CNY
                    else -> USD
                }
            }
        }
    }
}
