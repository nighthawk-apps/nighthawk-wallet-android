package com.nighthawkapps.wallet.android.ext

import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo.IME_ACTION_DONE
import android.widget.EditText
import android.widget.TextView
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.ext.safelyConvertToBigDecimal

fun EditText.onEditorActionDone(block: (EditText) -> Unit) {
    this.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == IME_ACTION_DONE) {
            block(this)
            true
        } else {
            false
        }
    }
}

inline fun EditText.limitDecimalPlaces(max: Int) {
    val editText = this
    addTextChangedListener(object : TextWatcher {
        var previousValue = ""

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // Cache the previous value
            previousValue = text.toString()
        }

        override fun afterTextChanged(s: Editable?) {
            var textStr = text.toString()

            if (textStr.isNotEmpty()) {
                val oldText = text.toString()
                val number = textStr.safelyConvertToBigDecimal()

                if (number != null && number.scale() > 8) {
                    // Prevent the user from adding a new decimal place somewhere in the middle if we're already at the limit
                    if (editText.selectionStart == editText.selectionEnd && editText.selectionStart != textStr.length) {
                        textStr = previousValue
                    } else {
                        textStr = number.toZecStringUniform(8)
                    }
                }

                // Trim leading zeroes
                textStr = textStr.trimStart('0')
                // Append a zero if this results in an empty string or if the first symbol is not a digit
                if (textStr.isEmpty() || !textStr.first().isDigit()) {
                    textStr = "0$textStr"
                }

                // Restore the cursor position
                if (oldText != textStr) {
                    val cursorPosition = editText.selectionEnd
                    editText.setText(textStr)
                    editText.setSelection(
                        (cursorPosition - (oldText.length - textStr.length)).coerceIn(
                            0,
                            editText.text.toString().length
                        )
                    )
                }
            }
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }
    })
}

fun TextView.convertZecToZatoshi(): Long? {
    return try {
        text.toString().replace(',', '.').safelyConvertToBigDecimal()?.convertZecToZatoshi() ?: null
    } catch (t: Throwable) {
        null
    }
}
