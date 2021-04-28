package com.nighthawkapps.wallet.android.ui.home

import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.Synchronizer.Status.DISCONNECTED
import cash.z.ecc.android.sdk.Synchronizer.Status.STOPPED
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.type.WalletBalance
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentHomeBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.gone
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.invisibleIf
import com.nighthawkapps.wallet.android.ext.onClickNavTo
import com.nighthawkapps.wallet.android.ext.showSharedLibraryCriticalError
import com.nighthawkapps.wallet.android.ext.toColoredSpan
import com.nighthawkapps.wallet.android.ext.visible
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.detail.TransactionAdapter
import com.nighthawkapps.wallet.android.ui.detail.TransactionsFooter
import com.nighthawkapps.wallet.android.ui.detail.WalletDetailViewModel
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel.WalletSetupState.NO_SEED
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scanReduce
import kotlinx.coroutines.isActive

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private lateinit var uiModel: HomeViewModel.UiModel
    private lateinit var adapter: TransactionAdapter<ConfirmedTransaction>

    private val walletSetup: WalletSetupViewModel by activityViewModel(false)
    private val viewModel: HomeViewModel by viewModel()
    private val walletViewModel: WalletDetailViewModel by activityViewModel()

    lateinit var snake: MagicSnakeLoader

    override fun inflate(inflater: LayoutInflater): FragmentHomeBinding = FragmentHomeBinding.inflate(inflater)

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // this will call startSync either now or later (after initializing with newly created seed)
        walletSetup.checkSeed().onEach {
            twig("Checking seed")
            if (it == NO_SEED) {
                // interact with user to create, backup and verify seed
                // leads to a call to startSync(), later (after accounts are created from seed)
                twig("Seed not found, therefore, launching seed creation flow")
                mainActivity?.setLoading(false)
                mainActivity?.safeNavigate(R.id.action_nav_home_to_create_wallet)
            } else {
                twig("Found seed. Re-opening existing wallet")
                mainActivity?.setLoading(true)
                try {
                    mainActivity?.startSync(walletSetup.openStoredWallet())
                } catch (e: UnsatisfiedLinkError) {
                    mainActivity?.showSharedLibraryCriticalError(e)
                }
            }
        }.launchIn(lifecycleScope)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        twig("HomeFragment.onViewCreated  uiModel: ${::uiModel.isInitialized}  saved: ${savedInstanceState != null}")
        snake = MagicSnakeLoader(binding.lottieButtonLoading)
        binding.hitAreaProfile.onClickNavTo(R.id.action_nav_home_to_nav_profile)
        binding.buttonSendAmount.setOnClickListener { onSend() }
        binding.textMyAddress.onClickNavTo(R.id.action_nav_scan_to_nav_receive)
        binding.textMyAddress.paintFlags = binding.textMyAddress.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.textSideShift.paintFlags = binding.textMyAddress.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.hitAreaInfo.setOnClickListener {
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
        binding.textSideShift.setOnClickListener {
            mainActivity?.copyAddress()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.fund_wallet_sideshift_title))
                .setMessage(getString(R.string.fund_wallet_sideshift_description))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.open_browser)) { dialog, _ ->
                    mainActivity?.onLaunchUrl(getString(R.string.sideshift_affiliate_link))
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        if (::uiModel.isInitialized) {
            twig("uiModel exists!")
            onModelUpdated(null, uiModel)
        }
    }

    override fun onResume() {
        super.onResume()
        twig("HomeFragment.onResume  resumeScope.isActive: ${resumedScope.isActive}  $resumedScope")

        monitorTransactions()
        monitorBalance()
        monitorUiModelChanges()
    }

    private fun onBalanceUpdated(balance: WalletBalance) {
        binding.textBalanceAvailable.text = balance.availableZatoshi.convertZatoshiToZecString()
        val change = (balance.totalZatoshi - balance.availableZatoshi)
        binding.textBalanceDescription.apply {
            if (change <= 0L) {
                if (walletViewModel.priceModel != null) {
                    try {
                        val usdPrice = walletViewModel.priceModel?.price!!.toFloat()
                        val zecBalance = roundFloat(
                            balance.availableZatoshi.convertZatoshiToZecString().toFloat()
                        )
                        val usdTotal = "%.2f".format(usdPrice * zecBalance)
                        text = "~$$usdTotal"
                        visible()
                    } catch (e: NumberFormatException) {
                        gone()
                    }
                } else {
                    gone()
                    walletViewModel.initPrice()
                }
            } else {
                val changeString = change.convertZatoshiToZecString()
                text = "(expecting +$changeString ZEC)".toColoredSpan(
                    R.color.text_light,
                    "+$changeString"
                )
                visible()
            }
        }
    }

    private fun roundFloat(number: Float): Float {
        var pow = 10
        for (i in 1 until 2) pow *= 10
        val tmp = number * pow
        return ((if (tmp - tmp.toInt() >= 0.5f) tmp + 1 else tmp).toInt()).toFloat() / pow
    }

    private fun monitorTransactions() {
        adapter = TransactionAdapter()
        binding.recyclerTransactions.apply {
            layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(TransactionsFooter(binding.recyclerTransactions.context))
            adapter = this@HomeFragment.adapter
            scrollToTop()
        }
        walletViewModel.transactions.collectWith(resumedScope, ::onTransactionsUpdated)
    }

    private fun monitorBalance() {
        walletViewModel.balance.collectWith(resumedScope, ::onBalanceUpdated)
    }

    private fun monitorUiModelChanges() {
        viewModel.initializeMaybe()
        viewModel.uiModels.scanReduce { old, new ->
            onModelUpdated(old, new)
            new
        }.onCompletion {
            twig("uiModel.scanReduce completed.")
        }.catch { e ->
            twig("exception while processing uiModels $e")
            throw e
        }.launchIn(resumedScope)
    }

    private fun onTransactionsUpdated(transactions: PagedList<ConfirmedTransaction>) {
        twig("got a new paged list of transactions")
        transactions.size.let { newCount ->
            binding.groupEmptyViews.goneIf(newCount > 0)
            val preSize = adapter.itemCount
            adapter.submitList(transactions)
            // don't rescroll while the user is looking at the list, unless it's initialization
            // using 4 here because there might be headers or other things that make 0 a bad pick
            // 4 is about how many can fit before scrolling becomes necessary on many screens
            if (preSize < 4 && newCount > preSize) {
                scrollToTop()
            }
        }
    }

    private fun scrollToTop() {
        twig("scrolling to the top")
        binding.recyclerTransactions.apply {
            postDelayed({
                smoothScrollToPosition(0)
            }, 5L)
        }
    }

    //
    // Public UI API
    //

    var isSendEnabled = false
    fun setSendEnabled(enabled: Boolean, isSynced: Boolean) {
        isSendEnabled = enabled
        binding.buttonSendAmount.apply {
            if (enabled || !isSynced) {
                isEnabled = true
                isClickable = isSynced
                binding.lottieButtonLoading.alpha = 1.0f
            } else {
                isEnabled = false
                isClickable = false
                binding.lottieButtonLoading.alpha = 0.32f
            }
        }
    }

    fun setProgress(uiModel: HomeViewModel.UiModel) {
        if (!uiModel.processorInfo.hasData && !uiModel.isDisconnected) {
            twig("Warning: ignoring progress update because the processor is still starting.")
            return
        }

        snake.isSynced = uiModel.isSynced
        if (!uiModel.isSynced) {
            snake.downloadProgress = uiModel.downloadProgress
            snake.scanProgress = uiModel.scanProgress
        }

        val sendText = when {
            uiModel.status == DISCONNECTED -> "Reconnecting . . ."
            uiModel.isSynced -> if (uiModel.hasFunds) "Send Zcash" else "NO FUNDS AVAILABLE"
            uiModel.status == STOPPED -> "IDLE"
            uiModel.isDownloading -> "Downloading . . . ${snake.downloadProgress}%"
            uiModel.isValidating -> "Validating . . ."
            uiModel.isScanning -> "Scanning . . . ${snake.scanProgress}%"
            else -> "Updating"
        }

        binding.buttonSendAmount.text = sendText
        twig("Send button set to: $sendText")

        val resId = if (uiModel.isSynced) R.color.selector_button_text_dark else R.color.selector_button_text_light
        binding.buttonSendAmount.setTextColor(resources.getColorStateList(resId))
        binding.lottieButtonLoading.invisibleIf(uiModel.isDisconnected)
    }

    //
    // Private UI Events
    //

    private fun onModelUpdated(old: HomeViewModel.UiModel?, new: HomeViewModel.UiModel) {
        logUpdate(old, new)
        uiModel = new
        setProgress(uiModel) // TODO: we may not need to separate anymore
        if (new.status == Synchronizer.Status.SYNCED) onSynced(new) else onSyncing(new)
        setSendEnabled(new.isSendEnabled, new.status == Synchronizer.Status.SYNCED)
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
                    if (old.availableBalance != new.availableBalance) append("${maybeComma()}availableBalance=${new.availableBalance}")
                    if (old.totalBalance != new.totalBalance) append("${maybeComma()}totalBalance=${new.totalBalance}")
                    if (old.pendingSend != new.pendingSend) append("${maybeComma()}pendingSend=${new.pendingSend}")
                    append(")")
                }
            }
        }
        twig("onModelUpdated: $message")
    }

    private fun onSyncing(uiModel: HomeViewModel.UiModel) {
    }

    private fun onSynced(uiModel: HomeViewModel.UiModel) {
        snake.isSynced = true
    }

    private fun onSend() {
        if (isSendEnabled) mainActivity?.safeNavigate(R.id.action_nav_home_to_send)
    }
}
