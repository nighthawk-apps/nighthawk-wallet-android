package com.nighthawkapps.wallet.android.ui.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.twig
import com.google.gson.Gson
import com.nighthawkapps.wallet.android.ui.util.price.PriceModel
import com.squareup.okhttp.HttpUrl
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.ResponseBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.io.IOException
import java.lang.IllegalStateException
import javax.inject.Inject

class WalletDetailViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var synchronizer: Synchronizer

    val transactions get() = synchronizer.clearedTransactions
    val balance get() = synchronizer.balances
    var priceModel: PriceModel? = null

    private val fetchPriceScope = CoroutineScope(Dispatchers.IO)

    fun initPrice() {
        fetchPriceScope.launch {
            supervisorScope {
                if (priceModel == null) {
                    val client = OkHttpClient()
                    val urlBuilder = HttpUrl.parse("https://api.lightwalletd.com/price.json").newBuilder()
                    val url = urlBuilder.build().toString()
                    val request: Request = Request.Builder().url(url).build()
                    val gson = Gson()
                    var responseBody: ResponseBody? = null

                    try {
                        responseBody = client.newCall(request).execute().body()
                    } catch (e: IOException) {
                        Log.e("initPrice + ${e.message}", "$responseBody")
                    } catch (e: IllegalStateException) {
                        Log.e("initPrice + ${e.message}", "$responseBody")
                    }
                    if (responseBody != null) {
                        try {
                            priceModel = gson.fromJson(responseBody.string(), PriceModel::class.java)
                        } catch (e: IOException) {
                            Log.e("initPrice + ${e.message}", "$priceModel")
                        }
                    }
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
