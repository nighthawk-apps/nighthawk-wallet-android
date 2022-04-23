package com.nighthawkapps.wallet.android.network

import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.network.models.CoinMetricsMarketResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET(Const.Network.URL_GET_COIN_METRICS_MARKET_TRADE)
    suspend fun getCoinMetricsMarketTrades(
        @Query("start_time") startTime: String,
        @Query("markets") market: String,
        @Query("paging_from") pagingFrom: String,
        @Query("page_size") pageSize: Int
    ): CoinMetricsMarketResponse
}
