package com.nighthawkapps.wallet.android.ui.setup

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.twig
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentBackupBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel.WalletSetupState.SEED_WITH_BACKUP
import com.nighthawkapps.wallet.android.ui.util.AddressPartNumberSpan
import com.nighthawkapps.wallet.android.ui.util.EncryptedPdfDialog
import com.nighthawkapps.wallet.android.ui.util.PdfUtil
import com.nighthawkapps.wallet.kotlin.mnemonic.Mnemonics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupFragment : BaseFragment<FragmentBackupBinding>(),
    EncryptedPdfDialog.OnPdfClickListener {

    private val walletSetup: WalletSetupViewModel by activityViewModel(false)

    private var hasBackUp: Boolean = true // TODO: implement backup and then check for it here-ish

    override fun inflate(inflater: LayoutInflater): FragmentBackupBinding =
        FragmentBackupBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            applySpan(
                textAddressPart1, textAddressPart2, textAddressPart3,
                textAddressPart4, textAddressPart5, textAddressPart6,
                textAddressPart7, textAddressPart8, textAddressPart9,
                textAddressPart10, textAddressPart11, textAddressPart12,
                textAddressPart13, textAddressPart14, textAddressPart15,
                textAddressPart16, textAddressPart17, textAddressPart18,
                textAddressPart19, textAddressPart20, textAddressPart21,
                textAddressPart22, textAddressPart23, textAddressPart24
            )
        }
        binding.buttonPositive.setOnClickListener {
            showEnterPasswordDialog()
        }
        if (hasBackUp) {
            binding.buttonPositive.text = getString(R.string.backup_button_done)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity?.onBackPressedDispatcher?.addCallback(this) {
            onEnterWallet(false)
        }
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        walletSetup.checkSeed().onEach {
            hasBackUp = when (it) {
                SEED_WITH_BACKUP -> true
                else -> false
            }
        }.launchIn(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        resumedScope.launch {
            binding.textBirtdate.text = getString(R.string.backup_format_birthday_height, calculateBirthday())
        }
    }

    // TODO: expose this in the SDK
    private suspend fun calculateBirthday(): Int {
        var storedBirthday = 0
        var oldestTransactionHeight = 0
        var activationHeight = 0
        try {
            activationHeight = mainActivity?.synchronizerComponent?.synchronizer()?.network?.saplingActivationHeight ?: 0
            storedBirthday = walletSetup.loadBirthdayHeight() ?: 0
            oldestTransactionHeight = mainActivity?.synchronizerComponent?.synchronizer()?.receivedTransactions?.first()?.last()?.minedHeight ?: 0
            // to be safe adjust for reorgs (and generally a little cushion is good for privacy)
            // so we round down to the nearest 100 and then subtract 100 to ensure that the result is always at least 100 blocks away
            oldestTransactionHeight = ZcashSdk.MAX_REORG_SIZE.let { boundary ->
                oldestTransactionHeight.let { it - it.rem(boundary) - boundary }
            }
        } catch (t: Throwable) {
            twig("failed to calculate birthday due to: $t")
        }
        return maxOf(storedBirthday, oldestTransactionHeight, activationHeight)
    }

    private fun onEnterWallet(showMessage: Boolean = !this.hasBackUp) {
        if (showMessage) {
            Toast.makeText(activity, R.string.backup_verification_not_implemented, Toast.LENGTH_LONG).show()
        }
        mainActivity?.navController?.popBackStack()
    }

    private fun applySpan(vararg textViews: TextView) = lifecycleScope.launch {
        val words = loadSeedWords()
        val thinSpace = "\u2005" // 0.25 em space
        textViews.forEachIndexed { index, textView ->
            val numLength = "$index".length
            val word = words[index]
            // TODO: work with a charsequence here, rather than constructing a String
            textView.text = SpannableString("${index + 1}$thinSpace${String(word)}").apply {
                setSpan(AddressPartNumberSpan(), 0, 1 + numLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private suspend fun loadSeedWords(): List<CharArray> = withContext(Dispatchers.IO) {
        val lockBox = LockBox(NighthawkWalletApp.instance)
        val mnemonics = Mnemonics()
        val seedPhrase = lockBox.getCharsUtf8(Const.Backup.SEED_PHRASE)
            ?: throw RuntimeException("Seed Phrase expected but not found in storage!!")
        val result = mnemonics.toWordList(seedPhrase)
        result
    }

    private fun showEnterPasswordDialog() {
        EncryptedPdfDialog().also {
            it.isCancelable = false
            it.setClickListener(this)
        }.show(parentFragmentManager, "Pdf Dialog")
    }

    override fun onPositiveClicked(password: String) {
        makePasswordProtectedPdf(password)
    }

    private fun makePasswordProtectedPdf(password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            PdfUtil.exportPasswordProtectedPdf(requireContext(), password, loadSeedWords(), calculateBirthday())
        }
    }
}
