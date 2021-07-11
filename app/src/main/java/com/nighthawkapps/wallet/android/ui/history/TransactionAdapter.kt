package com.nighthawkapps.wallet.android.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import com.nighthawkapps.wallet.android.R

class TransactionAdapter<T : ConfirmedTransaction> :
    PagedListAdapter<T, TransactionViewHolder<T>>(
        object : DiffUtil.ItemCallback<T>() {
            override fun areItemsTheSame(
                oldItem: T,
                newItem: T
            ) = oldItem.minedHeight == newItem.minedHeight && oldItem.noteId == newItem.noteId &&
                    // bugfix: distinguish between self-transactions so they don't overwrite each other in the UI // TODO confirm that this is working, as intended
                    ((oldItem.raw == null && newItem.raw == null) || (oldItem.raw != null && newItem.raw != null && oldItem.raw!!.contentEquals(
                newItem.raw!!
            )))

            override fun areContentsTheSame(
                oldItem: T,
                newItem: T
            ) = oldItem == newItem
        }
    ) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = TransactionViewHolder<T>(
        LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
    )

    override fun onBindViewHolder(
        holder: TransactionViewHolder<T>,
        position: Int
    ) = holder.bindTo(getItem(position))

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id ?: -1
    }
}
