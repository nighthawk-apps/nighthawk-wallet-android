package com.nighthawkapps.wallet.android.ui.util

import android.net.Uri
import android.text.TextUtils
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.ext.twig
import com.nighthawkapps.wallet.android.ext.Const

object DeepLinkUtil {

    fun getSendDeepLinkData(uri: Uri): SendDeepLinkData? {
        // sample deep link: zcash:zs1j29m7zdhhyy2eqrz89l4zhk0angqjh368gqkj2vgdyqmeuultteny36n3qsm47zn8du5sw3ts7f?amount=0.001&memo=c2RrZmp3cw
        try {
            if (TextUtils.isEmpty(uri.scheme)) return null
            if (TextUtils.isEmpty(uri.query)) return null // to check ?amount=
            val query = uri.query ?: ""
            val queryData = query.split("&") // to check memo
            if (queryData.isNullOrEmpty()) return null
            val amountString = queryData[0].replace("${Const.AppConstants.AMOUNT_QUERY}=", "") // amount=0.001 -> 0.001
            val amount = amountString.toBigDecimal().convertZecToZatoshi()
            if (amount > Const.AppConstants.ZEC_MAX_AMOUNT || amount < 0) return null
            var memo: String? = null
            if (queryData.size > 1) { // memo is also available -> memo=c2RrZmp3cw
                memo = queryData[1].replace("${Const.AppConstants.MEMO_QUERY}=", "")
            }
            var uriString = uri.toString()
            uriString = uriString.removePrefix("${uri.scheme}:")
            if (!uriString.startsWith("z", ignoreCase = true)) return null
            uriString = uriString.replace("?$query", "") // address

            twig("DeepLinkUtil: uri is: $uri address is $uriString amount is $amount memo is $memo")

            return SendDeepLinkData(address = uriString, amount = amount, memo = memo)
        } catch (e: Exception) {
            twig("Error in parsing deep link $uri and error is $e")
            return null
        }
    }

    data class SendDeepLinkData(val address: String, val amount: Long, val memo: String?)
}
