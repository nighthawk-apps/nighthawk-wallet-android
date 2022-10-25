package com.nighthawkapps.wallet.android.ui.setup

import androidx.lifecycle.ViewModel
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.UnitConversion
import com.nighthawkapps.wallet.android.lockbox.LockBox
import javax.inject.Inject

class FiatCurrencyViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var lockBox: LockBox

    fun updateLocalCurrency(fiatCurrency: FiatCurrency) {
        if (fiatCurrency == FiatCurrency.OFF) {
            resetSavedCurrencyData()
        } else {
            lockBox[Const.AppConstants.KEY_LOCAL_CURRENCY] = fiatCurrency.currencyName
        }
    }

    fun localCurrencySelected(): FiatCurrency {
        val currencyName: String? = lockBox[Const.AppConstants.KEY_LOCAL_CURRENCY]
        return FiatCurrency.getFiatCurrencyByName(currencyName ?: "")
    }

    private fun resetSavedCurrencyData() {
        lockBox[Const.AppConstants.KEY_ZEC_AMOUNT] = "0"
        lockBox[Const.AppConstants.KEY_LOCAL_CURRENCY] = ""
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
        CNY("CNY", "Chinese Yuan", "cny"),
        OFF("", "OFF", "");

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
                    else -> OFF
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
                    else -> OFF
                }
            }
        }
    }

    fun updateLocalUnit(fiatUnit: FiatUnit) {
        lockBox[Const.AppConstants.KEY_LOCAL_UNIT] = fiatUnit.unit
    }

    fun localUnitSelected(): FiatUnit {
        return FiatUnit.getFiatUnit(lockBox[Const.AppConstants.KEY_LOCAL_UNIT] ?: "")
    }

    enum class FiatUnit(val unit: String, val currencyName: String, val zatoshiPerUnit: Long) {
        ZEC("ZEC", "ZEC", UnitConversion.ZATOSHI_PER_ZEC),
        ZATOSHI("zat", "Zatoshi", 1),
        DECIZ("deciz", "Deciz", UnitConversion.ZATOSHI_PER_DECIZ),
        CENTZ("centz", "Centz", UnitConversion.ZATOSHI_PER_CENTZ),
        MILLIZ("milliz", "Milliz", UnitConversion.ZATOSHI_PER_MILLIZ),
        MICROZ("microz", "Microz", UnitConversion.ZATOSHI_PER_MICROS),
        ZED("zed", "Zed", UnitConversion.ZATOSHI_PER_ZED);

        companion object {
            fun getFiatUnit(unit: String): FiatUnit {
                return when (unit.lowercase()) {
                    ZEC.unit.lowercase() -> ZEC
                    ZATOSHI.unit.lowercase() -> ZATOSHI
                    DECIZ.unit.lowercase() -> DECIZ
                    CENTZ.unit.lowercase() -> CENTZ
                    MILLIZ.unit.lowercase() -> MILLIZ
                    MICROZ.unit.lowercase() -> MICROZ
                    ZED.unit.lowercase() -> ZED
                    else -> ZEC // By default return ZEC
                }
            }
        }
    }
}
