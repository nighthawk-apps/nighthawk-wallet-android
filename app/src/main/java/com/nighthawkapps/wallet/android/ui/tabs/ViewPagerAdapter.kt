package com.nighthawkapps.wallet.android.ui.tabs

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(parent: Fragment, creator: FragmentCreator) :
    FragmentStateAdapter(parent),
    FragmentCreator by creator

interface FragmentCreator {
    fun createFragment(position: Int): Fragment
    fun getItemCount(): Int
}
