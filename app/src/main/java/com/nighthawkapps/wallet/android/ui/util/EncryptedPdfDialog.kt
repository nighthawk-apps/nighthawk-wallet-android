package com.nighthawkapps.wallet.android.ui.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.nighthawkapps.wallet.android.databinding.DialogEncryptedPdfBinding

class EncryptedPdfDialog : DialogFragment() {

    private lateinit var binding: DialogEncryptedPdfBinding
    private var pdfClickListener: OnPdfClickListener? = null

    interface OnPdfClickListener {
        fun onPositiveClicked(password: String)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogEncryptedPdfBinding.inflate(inflater, container, false)

        binding.btnNegative.setOnClickListener {
            dismiss()
        }
        binding.btnPositive.setOnClickListener {
            pdfClickListener?.onPositiveClicked(binding.etPassword.text?.toString() ?: "")
            dismiss()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    fun setClickListener(listener: OnPdfClickListener) {
        pdfClickListener = listener
    }
}
