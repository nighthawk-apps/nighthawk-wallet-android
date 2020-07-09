package com.nighthawkapps.wallet.android.ui.detail

import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.isShielded
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.toAppColor
import com.nighthawkapps.wallet.android.ui.MainActivity
import com.nighthawkapps.wallet.android.ui.util.INCLUDE_MEMO_PREFIX
import com.nighthawkapps.wallet.android.ui.util.toUtf8Memo
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionViewHolder<T : ConfirmedTransaction>(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    private val indicator = itemView.findViewById<View>(R.id.indicator)
    private val amountText = itemView.findViewById<TextView>(R.id.text_transaction_amount)
    private val topText = itemView.findViewById<TextView>(R.id.text_transaction_top)
    private val bottomText = itemView.findViewById<TextView>(R.id.text_transaction_bottom)
    private val shieldIcon = itemView.findViewById<View>(R.id.image_shield)
    private val formatter = SimpleDateFormat("M/d h:mma", Locale.getDefault())
    private val addressRegex = """zs\d\w{65,}""".toRegex()

    fun bindTo(transaction: T?) {
        (itemView.context as MainActivity).lifecycleScope.launch {
            // update view
            var lineOne: String = ""
            var lineTwo: String = ""
            var amountZec: String = ""
            var amountDisplay: String = ""
            var amountColor: Int = R.color.text_light_dimmed
            var lineOneColor: Int = R.color.text_light
            var lineTwoColor: Int = R.color.text_light_dimmed
            var indicatorBackground: Int = R.drawable.background_indicator_unknown

            transaction?.apply {
                itemView.setOnClickListener {
                    onTransactionClicked(this)
                }
                itemView.setOnLongClickListener {
                    onTransactionLongPressed(this)
                    true
                }
                amountZec = value.convertZatoshiToZecString()
                // TODO: these might be good extension functions
                val timestamp = formatter.format(blockTimeInSeconds * 1000L)
                val isMined = blockTimeInSeconds != 0L
                when {
                    !toAddress.isNullOrEmpty() -> {
                        lineOne = "You paid ${toAddress?.toAbbreviatedAddress()}"
                        lineTwo = if (isMined) "Sent $timestamp" else "Pending confirmation"
                        amountDisplay = "- $amountZec"
                        if (isMined) {
                            amountColor = R.color.zcashRed
                            indicatorBackground = R.drawable.background_indicator_outbound
                        } else {
                            lineOneColor = R.color.text_light_dimmed
                            lineTwoColor = R.color.text_light
                        }
                    }
                    toAddress.isNullOrEmpty() && value > 0L && minedHeight > 0 -> {
                        lineOne = getSender(transaction)
                        lineTwo = "Received $timestamp"
                        amountDisplay = "+ $amountZec"
                        amountColor = R.color.zcashGreen
                        indicatorBackground = R.drawable.background_indicator_inbound
                    }
                    else -> {
                        lineOne = "Unknown"
                        lineTwo = "Unknown"
                        amountDisplay = "$amountZec"
                        amountColor = R.color.text_light
                    }
                }
                // sanitize amount
                if (value < ZcashSdk.MINERS_FEE_ZATOSHI) amountDisplay = "< 0.001"
                else if (amountZec.length > 10) { // 10 allows 3 digits to the left and 6 to the right of the decimal
                    amountDisplay = "tap to view"
                }
            }

            topText.text = lineOne
            bottomText.text = lineTwo
            amountText.text = amountDisplay
            amountText.setTextColor(amountColor.toAppColor())
            topText.setTextColor(lineOneColor.toAppColor())
            bottomText.setTextColor(lineTwoColor.toAppColor())
            val context = itemView.context
            indicator.background = context.resources.getDrawable(indicatorBackground)
            shieldIcon.goneIf((transaction?.raw != null || transaction?.expiryHeight != null) && !transaction?.toAddress.isShielded())
        }
    }

    private suspend fun getSender(transaction: ConfirmedTransaction): String {
        val memo = transaction.memo.toUtf8Memo()
        return when {
            memo.contains(INCLUDE_MEMO_PREFIX) -> {
                val address =
                    memo.split(INCLUDE_MEMO_PREFIX)[1].trim().validateAddress() ?: "Unknown"
                "${address.toAbbreviatedAddress()} paid you"
            }
            memo.contains("eply to:") -> {
                val address = memo.split("eply to:")[1].trim().validateAddress() ?: "Unknown"
                "${address.toAbbreviatedAddress()} paid you"
            }
            memo.contains("zs") -> {
                val who =
                    extractAddress(memo).validateAddress()?.toAbbreviatedAddress() ?: "Unknown"
                "$who paid you"
            }
            else -> "Unknown paid you"
        }
    }

    private fun extractAddress(memo: String?) =
        addressRegex.findAll(memo ?: "").lastOrNull()?.value

    private fun onTransactionClicked(transaction: ConfirmedTransaction) {
        val txId = transaction.rawTransactionId.toTxId()
        val detailsMessage: String = "Zatoshi amount: ${transaction.value}\n\n" +
                "Transaction: $txId" +
                "${if (transaction.toAddress != null) "\n\nTo: ${transaction.toAddress}" else ""}" +
                "${if (transaction.memo != null) "\n\nMemo: \n${String(
                    transaction.memo!!,
                    Charset.forName("UTF-8")
                )}" else ""}"

        MaterialAlertDialogBuilder(itemView.context)
            .setMessage(detailsMessage)
            .setTitle("Transaction Details")
            .setCancelable(true)
            .setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Copy TX") { dialog, _ ->
                (itemView.context as MainActivity).copyText(txId, "Transaction Id")
                dialog.dismiss()
            }
            .show()
    }

    private fun onTransactionLongPressed(transaction: ConfirmedTransaction) {
        (transaction.toAddress ?: extractAddress(transaction.memo.toUtf8Memo()))?.let {
            (itemView.context as MainActivity).copyText(it, "Transaction Address")
        }
    }

    private suspend fun String?.validateAddress(): String? {
        if (this == null) return null
        return if ((itemView.context as MainActivity).isValidAddress(this)) this else null
    }
}

private fun ByteArray.toTxId(): String {
    val sb = StringBuilder(size * 2)
    for (i in (size - 1) downTo 0) {
        sb.append(String.format("%02x", this[i]))
    }
    return sb.toString()
}
