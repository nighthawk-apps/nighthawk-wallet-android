package com.tylersuehr.chips

import android.content.Context
import android.text.TextUtils
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.toAppColor
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.feedback.Report.Funnel.Restore
import com.nighthawkapps.wallet.android.ui.MainActivity
import com.nighthawkapps.wallet.android.ui.setup.SeedWordChip
import cash.z.ecc.android.sdk.ext.twig

class SeedWordAdapter :  ChipsAdapter {

    constructor(existingAdapter: ChipsAdapter) : super(existingAdapter.mDataSource, existingAdapter.mEditText, existingAdapter.mOptions)

    val editText = mEditText
    private var onDataSetChangedListener: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == CHIP) SeedWordHolder(SeedWordChipView(parent.context))
        else object : RecyclerView.ViewHolder(mEditText) {}
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
         if (getItemViewType(position) == CHIP) { // Chips
            // Display the chip information on the chip view
             (holder as SeedWordHolder).seedChipView.bind(mDataSource.getSelectedChip(position), position);
        } else {
            val size = mDataSource.selectedChips.size
            mEditText.hint = if (size < 3) {
                mEditText.isEnabled = true
                mEditText.setHintTextColor(R.color.text_light_dimmed.toAppColor())
                val ordinal = when(size) {2 -> "3rd"; 1 -> "2nd"; else -> "1st"}
                "Enter $ordinal seed word"
            } else if(size >= 24) {
                mEditText.setHintTextColor(R.color.zcashGreen.toAppColor())
                mEditText.isEnabled = false
                "done"
            } else {
                mEditText.isEnabled = true
                mEditText.setHintTextColor(R.color.zcashYellow.toAppColor())
                "${size + 1}"
            }
        }
    }

    override fun onChipDataSourceChanged() {
        super.onChipDataSourceChanged()
        onDataSetChangedListener?.invoke()
    }

    fun onDataSetChanged(block: () -> Unit): SeedWordAdapter {
        onDataSetChangedListener = block
        return this
    }

    override fun onKeyboardActionDone(text: String?) {
        if (TextUtils.isEmpty(text)) return

        if (mDataSource.originalChips.firstOrNull { it.title == text } != null) {
            mDataSource.addSelectedChip(DefaultCustomChip(text))
            mEditText.apply {
                postDelayed({
                    setText("")
                    requestFocus()
                }, 50L)
            }
        }
    }

    override fun onKeyboardDelimiter(text: String) {
        if (mDataSource.filteredChips.size > 0) {
            onKeyboardActionDone((mDataSource.filteredChips.first() as SeedWordChip).word)
        }
    }

    private inner class SeedWordHolder(chipView: SeedWordChipView) : ChipsAdapter.ChipHolder(chipView) {
        val seedChipView = super.chipView as SeedWordChipView
    }

    private inner class SeedWordChipView(context: Context) : ChipView(context) {
        private val indexView: TextView = findViewById(R.id.chip_index)

        fun bind(chip: Chip, index: Int) {
            super.inflateFromChip(chip)
            indexView.text = (index + 1).toString()
        }
    }
}

