package com.nighthawkapps.wallet.android.di.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nighthawkapps.wallet.android.ui.base.BaseFragment


inline fun <reified VM : ViewModel> BaseFragment<*>.viewModel() = object : Lazy<VM> {
    val cached: VM? = null
    override fun isInitialized(): Boolean = cached != null
    override val value: VM
        get() = cached
            ?: ViewModelProvider(this@viewModel, scopedFactory<VM>())[VM::class.java]
}

/**
 * Create a view model that is scoped to the lifecycle of the activity.
 *
 * @param isSynchronizerScope true when this view model depends on the Synchronizer. False when this
 * viewModel needs to be created before the synchronizer or otherwise has no dependency on it being
 * available for use.
 */
inline fun <reified VM : ViewModel> BaseFragment<*>.activityViewModel(isSynchronizerScope: Boolean = true) = object : Lazy<VM> {
    val cached: VM? = null
    override fun isInitialized(): Boolean = cached != null
    override val value: VM
        get() {
            return cached
                ?: scopedFactory<VM>(isSynchronizerScope)?.let { factory ->
                    ViewModelProvider(this@activityViewModel.mainActivity!!, factory)[VM::class.java]
                }
        }
}

inline fun <reified VM : ViewModel> BaseFragment<*>.scopedFactory(isSynchronizerScope: Boolean = true): ViewModelProvider.Factory {
    val factory = if (isSynchronizerScope) mainActivity?.synchronizerComponent?.viewModelFactory() else mainActivity?.component?.viewModelFactory()
    return factory ?: throw IllegalStateException("Error: mainActivity should not be null by the time the ${VM::class.java.simpleName} viewmodel is lazily accessed!")
}