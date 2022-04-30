package com.nighthawkapps.wallet.android.ui.util

import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.toAppColor
import com.nighthawkapps.wallet.android.network.models.CoinMetricsMarketResponse
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

    fun getZecConvertedAmountText(toZecStringShort: String, coinMetricsMarketData: CoinMetricsMarketResponse.CoinMetricsMarketData?): String? {
        if (coinMetricsMarketData == null) return null
        return getZecConvertedAmountText(toZecStringShort, coinMetricsMarketData.price)
    }

    fun getZecConvertedAmountText(toZecStringShort: String, price: String): String {
        return DecimalFormat("#.##").format((toZecStringShort.toFloatOrNull() ?: 0F).times(price.toFloatOrNull() ?: 0F)) + " ${getCurrencySymbol()}"
    }

    /**
     * Get currency symbol as per market selected. // TODO: need to change in this after all markets implemented
     */
    private fun getCurrencySymbol(): String {
        return "USD"
    }
}
