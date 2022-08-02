package com.nighthawkapps.wallet.android.ui.setup

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.model.BlockHeight
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentBackupBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel.WalletSetupState.SEED_WITH_BACKUP
import com.nighthawkapps.wallet.android.ui.util.AddressPartNumberSpan
import com.nighthawkapps.wallet.android.ui.util.EncryptedPdfDialog
import com.nighthawkapps.wallet.android.ui.util.PdfUtil
import com.nighthawkapps.wallet.android.ui.util.seedwords.SeedWord
import com.nighthawkapps.wallet.android.ui.util.seedwords.SeedWordAdapter
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
    private val args: BackupFragmentArgs by navArgs()

    override fun inflate(inflater: LayoutInflater): FragmentBackupBinding =
        FragmentBackupBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showSeedWords()
        binding.buttonPositive.text = getString(if (args.showExportPdf) R.string.ns_backup_wallet_pdf else R.string.ns_continue)
        binding.hitAreaExit.setOnClickListener {
            onEnterWallet()
        }
        binding.buttonPositive.setOnClickListener {
            if (args.showExportPdf) {
                showEnterPasswordDialog()
            } else {
                onEnterWallet()
            }
        }
        if (hasBackUp) {
            binding.buttonPositive.text = getString(R.string.ns_backup_done)
        }

        if (args.showExportPdf) {
            binding.buttonPositive.isEnabled = true
            binding.checkBoxConfirmation.visibility = View.GONE
        } else {
            binding.checkBoxConfirmation.setOnCheckedChangeListener { _, isChecked ->
                binding.buttonPositive.isEnabled = isChecked
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity?.onBackPressedDispatcher?.addCallback(this) {
            onEnterWallet()
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
            binding.textBirtdate.text = "${calculateBirthday().value}"
        }
    }

    // TODO: expose this in the SDK
    private suspend fun calculateBirthday(): BlockHeight {
        var storedBirthday: BlockHeight? = null
        var oldestTransactionHeight: BlockHeight? = null
        var activationHeight: BlockHeight? = null
        try {
            activationHeight = mainActivity?.synchronizerComponent?.synchronizer()?.network?.saplingActivationHeight
            storedBirthday = walletSetup.loadBirthdayHeight()
            oldestTransactionHeight = mainActivity?.synchronizerComponent?.synchronizer()?.receivedTransactions?.first()?.last()?.minedHeight?.let {
                BlockHeight.new(NighthawkWalletApp.instance.defaultNetwork, it)
            }
            // to be safe adjust for reorgs (and generally a little cushion is good for privacy)
            // so we round down to the nearest 100 and then subtract 100 to ensure that the result is always at least 100 blocks away
            oldestTransactionHeight = ZcashSdk.MAX_REORG_SIZE.let { boundary ->
                oldestTransactionHeight?.let { it.value - it.value.rem(boundary) - boundary }?.let { BlockHeight.new(NighthawkWalletApp.instance.defaultNetwork, it) }
            }
        } catch (t: Throwable) {
            twig("failed to calculate birthday due to: $t")
        }
        return listOfNotNull(storedBirthday, oldestTransactionHeight, activationHeight).maxBy { it.value }
    }

    private fun onEnterWallet() {
        if (args.showExportPdf) {
            mainActivity?.navController?.popBackStack()
        } else {
            mainActivity?.safeNavigate(R.id.action_nav_backup_to_nav_home)
        }
    }

    private fun showSeedWords() = viewLifecycleOwner.lifecycleScope.launch {
        val seedWordsList =
            loadSeedWords().mapIndexed { index, chars -> SeedWord(index + 1, String(chars)) }
        val seedWordAdapter = SeedWordAdapter(requireContext(), seedWordsList)
        binding.gridViewSeedWords.adapter = seedWordAdapter
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
            PdfUtil.exportPasswordProtectedPdf(requireContext(), password, loadSeedWords(), calculateBirthday().value.toInt())
        }
    }
}
