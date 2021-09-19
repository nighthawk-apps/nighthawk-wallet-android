package com.nighthawkapps.wallet.android.test

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import java.lang.AssertionError

/**
 * Subclass this for UI tests to ensure they run correctly.  This helps when developers run tests
 * against a physical device that might have gone to sleep.
 */
open class UiTestPrerequisites {
    @Before
    fun verifyScreenOn() {
        if (!isScreenOn()) {
            throw AssertionError("Screen must be on for UI tests to run") // $NON-NLS
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private fun isScreenOn(): Boolean {
        val powerService = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerService.isInteractive
    }
}
