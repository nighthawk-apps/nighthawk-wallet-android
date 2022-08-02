package com.nighthawkapps.wallet.android.ui.home

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.model.Zatoshi
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.toAppString

class BalanceViewModel : ViewModel() {

    var isZecAmountState = true // To track user selected the zec balance mode or converted balance mode
    var balanceAmountZec: String = ""

    fun getBalanceUIModel(
        sectionType: BalanceFragment.Companion.SectionType,
        homeUiModel: HomeViewModel.UiModel
    ): BalanceUIModel {
        return when (sectionType) {
            BalanceFragment.Companion.SectionType.SWIPE_LEFT_DIRECTION -> {
                balanceAmountZec = ""
                BalanceUIModel(
                    icon = ContextCompat.getDrawable(NighthawkWalletApp.instance.applicationContext, R.drawable.ic_icon_left_swipe),
                    balanceAmount = balanceAmountZec,
                    messageText = R.string.ns_swipe_left.toAppString(),
                    showIndicator = false
                )
            }
            BalanceFragment.Companion.SectionType.TOTAL_BALANCE -> {
                if (homeUiModel.saplingBalance != null && homeUiModel.transparentBalance != null) {
                        balanceAmountZec = (homeUiModel.saplingBalance.total + homeUiModel.transparentBalance.total).convertZatoshiToZecString()
                        BalanceUIModel(
                            icon = ContextCompat.getDrawable(NighthawkWalletApp.instance.applicationContext, R.drawable.ic_icon_total),
                            balanceAmount = balanceAmountZec,
                            expectingBalance = getExpectingBalance(homeUiModel.saplingBalance.total.value, homeUiModel.saplingBalance.available.value, homeUiModel.unminedCount),
                            messageText = R.string.ns_total_balance.toAppString(),
                            showIndicator = true
                        )
                    } else {
                    BalanceUIModel(
                        icon = ContextCompat.getDrawable(NighthawkWalletApp.instance.applicationContext, R.drawable.ic_icon_total),
                        balanceAmount = balanceAmountZec,
                        expectingBalance = "",
                        messageText = R.string.ns_total_balance.toAppString(),
                        showIndicator = true
                    )
                }
            }
            BalanceFragment.Companion.SectionType.SHIELDED_BALANCE -> {
                balanceAmountZec = homeUiModel.saplingBalance?.total.convertZatoshiToZecString()
                BalanceUIModel(
                    icon = ContextCompat.getDrawable(NighthawkWalletApp.instance.applicationContext, R.drawable.ic_icon_shielded),
                    balanceAmount = balanceAmountZec,
                    messageText = R.string.ns_shielded_balance.toAppString(),
                    showIndicator = true
                )
            }
            BalanceFragment.Companion.SectionType.TRANSPARENT_BALANCE -> {
                balanceAmountZec = homeUiModel.transparentBalance?.total.convertZatoshiToZecString()
                BalanceUIModel(
                    icon = ContextCompat.getDrawable(NighthawkWalletApp.instance.applicationContext, R.drawable.ic_icon_transparent),
                    balanceAmount = balanceAmountZec,
                    messageText = R.string.ns_transparent_balance.toAppString(),
                    showIndicator = true
                )
            }
        }
    }

    private fun getExpectingBalance(total: Long, available: Long, unMinedCount: Int): String {
        if (available != -1L && available < total && (total - available > 0) && unMinedCount < 1) {
            val change = WalletZecFormmatter.toZecStringFull(Zatoshi(total - available))
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