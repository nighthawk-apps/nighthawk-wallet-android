package com.nighthawkapps.wallet.android.ui.history

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.isShielded
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.twig
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.locale
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.toColoredSpan
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.toAppColor
import com.nighthawkapps.wallet.android.ext.toAppInt
import com.nighthawkapps.wallet.android.ui.MainActivity
import com.nighthawkapps.wallet.android.ui.util.toUtf8Memo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class TransactionViewHolder<T : ConfirmedTransaction>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val indicator = itemView.findViewById<View>(R.id.indicator)
    private val amountText = itemView.findViewById<TextView>(R.id.text_transaction_amount)
    private val topText = itemView.findViewById<TextView>(R.id.text_transaction_top)
    private val bottomText = itemView.findViewById<TextView>(R.id.text_transaction_bottom)
    private val transactionArrow = itemView.findViewById<ImageView>(R.id.image_transaction_arrow)
    private val formatter = SimpleDateFormat(itemView.context.getString(R.string.format_transaction_history_date_time), itemView.context.locale())
    private val iconMemo = itemView.findViewById<ImageView>(R.id.image_memo)

    fun bindTo(transaction: T?) {
        val mainActivity = itemView.context as MainActivity
        mainActivity.lifecycleScope.launch {
            // update view
            var lineOne: CharSequence = ""
            var lineTwo = ""
            var amountZec = ""
            var amountDisplay = ""
            var amountColor: Int = R.color.text_light
            var lineOneColor: Int = R.color.text_light
            var lineTwoColor: Int = R.color.text_light_dimmed
            var indicatorBackground: Int = R.color.text_light_dimmed
            var arrowRotation: Int = R.integer.transaction_arrow_rotation_send
            var arrowBackgroundTint: Int = R.color.text_light
            var isLineOneSpanned = false

            try {
                transaction?.apply {
                    itemView.setOnClickListener {
                        onTransactionClicked(this)
                    }
                    itemView.setOnLongClickListener {
                        onTransactionLongPressed(this)
                        true
                    }
                    amountZec = WalletZecFormmatter.toZecStringShort(value)
                    // TODO: these might be good extension functions
                    val timestamp = formatter.format(blockTimeInSeconds * 1000L)
                    val isMined = blockTimeInSeconds != 0L
                    when {
                        !toAddress.isNullOrEmpty() -> {
                            indicatorBackground =
                                if (isMined) R.color.zcashRed else R.color.zcashGray
                            lineOne = "${
                                if (isMined) str(R.string.transaction_address_you_paid) else str(R.string.transaction_address_paying)
                            } ${toAddress?.toAbbreviatedAddress()}"
                            lineTwo =
                                if (isMined) "${str(R.string.transaction_status_sent)} $timestamp" else str(
                                    R.string.transaction_status_pending
                                )
                            // TODO: this logic works but is sloppy. Find a more robust solution to displaying information about expiration (such as expires in 1 block, etc). Then if it is way beyond expired, remove it entirely. Perhaps give the user a button for that (swipe to dismiss?)
                            if (!isMined && (expiryHeight != null) && (expiryHeight!! < mainActivity.latestHeight ?: -1)) lineTwo =
                                str(R.string.transaction_status_expired)
                            amountDisplay = "- $amountZec"
                            if (isMined) {
                                arrowRotation = R.integer.transaction_arrow_rotation_send
                                amountColor = R.color.transaction_sent
                                if (toAddress.isShielded()) {
                                    lineOneColor = R.color.zcashYellow
                                } else {
                                    toAddress?.toAbbreviatedAddress()?.let {
                                        lineOne = lineOne.toColoredSpan(R.color.zcashBlueDark, it)
                                    }
                                }
                            } else {
                                arrowRotation = R.integer.transaction_arrow_rotation_pending
                            }
                        }
                        toAddress.isNullOrEmpty() && value > 0L && minedHeight > 0 -> {
                            indicatorBackground = R.color.zcashGreen
                            val senderAddress = mainActivity.getSender(transaction)
                            lineOne = "${str(R.string.transaction_received_from)} $senderAddress"
                            lineTwo = "${str(R.string.transaction_received)} $timestamp"
                            amountDisplay = "+ $amountZec"
                            if (senderAddress.isShielded()) {
                                amountColor = R.color.zcashYellow
                                lineOneColor = R.color.zcashYellow
                            } else {
                                senderAddress.toAbbreviatedAddress().let {
                                    lineOne =
                                        if (senderAddress.equals(str(R.string.unknown), true)) {
                                            lineOne.toColoredSpan(R.color.zcashYellow, it)
                                        } else {
                                            lineOne.toColoredSpan(R.color.zcashBlueDark, it)
                                        }
                                }
                            }
                            arrowRotation = R.integer.transaction_arrow_rotation_received
                        }
                        else -> {
                            lineOne = str(R.string.unknown)
                            lineTwo = str(R.string.unknown)
                            amountDisplay = amountZec
                            amountColor = R.color.text_light
                            arrowRotation = R.integer.transaction_arrow_rotation_received
                        }
                    }
                    // sanitize amount
                    if (value < ZcashSdk.MINERS_FEE_ZATOSHI * 10) amountDisplay = "< 0.0001"
                    else if (amountZec.length > 10) { // 10 allows 3 digits to the left and 6 to the right of the decimal
                        amountDisplay = str(R.string.transaction_instruction_tap)
                    }
                }

                topText.text = lineOne
                bottomText.text = lineTwo
                amountText.text = amountDisplay
                amountText.setTextColor(amountColor.toAppColor())
                if (!isLineOneSpanned) {
                    topText.setTextColor(lineOneColor.toAppColor())
                }
                bottomText.setTextColor(lineTwoColor.toAppColor())
                indicator.setBackgroundColor(indicatorBackground.toAppColor())
                transactionArrow.setColorFilter(arrowBackgroundTint.toAppColor())
                transactionArrow.rotation = arrowRotation.toAppInt().toFloat()

                var bottomTextRightDrawable: Drawable? = null
                iconMemo.goneIf(!transaction?.memo.toUtf8Memo().isNotEmpty())
                bottomText.setCompoundDrawablesWithIntrinsicBounds(null, null, bottomTextRightDrawable, null)
            } catch (t: Throwable) {
                twig("Failed to parse the transaction due to $t")
            }
        }
    }

    private fun onTransactionClicked(transaction: ConfirmedTransaction) {
        (itemView.context as MainActivity).apply {
            historyViewModel.selectedTransaction.value = transaction
            safeNavigate(R.id.action_nav_history_to_nav_transaction)
        }
    }

    private fun onTransactionLongPressed(transaction: ConfirmedTransaction) {
        val mainActivity = itemView.context as MainActivity
        toTxId(transaction.rawTransactionId).let {
            mainActivity.copyText(it.toString(), "Transaction ID")
        }
    }

    private fun toTxId(tx: ByteArray?): String? {
        if (tx == null) return null
        val sb = StringBuilder(tx.size * 2)
        for (i in (tx.size - 1) downTo 0) {
            sb.append(String.format("%02x", tx[i]))
        }
        return sb.toString()
    }

    private inline fun str(@StringRes resourceId: Int) = itemView.context.getString(resourceId)
}
