package com.nighthawkapps.wallet.android.ui.setup

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentSyncNotificationBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.base.BaseFragment

class SyncNotificationFragment : BaseFragment<FragmentSyncNotificationBinding>() {

    private val syncNotificationViewModel: SyncNotificationViewModel by viewModel()

    override fun inflate(inflater: LayoutInflater): FragmentSyncNotificationBinding {
        return FragmentSyncNotificationBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hitAreaClose.onClickNavBack()
        addSyncOptions()
    }

    private fun addSyncOptions() {
        val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.radio_button_padding_horizontal)
        val paddingVertical = resources.getDimensionPixelSize(R.dimen.radio_button_padding_vertical)
        SyncNotificationViewModel.NotificationSyncPref.values().forEachIndexed { index, syncPref ->
            val radioButton = RadioButton(requireContext()).also {
                it.id = index
                it.isChecked = syncPref == syncNotificationViewModel.getPreSelectedNotificationSyncPref()
                it.text = syncPref.text
                it.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
                it.buttonTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.switch_tracker_color
                    )
                )
                it.setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        twig("Selected local sync notification pref is $syncPref")
                        syncNotificationViewModel.updateNotificationSyncPref(syncPref)
                    }
                }
            }
            binding.radioGroupSyncFrequency.addView(radioButton)
        }
    }
}
