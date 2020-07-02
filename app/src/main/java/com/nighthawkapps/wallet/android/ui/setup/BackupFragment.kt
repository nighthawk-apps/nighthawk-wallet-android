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
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.databinding.FragmentBackupBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.feedback.Report.MetricType.SEED_PHRASE_LOADED
import com.nighthawkapps.wallet.android.feedback.Report.Tap.BACKUP_DONE
import com.nighthawkapps.wallet.android.feedback.Report.Tap.BACKUP_VERIFY
import com.nighthawkapps.wallet.android.feedback.measure
import com.nighthawkapps.wallet.android.lockbox.LockBox
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel.LockBoxKey
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel.WalletSetupState.SEED_WITH_BACKUP
import com.nighthawkapps.wallet.android.ui.util.AddressPartNumberSpan
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel
import com.nighthawkapps.wallet.kotlin.mnemonic.Mnemonics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupFragment : BaseFragment<FragmentBackupBinding>() {
    override val screen = Report.Screen.BACKUP

    val walletSetup: WalletSetupViewModel by activityViewModel(false)

    private var hasBackUp: Boolean = true //TODO: implement backup and then check for it here-ish

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
            onEnterWallet().also { if (hasBackUp) tapped(BACKUP_DONE) else tapped(BACKUP_VERIFY) }
        }
        if (hasBackUp) {
            binding.buttonPositive.text = "Done"
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
            when(it) {
                SEED_WITH_BACKUP -> {
                    hasBackUp = true
                }
            }
        }.launchIn(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        resumedScope.launch {
            binding.textBirtdate.text = "Birthday Height: %,d".format(walletSetup.loadBirthdayHeight())
        }
    }

    private fun onEnterWallet(showMessage: Boolean = !this.hasBackUp) {
        if (showMessage) {
            Toast.makeText(activity, "Backup verification coming soon!", Toast.LENGTH_LONG).show()
        }
        mainActivity?.navController?.popBackStack()
    }

    private fun applySpan(vararg textViews: TextView) = lifecycleScope.launch {
        val words = loadSeedWords()
        val thinSpace = "\u2005" // 0.25 em space
        textViews.forEachIndexed { index, textView ->
            val numLength = "$index".length
            val word = words?.get(index)
            // TODO: work with a charsequence here, rather than constructing a String
            textView.text = SpannableString("${index + 1}$thinSpace${word?.let { String(it) }}").apply {
                setSpan(AddressPartNumberSpan(), 0, 1 + numLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private suspend fun loadSeedWords(): List<CharArray>? = withContext(Dispatchers.IO) {
        mainActivity?.feedback?.measure(SEED_PHRASE_LOADED) {
            val lockBox = LockBox(NighthawkWalletApp.instance)
            val mnemonics = Mnemonics()
            val seedPhrase =  lockBox.getCharsUtf8(LockBoxKey.SEED_PHRASE)
            val result = seedPhrase?.let { mnemonics.toWordList(it) }
            result
        }
    }
}