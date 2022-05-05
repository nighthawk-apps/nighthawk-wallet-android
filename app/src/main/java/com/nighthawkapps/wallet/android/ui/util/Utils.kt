package com.nighthawkapps.wallet.android.ui.util

import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import cash.z.ecc.android.sdk.ext.toZecString
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.toAppColor
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.android.network.models.ZcashPriceApiResponse
import com.nighthawkapps.wallet.android.ui.setup.FiatCurrencyViewModel
import java.text.DecimalFormat

object Utils {

        fun createCustomTabIntent(toolBarColor: Int = R.color.colorPrimary.toAppColor(), secondaryToolBarColor: Int = R.color.colorPrimaryDark.toAppColor()): CustomTabsIntent {
            val customIntent = CustomTabsIntent.Builder()

            val defaultColors = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(toolBarColor)
                .setSecondaryToolbarColor(secondaryToolBarColor)
                .build()

            customIntent.setDefaultColorSchemeParams(defaultColors)

            return customIntent.build()
        }

        fun openCustomTab(activity: Activity, customTabsIntent: CustomTabsIntent, uri: Uri) {
            try {
                // package name is the default package for our custom chrome tab
                val packageName = Const.Default.Server.CHROME_PACKAGE
                customTabsIntent.intent.setPackage(packageName)
                customTabsIntent.launchUrl(activity, uri)
            } catch (e: Exception) {
                // if the custom tabs fails to load then we are simply redirecting our user to users device default browser.
                activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }

    fun getZecConvertedAmountText(toZecStringShort: String, zcashPriceApiData: ZcashPriceApiResponse?): String? {
        if (zcashPriceApiData == null || zcashPriceApiData.data.isEmpty()) return null
        return getZecConvertedAmountText(toZecStringShort, zcashPriceApiData.data.values.firstOrNull().toString(), marketName = zcashPriceApiData.data.keys.firstOrNull())
    }

    fun getZecConvertedAmountText(toZecStringShort: String, price: String, currencyName: String? = null, marketName: String? = null): String {
        return DecimalFormat("#.##").format((toZecStringShort.toFloatOrNull() ?: 0F).times(price.toFloatOrNull() ?: 0F)) + " ${currencyName ?: getCurrencySymbol(marketName)}"
    }

    fun calculateZecToOtherCurrencyValue(zec: String, currencyValue: String): String {
        return DecimalFormat("#.##").format((zec.toFloatOrNull() ?: 0F).times(currencyValue.toFloatOrNull() ?: 0F))
    }

    fun calculateOtherCurrencyToZec(totalAmountInLocalCurrency: String, currencyValue: String): String {
        return ((totalAmountInLocalCurrency.toFloatOrNull() ?: 0F).div(currencyValue.toFloatOrNull() ?: 0F)).toDouble().toZecString()
    }

    fun calculateLocalCurrencyToZatoshi(currencyRate: String, totalLocalAmount: String): Long? {
        return WalletZecFormmatter.toZatoshi(((totalLocalAmount.toFloatOrNull() ?: 0F).div(currencyRate.toFloatOrNull() ?: 0F)).toDouble().toZecString())
    }

    /**
     * Get currency symbol as per market selected.
     */
    private fun getCurrencySymbol(marketName: String?): String {
        return marketName?.let { FiatCurrencyViewModel.FiatCurrency.getFiatCurrencyByMarket(it).currencyName }
        ?: FiatCurrencyViewModel.FiatCurrency.getFiatCurrencyByName(LockBox(NighthawkWalletApp.instance)[Const.AppConstants.KEY_LOCAL_CURRENCY] ?: "").currencyName
    }
}
