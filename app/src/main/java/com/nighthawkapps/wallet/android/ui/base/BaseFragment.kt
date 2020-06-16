package com.nighthawkapps.wallet.android.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.ui.MainActivity
import kotlinx.coroutines.*

abstract class BaseFragment<T : ViewBinding> : Fragment() {
    val mainActivity: MainActivity? get() = activity as MainActivity?

    lateinit var binding: T

    lateinit var resumedScope: CoroutineScope

    open val screen: Report.Screen? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = inflate(inflater)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mainActivity?.reportScreen(screen)
        resumedScope = lifecycleScope.coroutineContext.let {
            CoroutineScope(Dispatchers.Main + SupervisorJob(it[Job]))
        }
    }

    override fun onPause() {
        super.onPause()
        resumedScope.cancel()
    }

    // inflate is static in the ViewBinding class so we can't handle this ourselves
    // each fragment must call FragmentMyLayoutBinding.inflate(inflater)
    abstract fun inflate(@NonNull inflater: LayoutInflater): T

    fun onBackPressNavTo(navResId: Int, block: (() -> Unit) = {}) {
        mainActivity?.onFragmentBackPressed(this) {
            block()
            mainActivity?.safeNavigate(navResId)
        }
    }

    fun tapped(tap: Report.Tap) {
        mainActivity?.reportTap(tap)
    }

}