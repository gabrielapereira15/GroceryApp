package com.example.gpgrocery.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.gpgrocery.data.model.ThemeMode
import com.example.gpgrocery.domain.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

data class StoreSettings(
    val setupComplete: Boolean = false,
    val storeName: String = "",
    val ownerName: String = "",
    /** 1300 = 13% (Ontario HST). */
    val taxRateBasisPoints: Int = DEFAULT_TAX_RATE,
    val pin: PinHasher.Stored? = null,
    val biometricEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val sampleStore: Boolean = false,
) {
    val hasPin: Boolean get() = pin != null

    companion object {
        const val DEFAULT_TAX_RATE = 1300
    }
}

sealed interface PinCheck {
    data object Correct : PinCheck
    data class Wrong(val attemptsLeft: Int) : PinCheck
    data class Paused(val untilMillis: Long) : PinCheck
}

/**
 * The store's own details and preferences, kept in DataStore in the app's
 * private storage. Everything else the app knows is in the Room database.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val settings: Flow<StoreSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSettings() }

    suspend fun current(): StoreSettings = settings.first()

    suspend fun completeSetup(storeName: String, ownerName: String, pin: String, sampleStore: Boolean) {
        val stored = withContext(Dispatchers.Default) { PinHasher.create(pin) }
        dataStore.edit {
            it.clear()
            it[SETUP_COMPLETE] = true
            it[STORE_NAME] = storeName.trim()
            it[OWNER_NAME] = ownerName.trim()
            it[TAX_RATE] = StoreSettings.DEFAULT_TAX_RATE
            it[SAMPLE_STORE] = sampleStore
            it.putPin(stored)
        }
    }

    /**
     * Checks a PIN. After [MAX_ATTEMPTS] misses in a row the screen pauses for
     * [PAUSE_MILLIS]; the count starts again after a correct PIN.
     */
    suspend fun checkPin(pin: String): PinCheck {
        val prefs = dataStore.data.first()
        val pausedUntil = prefs[PAUSED_UNTIL] ?: 0L
        if (pausedUntil > now()) return PinCheck.Paused(pausedUntil)
        val stored = prefs.toSettings().pin ?: return PinCheck.Correct
        val correct = withContext(Dispatchers.Default) { PinHasher.matches(pin, stored) }
        if (correct) {
            dataStore.edit {
                it.remove(FAILED_ATTEMPTS)
                it.remove(PAUSED_UNTIL)
            }
            return PinCheck.Correct
        }
        var result: PinCheck = PinCheck.Wrong(0)
        dataStore.edit {
            val failed = (it[FAILED_ATTEMPTS] ?: 0) + 1
            if (failed >= MAX_ATTEMPTS) {
                val until = now() + PAUSE_MILLIS
                it[FAILED_ATTEMPTS] = 0
                it[PAUSED_UNTIL] = until
                result = PinCheck.Paused(until)
            } else {
                it[FAILED_ATTEMPTS] = failed
                result = PinCheck.Wrong(MAX_ATTEMPTS - failed)
            }
        }
        return result
    }

    suspend fun changePin(pin: String) {
        val stored = withContext(Dispatchers.Default) { PinHasher.create(pin) }
        dataStore.edit { it.putPin(stored) }
    }

    suspend fun updateStore(storeName: String, ownerName: String, taxRateBasisPoints: Int) {
        dataStore.edit {
            it[STORE_NAME] = storeName.trim()
            it[OWNER_NAME] = ownerName.trim()
            it[TAX_RATE] = taxRateBasisPoints
        }
    }

    suspend fun setBiometric(enabled: Boolean) = dataStore.edit { it[BIOMETRIC] = enabled }

    suspend fun setTheme(mode: ThemeMode) = dataStore.edit { it[THEME] = mode.name }

    /** Forgets the store, PIN included. The database is cleared separately. */
    suspend fun erase() {
        dataStore.edit { it.clear() }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.putPin(stored: PinHasher.Stored) {
        this[PIN_HASH] = stored.hash
        this[PIN_SALT] = stored.salt
        this[PIN_ITERATIONS] = stored.iterations
        remove(FAILED_ATTEMPTS)
        remove(PAUSED_UNTIL)
    }

    private fun Preferences.toSettings(): StoreSettings {
        val hash = this[PIN_HASH]
        val salt = this[PIN_SALT]
        return StoreSettings(
            setupComplete = this[SETUP_COMPLETE] ?: false,
            storeName = this[STORE_NAME].orEmpty(),
            ownerName = this[OWNER_NAME].orEmpty(),
            taxRateBasisPoints = this[TAX_RATE] ?: StoreSettings.DEFAULT_TAX_RATE,
            pin = if (hash != null && salt != null) PinHasher.Stored(hash, salt, this[PIN_ITERATIONS] ?: PinHasher.ITERATIONS) else null,
            biometricEnabled = this[BIOMETRIC] ?: false,
            themeMode = this[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
            sampleStore = this[SAMPLE_STORE] ?: false,
        )
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        const val PAUSE_MILLIS = 30_000L

        private val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
        private val STORE_NAME = stringPreferencesKey("store_name")
        private val OWNER_NAME = stringPreferencesKey("owner_name")
        private val TAX_RATE = intPreferencesKey("tax_rate_bp")
        private val PIN_HASH = stringPreferencesKey("pin_hash")
        private val PIN_SALT = stringPreferencesKey("pin_salt")
        private val PIN_ITERATIONS = intPreferencesKey("pin_iterations")
        private val FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        private val PAUSED_UNTIL = longPreferencesKey("pin_paused_until")
        private val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        private val THEME = stringPreferencesKey("theme_mode")
        private val SAMPLE_STORE = booleanPreferencesKey("sample_store")
    }
}
