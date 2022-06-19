package com.nighthawkapps.wallet.android.ui.home

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.convertZatoshiToSelectedUnit
import com.nighthawkapps.wallet.android.ext.toAppString
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.android.ui.setup.FiatCurrencyViewModel
import javax.inject.Inject

class BalanceViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var lockBox: LockBox
    var isZecAmountState = true // To track user selected the zec balance mode or converted balance mode
    var balanceAmountZatoshi: Long = -1L

    fun getSelectedFiatUnit(): FiatCurrencyViewModel.FiatUnit {
        return FiatCurrencyViewModel.FiatUnit.getFiatUnit(lockBox[Const.AppConstants.KEY_LOCAL_UNIT] ?: "")
    }

    fun getConvertedBalanceAmount(): String {
        if (balanceAmountZatoshi == -1L) return ""
        return balanceAmountZatoshi.convertZatoshiToSelectedUnit(getSelectedFiatUnit())
    }

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
                balanceAmountZatoshi = (homeUiModel.saplingBalance.totalZatoshi + homeUiModel.transparentBalance.totalZatoshi)
                val balanceAmount = balanceAmountZatoshi.convertZatoshiToSelectedUnit(getSelectedFiatUnit())
                BalanceUIModel(
                    icon = ContextCompat.getDrawable(NighthawkWalletApp.instance.applicationContext, R.drawable.ic_icon_total),
                    balanceAmount = balanceAmount,
                    expectingBalance = getExpectingBalance(homeUiModel.saplingBalance.totalZatoshi, homeUiModel.saplingBalance.availableZatoshi, homeUiModel.unminedCount),
                    messageText = R.string.ns_total_balance.toAppString(),
                    showIndicator = true
                )
            }
            BalanceFragment.Companion.SectionType.SHIELDED_BALANCE -> {
                balanceAmountZatoshi = (homeUiModel.saplingBalance.totalZatoshi + homeUiModel.transparentBalance.totalZatoshi)
                val balanceAmount = balanceAmountZatoshi.convertZatoshiToSelectedUnit(getSelectedFiatUnit())
                BalanceUIModel(
                    icon = ContextCompat.getDrawable(NighthawkWalletApp.instance.applicationContext, R.drawable.ic_icon_shielded),
                    balanceAmount = balanceAmount,
                    messageText = R.string.ns_shielded_balance.toAppString(),
                    showIndicator = true
                )
            }
            BalanceFragment.Companion.SectionType.TRANSPARENT_BALANCE -> {
                balanceAmountZatoshi = (homeUiModel.saplingBalance.totalZatoshi + homeUiModel.transparentBalance.totalZatoshi)
                val balanceAmount = balanceAmountZatoshi.convertZatoshiToSelectedUnit(getSelectedFiatUnit())
                BalanceUIModel(
                    icon = ContextCompat.getDrawable(NighthawkWalletApp.instance.applicationContext, R.drawable.ic_icon_transparent),
                    balanceAmount = balanceAmount,
                    messageText = R.string.ns_transparent_balance.toAppString(),
                    showIndicator = true
                )
            }
        }
    }

    private fun getExpectingBalance(total: Long, available: Long, unMinedCount: Int): String {
        if (available != -1L && available < total && (total - available > 0) && unMinedCount < 1) {
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