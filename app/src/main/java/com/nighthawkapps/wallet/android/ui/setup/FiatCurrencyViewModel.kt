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
        USD("USD", "USD - United States Dollar", "usd"),
        EUR("EUR", "EUR - Eur", "eur"),
        INDIAN_RUPEE("INR", "Indian Rupee", "inr"),
        JAPANESE_YAN("JPY", "Japanese Yan", "jpy");

        companion object {
            fun getFiatCurrencyByName(currencyName: String): FiatCurrency {
                return when (currencyName.lowercase()) {
                    USD.currencyName.lowercase() -> USD
                    EUR.currencyName.lowercase() -> EUR
                    INDIAN_RUPEE.currencyName.lowercase() -> INDIAN_RUPEE
                    JAPANESE_YAN.currencyName.lowercase() -> JAPANESE_YAN
                    else -> USD
                }
            }
            fun getFiatCurrencyByMarket(marketName: String): FiatCurrency {
                return when (marketName.lowercase()) {
                    USD.serverUrl.lowercase() -> USD
                    EUR.serverUrl.lowercase() -> EUR
                    INDIAN_RUPEE.serverUrl.lowercase() -> INDIAN_RUPEE
                    JAPANESE_YAN.serverUrl.lowercase() -> JAPANESE_YAN
                    else -> USD
                }
            }
        }
    }
}
