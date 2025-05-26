package com.example.timekeeper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timekeeper.data.db.dao.AppDao
import com.example.timekeeper.data.db.entity.AppEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class P02ViewModel @Inject constructor(
    private val appDao: AppDao
) : ViewModel() {

    fun addOrUpdateApp(
        packageName: String,
        label: String,
        initialTimeMinutes: Int,
        targetTimeMinutes: Int
    ) {
        viewModelScope.launch {
            val appEntity = AppEntity(
                packageName = packageName,
                label = label,
                initial_limit_minutes = initialTimeMinutes,
                target_limit_minutes = targetTimeMinutes,
                current_limit_minutes = initialTimeMinutes, // 初期値は initialTimeMinutes
                used_minutes_today = 0 // 初期値は 0
            )
            appDao.insertApp(appEntity) // OnConflictStrategy.REPLACE なので更新も兼ねる
        }
    }
} 