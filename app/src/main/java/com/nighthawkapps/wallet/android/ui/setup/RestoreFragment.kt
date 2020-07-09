package com.nighthawkapps.wallet.android.ui.setup

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import cash.z.ecc.android.sdk.ext.ZcashSdk
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentRestoreBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.showInvalidSeedPhraseError
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.tylersuehr.chips.Chip
import com.tylersuehr.chips.ChipsAdapter
import com.tylersuehr.chips.SeedWordAdapter
import kotlinx.coroutines.launch

class RestoreFragment : BaseFragment<FragmentRestoreBinding>(), View.OnKeyListener {

    private val walletSetup: WalletSetupViewModel by activityViewModel(false)

    private lateinit var seedWordRecycler: RecyclerView
    private var seedWordAdapter: SeedWordAdapter? = null

    override fun inflate(inflater: LayoutInflater): FragmentRestoreBinding =
        FragmentRestoreBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        seedWordRecycler = binding.chipsInput.findViewById<RecyclerView>(R.id.chips_recycler)
        seedWordAdapter =
            SeedWordAdapter(seedWordRecycler.adapter as ChipsAdapter).onDataSetChanged {
                onChipsModified()
            }.also { onChipsModified() }
        seedWordRecycler.adapter = seedWordAdapter

        binding.chipsInput.apply {
            setFilterableChipList(getChips())
            setDelimiter("[ ;,]", true)
        }

        binding.buttonDone.setOnClickListener {
            onDone()
        }

        binding.buttonSuccess.setOnClickListener {
            onEnterWallet()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity?.onFragmentBackPressed(this) {
            if (seedWordAdapter == null || seedWordAdapter?.itemCount == 1) {
                onExit()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Are you sure? For security, the words that you have entered will be cleared!")
                    .setTitle("Abort?")
                    .setPositiveButton("Stay") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNegativeButton("Exit") { dialog, _ ->
                        dialog.dismiss()
                        onExit()
                    }
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Require one less tap to enter the seed words
        touchScreenForUser()
    }

    private fun onExit() {
        hideAutoCompleteWords()
        mainActivity?.hideKeyboard()
        mainActivity?.navController?.popBackStack()
    }

    private fun onEnterWallet() {
        mainActivity?.safeNavigate(R.id.action_nav_restore_to_nav_home)
    }

    private fun onDone() {
        mainActivity?.hideKeyboard()
        val seedPhrase = binding.chipsInput.selectedChips.joinToString(" ") {
            it.title
        }
        val birthday = binding.root.findViewById<TextView>(R.id.input_birthdate).text.toString()
            .let { birthdateString ->
                if (birthdateString.isNullOrEmpty()) ZcashSdk.SAPLING_ACTIVATION_HEIGHT else birthdateString.toInt()
            }.coerceAtLeast(ZcashSdk.SAPLING_ACTIVATION_HEIGHT)

        try {
            walletSetup.validatePhrase(seedPhrase)
            importWallet(seedPhrase, birthday)
        } catch (t: Throwable) {
            mainActivity?.showInvalidSeedPhraseError(t)
        }
    }

    private fun importWallet(seedPhrase: String, birthday: Int) {
        mainActivity?.hideKeyboard()
        mainActivity?.apply {
            lifecycleScope.launch {
                mainActivity?.startSync(walletSetup.importWallet(seedPhrase, birthday))
                // bugfix: if the user proceeds before the synchronizer is created the app will crash!
                binding.buttonSuccess.isEnabled = true
            }
            playSound("sound_receive_small.mp3")
            vibrateSuccess()
        }

        binding.groupDone.visibility = View.GONE
        binding.groupStart.visibility = View.GONE
        binding.groupSuccess.visibility = View.VISIBLE
        binding.buttonSuccess.isEnabled = false
    }

    private fun onChipsModified() {
        seedWordAdapter?.editText?.apply {
            postDelayed({
                requestFocus()
            }, 40L)
        }
        setDoneEnabled()

        view?.postDelayed({
            mainActivity?.showKeyboard(seedWordAdapter!!.editText)
            seedWordAdapter?.editText?.requestFocus()
        }, 500L)
    }

    private fun setDoneEnabled() {
        val count = seedWordAdapter?.itemCount ?: 0
        binding.groupDone.goneIf(count <= 24)
    }

    private fun hideAutoCompleteWords() {
        seedWordAdapter?.editText?.setText("")
    }

    private fun getChips(): List<Chip> {
        return resources.getStringArray(R.array.word_list).map {
            SeedWordChip(it)
        }
    }

    private fun touchScreenForUser() {
        seedWordAdapter?.editText?.apply {
            postDelayed({
                seedWordAdapter?.editText?.inputType =
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                dispatchTouchEvent(motionEvent(ACTION_DOWN))
                dispatchTouchEvent(motionEvent(ACTION_UP))
            }, 100L)
        }
    }

    private fun motionEvent(action: Int) = SystemClock.uptimeMillis().let { now ->
        MotionEvent.obtain(now, now, action, 0f, 0f, 0)
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }
}

class SeedWordChip(val word: String, var index: Int = -1) : Chip() {
    override fun getSubtitle(): String? = null // "subtitle for $word"
    override fun getAvatarDrawable(): Drawable? = null
    override fun getId() = index
    override fun getTitle() = word
    override fun getAvatarUri() = null
}