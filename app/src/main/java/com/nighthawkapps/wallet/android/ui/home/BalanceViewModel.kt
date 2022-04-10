package com.nighthawkapps.wallet.android.ui.home

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.toAppString

class BalanceViewModel : ViewModel() {

    fun getBalanceUIModel(
        sectionType: BalanceFragment.Companion.SectionType,
        homeUiModel: HomeViewModel.UiModel
    ): BalanceUIModel {
        return when (sectionType) {
            BalanceFragment.Companion.SectionType.SWIPE_LEFT_DIRECTION -> {
                BalanceUIModel(
                    icon = ContextCompat.getDrawable(NighthawkWalletApp.instance.applicationContext, R.drawable.ic_icon_left_swipe),
                    balanceAmount = "",
                    messageText = R.string.ns_swipe_left.toAppString(),
                    showIndicator = false
                )
            }
            BalanceFragment.Companion.SectionType.TOTAL_BALANCE -> {
                BalanceUIModel(
                    icon = ContextCompat.getDrawable(NighthawkWalletApp.instance.applicationContext, R.drawable.ic_icon_total),
                    balanceAmount = (homeUiModel.saplingBalance.availableZatoshi + homeUiModel.transparentBalance.availableZatoshi).convertZatoshiToZecString(),
                    expectingBalance = getExpectingBalance(homeUiModel.saplingBalance.totalZatoshi, homeUiModel.saplingBalance.availableZatoshi),
                    messageText = R.string.ns_total_balance.toAppString(),
                    showIndicator = true
                )
            }
            BalanceFragment.Companion.SectionType.SHIELDED_BALANCE -> {
                BalanceUIModel(
                    icon = ContextCompat.getDrawable(NighthawkWalletApp.instance.applicationContext, R.drawable.ic_icon_shielded),
                    balanceAmount = homeUiModel.saplingBalance.availableZatoshi.convertZatoshiToZecString(),
                    messageText = R.string.ns_shielded_balance.toAppString(),
                    showIndicator = true
                )
            }
            BalanceFragment.Companion.SectionType.TRANSPARENT_BALANCE -> {
                BalanceUIModel(
                    icon = ContextCompat.getDrawable(NighthawkWalletApp.instance.applicationContext, R.drawable.ic_icon_transparent),
                    balanceAmount = homeUiModel.transparentBalance.availableZatoshi.convertZatoshiToZecString(),
                    messageText = R.string.ns_transparent_balance.toAppString(),
                    showIndicator = true
                )
            }
        }
    }

    private fun getExpectingBalance(total: Long, available: Long): String {
        if (available != -1L && available < total) {
            val change = WalletZecFormmatter.toZecStringFull(total - available)
            return "${R.string.home_banner_expecting.toAppString()} $change ZEC"
        }
        return ""
    }

    data class BalanceUIModel(
        var icon: Drawable? = ContextCompat.getDrawable(
            NighthawkWalletApp.instance.applicationContext,
            R.drawable.ic_icon_left_swipe
        ),
        var balanceAmount: String = "",
        var expectingBalance: String = "",
        var messageText: String = "",
        var showIndicator: Boolean = true
    )
}