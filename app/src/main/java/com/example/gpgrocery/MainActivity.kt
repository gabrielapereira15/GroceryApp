package com.example.gpgrocery

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gpgrocery.data.model.ThemeMode
import com.example.gpgrocery.data.settings.StoreSettings
import com.example.gpgrocery.ui.CrateRoot
import com.example.gpgrocery.ui.theme.CrateTheme

/**
 * The only activity. A FragmentActivity because the fingerprint prompt
 * needs one; everything on screen is Compose.
 */
class MainActivity : FragmentActivity() {
    private val container get() = (application as CrateApp).container
    private var settingsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !settingsLoaded }

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle<StoreSettings?>(initialValue = null)
            val current = settings ?: return@setContent
            SideEffect { settingsLoaded = true }

            val dark = when (current.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            CrateTheme(darkTheme = dark) {
                CrateRoot(container = container, settings = current)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // A till left unattended locks itself: back after five minutes away means the PIN again.
        container.session.cameBack(SystemClock.elapsedRealtime(), LOCK_AFTER_MILLIS)
    }

    override fun onStop() {
        super.onStop()
        container.session.wentAway(SystemClock.elapsedRealtime())
    }

    private companion object {
        const val LOCK_AFTER_MILLIS = 5 * 60 * 1000L
    }
}
