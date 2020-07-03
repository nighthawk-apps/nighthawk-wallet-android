package com.nighthawkapps.wallet.android.ui.detail

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.nighthawkapps.wallet.android.R

class TransactionsFooter(context: Context) : RecyclerView.ItemDecoration() {

    private var footer: Drawable = context.resources.getDrawable(R.drawable.background_footer)
    val bounds = Rect()

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
                val top: Int = bottom - footer.intrinsicHeight
                footer.setBounds(left, top, right, bottom)
                footer.draw(c)
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
            outRect.set(0, 0, 0, footer.intrinsicHeight)
        } else {
            outRect.setEmpty()
        }
    }
}
