package com.nighthawkapps.wallet.android.ui.util

sealed class Resource<T>(val status: Status, data: T?, message: String?) {
    data class Success<T>(val data: T) :
        Resource<T>(status = Status.SUCCESS, data = data, message = null)

    data class Error<T>(val data: T?, val message: String) :
        Resource<T>(status = Status.ERROR, data = data, message = message)

    data class Loading<T>(val data: T?) :
        Resource<T>(status = Status.LOADING, data = data, message = null)
}

enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}