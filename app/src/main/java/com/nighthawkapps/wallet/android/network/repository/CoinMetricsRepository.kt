package com.nighthawkapps.wallet.android.network.repository

import com.nighthawkapps.wallet.android.ext.Const
import com.nighthawkapps.wallet.android.network.ApiService
import com.nighthawkapps.wallet.android.network.models.ZcashPriceApiResponse
import com.nighthawkapps.wallet.android.ui.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CoinMetricsRepository @Inject constructor(private val apiService: ApiService) {

    suspend fun getZecMarketData(
        currency: String,
        id: String = Const.Network.ZCASH_ID
    ): Flow<Resource<ZcashPriceApiResponse>> {
        return flow {
            emit(Resource.Loading(null))
            try {
                val response = apiService.getZcashPrice(id, currency)
                emit(Resource.Success(response))
            } catch (e: Exception) {
                emit(Resource.Error(null, e.message ?: "Error while getting coin metrics market data"))
            }
        }
    }
}
