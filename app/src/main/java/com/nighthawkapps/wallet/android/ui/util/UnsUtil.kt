package com.nighthawkapps.wallet.android.ui.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.nighthawkapps.wallet.android.ext.twig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import okhttp3.OkHttpClient
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

data class Domain(
    val records: ZECRecord? = null
)
data class ZECRecord(
    @SerializedName("crypto.ZEC.address") val zecAddress: String? = null
)

data class TLD(
    val tlds: List<String>
)

class UnsUtil {
    private var supportedTLDs = emptyList<String>()
    private val client = OkHttpClient()
    private val token: String = "bKmEKAC4HJUEDNlnoYITvXYuhrIshFsa"
    private val resolutionService = "https://unstoppabledomains.g.alchemy.com/domains/"
    private val tldAPI = "https://resolve.unstoppabledomains.com/supported_tlds"
    suspend fun isValidUNSAddress(address: String): String? {
        val domain = prepareDomain(address)
        if (isValidTLD(domain)) {
            val urlBuilder = "$resolutionService$domain".toHttpUrlOrNull()?.newBuilder()
            val url = urlBuilder?.build().toString()
            val request: Request = Request.Builder()
                .addHeader("Authorization", "Bearer $token").url(url).build()
            val gson = Gson()

            return suspendCoroutine { cont ->
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        cont.resumeWith(Result.failure(e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (response.isSuccessful.not()) {
                                cont.resumeWith(Result.failure(IOException("Unexpected code $response")))
                            } else {
                                val responseBody = response.body
                                if (responseBody != null) {
                                    try {
                                        val data = gson.fromJson(responseBody.string(), Domain::class.java).records?.zecAddress
                                        cont.resumeWith(Result.success(data))
                                    } catch (e: IOException) {
                                        twig("uns + ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                })
            }
        }
        return null
    }
    private suspend fun isValidTLD(domain: String): Boolean {
        if (supportedTLDs.isEmpty()) {
            supportedTLDs = getTLDs()
        }

        return supportedTLDs.firstOrNull { s -> domain.contains(s) } != null || supportedTLDs.isEmpty()
    }

    private suspend fun getTLDs(): List<String> {
        val urlBuilder = tldAPI.toHttpUrlOrNull()?.newBuilder()
        val url = urlBuilder?.build().toString()
        val request: Request = Request.Builder().url(url).build()
        val gson = Gson()
        return suspendCoroutine { cont ->
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (response.isSuccessful.not()) {
                            cont.resumeWith(Result.failure(IOException("Unexpected code $response")))
                        } else {
                            val responseBody = response.body
                            if (responseBody != null) {
                                try {
                                    val data = gson.fromJson(responseBody.string(), TLD::class.java).tlds
                                    cont.resumeWith(Result.success(data))
                                } catch (e: IOException) {
                                    twig("uns + ${e.message}")
                                }
                            }
                        }
                    }
                }
            })
        }
    }
    private fun prepareDomain(address: String): String {
        return address.lowercase()
    }
}
