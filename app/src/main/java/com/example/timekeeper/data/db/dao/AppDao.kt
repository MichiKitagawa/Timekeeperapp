package com.example.timekeeper.data.db.dao

import androidx.room.*
import com.example.timekeeper.data.db.entity.AppEntity

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)

    @Update
    suspend fun updateApp(app: AppEntity)

    @Delete
    suspend fun deleteApp(app: AppEntity)

    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): AppEntity?

    @Query("SELECT * FROM apps")
    suspend fun getAllApps(): List<AppEntity>
} 