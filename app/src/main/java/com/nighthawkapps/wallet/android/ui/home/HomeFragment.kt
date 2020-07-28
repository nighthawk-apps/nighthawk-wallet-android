package com.nighthawkapps.wallet.android.ui.home

import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.Synchronizer.Status.DISCONNECTED
import cash.z.ecc.android.sdk.Synchronizer.Status.STOPPED
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.collectWith
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.twig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentHomeBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.invisibleIf
import com.nighthawkapps.wallet.android.ext.onClickNavTo
import com.nighthawkapps.wallet.android.ext.toColoredSpan
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.detail.TransactionAdapter
import com.nighthawkapps.wallet.android.ui.detail.TransactionsFooter
import com.nighthawkapps.wallet.android.ui.detail.WalletDetailViewModel
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel.WalletSetupState.NO_SEED
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scanReduce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private lateinit var uiModel: HomeViewModel.UiModel
    private lateinit var adapter: TransactionAdapter<ConfirmedTransaction>

    private val walletSetup: WalletSetupViewModel by activityViewModel(false)
    private val viewModel: HomeViewModel by viewModel()
    private val walletViewModel: WalletDetailViewModel by viewModel()

    lateinit var snake: MagicSnakeLoader

    override fun inflate(inflater: LayoutInflater): FragmentHomeBinding =
        FragmentHomeBinding.inflate(inflater)

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // this will call startSync either now or later (after initializing with newly created seed)
        walletSetup.checkSeed().onEach {
            twig("Checking seed")
            if (it == NO_SEED) {
                // interact with user to create, backup and verify seed
                // leads to a call to startSync(), later (after accounts are created from seed)
                twig("Seed not found, therefore, launching seed creation flow")
                mainActivity?.safeNavigate(R.id.action_nav_home_to_create_wallet)
            } else {
                twig("Found seed. Re-opening existing wallet")
                mainActivity?.startSync(walletSetup.openWallet())
            }
        }.launchIn(lifecycleScope)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        twig("HomeFragment.onViewCreated  uiModel: ${::uiModel.isInitialized}  saved: ${savedInstanceState != null}")
        snake = MagicSnakeLoader(binding.lottieButtonLoading)
        binding.hitAreaProfile.onClickNavTo(R.id.action_nav_home_to_nav_profile)
        binding.hitAreaProfile.onClickNavTo(R.id.action_nav_home_to_nav_profile)
        binding.buttonSendAmount.setOnClickListener { onSend() }
        binding.textMyAddress.onClickNavTo(R.id.action_nav_scan_to_nav_receive)
        binding.textMyAddress.paintFlags = binding.textMyAddress.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG
        binding.textSideShift.paintFlags = binding.textMyAddress.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG
        binding.textSideShift.setOnClickListener {
            mainActivity?.copyAddress()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Fund wallet with SideShift.ai?")
                .setMessage("Your Z-Address has been copied to the clipboard, please paste it in the address bar after selecting Shielded Zcash as the receiving address in the browser.")
                .setCancelable(false)
                .setPositiveButton("Open Browser") { dialog, _ ->
                    mainActivity?.openSideShift()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
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
        initTransactionUI()
        walletViewModel.balance.collectWith(resumedScope) {
            onBalanceUpdated(it)
        }
        twig("HomeFragment.onResume  resumeScope.isActive: ${resumedScope.isActive}  $resumedScope")
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

        // TODO: see if there is a better way to trigger a refresh of the uiModel on resume
        //       the latest one should just be in the viewmodel and we should just "resubscribe"
        //       but for some reason, this doesn't always happen, which kind of defeats the purpose
        //       of having a cold stream in the view model
        resumedScope.launch {
            viewModel.refreshBalance()
        }
    }

    private fun onBalanceUpdated(balance: CompactBlockProcessor.WalletBalance) {
        binding.textBalanceAvailable.text = (getString(R.string.zec) + balance.availableZatoshi.convertZatoshiToZecString())
            .toColoredSpan(R.color.text_light_dimmed, getString(R.string.zec))
        val change = (balance.totalZatoshi - balance.availableZatoshi)
        binding.textBalanceDescription.apply {
            goneIf(change <= 0L)
            val changeString = change.convertZatoshiToZecString()
            text =
                "(expecting +$changeString ZEC)".toColoredSpan(R.color.text_light, "+$changeString")
        }
    }

    private fun initTransactionUI() {
        binding.recyclerTransactions.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        binding.recyclerTransactions.addItemDecoration(TransactionsFooter(binding.recyclerTransactions.context))
        adapter = TransactionAdapter()
        walletViewModel.transactions.collectWith(resumedScope) { onTransactionsUpdated(it) }
        binding.recyclerTransactions.adapter = adapter
        binding.recyclerTransactions.smoothScrollToPosition(0)
    }

    private fun onTransactionsUpdated(transactions: PagedList<ConfirmedTransaction>) {
        twig("got a new paged list of transactions")
        binding.groupEmptyViews.goneIf(transactions.size > 0)
        adapter.submitList(transactions)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        twig("HomeFragment.onSaveInstanceState")
        if (::uiModel.isInitialized) {
//            outState.putParcelable("uiModel", uiModel)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let { inState ->
            twig("HomeFragment.onViewStateRestored")
//            onModelUpdated(HomeViewModel.UiModel(), inState.getParcelable("uiModel")!!)
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
            uiModel.isSynced -> if (uiModel.hasFunds) "SEND ZCASH" else "NO FUNDS AVAILABLE"
            uiModel.status == STOPPED -> "IDLE"
            uiModel.isDownloading -> "Downloading . . . ${snake.downloadProgress}%"
            uiModel.isValidating -> "Validating . . ."
            uiModel.isScanning -> "Scanning . . . ${snake.scanProgress}%"
            else -> "Updating"
        }

        binding.buttonSendAmount.text = sendText
        twig("Send button set to: $sendText")

        val resId =
            if (uiModel.isSynced) R.color.selector_button_text_dark else R.color.selector_button_text_light
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
//        if (new.status = SYNCING) onSyncing(new) else onSynced(new)
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
        if (!uiModel.hasBalance) {
            onNoFunds()
        } else {
        }
    }

    private fun onSend() {
        if (isSendEnabled) mainActivity?.safeNavigate(R.id.action_nav_home_to_send)
    }

    private fun onBannerAction(action: BannerAction) {
        when (action) {
            BannerAction.FUND_NOW -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("To make full use of this wallet, deposit funds to your address.")
                    .setTitle("No Balance")
                    .setCancelable(true)
                    .setPositiveButton("View Address") { dialog, _ ->
                        dialog.dismiss()
                        mainActivity?.safeNavigate(R.id.action_nav_home_to_nav_receive)
                    }
                    .show()
            }
            BannerAction.CANCEL -> {
                // TODO: trigger banner / balance update
                onNoFunds()
            }
        }
    }

    private fun onNoFunds() {
    }

    //
    // Inner classes and extensions
    //

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

    private fun TextView.asKey(): TextView {
        val c = text[0]
        setOnClickListener {
            lifecycleScope.launch {
                twig("CHAR TYPED: $c")
                viewModel.onChar(c)
            }
        }
        return this
    }
}