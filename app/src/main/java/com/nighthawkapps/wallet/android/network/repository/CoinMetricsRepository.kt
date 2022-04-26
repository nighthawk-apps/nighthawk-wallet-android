package com.nighthawkapps.wallet.android.network.repository

import com.nighthawkapps.wallet.android.network.ApiService
import com.nighthawkapps.wallet.android.network.models.CoinMetricsMarketResponse
import com.nighthawkapps.wallet.android.ui.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CoinMetricsRepository @Inject constructor(private val apiService: ApiService) {

    suspend fun getZecMarketData(
        startTime: String,
        market: String,
        pagingFrom: String = "end",
        pageSize: Int = 1
    ): Flow<Resource<CoinMetricsMarketResponse>> {
        return flow {
            emit(Resource.Loading(null))
            try {
                val response = apiService.getCoinMetricsMarketTrades(startTime, market, pagingFrom, pageSize)
                emit(Resource.Success(response))
            } catch (e: Exception) {
                emit(Resource.Error(null, e.message ?: "Error while getting coin metrics market data"))
            }
        }
    }
}
