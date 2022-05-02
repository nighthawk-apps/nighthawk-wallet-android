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
        USD("USD", "USD - United States Dollar", "bitfinex-zec-usd-spot"),
        EUR("EUR", "EUR - Eur", "cex.io-zec-eur-spot"); // TODO change working server url

        companion object {
            fun getFiatCurrencyByName(currencyName: String): FiatCurrency {
                return when (currencyName.lowercase()) {
                    USD.currencyName.lowercase() -> USD
                    EUR.currencyName.lowercase() -> EUR
                    else -> USD
                }
            }
            fun getFiatCurrencyByMarket(marketName: String): FiatCurrency {
                return when (marketName.lowercase()) {
                    USD.serverUrl.lowercase() -> USD
                    EUR.serverUrl.lowercase() -> EUR
                    else -> USD
                }
            }
        }
    }
}
