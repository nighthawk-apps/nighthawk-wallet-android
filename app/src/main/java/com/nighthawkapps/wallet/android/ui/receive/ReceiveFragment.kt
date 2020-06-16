package com.nighthawkapps.wallet.android.ui.receive

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.nighthawkapps.wallet.android.qrecycler.QRecycler
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentReceiveNewBinding
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.onClickNavTo
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.feedback.Report.Tap.*
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.twig
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ReceiveFragment : BaseFragment<FragmentReceiveNewBinding>() {
    override val screen = Report.Screen.RECEIVE

    private val viewModel: ReceiveViewModel by viewModel()

    lateinit var qrecycler: QRecycler

//    lateinit var addressParts: Array<TextView>

    override fun inflate(inflater: LayoutInflater): FragmentReceiveNewBinding =
        FragmentReceiveNewBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        addressParts = arrayOf(
//            text_address_part_1,
//            text_address_part_2,
//            text_address_part_3,
//            text_address_part_4,
//            text_address_part_5,
//            text_address_part_6,
//            text_address_part_7,
//            text_address_part_8
//        )
        binding.buttonScan.setOnClickListener {
            mainActivity?.maybeOpenScan(R.id.action_nav_receive_to_nav_scan).also { tapped(RECEIVE_SCAN) }
        }
        binding.backButtonHitArea.onClickNavBack() { tapped(RECEIVE_BACK) }
    }

    override fun onAttach(context: Context) {
        qrecycler = QRecycler() // inject! :)
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        resumedScope.launch {
            onAddressLoaded(viewModel.getAddress())
        }
    }

    private fun onAddressLoaded(address: String) {
        twig("address loaded:  $address length: ${address.length}")
        qrecycler.load(address)
            .withQuietZoneSize(3)
            .withCorrectionLevel(QRecycler.CorrectionLevel.MEDIUM)
            .into(binding.receiveQrCode)

        binding.receiveAddress.text = address.toAbbreviatedAddress(12, 12)

//        address.distribute(8) { i, part ->
//            setAddressPart(i, part)
//        }
    }

    private fun <T> String.distribute(chunks: Int, block: (Int, String) -> T) {
        val charsPerChunk = length / 8.0
        val wholeCharsPerChunk = charsPerChunk.toInt()
        val chunksWithExtra = ((charsPerChunk - wholeCharsPerChunk) * chunks).roundToInt()
        repeat(chunks) { i ->
            val part = if (i < chunksWithExtra) {
                substring(i * (wholeCharsPerChunk + 1), (i + 1) * (wholeCharsPerChunk + 1))
            } else {
                substring(i * wholeCharsPerChunk + chunksWithExtra, (i + 1) * wholeCharsPerChunk + chunksWithExtra)
            }
            block(i, part)
        }
    }

//    private fun setAddressPart(index: Int, addressPart: String) {
//        Log.e("TWIG", "setting address for part $index) $addressPart")
//        val thinSpace = "\u2005" // 0.25 em space
//        val textSpan = SpannableString("${index + 1}$thinSpace$addressPart")
//
//        textSpan.setSpan(AddressPartNumberSpan(), 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//
//        addressParts[index].text = textSpan
//    }
}