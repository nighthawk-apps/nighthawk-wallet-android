package com.nighthawkapps.wallet.android.ui.detail

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nighthawkapps.wallet.android.R

class TransactionsFooter(context: Context) : RecyclerView.ItemDecoration() {

    private var footer: Drawable? = ContextCompat.getDrawable(context, R.drawable.background_footer)
    private val bounds = Rect()

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        c.save()
        val left: Int = 0
        val right: Int = parent.width
        val childCount = parent.childCount
        val adapterItemCount = parent.adapter!!.itemCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            if (parent.getChildAdapterPosition(child) == adapterItemCount - 1) {
                parent.getDecoratedBoundsWithMargins(child, bounds)
                val bottom: Int = bounds.bottom + Math.round(child.translationY)
                footer?.let {
                    val top: Int = bottom - it.intrinsicHeight
                    it.setBounds(left, top, right, bottom)
                    it.draw(c)
                }
            }
        }
        c.restore()
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        if (parent.getChildAdapterPosition(view) == parent.adapter!!.itemCount - 1) {
            outRect.set(0, 0, 0, footer?.intrinsicHeight ?: 0)
        } else {
            outRect.setEmpty()
        }
    }
}
