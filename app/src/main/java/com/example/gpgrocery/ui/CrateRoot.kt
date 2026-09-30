package com.example.gpgrocery.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gpgrocery.AppContainer
import com.example.gpgrocery.CrateApp
import com.example.gpgrocery.data.settings.StoreSettings
import com.example.gpgrocery.ui.navigation.CrateNavHost
import com.example.gpgrocery.ui.onboarding.OnboardingFlow
import com.example.gpgrocery.ui.unlock.UnlockScreen

/**
 * What the app shows: the welcome and set-up screens until there is a store,
 * the lock screen while it is locked, the store itself otherwise. The
 * store's screens keep their place while it is locked.
 */
@Composable
fun CrateRoot(container: AppContainer, settings: StoreSettings) {
    val unlocked by container.session.unlocked.collectAsStateWithLifecycle()
    val saved = rememberSaveableStateHolder()
    when {
        !settings.setupComplete -> OnboardingFlow()
        settings.hasPin && !unlocked -> UnlockScreen(settings)
        else -> saved.SaveableStateProvider("store") { CrateNavHost(settings) }
    }
}

/** A ViewModel built from the app's container, scoped to the current screen. */
@Composable
inline fun <reified VM : ViewModel> crateViewModel(
    key: String? = null,
    crossinline create: CreationExtras.(AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as CrateApp).container
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}
