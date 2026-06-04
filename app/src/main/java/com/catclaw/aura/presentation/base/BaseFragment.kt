package com.catclaw.aura.presentation.base

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Base [Fragment] with helpers for lifecycle-aware Flow collection.
 * Feature fragments should extend this class and live under [com.catclaw.aura.ui].
 */
abstract class BaseFragment(@LayoutRes layoutId: Int) : Fragment(layoutId) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onBind(view, savedInstanceState)
    }

    /**
     * Called from [onViewCreated]. Subclasses set up UI and observers here.
     */
    protected open fun onBind(view: View, savedInstanceState: Bundle?) = Unit

    protected fun <T> Flow<T>.collectWithLifecycle(
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
        collector: suspend (T) -> Unit,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(minActiveState) {
                collect(collector)
            }
        }
    }
}
