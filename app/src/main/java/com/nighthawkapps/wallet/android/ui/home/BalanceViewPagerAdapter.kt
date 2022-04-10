package com.nighthawkapps.wallet.android.ui.home

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class BalanceViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = BalanceFragment.Companion.SectionType.values().size

    override fun createFragment(position: Int): Fragment {
        return BalanceFragment.newInstance(BalanceFragment.Companion.SectionType.values()[position].sectionNo)
    }
}
