package com.example.timekeeper.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.timekeeper.data.db.dao.AppDao
import com.example.timekeeper.data.db.entity.AppEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import com.google.common.truth.Truth.assertThat

@RunWith(AndroidJUnit4::class)
class AppDaoTest {

    private lateinit var appDao: AppDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appDao = db.appDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetApp() {
        runBlocking {
            val app = AppEntity("com.example.app", "Test App", 60, 30, 60, 0)
            appDao.insertApp(app)
            val retrievedApp = appDao.getAppByPackageName("com.example.app")
            assertThat(retrievedApp).isEqualTo(app)
        }
    }

    @Test
    @Throws(Exception::class)
    fun getAllApps() {
        runBlocking {
            val app1 = AppEntity("com.example.app1", "Test App 1", 60, 30, 60, 0)
            val app2 = AppEntity("com.example.app2", "Test App 2", 120, 60, 120, 0)
            appDao.insertApp(app1)
            appDao.insertApp(app2)
            val allApps = appDao.getAllApps()
            assertThat(allApps).containsExactly(app1, app2)
        }
    }

    @Test
    @Throws(Exception::class)
    fun updateApp() {
        runBlocking {
            val app = AppEntity("com.example.app", "Test App", 60, 30, 60, 0)
            appDao.insertApp(app)
            val updatedApp = app.copy(label = "Updated Test App", current_limit_minutes = 50)
            appDao.updateApp(updatedApp)
            val retrievedApp = appDao.getAppByPackageName("com.example.app")
            assertThat(retrievedApp).isEqualTo(updatedApp)
        }
    }

    @Test
    @Throws(Exception::class)
    fun deleteApp() {
        runBlocking {
            val app = AppEntity("com.example.app", "Test App", 60, 30, 60, 0)
            appDao.insertApp(app)
            appDao.deleteApp(app)
            val retrievedApp = appDao.getAppByPackageName("com.example.app")
            assertThat(retrievedApp).isNull()
        }
    }
} 