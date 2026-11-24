package com.example

import android.view.View
import android.view.Window
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Reusable Lifecycle + SavedState + ViewModelStore owner for hosting Jetpack Compose (`ComposeView`)
 * inside Android Services (`InputMethodService`, `AccessibilityService`, or WindowManager overlays)
 * where no `ComponentActivity` exists.
 */
class ServiceComposeLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    private var isCreated = false
    private var isDestroyed = false

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    fun onCreate() {
        if (isCreated || isDestroyed) return
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        isCreated = true
    }

    fun onResume() {
        if (isDestroyed) return
        if (!isCreated) {
            onCreate()
        }
        if (lifecycleRegistry.currentState != Lifecycle.State.RESUMED) {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }
    }

    fun onPause() {
        if (isDestroyed || !isCreated) return
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
    }

    fun onDestroy() {
        if (isDestroyed) return
        if (isCreated) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        store.clear()
        isDestroyed = true
    }

    fun attachToView(view: View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
    }

    fun attachToWindowDecorView(window: Window?) {
        val decor = window?.decorView ?: return
        attachToView(decor)
    }
}
