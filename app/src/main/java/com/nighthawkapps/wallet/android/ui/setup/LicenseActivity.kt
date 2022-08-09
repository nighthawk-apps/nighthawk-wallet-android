package com.nighthawkapps.wallet.android.ui.setup

import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.aboutlibraries.ui.LibsActivity

import android.os.Bundle
import com.nighthawkapps.wallet.android.R

class LicenseActivity : LibsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        intent = LibsBuilder()
            .withActivityTitle(getString(R.string.ns_view_license))
            .intent(this)
        super.onCreate(savedInstanceState)
    }
}