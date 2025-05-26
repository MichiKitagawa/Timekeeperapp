package com.example.timekeeper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DeviceIdManagerTest {

    private lateinit var context: Context
    private val prefsName = "TimekeeperPrefs"
    private val deviceIdKey = "DEVICE_ID"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // Clear SharedPreferences before each test
        val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit() // Use commit() for synchronous behavior in tests
    }

    @After
    fun tearDown() {
        // Clear SharedPreferences after each test to ensure isolation
        val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
    }

    private fun getDeviceIdFromPreferences(): String? {
        val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        return sharedPreferences.getString(deviceIdKey, null)
    }

    private fun generateAndStoreDeviceId(): String {
        val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        var deviceId = sharedPreferences.getString(deviceIdKey, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(deviceIdKey, deviceId).commit() // Use commit() for tests
        }
        return deviceId
    }

    @Test
    fun testDeviceIdGenerationAndPersistence() {
        // 1. Test初回起動時に `device_id` が生成され、SharedPreferencesに保存されること
        val firstDeviceId = generateAndStoreDeviceId()
        assertNotNull("Device ID should be generated on first call", firstDeviceId)
        val storedDeviceId = getDeviceIdFromPreferences()
        assertEquals("Generated Device ID should be stored in SharedPreferences", firstDeviceId, storedDeviceId)

        // 2. Test 2回目以降の起動時には保存された `device_id` が使用されること
        val secondDeviceId = generateAndStoreDeviceId() // Call again, should retrieve existing
        assertEquals("Device ID should be the same on subsequent calls", firstDeviceId, secondDeviceId)

        // Optional: Verify it's a valid UUID format (basic check)
        try {
            UUID.fromString(firstDeviceId)
        } catch (e: IllegalArgumentException) {
            throw AssertionError("Generated Device ID is not a valid UUID format")
        }
    }

    @Test
    fun testDeviceIdIsUniqueOnFirstGeneration() {
        // Clear preferences to simulate a fresh start for this specific test case if needed
        val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()

        val deviceId1 = generateAndStoreDeviceId()

        // Clear again and regenerate to ensure a different ID is created for a truly "new" first time
        sharedPreferences.edit().clear().commit()
        val deviceId2 = generateAndStoreDeviceId()

        assertNotNull(deviceId1)
        assertNotNull(deviceId2)
        assertNotEquals("Device IDs generated on two separate 'first' calls should be different", deviceId1, deviceId2)
    }
} 