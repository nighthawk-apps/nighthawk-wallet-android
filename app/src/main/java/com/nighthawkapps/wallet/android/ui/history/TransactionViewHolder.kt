package com.nighthawkapps.wallet.android.ui.history

import android.text.format.DateFormat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.isShielded
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.WalletZecFormmatter
import com.nighthawkapps.wallet.android.ext.toAppInt
import com.nighthawkapps.wallet.android.ext.toAppString
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.network.models.CoinMetricsMarketResponse
import com.nighthawkapps.wallet.android.ui.MainActivity
import com.nighthawkapps.wallet.android.ui.util.Utils
import com.nighthawkapps.wallet.android.ui.util.toUtf8Memo
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionViewHolder<T : ConfirmedTransaction>(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val amountText = itemView.findViewById<TextView>(R.id.tvTransactionAmount)
    private val topText = itemView.findViewById<TextView>(R.id.tvTransactionDirection)
    private val bottomText = itemView.findViewById<TextView>(R.id.tvTransactionDate)
    private val transactionArrow = itemView.findViewById<ImageView>(R.id.ivLeftTransactionDirection)
    private val formatter = SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), str(R.string.ns_format_date_time)), Locale.getDefault())
    private val iconTransactionType = itemView.findViewById<ImageView>(R.id.ivTransactionType)
    private val convertedValueTextView = itemView.findViewById<TextView>(R.id.tvTransactionConversionPrice)

    fun bindTo(
        transaction: T?,
        coinMetricsMarketData: CoinMetricsMarketResponse.CoinMetricsMarketData?
    ) {
        val mainActivity = itemView.context as MainActivity
        mainActivity.lifecycleScope.launch {
            // update view
            var lineOne: CharSequence = ""
            var lineTwo = ""
            var amountZec = ""
            var amountDisplay = ""
            var arrowRotation: Int = R.integer.transaction_arrow_rotation_send
            @DrawableRes var iconTransactionDrawable: Int? = null

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
                            lineOne = if (isMined) str(R.string.ns_sent) else str(R.string.ns_sending)
                            lineTwo = if (isMined) timestamp else str(R.string.ns_pending_confirmation)
                            // TODO: this logic works but is sloppy. Find a more robust solution to displaying information about expiration (such as expires in 1 block, etc). Then if it is way beyond expired, remove it entirely. Perhaps give the user a button for that (swipe to dismiss?)
                            if (!isMined && (expiryHeight != null) && (expiryHeight!! < mainActivity.latestHeight ?: -1)) lineTwo =
                                str(R.string.ns_expired)
                            amountDisplay = amountZec
                            if (memo.toUtf8Memo().isNotBlank()) {
                                iconTransactionDrawable = R.drawable.ic_icon_memo
                            }
                            arrowRotation = R.integer.transaction_arrow_rotation_send
                        }
                        toAddress.isNullOrEmpty() && value > 0L && minedHeight > 0 -> {
                            val senderAddress = mainActivity.getSender(transaction)
                            lineOne = str(R.string.ns_received)
                            lineTwo = timestamp
                            amountDisplay = amountZec
                            iconTransactionDrawable = if (senderAddress.equals(R.string.unknown.toAppString(), true) || senderAddress.isShielded()) {
                                R.drawable.ic_icon_shielded
                            } else {
                                R.drawable.ic_icon_transparent
                            }
                            arrowRotation = R.integer.transaction_arrow_rotation_received
                        }
                        else -> {
                            lineOne = str(R.string.ns_unknown)
                            lineTwo = str(R.string.ns_unknown)
                            amountDisplay = amountZec
                            arrowRotation = R.integer.transaction_arrow_rotation_received
                        }
                    }
                    // sanitize amount
                    if (value < ZcashSdk.MINERS_FEE_ZATOSHI * 10) amountDisplay = "< 0.0001"
                }

                topText.text = lineOne
                bottomText.text = lineTwo
                amountText.text = itemView.context.getString(R.string.ns_zec_amount, amountDisplay)
                transactionArrow.rotation = arrowRotation.toAppInt().toFloat()
                convertedValueTextView.text = Utils.getZecConvertedAmountText(amountZec, coinMetricsMarketData)
                iconTransactionDrawable?.let { iconTransactionType.setImageResource(it) }
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
