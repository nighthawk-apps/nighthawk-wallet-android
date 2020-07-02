package com.nighthawkapps.wallet.android.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentHomeBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.*
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.feedback.Report.Tap.*
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.home.HomeFragment.BannerAction.*
import com.nighthawkapps.wallet.android.ui.send.SendViewModel
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel.WalletSetupState.NO_SEED
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.Synchronizer.Status.*
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.ext.safelyConvertToBigDecimal
import cash.z.ecc.android.sdk.ext.twig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    override val screen = Report.Screen.HOME

    private lateinit var numberPad: List<TextView>
    private lateinit var uiModel: HomeViewModel.UiModel

    private val walletSetup: WalletSetupViewModel by activityViewModel(false)
    private val sendViewModel: SendViewModel by activityViewModel()
    private val viewModel: HomeViewModel by viewModel()

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
        with(binding) {
            numberPad = arrayListOf(
                buttonNumberPad0.asKey(),
                buttonNumberPad1.asKey(),
                buttonNumberPad2.asKey(),
                buttonNumberPad3.asKey(),
                buttonNumberPad4.asKey(),
                buttonNumberPad5.asKey(),
                buttonNumberPad6.asKey(),
                buttonNumberPad7.asKey(),
                buttonNumberPad8.asKey(),
                buttonNumberPad9.asKey(),
                buttonNumberPadDecimal.asKey(),
                buttonNumberPadBack.asKey()
            )
            hitAreaReceive.onClickNavTo(R.id.action_nav_home_to_nav_profile) { tapped(HOME_PROFILE) }
            textDetail.onClickNavTo(R.id.action_nav_home_to_nav_detail) { tapped(HOME_DETAIL) }
            hitAreaScan.setOnClickListener {
                mainActivity?.maybeOpenScan().also { tapped(HOME_SCAN) }
            }

            textBannerAction.setOnClickListener {
                onBannerAction(BannerAction.from((it as? TextView)?.text?.toString()))
            }
            buttonSendAmount.setOnClickListener {
                onSend().also { tapped(HOME_SEND) }
            }
            setSendAmount("0", false)

            snake = MagicSnakeLoader(binding.lottieButtonLoading)
        }

        binding.buttonNumberPadBack.setOnLongClickListener {
            onClearAmount().also { tapped(HOME_CLEAR_AMOUNT) }
            true
        }

        if (::uiModel.isInitialized) {
            twig("uiModel exists!")
            onModelUpdated(null, uiModel)
        }
    }

    private fun onClearAmount() {
        if (::uiModel.isInitialized) {
            resumedScope.launch {
                binding.textSendAmount.text.apply {
                    while (uiModel.pendingSend != "0") {
                        viewModel.onChar('<')
                        delay(5)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        twig("HomeFragment.onResume  resumeScope.isActive: ${resumedScope.isActive}  $resumedScope")
        viewModel.initializeMaybe()
        onClearAmount()
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
            uiModel.isSynced -> if (uiModel.hasFunds) "SEND AMOUNT" else "NO FUNDS AVAILABLE"
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

    /**
     * @param amount the amount to send represented as ZEC, without the dollar sign.
     */
    fun setSendAmount(amount: String, updateModel: Boolean = true) {
        binding.textSendAmount.text = "\$$amount".toColoredSpan(R.color.text_light_dimmed, "$")
        if (updateModel) {
            sendViewModel.zatoshiAmount = amount.safelyConvertToBigDecimal().convertZecToZatoshi()
        }
        binding.buttonSendAmount.disabledIf(amount == "0")
    }

    fun setAvailable(availableBalance: Long = -1L, totalBalance: Long = -1L) {
        val missingBalance = availableBalance < 0
        val availableString = if (missingBalance) "Updating" else availableBalance.convertZatoshiToZecString()
        binding.textBalanceAvailable.text = availableString
        binding.textBalanceAvailable.transparentIf(missingBalance)
        binding.labelBalance.transparentIf(missingBalance)
        binding.textBalanceDescription.apply {
            goneIf(missingBalance)
            text = if (availableBalance != -1L && (availableBalance < totalBalance)) {
                val change = (totalBalance - availableBalance).convertZatoshiToZecString()
                "(expecting +$change ZEC)".toColoredSpan(R.color.text_light, "+$change")
            } else {
                "(enter an amount to send)"
            }
        }
    }

    fun setBanner(message: String = "", action: BannerAction = CLEAR) {
        with(binding) {
            val hasMessage = !message.isEmpty() || action != CLEAR
            groupBalance.goneIf(hasMessage)
            groupBanner.goneIf(!hasMessage)
            layerLock.goneIf(!hasMessage)

            textBannerMessage.text = message
            textBannerAction.text = action.action
        }
    }


    //
    // Private UI Events
    //

    private fun onModelUpdated(old: HomeViewModel.UiModel?, new: HomeViewModel.UiModel) {
        logUpdate(old, new)
        uiModel = new
        if (old?.pendingSend != new.pendingSend) {
            setSendAmount(new.pendingSend)
        }
        setProgress(uiModel) // TODO: we may not need to separate anymore
//        if (new.status = SYNCING) onSyncing(new) else onSynced(new)
        if (new.status == SYNCED) onSynced(new) else onSyncing(new)
        setSendEnabled(new.isSendEnabled, new.status == SYNCED)
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
                    if (old.status != new.status) append ("status=${new.status}")
                    if (old.processorInfo != new.processorInfo) {
                        append ("${maybeComma()}processorInfo=ProcessorInfo(")
                        val startLength = length
                        fun innerComma() = if (length > startLength) ", " else ""
                        if (old.processorInfo.networkBlockHeight != new.processorInfo.networkBlockHeight) append("networkBlockHeight=${new.processorInfo.networkBlockHeight}")
                        if (old.processorInfo.lastScannedHeight != new.processorInfo.lastScannedHeight) append("${innerComma()}lastScannedHeight=${new.processorInfo.lastScannedHeight}")
                        if (old.processorInfo.lastDownloadedHeight != new.processorInfo.lastDownloadedHeight) append("${innerComma()}lastDownloadedHeight=${new.processorInfo.lastDownloadedHeight}")
                        if (old.processorInfo.lastDownloadRange != new.processorInfo.lastDownloadRange) append("${innerComma()}lastDownloadRange=${new.processorInfo.lastDownloadRange}")
                        if (old.processorInfo.lastScanRange != new.processorInfo.lastScanRange) append("${innerComma()}lastScanRange=${new.processorInfo.lastScanRange}")
                        append(")")
                    }
                    if (old.availableBalance != new.availableBalance) append ("${maybeComma()}availableBalance=${new.availableBalance}")
                    if (old.totalBalance != new.totalBalance) append ("${maybeComma()}totalBalance=${new.totalBalance}")
                    if (old.pendingSend != new.pendingSend) append ("${maybeComma()}pendingSend=${new.pendingSend}")
                    append(")")
                }
            }
        }
        twig("onModelUpdated: $message")
    }

    private fun onSyncing(uiModel: HomeViewModel.UiModel) {
        setAvailable()
    }

    private fun onSynced(uiModel: HomeViewModel.UiModel) {
        snake.isSynced = true
        if (!uiModel.hasBalance) {
            onNoFunds()
        } else {
            setBanner("")
            setAvailable(uiModel.availableBalance, uiModel.totalBalance)
        }
    }

    private fun onSend() {
        if (isSendEnabled) mainActivity?.safeNavigate(R.id.action_nav_home_to_send)
    }

    private fun onBannerAction(action: BannerAction) {
        when (action) {
            FUND_NOW -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("To make full use of this wallet, deposit funds to your address.")
                    .setTitle("No Balance")
                    .setCancelable(true)
                    .setPositiveButton("View Address") { dialog, _ ->
                        tapped(HOME_FUND_NOW)
                        dialog.dismiss()
                        mainActivity?.safeNavigate(R.id.action_nav_home_to_nav_receive)
                    }
                    .show()
            }
            CANCEL -> {
                // TODO: trigger banner / balance update
                onNoFunds()
            }
        }
    }

    private fun onNoFunds() {
        setBanner("No Balance", FUND_NOW)
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