package com.nighthawkapps.wallet.android.ui.util.seedwords

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.nighthawkapps.wallet.android.R

class SeedWordAdapter(context: Context, seedWordList: List<SeedWord>) : ArrayAdapter<SeedWord>(context, 0, seedWordList) {

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.layout_seed_word, parent, false)
        }
        getItem(position)?.let { seedWord: SeedWord ->
            view?.findViewById<TextView>(R.id.tvSeedNo)?.text = "${seedWord.seedWordNo}."
            view?.findViewById<TextView>(R.id.tvSeedWord)?.text = seedWord.seedWord
        }
        return view!!
    }
}