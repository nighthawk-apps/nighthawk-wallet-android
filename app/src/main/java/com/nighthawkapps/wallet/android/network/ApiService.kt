package com.nighthawkapps.wallet.android.network

import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.network.models.ZcashPriceApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET(Const.Network.URL_GET_PRICE)
    suspend fun getZcashPrice(
        @Query("ids") id: String,
        @Query("vs_currencies") currency: String
    ): ZcashPriceApiResponse
}
