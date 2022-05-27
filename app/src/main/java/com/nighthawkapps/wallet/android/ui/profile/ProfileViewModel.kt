package com.nighthawkapps.wallet.android.ui.profile

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ProfileViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    suspend fun getAddress(): String = synchronizer.getAddress()

    override fun onCleared() {
        super.onCleared()
        twig("ProfileViewModel cleared!")
    }

    fun scanDistance() =
        (synchronizer.latestHeight - synchronizer.latestBirthdayHeight).coerceAtLeast(0)

    fun blocksToMinutesString(blocks: Int): String {
        // TODO: track bps in the app and then fetch. For now, just estimate the blocks per second.
        val bps = 160
        val duration = (blocks / bps.toDouble()).toDuration(DurationUnit.SECONDS)
        return duration.toString(DurationUnit.MINUTES).replace("m", if (duration.inSeconds < 90) " minute" else " minutes")
    }

    fun wipe() {
        viewModelScope.launch {
            synchronizer.stop()
            Toast.makeText(
                NighthawkWalletApp.instance,
                "SUCCESS! Wallet data cleared. Please relaunch to rescan!",
                Toast.LENGTH_LONG
            ).show()
            Initializer.erase(
                NighthawkWalletApp.instance,
                NighthawkWalletApp.instance.defaultNetwork
            )
        }
    }

    suspend fun fullRescan() {
        rewindTo(synchronizer.latestBirthdayHeight)
    }

    private suspend fun rewindTo(targetHeight: Int) {
        twig("TMP: rewinding to $targetHeight")
        synchronizer.rewindToNearestHeight(targetHeight, true)
    }
}
