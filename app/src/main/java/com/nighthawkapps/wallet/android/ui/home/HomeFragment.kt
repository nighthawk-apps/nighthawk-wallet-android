package com.nighthawkapps.wallet.android.ui.home

import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.Synchronizer.Status.DISCONNECTED
import cash.z.ecc.android.sdk.Synchronizer.Status.STOPPED
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.twig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentHomeBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.gone
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.invisibleIf
import com.nighthawkapps.wallet.android.ext.onClickNavTo
import com.nighthawkapps.wallet.android.ext.showSharedLibraryCriticalError
import com.nighthawkapps.wallet.android.ext.toAppColor
import com.nighthawkapps.wallet.android.ext.toColoredSpan
import com.nighthawkapps.wallet.android.ext.transparentIf
import com.nighthawkapps.wallet.android.ext.visible
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel.WalletSetupState.NO_SEED
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.isActive

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private lateinit var uiModel: HomeViewModel.UiModel

    private val walletSetup: WalletSetupViewModel by activityViewModel(false)
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
        binding.buttonSendAmount.text = getString(R.string.home_button_send_disconnected)
        binding.buttonSendAmount.setTextColor(R.color.text_light.toAppColor())
        binding.textMyAddress.onClickNavTo(R.id.action_nav_scan_to_nav_receive)
        binding.textMyAddress.paintFlags =
            binding.textMyAddress.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.textSideShift.paintFlags =
            binding.textMyAddress.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.textWalletHistory.onClickNavTo(R.id.action_nav_home_to_nav_history)
        binding.textTransparentBalance.onClickNavTo(R.id.action_nav_home_to_nav_balance_detail)
        binding.textWalletHistory.paintFlags =
            binding.textMyAddress.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.textTransparentBalance.paintFlags =
            binding.textMyAddress.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.hitAreaBalance.onClickNavTo(R.id.action_nav_home_to_nav_balance_detail)
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
        // Once the synchronizer is created, monitor state changes, while this fragment is resumed
        launchWhenSyncReady(::onSyncReady)
    }

    fun setBanner(message: String = "", action: BannerAction = BannerAction.CLEAR) {
        with(binding) {
            val hasMessage = !message.isEmpty() || action != BannerAction.CLEAR
            groupBalance.goneIf(hasMessage)
            groupBanner.goneIf(!hasMessage)

            textBannerMessage.text = message
            textBannerAction.text = action.action
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
            uiModel.isDownloading -> {
                when (snake.downloadProgress) {
                    0 -> "Preparing to download..."
                    else -> "Downloading . . . ${snake.downloadProgress}%"
                }
            }
            uiModel.isValidating -> "Validating . . ."
            uiModel.isScanning -> {
                when (snake.scanProgress) {
                    0 -> "Preparing to scan..."
                    100 -> "Finalizing..."
                    else -> "Scanning . . . ${snake.scanProgress}%"
                }
            }
            else -> "Updating"
        }

        binding.buttonSendAmount.text = sendText
        twig("Send button set to: $sendText")

        val resId =
            if (uiModel.isSynced) R.color.selector_button_text_dark else R.color.selector_button_text_light
        binding.buttonSendAmount.setTextColor(resources.getColorStateList(resId))
        binding.lottieButtonLoading.invisibleIf(uiModel.isDisconnected)
    }

    fun setAvailable(
        availableBalance: Long = -1L,
        totalBalance: Long = -1L,
        availableTransparentBalance: Long = -1L,
        unminedCount: Int = 0
    ) {
        val missingBalance = availableBalance < 0
        val availableString =
            if (missingBalance) getString(R.string.home_button_send_updating) else WalletZecFormmatter.toZecStringFull(
                availableBalance
            )
        binding.textBalanceAvailable.text = availableString
        binding.textBalanceAvailable.transparentIf(missingBalance)
        binding.labelBalance.transparentIf(missingBalance)
        binding.textBalanceDescription.apply {
            if (missingBalance) {
                if (viewModel.priceModel != null) {
                    try {
                        val usdPrice = viewModel.priceModel?.price!!.toFloat()
                        val zecBalance = roundFloat(
                            availableBalance.convertZatoshiToZecString().toFloat()
                        )
                        val usdTotal = "%.2f".format(usdPrice * zecBalance)
                        text = "~$$usdTotal"
                        visible()
                    } catch (e: NumberFormatException) {
                        gone()
                    }
                } else {
                    gone()
                    viewModel.initPrice()
                }
            } else {
                text = when {
                    unminedCount > 0 -> "(excludes $unminedCount unconfirmed ${if (unminedCount > 1) "transactions" else "transaction"})"
                    availableBalance != -1L && (availableBalance < totalBalance) -> {
                        val change =
                            WalletZecFormmatter.toZecStringFull(totalBalance - availableBalance)
                        val symbol = getString(R.string.symbol)
                        "(${getString(R.string.home_banner_expecting)} +$change $symbol)".toColoredSpan(
                            R.color.text_light,
                            "+$change"
                        )
                    }
                    else -> getString(R.string.home_instruction_enter_amount)
                }
                visible()
            }
        }
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
    }

    private fun onSynced(uiModel: HomeViewModel.UiModel) {
        snake.isSynced = true
        if (!uiModel.hasSaplingBalance) {
            onNoFunds()
        } else {
            setBanner("")
            setAvailable(
                uiModel.saplingBalance.availableZatoshi,
                uiModel.saplingBalance.totalZatoshi,
                uiModel.transparentBalance.availableZatoshi,
                uiModel.unminedCount
            )
        }
        autoShield(uiModel)
    }

    private fun autoShield(uiModel: HomeViewModel.UiModel) {
        if (uiModel.hasAutoshieldFunds && canAutoshield()) {
            twig("Autoshielding is available! Let's do this!!!")
            mainActivity?.lastAutoShieldTime = System.currentTimeMillis()
            mainActivity?.safeNavigate(R.id.action_nav_home_to_nav_funds_available)
        } else {
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
            } else if (!canAutoshield()) {
                twig("Could not autoshield probably because the last one occurred ${System.currentTimeMillis() - (mainActivity?.lastAutoShieldTime ?: 0)}ms ago which is less than the required cool off time of ${mainActivity?.maxAutoshieldFrequency}ms")
            }
        }
    }

    private fun onSend() {
        if (isSendEnabled) mainActivity?.safeNavigate(R.id.action_nav_home_to_send)
    }

    private fun onNoFunds() {
        setBanner(getString(R.string.home_no_balance), BannerAction.FUND_NOW)
    }

    private fun canAutoshield(): Boolean {
        return mainActivity?.let { main ->
            System.currentTimeMillis().let { now ->
                val delta = now - main.lastAutoShieldTime
                return delta > main.maxAutoshieldFrequency
            }
        } ?: false
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
