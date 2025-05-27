package com.example.timekeeper.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timekeeper.data.MonitoredAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MonitoringSetupViewModel @Inject constructor(
    private val monitoredAppRepository: MonitoredAppRepository
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
        return monitoredAppRepository.addOrUpdateMonitoredApp(packageName, initialLimit, targetLimit)
    }

    /**
     * 監視対象アプリを削除
     */
    fun removeMonitoredApp(packageName: String) {
        Log.d(TAG, "Removing monitored app: $packageName")
        monitoredAppRepository.removeMonitoredApp(packageName)
    }
} 