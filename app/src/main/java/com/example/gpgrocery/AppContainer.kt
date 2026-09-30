package com.example.gpgrocery

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.gpgrocery.data.ProductRepository
import com.example.gpgrocery.data.RestockRepository
import com.example.gpgrocery.data.SalesRepository
import com.example.gpgrocery.data.SupplierRepository
import com.example.gpgrocery.data.db.CrateDatabase
import com.example.gpgrocery.data.photos.PhotoStore
import com.example.gpgrocery.data.sample.SampleStore
import com.example.gpgrocery.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Clock

private val Context.storeDataStore by preferencesDataStore(name = "store")

/**
 * Everything the screens need, built once for the whole app. Small enough
 * that plain constructors do the job a dependency-injection framework would.
 */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    val clock: Clock = Clock.systemDefaultZone()
    private val now: () -> Long = { clock.millis() }

    val database: CrateDatabase by lazy { CrateDatabase.build(app) }
    val settings = SettingsRepository(app.storeDataStore, now)
    val products by lazy { ProductRepository(database, now) }
    val suppliers by lazy { SupplierRepository(database) }
    val sales by lazy { SalesRepository(database, now) }
    val restocks by lazy { RestockRepository(database, now) }
    val photos by lazy { PhotoStore(app) }
    val session = Session()

    /** Loads Maple Street Market and opens it. */
    suspend fun openSampleStore() {
        withContext(Dispatchers.IO) { SampleStore(database).load(clock.instant(), clock.zone) }
        // Unlocked first: the moment set-up is saved, the store opens rather than asking for the PIN just chosen.
        session.unlock()
        settings.completeSetup(SampleStore.STORE_NAME, SampleStore.OWNER_NAME, SampleStore.PIN, sampleStore = true)
    }

    /** Starts an empty store of the owner's own. */
    suspend fun openNewStore(storeName: String, ownerName: String, pin: String) {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        photos.clear()
        session.unlock()
        settings.completeSetup(storeName, ownerName, pin, sampleStore = false)
    }

    /** Swaps the current data for a fresh copy of the sample store, keeping the PIN and preferences. */
    suspend fun reloadSampleData() {
        withContext(Dispatchers.IO) { SampleStore(database).load(clock.instant(), clock.zone) }
        photos.clear()
    }

    /** Forgets everything on this phone and goes back to the welcome screen. */
    suspend fun eraseEverything() {
        withContext(Dispatchers.IO) { database.clearAllTables() }
        photos.clear()
        settings.erase()
        session.lock()
    }
}

/** Whether the store is unlocked right now. Held in memory only: a restart locks it again. */
class Session {
    private val unlockedState = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = unlockedState.asStateFlow()
    private var awaySince = 0L

    fun unlock() {
        unlockedState.value = true
    }

    fun lock() {
        unlockedState.value = false
    }

    /** Out of sight, at [at] on the elapsed-time clock. */
    fun wentAway(at: Long) {
        awaySince = at
    }

    /** Back in sight: locks when it was away for longer than [lockAfterMillis]. */
    fun cameBack(at: Long, lockAfterMillis: Long) {
        if (awaySince != 0L && at - awaySince > lockAfterMillis) lock()
        awaySince = 0L
    }
}
