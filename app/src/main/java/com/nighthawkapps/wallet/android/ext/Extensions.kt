package com.nighthawkapps.wallet.android.ext

fun Boolean.asString(ifTrue: String = "", ifFalse: String = "") = if(this) ifTrue else ifFalse