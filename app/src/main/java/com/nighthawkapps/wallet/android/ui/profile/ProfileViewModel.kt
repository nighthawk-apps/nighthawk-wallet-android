package com.nighthawkapps.wallet.android.ui.profile

import android.widget.Toast
import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.WalletBalance
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.lockbox.LockBox
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ProfileViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    @Inject
    lateinit var lockBox: LockBox

    @Inject
    @Named(Const.Name.APP_PREFS)
    lateinit var prefs: LockBox

    suspend fun getAddress(): String = synchronizer.getAddress()

    // TODO: track this in the app and then fetch. For now, just estimate the blocks per second.
    val bps = 40

    suspend fun getShieldedAddress(): String = synchronizer.getAddress()

    suspend fun getTransparentAddress(): String {
        return synchronizer.getTransparentAddress()
    }

    override fun onCleared() {
        super.onCleared()
        twig("ProfileViewModel cleared!")
    }

    suspend fun fetchUtxos(): Int {
        val address = getTransparentAddress()
        val height: Long = lockBox[Const.Backup.BIRTHDAY_HEIGHT]
            ?: synchronizer.network.saplingActivationHeight.value
        return synchronizer.refreshUtxos(address, BlockHeight.new(synchronizer.network, height))
            ?: 0
    }

    suspend fun getTransparentBalance(): WalletBalance {
        val address = getTransparentAddress()
        return synchronizer.getTransparentBalance(address)
    }

    suspend fun cancel(id: Long) {
        synchronizer.cancelSpend(id)
    }

    fun wipe() {
        synchronizer.stop()
        Toast.makeText(
            NighthawkWalletApp.instance,
            "SUCCESS! Wallet data cleared. Please relaunch to rescan!",
            Toast.LENGTH_LONG
        ).show()
        runBlocking {
            Initializer.erase(
                NighthawkWalletApp.instance,
                NighthawkWalletApp.instance.defaultNetwork
            )
        }
    }

    suspend fun fullRescan() {
        synchronizer.latestBirthdayHeight?.let {
            rewindTo(it)
        }
    }

    suspend fun quickRescan() {
        synchronizer.latestHeight?.let {
            val newHeightValue =
                (it.value - 8064L).coerceAtLeast(synchronizer.network.saplingActivationHeight.value)
            rewindTo(BlockHeight.new(synchronizer.network, newHeightValue))
        }
    }

    private suspend fun rewindTo(targetHeight: BlockHeight) {
        twig("TMP: rewinding to targetHeight $targetHeight")
        synchronizer.rewindToNearestHeight(targetHeight, true)
    }

    fun fullScanDistance(): Long {
        synchronizer.latestHeight?.let { latestHeight ->
            synchronizer.latestBirthdayHeight?.let { latestBirthdayHeight ->
                return (latestHeight.value - latestBirthdayHeight.value).coerceAtLeast(0)
            }
        }
        return 0
    }

    fun quickScanDistance(): Int {
        val latest = synchronizer.latestHeight
        val oneWeek = 60 * 60 * 24 / 75 * 7 // a week's worth of blocks
        val height = BlockHeight.new(
            synchronizer.network,
            ((latest?.value ?: synchronizer.network.saplingActivationHeight.value) - oneWeek)
                .coerceAtLeast(synchronizer.network.saplingActivationHeight.value)
        )
        val foo = runBlocking {
            synchronizer.getNearestRewindHeight(height)
        }
        return ((latest?.value ?: 0) - foo.value).toInt().coerceAtLeast(0)
    }

    fun blocksToMinutesString(blocks: BlockHeight): String {
        val duration = (blocks.value / bps.toDouble()).toDuration(DurationUnit.SECONDS)
        return duration.toString(DurationUnit.MINUTES).replace("m", " minutes")
    }

    fun blocksToMinutesString(blocks: Int): String {
        val duration = (blocks / bps.toDouble()).toDuration(DurationUnit.SECONDS)
        return duration.toString(DurationUnit.MINUTES).replace("m", " minutes")
    }

    fun blocksToMinutesString(blocks: Long): String {
        val duration = (blocks / bps.toDouble()).toDuration(DurationUnit.SECONDS)
        return duration.toString(DurationUnit.MINUTES).replace("m", " minutes")
    }
}
