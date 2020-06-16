package com.nighthawkapps.wallet.android.ui.detail
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Rect
//import android.view.LayoutInflater
//import android.view.View
//import androidx.recyclerview.widget.RecyclerView
//import cash.z.ecc.android.R
//
//
//class TransactionsDrawableFooter(context: Context) : RecyclerView.ItemDecoration() {
//
//    private var footer: View =
//        LayoutInflater.from(context).inflate(R.layout.footer_transactions, null, false)
//
//    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
//        super.onDraw(c, parent, state!!)
//        footer.measure(
//            View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.AT_MOST),
//            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
//        )
//        // layout basically just gets drawn on the reserved space on top of the first view
//        footer.layout(parent.left, 0, parent.right, footer.measuredHeight)
//        for (i in 0 until parent.childCount) {
//            val view: View = parent.getChildAt(i)
//            if (parent.getChildAdapterPosition(view) == parent.adapter!!.itemCount - 1) {
//                c.save()
//                val height: Int = footer.measuredHeight
//                val top: Int = view.top - height
//                c.translate(0.0f, top.toFloat())
//                footer.draw(c)
//                c.restore()
//                break
//            }
//        }
//    }
//
//    override fun getItemOffsets(
//        outRect: Rect,
//        view: View,
//        parent: RecyclerView,
//        state: RecyclerView.State
//    ) {
//        super.getItemOffsets(outRect, view, parent, state)
//        if (parent.getChildAdapterPosition(view) == parent.adapter!!.itemCount - 1) {
//            outRect.set(0, 0, 0, 150)
//        } else {
//            outRect.setEmpty()
//        }
//    }
//}
