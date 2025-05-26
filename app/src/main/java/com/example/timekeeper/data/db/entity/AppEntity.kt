package com.example.timekeeper.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val initial_limit_minutes: Int,
    val target_limit_minutes: Int,
    val current_limit_minutes: Int,
    val used_minutes_today: Int
) 