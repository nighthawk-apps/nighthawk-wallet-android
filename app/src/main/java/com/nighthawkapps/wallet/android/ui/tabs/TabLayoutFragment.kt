package com.nighthawkapps.wallet.android.ui.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentTabLayoutBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.receive.ReceiveTabFragment
import com.nighthawkapps.wallet.android.ui.receive.ReceiveViewModel
import com.nighthawkapps.wallet.android.ui.receive.TransparentTabFragment
import kotlinx.coroutines.launch

class TabLayoutFragment : BaseFragment<FragmentTabLayoutBinding>(), FragmentCreator, TabLayout.OnTabSelectedListener {

    private val viewModel: ReceiveViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentTabLayoutBinding =
        FragmentTabLayoutBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaExit.onClickNavBack()
        binding.textTitle.text = "Receive ${getString(R.string.symbol)}"
        binding.viewPager.adapter = ViewPagerAdapter(this, this)
        binding.viewPager.setPageTransformer(ZoomOutPageTransformer())
        binding.tabLayout.addOnTabSelectedListener(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Shielded" else "Transparent"
        }.attach()
        binding.buttonShareAddress.setOnClickListener {
            shareActiveAddress()
        }
    }

    private fun shareActiveAddress() {
        mainActivity?.apply {
            lifecycleScope.launch {
                val address =
                    if (binding.viewPager.currentItem == 1) viewModel.getTranparentAddress() else viewModel.getAddress()
                shareText(address)
            }
        }
    }

    //
    // TabLayout.OnTabSelectedListener implementation
    //

    override fun onTabSelected(tab: TabLayout.Tab) {
        when (tab.position) {
            0 -> setSelectedTab(R.color.zcashYellow)
            1 -> setSelectedTab(R.color.zcashBlueDark)
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {}

    override fun onTabReselected(tab: TabLayout.Tab) {}

    private fun setSelectedTab(@ColorRes color: Int) {
        binding.tabLayout.setSelectedTabIndicatorColor(
            ContextCompat.getColor(requireContext(), color)
        )
        binding.tabLayout.setTabTextColors(
            ContextCompat.getColor(requireContext(), R.color.unselected_tab_grey),
            ContextCompat.getColor(requireContext(), color)
        )
    }

    //
    // FragmentCreator implementation
    //

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ReceiveTabFragment()
            1 -> TransparentTabFragment()
            else -> throw IndexOutOfBoundsException("Cannot create a fragment for index $position")
        }
    }

    override fun getItemCount() = 2

    interface AddressFragment {
        suspend fun getAddress(): String
    }
}

private const val MIN_SCALE = 0.8f
private const val MIN_ALPHA = 0.1f

class ZoomOutPageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            val pageHeight = height
            when {
                position < -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    alpha = 0f
                }
                position <= 1 -> { // [-1,1]
                    // Modify the default slide transition to shrink the page as well
                    val scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position))
                    val vertMargin = pageHeight * (1 - scaleFactor) / 2
                    val horzMargin = pageWidth * (1 - scaleFactor) / 2
                    translationX = if (position < 0) {
                        horzMargin - vertMargin / 2
                    } else {
                        horzMargin + vertMargin / 2
                    }

                    // Scale the page down (between MIN_SCALE and 1)
                    scaleX = scaleFactor
                    scaleY = scaleFactor

                    // Fade the page relative to its size.
                    alpha = (
                            MIN_ALPHA +
                                    (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA))
                            )
                }
                else -> { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    alpha = 0f
                }
            }
        }
    }
}
