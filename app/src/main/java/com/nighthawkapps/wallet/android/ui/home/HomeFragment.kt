package com.nighthawkapps.wallet.android.ui.home

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.Synchronizer.Status.DOWNLOADING
import cash.z.ecc.android.sdk.Synchronizer.Status.SCANNING
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentHomeBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.gone
import com.nighthawkapps.wallet.android.ext.invisible
import com.nighthawkapps.wallet.android.ext.toAppString
import com.nighthawkapps.wallet.android.ext.onClickNavTo
import com.nighthawkapps.wallet.android.ext.requireApplicationContext
import com.nighthawkapps.wallet.android.ext.visible
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.preference.Preferences
import com.nighthawkapps.wallet.android.preference.model.get
import com.nighthawkapps.wallet.android.ui.MainViewModel
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.history.HistoryViewModel
import com.nighthawkapps.wallet.android.ui.send.AutoShieldFragment
import com.nighthawkapps.wallet.android.ui.util.DeepLinkUtil
import com.nighthawkapps.wallet.android.ui.util.Utils
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.isActive

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private lateinit var uiModel: HomeViewModel.UiModel

    private val viewModel: HomeViewModel by viewModel()
    private val mainViewModel: MainViewModel by activityViewModel()
    private val historyViewModel: HistoryViewModel by activityViewModel()

    lateinit var balanceViewPagerAdapter: BalanceViewPagerAdapter

    override fun inflate(inflater: LayoutInflater): FragmentHomeBinding =
        FragmentHomeBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        twig("HomeFragment.onViewCreated  uiModel: ${::uiModel.isInitialized}  saved: ${savedInstanceState != null}")
        binding.walletRecentActivityView.tvViewAllTransactions.onClickNavTo(R.id.action_nav_home_to_nav_history)
        binding.buttonShieldNow.setOnClickListener { if (isAutoShieldFundsAvailable()) { autoShield(uiModel) } }
        initViewPager()
        if (::uiModel.isInitialized) {
            twig("uiModel exists!")
            onModelUpdated(null, uiModel)
        }

        lifecycleScope.launchWhenResumed {
            mainViewModel.intentData.collect { uri ->
                uri?.let {
                    val data = DeepLinkUtil.getSendDeepLinkData(uri = it)
                    if (data == null) {
                        mainActivity?.showMessage("Error: No sufficient data to proceed this transaction")
                    } else {
                        mainViewModel.setSendZecDeepLinkData(data)
                    }
                }
            }
        }

        viewModel.getZecMarketPrice("bitfinex-zec-usd-spot")
    }

    override fun onResume() {
        super.onResume()
        twig("HomeFragment.onResume  resumeScope.isActive: ${resumedScope.isActive}  $resumedScope")
        // Once the synchronizer is created, monitor state changes, while this fragment is resumed
        launchWhenSyncReady {
            onSyncReady()
            combine(viewModel.transactions, viewModel.coinMetricsMarketData) { it1, it2 ->
                twig("recentUI $it1 and $it2")
                it1
            }.onEach { onTransactionsUpdated(it) }.launchIn(resumedScope)
        }
    }

    private suspend fun onTransactionsUpdated(confirmedTransaction: List<ConfirmedTransaction>) {
        updateRecentActivityUI(viewModel.getRecentUIModel(confirmedTransaction))
    }

    private fun updateRecentActivityUI(recentActivityUiModelList: List<HomeViewModel.RecentActivityUiModel>) {
        with(binding) {
            if (recentActivityUiModelList.isEmpty()) {
                walletRecentActivityView.root.invisible()
                return@with
            }
            recentActivityUiModelList.forEachIndexed { index, recentUiModel ->
                val binding = if (index == 0) walletRecentActivityView.receivedView else walletRecentActivityView.sentView
                binding.ivLeftTransactionDirection.rotation = if (recentUiModel.transactionType == HomeViewModel.RecentActivityUiModel.TransactionType.RECEIVED) 0F else 180F
                binding.tvTransactionDirection.text = if (recentUiModel.transactionType == HomeViewModel.RecentActivityUiModel.TransactionType.RECEIVED)
                    R.string.ns_received.toAppString() else R.string.ns_sent.toAppString()
                if (recentUiModel.transactionType == HomeViewModel.RecentActivityUiModel.TransactionType.RECEIVED) {
                    binding.ivTransactionType.setImageResource(if (recentUiModel.isTransactionShielded) R.drawable.ic_icon_shielded else R.drawable.ic_icon_transparent)
                } else {
                    if (recentUiModel.isMemoAvailable) {
                        binding.ivTransactionType.setImageResource(R.drawable.ic_icon_memo)
                    }
                }
                binding.tvTransactionDate.text = recentUiModel.transactionTime
                binding.tvTransactionAmount.text = getString(R.string.ns_zec_amount, recentUiModel.amount)
                recentUiModel.zecConvertedValueText?.let {
                    binding.tvTransactionConversionPrice.text = it
                    binding.tvTransactionConversionPrice.isVisible = true
                }
                binding.root.setOnClickListener { onRecentItemClicked(recentUiModel.confirmedTransaction) }
            }
            walletRecentActivityView.root.visible()
            if (recentActivityUiModelList.size < 2) {
                walletRecentActivityView.sentView.root.gone()
            }
            updateRecentActivityAmountViews(isValidTabToShowBalance())
        }
    }

    private fun onRecentItemClicked(confirmedTransaction: ConfirmedTransaction) {
        historyViewModel.selectedTransaction.value = confirmedTransaction
        mainActivity?.safeNavigate(R.id.action_nav_home_to_nav_transaction_detail)
    }

    private fun updateRecentActivityAmountViews(show: Boolean) {
        binding.walletRecentActivityView.receivedView.groupAmount.isVisible = show
        binding.walletRecentActivityView.sentView.groupAmount.isVisible = show
        binding.walletRecentActivityView.receivedView.groupAmountPrivate.isVisible = show.not()
        binding.walletRecentActivityView.sentView.groupAmountPrivate.isVisible = show.not()
        if (show.not()) {
            binding.walletRecentActivityView.receivedView.tvTransactionConversionPricePrivate.text = "--- USD" // TODO: change after currency slection
            binding.walletRecentActivityView.sentView.tvTransactionConversionPricePrivate.text = "--- USD"
        }
    }

    private fun onSyncReady() {
        twig("Sync ready! Monitoring synchronizer state...")
        monitorUiModelChanges()
    }

    private fun monitorUiModelChanges() {
        viewModel.initializeMaybe()
        viewModel.uiModels.runningReduce { old, new ->
            onModelUpdated(old, new)
            new
        }.onCompletion {
            twig("uiModel.scanReduce completed.")
        }.catch { e ->
            twig("exception while processing uiModels $e")
            throw e
        }.launchIn(resumedScope)
    }

    private fun setProgress(uiModel: HomeViewModel.UiModel) {
        updateProgressBar(uiModel.status)
    }

    private fun initViewPager() {
        balanceViewPagerAdapter = BalanceViewPagerAdapter(this)
        binding.viewPager.adapter = balanceViewPagerAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ ->
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.apply {
                    binding.tabLayout.visibility = if (position != BalanceFragment.Companion.SectionType.SWIPE_LEFT_DIRECTION.sectionNo) View.VISIBLE else View.GONE
                    binding.buttonShieldNow.visibility = if (position == BalanceFragment.Companion.SectionType.TRANSPARENT_BALANCE.sectionNo && isAutoShieldFundsAvailable()) View.VISIBLE else View.GONE
                    updateRecentActivityAmountViews(isValidTabToShowBalance())
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    private fun isValidTabToShowBalance(): Boolean {
        binding.tabLayout.let {
            if (it.selectedTabPosition <= BalanceFragment.Companion.SectionType.SWIPE_LEFT_DIRECTION.sectionNo) {
                return false
            }
            return true
        }
    }

    private fun updateProgressBar(status: Synchronizer.Status) {
        when (status) {
            Synchronizer.Status.SYNCED -> {
                startAndStopProgressBar(startProgressbar = false, filledProgress = true)
            }
            else -> startAndStopProgressBar(startProgressbar = true, filledProgress = false)
        }
    }

    private fun startAndStopProgressBar(startProgressbar: Boolean, filledProgress: Boolean) {
        binding.progressBarTop.apply {
            when {
                startProgressbar -> {
                    isIndeterminate = true
                    visibility = View.VISIBLE
                }
                filledProgress -> {
                    progress = 100
                    isIndeterminate = false
                    visibility = View.GONE
                }
                else -> {
                    isIndeterminate = false
                    visibility = View.GONE
                }
            }
        }
    }

    private fun isAutoShieldFundsAvailable(): Boolean {
        if (this@HomeFragment::uiModel.isInitialized) {
            return uiModel.hasAutoshieldFunds
        }
        return false
    }

    //
    // Private UI Events
    //

    private fun roundFloat(number: Float): Float {
        var pow = 10
        for (i in 1 until 2) pow *= 10
        val tmp = number * pow
        return ((if (tmp - tmp.toInt() >= 0.5f) tmp + 1 else tmp).toInt()).toFloat() / pow
    }

    private fun onModelUpdated(old: HomeViewModel.UiModel?, new: HomeViewModel.UiModel) {
        logUpdate(old, new)
        uiModel = new
        setProgress(uiModel)
        if (new.status == Synchronizer.Status.SYNCED) onSynced(new) else onSyncing(new)
    }

    private fun logUpdate(old: HomeViewModel.UiModel?, new: HomeViewModel.UiModel) {
        var message = ""
        fun maybeComma() = if (message.length > "UiModel(".length) ", " else ""
        message = when {
            old == null -> "$new"
            new == null -> "null"
            else -> {
                buildString {
                    append("UiModel(")
                    if (old.status != new.status) append("status=${new.status}")
                    if (old.processorInfo != new.processorInfo) {
                        append("${maybeComma()}processorInfo=ProcessorInfo(")
                        val startLength = length
                        fun innerComma() = if (length > startLength) ", " else ""
                        if (old.processorInfo.networkBlockHeight != new.processorInfo.networkBlockHeight) append(
                            "networkBlockHeight=${new.processorInfo.networkBlockHeight}"
                        )
                        if (old.processorInfo.lastScannedHeight != new.processorInfo.lastScannedHeight) append(
                            "${innerComma()}lastScannedHeight=${new.processorInfo.lastScannedHeight}"
                        )
                        if (old.processorInfo.lastDownloadedHeight != new.processorInfo.lastDownloadedHeight) append(
                            "${innerComma()}lastDownloadedHeight=${new.processorInfo.lastDownloadedHeight}"
                        )
                        if (old.processorInfo.lastDownloadRange != new.processorInfo.lastDownloadRange) append(
                            "${innerComma()}lastDownloadRange=${new.processorInfo.lastDownloadRange}"
                        )
                        if (old.processorInfo.lastScanRange != new.processorInfo.lastScanRange) append(
                            "${innerComma()}lastScanRange=${new.processorInfo.lastScanRange}"
                        )
                        append(")")
                    }
                    if (old.saplingBalance.availableZatoshi != new.saplingBalance.availableZatoshi) append(
                        "${maybeComma()}availableBalance=${new.saplingBalance.availableZatoshi}"
                    )
                    if (old.saplingBalance.totalZatoshi != new.saplingBalance.totalZatoshi) append("${maybeComma()}totalBalance=${new.saplingBalance.totalZatoshi}")
                    if (old.pendingSend != new.pendingSend) append("${maybeComma()}pendingSend=${new.pendingSend}")
                    append(")")
                }
            }
        }
        twig("onModelUpdated: $message")
    }

    private fun onSyncing(uiModel: HomeViewModel.UiModel) {
        mainActivity?.updateTransferTab(false)
        var iconResourceId = R.drawable.ic_icon_connecting
        var message = getString(R.string.ns_connecting)
        when (uiModel.status) {
            DOWNLOADING -> {
                if (!viewModel.isValidBlock(uiModel.processorInfo.lastDownloadedHeight, uiModel.processorInfo.lastDownloadRange.last)) return
                iconResourceId = R.drawable.ic_icon_downloading
                message = if (uiModel.downloadProgress == 0) {
                    getString(R.string.ns_preparing_download)
                } else {
                    getString(R.string.home_button_send_downloading, uiModel.downloadProgress)
                }
            }
            SCANNING -> {
                if (!viewModel.isValidBlock(uiModel.processorInfo.lastScannedHeight, uiModel.processorInfo.lastScanRange.last)) return
                message = when (uiModel.scanProgress) {
                    0 -> {
                        iconResourceId = R.drawable.ic_icon_preparing
                        getString(R.string.ns_preparing_scan)
                    }
                    100 -> {
                        iconResourceId = R.drawable.ic_icon_finalizing
                        getString(R.string.ns_finalizing)
                    }
                    else -> {
                        iconResourceId = R.drawable.ic_icon_syncing
                        getString(
                            R.string.ns_scanning,
                            uiModel.scanProgress
                        )
                    }
                }
            }
            Synchronizer.Status.DISCONNECTED -> {
                iconResourceId = R.drawable.ic_icon_reconnecting
                message = getString(R.string.ns_reconnecting)
            }
            Synchronizer.Status.VALIDATING -> {
                iconResourceId = R.drawable.ic_icon_validating
                message = getString(R.string.ns_validating)
            }
            Synchronizer.Status.ENHANCING -> {
                iconResourceId = R.drawable.ic_icon_enhancing
                message = getString(R.string.ns_enhancing)
            }
            else -> {}
        }
        binding.viewInit.apply {
            icon.setImageResource(iconResourceId)
            tvMessage.text = message
        }
    }

    private fun onSynced(uiModel: HomeViewModel.UiModel) {
        mainActivity?.updateTransferTab(enable = true)
        binding.hitAreaScan.onClickNavTo(R.id.action_nav_home_to_nav_receive)
        binding.iconScan.visibility = View.VISIBLE
        binding.viewInit.root.gone()
        binding.viewPager.visible()
        autoShield(uiModel)
        checkForDeepLink()
    }

    private fun autoShield(uiModel: HomeViewModel.UiModel) {
        // TODO: Move the preference read to a suspending function
        // First time SharedPreferences are hit, it'll perform disk IO
        val isAutoshieldingAcknowledged = Preferences.isAcknowledgedAutoshieldingInformationPrompt.get(requireContext().applicationContext)
        val canAutoshield = AutoShieldFragment.canAutoshield(requireApplicationContext())

        if (uiModel.hasAutoshieldFunds && canAutoshield) {
            twig("Autoshielding is available! Let's do this!!!")
            if (!isAutoshieldingAcknowledged) {
                mainActivity?.safeNavigate(
                    HomeFragmentDirections.actionNavHomeToAutoshieldingInfo(
                        true
                    )
                )
            } else {
                twig("Autoshielding is available! Let's do this!!!")
                mainActivity?.safeNavigate(HomeFragmentDirections.actionNavHomeToNavFundsAvailable())
            }
        } else {
            if (!isAutoshieldingAcknowledged) {
                mainActivity?.safeNavigate(
                    HomeFragmentDirections.actionNavHomeToAutoshieldingInfo(
                        false
                    )
                )
            }

            // troubleshooting logs
            if (uiModel.transparentBalance.availableZatoshi > 0) {
                twig(
                    "Transparent funds are available but not enough to autoshield. Available: ${
                        uiModel.transparentBalance.availableZatoshi.convertZatoshiToZecString(
                            10
                        )
                    }  Required: ${
                        NighthawkWalletApp.instance.autoshieldThreshold.convertZatoshiToZecString(
                            8
                        )
                    }"
                )
            } else if (uiModel.transparentBalance.totalZatoshi > 0) {
                twig("Transparent funds have been received but they require 10 confirmations for autoshielding.")
            } else if (!canAutoshield) {
                twig("Could not Autoshield probably because the last one occurred too recently")
            }
        }
    }

    private fun checkForDeepLink() {
        lifecycleScope.launchWhenResumed {
            if (mainViewModel.intentData.value == null || mainViewModel.sendZecDeepLinkData.value == null) return@launchWhenResumed
            mainActivity?.safeNavigate(HomeFragmentDirections.actionNavHomeToTransfer(forBuyZecDeeplink = mainViewModel.intentData.value?.toString() ?: ""))
            mainViewModel.setIntentData(null)
        }
    }

    private fun showBuyZecAlertDialog() {
        mainActivity?.copyTransparentAddress()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.buy_zec_dialog_title))
            .setMessage(getString(R.string.buy_zec_dialog_msg))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.open_browser)) { dialog, _ ->
                Utils.openCustomTab(requireActivity(), Utils.createCustomTabIntent(), Uri.parse(viewModel.getMoonPayUrl()))
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showOpenZcashSiteDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.visit_zcash_link_title))
            .setMessage(getString(R.string.visit_zcash_link_description))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.open_browser)) { dialog, _ ->
                mainActivity?.onLaunchUrl(getString(R.string.zcash_learn_more_link))
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    enum class BannerAction(val action: String) {
        FUND_NOW(""),
        CANCEL("Cancel"),
        NONE(""),
        CLEAR("clear");

        companion object {
            fun from(action: String?): BannerAction {
                values().forEach {
                    if (it.action == action) return it
                }
                throw IllegalArgumentException("Invalid BannerAction: $action")
            }
        }
    }
}
