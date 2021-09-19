package com.nighthawkapps.wallet.android.ui.home

import android.content.ComponentName
import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.nighthawkapps.wallet.android.preference.Preferences
import com.nighthawkapps.wallet.android.preference.SharedPreferenceFactory
import com.nighthawkapps.wallet.android.preference.model.get
import com.nighthawkapps.wallet.android.test.FragmentNavigationScenario
import com.nighthawkapps.wallet.android.test.UiTestPrerequisites
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutoshieldingInformationFragmentTest : UiTestPrerequisites() {
    @Test
    @MediumTest
    fun dismiss_returns_home_when_autoshield_not_available() {
        val fragmentNavigationScenario = newScenario(isAutoshieldAvailable = false)

        onView(withId(com.nighthawkapps.wallet.android.R.id.button_autoshield_dismiss)).also {
            it.perform(ViewActions.click())
        }

        assertThat(
            fragmentNavigationScenario.navigationController.currentDestination?.id,
            equalTo(com.nighthawkapps.wallet.android.R.id.nav_home)
        )
    }

    @Test
    @MediumTest
    fun dismiss_starts_autoshield_when_autoshield_available() {
        val fragmentNavigationScenario = newScenario(isAutoshieldAvailable = true)

        onView(withId(com.nighthawkapps.wallet.android.R.id.button_autoshield_dismiss)).also {
            it.perform(ViewActions.click())
        }

        assertThat(
            fragmentNavigationScenario.navigationController.currentDestination?.id,
            equalTo(com.nighthawkapps.wallet.android.R.id.nav_shield_final)
        )
    }

    @Test
    @MediumTest
    fun clicking_more_info_launches_browser() {
        val fragmentNavigationScenario = newScenario(isAutoshieldAvailable = false)

        onView(withId(cash.z.ecc.android.R.id.button_autoshield_more_info)).also {
            it.perform(ViewActions.click())
        }

        assertThat(
            fragmentNavigationScenario.navigationController.currentDestination?.id,
            equalTo(com.nighthawkapps.wallet.android.id.nav_autoshielding_info_details)
        )

        // Note: it is difficult to verify that the browser is launched, because of how the
        // navigation component works.
    }

    @Test
    @MediumTest
    fun starting_fragment_does_not_launch_activities() {
        Intents.init()
        try {
            val fragmentNavigationScenario = newScenario(isAutoshieldAvailable = false)

            // The test framework launches an Activity to host the Fragment under test
            // Since the class name is not a public API, this could break in the future with newer
            // versions of the AndroidX Test libraries.
            intended(
                hasComponent(
                    ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        "androidx.test.core.app.InstrumentationActivityInvoker\$BootstrapActivity"
                    )
                )
            )

            // Verifying that no other Activities (e.g. the link view) are launched without explicit
            // user interaction
            Intents.assertNoUnverifiedIntents()

            assertThat(
                fragmentNavigationScenario.navigationController.currentDestination?.id,
                equalTo(com.nighthawkapps.wallet.android.R.id.nav_autoshielding_info)
            )
        } finally {
            Intents.release()
        }
    }

    @Test
    @MediumTest
    fun display_fragment_sets_preference() {
        newScenario(isAutoshieldAvailable = false)

        assertThat(
            Preferences.isAcknowledgedAutoshieldingInformationPrompt.get(ApplicationProvider.getApplicationContext<Context>()),
            equalTo(true)
        )
    }

    @Test
    @MediumTest
    fun back_navigates_home() {
        val fragmentNavigationScenario = newScenario(isAutoshieldAvailable = false)

        fragmentNavigationScenario.fragmentScenario.onFragment {
            // Probably closest we can come to simulating back with the navigation test framework
            fragmentNavigationScenario.navigationController.navigateUp()
        }

        assertThat(
            fragmentNavigationScenario.navigationController.currentDestination?.id,
            equalTo(com.nighthawkapps.wallet.android.R.id.nav_home)
        )
    }

    companion object {
        private fun newScenario(isAutoshieldAvailable: Boolean): FragmentNavigationScenario<AutoshieldingInformationFragment> {
            // Clear preferences for each scenario, as this most closely reflects how this fragment
            // is used in the app, as it is displayed usually on first launch
            SharedPreferenceFactory.getSharedPreferences(ApplicationProvider.getApplicationContext())
                .edit().clear().apply()

            val scenario = FragmentScenario.launchInContainer(
                AutoshieldingInformationFragment::class.java,
                HomeFragmentDirections.actionNavHomeToAutoshieldingInfo(isAutoshieldAvailable).arguments,
                com.nighthawkapps.wallet.android.R.style.NighthawkTheme,
                null
            )

            return FragmentNavigationScenario.new(
                scenario,
                com.nighthawkapps.wallet.android.R.id.nav_autoshielding_info
            )
        }
    }
}
