package com.nighthawkapps.wallet.android.network.models

import com.google.gson.annotations.SerializedName

data class CoinMetricsMarketResponse(
    val data: List<CoinMetricsMarketData>
) {
    data class CoinMetricsMarketData(
        val market: String,
        val time: String,
        @SerializedName("coin_metrics_id")
        val coinMetricsId: String,
        val price: String
    )
}
