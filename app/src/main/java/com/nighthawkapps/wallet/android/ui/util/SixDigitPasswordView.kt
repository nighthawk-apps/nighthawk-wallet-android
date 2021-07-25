package com.nighthawkapps.wallet.android.ui.util

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton
import android.widget.LinearLayout
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.CustomSixDigitPasswordViewBinding
import com.nighthawkapps.wallet.android.ext.viewBinding

class SixDigitPasswordView(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private val binding = viewBinding(CustomSixDigitPasswordViewBinding::inflate)

    private lateinit var digitViewList: MutableList<ImageButton>
    private var password: MutableList<Int> = mutableListOf()

    init {
        orientation = HORIZONTAL
        initViewList()
    }

    private fun initViewList() {
        with(binding) {
            digitViewList = mutableListOf(
                digit1,
                digit2,
                digit3,
                digit4,
                digit5,
                digit6
            )
        }
    }

    fun onNewDigit(digit: Int): Boolean {
        return if (password.size < PASSWORD_LENGTH) {
            digitViewList[password.size].setImageResource(R.drawable.filled_password_digit)
            digitViewList[password.size].isPressed = true // this is for ripple effect
            digitViewList[password.size].isPressed = false
            password.add(digit)
            true
        } else {
            false
        }
    }

    fun getPasswordSize(): Int {
        return password.size
    }

    fun removeLastDigit() {
        if (password.size > 0) {
            password.removeAt(password.size - 1)
            digitViewList[password.size].setImageResource(R.drawable.unfilled_password_digit)
        }
    }

    fun getPassword(): String {
        return password.toString()
    }

    fun clear() {
        password.clear()
        digitViewList.forEach { digitImageView ->
            digitImageView.setImageResource(R.drawable.unfilled_password_digit)
        }
    }

    companion object {
        const val PASSWORD_LENGTH = 6
    }
}
