package com.example.gpgrocery.data

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import com.example.gpgrocery.data.model.ThemeMode
import com.example.gpgrocery.data.settings.PinCheck
import com.example.gpgrocery.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var now = 1_000_000L
    private lateinit var scope: CoroutineScope
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        // Okio storage: DataStore's plain File storage cannot replace its file on Windows, where these
        // tests also run.
        val storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = PreferencesSerializer,
            producePath = { File(folder.root, "store.preferences_pb").absolutePath.toPath() },
        )
        val store = PreferenceDataStoreFactory.create(storage = storage, scope = scope)
        settings = SettingsRepository(store) { now }
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `setup keeps a hash of the PIN, never the PIN`() = runBlocking {
        settings.completeSetup(" Corner Grocer ", "Sam", "2580", sampleStore = false)

        val stored = settings.current()
        assertTrue(stored.setupComplete)
        assertEquals("Corner Grocer", stored.storeName)
        assertEquals(1_300, stored.taxRateBasisPoints)
        assertTrue(stored.hasPin)
        assertNotEquals("2580", stored.pin!!.hash)
    }

    @Test
    fun `five wrong PINs in a row pause the lock screen for thirty seconds`() = runBlocking {
        settings.completeSetup("Corner Grocer", "Sam", "2580", sampleStore = false)

        repeat(4) { assertEquals(PinCheck.Wrong(attemptsLeft = 4 - it), settings.checkPin("0000")) }
        assertEquals(PinCheck.Paused(untilMillis = 1_030_000), settings.checkPin("0000"))

        // Even the right PIN has to wait for the pause to end.
        now += 29_000
        assertTrue(settings.checkPin("2580") is PinCheck.Paused)
        now += 1_001
        assertEquals(PinCheck.Correct, settings.checkPin("2580"))
    }

    @Test
    fun `the right PIN starts the count again`() = runBlocking {
        settings.completeSetup("Corner Grocer", "Sam", "2580", sampleStore = false)

        repeat(3) { settings.checkPin("0000") }
        assertEquals(PinCheck.Correct, settings.checkPin("2580"))
        assertEquals(PinCheck.Wrong(attemptsLeft = 4), settings.checkPin("0000"))
    }

    @Test
    fun `a new PIN replaces the old one`() = runBlocking {
        settings.completeSetup("Corner Grocer", "Sam", "2580", sampleStore = false)

        settings.changePin("1397")

        assertEquals(PinCheck.Wrong(attemptsLeft = 4), settings.checkPin("2580"))
        assertEquals(PinCheck.Correct, settings.checkPin("1397"))
    }

    @Test
    fun `store details and preferences are kept`() = runBlocking {
        settings.completeSetup("Corner Grocer", "Sam", "2580", sampleStore = true)

        settings.updateStore("Corner Grocer & Deli ", " Sam Lee", 500)
        settings.setTheme(ThemeMode.DARK)
        settings.setBiometric(true)

        val stored = settings.current()
        assertEquals("Corner Grocer & Deli", stored.storeName)
        assertEquals("Sam Lee", stored.ownerName)
        assertEquals(500, stored.taxRateBasisPoints)
        assertEquals(ThemeMode.DARK, stored.themeMode)
        assertTrue(stored.biometricEnabled)
        assertTrue(stored.sampleStore)
    }

    @Test
    fun `erasing forgets the store and its PIN`() = runBlocking {
        settings.completeSetup("Corner Grocer", "Sam", "2580", sampleStore = false)

        settings.erase()

        val stored = settings.current()
        assertFalse(stored.setupComplete)
        assertFalse(stored.hasPin)
    }
}
