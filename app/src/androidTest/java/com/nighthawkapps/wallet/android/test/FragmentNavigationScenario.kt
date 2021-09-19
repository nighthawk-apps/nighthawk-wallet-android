package com.nighthawkapps.wallet.android.test

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider

data class FragmentNavigationScenario<T : Fragment>(
    val fragmentScenario: FragmentScenario<T>,
    val navigationController: TestNavHostController
) {

    companion object {
        fun <T : Fragment> new(
            fragmentScenario: FragmentScenario<T>,
            @IdRes currentDestination: Int
        ): FragmentNavigationScenario<T> {
            val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

            fragmentScenario.onFragment {
                navController.setGraph(com.nighthawkapps.wallet.android.R.navigation.mobile_navigation)
                navController.setCurrentDestination(currentDestination)

                Navigation.setViewNavController(it.requireView(), navController)
            }

            return FragmentNavigationScenario(fragmentScenario, navController)
        }
    }
}
