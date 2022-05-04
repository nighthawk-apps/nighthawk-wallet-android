package com.nighthawkapps.wallet.android.network.models

import com.google.gson.annotations.SerializedName

data class ZcashPriceApiResponse(
    @SerializedName("zcash")
    val data: Map<String, Double>
)
