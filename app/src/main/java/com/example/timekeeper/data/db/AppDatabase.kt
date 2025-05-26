package com.example.timekeeper.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.timekeeper.data.db.dao.AppDao
import com.example.timekeeper.data.db.entity.AppEntity

@Database(entities = [AppEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
} 