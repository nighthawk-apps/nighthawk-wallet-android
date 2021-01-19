package com.nighthawkapps.wallet.android.ui.detail

import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.twig
import com.google.gson.Gson
import com.nighthawkapps.wallet.android.ui.util.price.PriceModel
import com.squareup.okhttp.HttpUrl
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WalletDetailViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    val transactions get() = synchronizer.clearedTransactions
    val balance get() = synchronizer.balances
    var priceModel: PriceModel? = null

    private val fetchPriceScope = CoroutineScope(Dispatchers.Main)

    fun initPrice() {
        fetchPriceScope.launch {
            withContext(Dispatchers.IO) {
                if (priceModel == null) {
                    val client = OkHttpClient()
                    val urlBuilder = HttpUrl.parse("https://api.nighthawkwallet.com/price.json").newBuilder()
                    val url = urlBuilder.build().toString()
                    val request: Request = Request.Builder().url(url).build()
                    val gson = Gson()
                    val responseBody = client.newCall(request).execute().body()
                    priceModel = gson.fromJson(responseBody.string(), PriceModel::class.java)
                }
            }
        }
    }

    suspend fun getAddress() = synchronizer.getAddress()

    override fun onCleared() {
        super.onCleared()
        twig("WalletDetailViewModel cleared!")
    }
}
