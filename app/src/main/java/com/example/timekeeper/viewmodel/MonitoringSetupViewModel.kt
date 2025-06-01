package com.example.timekeeper.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timekeeper.data.MonitoredAppRepository
import com.example.timekeeper.data.AppUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MonitoringSetupViewModel @Inject constructor(
    private val monitoredAppRepository: MonitoredAppRepository,
    private val appUsageRepository: AppUsageRepository
) : ViewModel() {

    companion object {
        private const val TAG = "MonitoringSetupViewModel"
    }

    val installedApps: StateFlow<List<MonitoredAppRepository.AppInfo>> = 
        monitoredAppRepository.getInstalledAppsFlow()
    
    val monitoredApps: StateFlow<List<MonitoredAppRepository.MonitoredApp>> = 
        monitoredAppRepository.monitoredApps

    init {
        Log.d(TAG, "ViewModel initialized")
        // インストール済みアプリ一覧を読み込み
        viewModelScope.launch {
            Log.d(TAG, "Loading installed apps...")
            monitoredAppRepository.loadInstalledApps()
            Log.d(TAG, "Installed apps loaded, current count: ${installedApps.value.size}")
        }
    }

    /**
     * 監視対象アプリを追加
     */
    fun addMonitoredApp(packageName: String, initialLimit: Int, targetLimit: Int): Boolean {
        Log.d(TAG, "Adding monitored app: $packageName")
        val success = monitoredAppRepository.addOrUpdateMonitoredApp(packageName, initialLimit, targetLimit)
        
        if (success) {
            // アプリ追加成功時に使用時間をリセット
            Log.d(TAG, "Resetting today usage for newly added app: $packageName")
            appUsageRepository.resetTodayUsage(packageName)
        }
        
        return success
    }

    /**
     * 監視対象アプリを削除（機能無効化）
     * 
     * 注意: 一度設定したアプリの制限は削除できません。
     * この機能は無効化されています。
     */
    fun removeMonitoredApp(packageName: String) {
        Log.w(TAG, "Removing monitored app is disabled: $packageName")
        // 削除機能は無効化されています
        // monitoredAppRepository.removeMonitoredApp(packageName)
    }

    /**
     * 監視対象アプリの目標時間を更新
     * 
     * 注意: 目標時間は短縮のみ可能です。
     */
    fun updateMonitoredAppTarget(packageName: String, newTargetLimit: Int): Boolean {
        Log.d(TAG, "Updating target limit for $packageName to $newTargetLimit minutes")
        return monitoredAppRepository.updateMonitoredAppTarget(packageName, newTargetLimit)
    }
} 