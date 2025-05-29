package com.example.timekeeper.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoredAppRepository @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("monitored_apps", Context.MODE_PRIVATE)
    
    private val _monitoredApps = MutableStateFlow<List<MonitoredApp>>(emptyList())
    val monitoredApps: StateFlow<List<MonitoredApp>> = _monitoredApps
    
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    
    data class MonitoredApp(
        val packageName: String,
        val appName: String,
        val initialLimitMinutes: Int,
        val targetLimitMinutes: Int,
        val currentLimitMinutes: Int
    )
    
    init {
        loadMonitoredApps()
        loadInstalledApps()
    }
    
    /**
     * アプリが監視対象かどうかチェック
     */
    fun isAppMonitored(packageName: String): Boolean {
        val monitoredApps = prefs.getStringSet("monitored_apps", emptySet()) ?: emptySet()
        return monitoredApps.contains(packageName)
    }
    
    /**
     * 監視対象アプリを追加/更新
     */
    fun addOrUpdateMonitoredApp(
        packageName: String,
        initialLimitMinutes: Int,
        targetLimitMinutes: Int
    ): Boolean {
        // バリデーション: target_limit < initial_limit
        if (targetLimitMinutes >= initialLimitMinutes) {
            android.util.Log.w("MonitoredAppRepository", 
                "Invalid limits: target=$targetLimitMinutes >= initial=$initialLimitMinutes")
            return false
        }
        
        val monitoredApps = prefs.getStringSet("monitored_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
        monitoredApps.add(packageName)
        
        android.util.Log.d("MonitoredAppRepository", 
            "Adding app $packageName: initial=$initialLimitMinutes, target=$targetLimitMinutes, current=$initialLimitMinutes")
        
        prefs.edit()
            .putStringSet("monitored_apps", monitoredApps)
            .putInt("${packageName}_initial_limit", initialLimitMinutes)
            .putInt("${packageName}_target_limit", targetLimitMinutes)
            .putInt("${packageName}_current_limit", initialLimitMinutes) // 初期値は initial_limit
            .apply()
        
        android.util.Log.d("MonitoredAppRepository", 
            "App $packageName saved successfully")
        
        loadMonitoredApps()
        return true
    }
    
    /**
     * 監視対象アプリを削除
     */
    fun removeMonitoredApp(packageName: String) {
        val monitoredApps = prefs.getStringSet("monitored_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
        monitoredApps.remove(packageName)
        
        prefs.edit()
            .putStringSet("monitored_apps", monitoredApps)
            .remove("${packageName}_initial_limit")
            .remove("${packageName}_target_limit")
            .remove("${packageName}_current_limit")
            .apply()
        
        loadMonitoredApps()
    }
    
    /**
     * インストール済みアプリ一覧を取得（システムアプリ除く）
     */
    fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        android.util.Log.d("MonitoredAppRepository", "Total installed apps: ${installedApps.size}")
        
        val filteredApps = installedApps
            // 一時的にシステムアプリフィルタを無効化してテスト
            // .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 } // システムアプリ除外
            .filter { it.packageName != context.packageName } // 自分自身を除外
            .filter { 
                // ランチャーアイコンがあるアプリのみ（ユーザーが起動できるアプリ）
                val intent = packageManager.getLaunchIntentForPackage(it.packageName)
                intent != null
            }
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString()
                )
            }
            .sortedBy { it.appName }
        
        android.util.Log.d("MonitoredAppRepository", "Filtered apps count: ${filteredApps.size}")
        filteredApps.take(5).forEach { app ->
            android.util.Log.d("MonitoredAppRepository", "App: ${app.appName} (${app.packageName})")
        }
        
        return filteredApps
    }
    
    data class AppInfo(
        val packageName: String,
        val appName: String
    )
    
    /**
     * 監視対象アプリ一覧を読み込み
     */
    fun loadMonitoredApps() {
        val monitoredPackages = prefs.getStringSet("monitored_apps", emptySet()) ?: emptySet()
        val packageManager = context.packageManager
        
        val monitoredAppsList = monitoredPackages.mapNotNull { packageName ->
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                
                MonitoredApp(
                    packageName = packageName,
                    appName = appName,
                    initialLimitMinutes = prefs.getInt("${packageName}_initial_limit", 0),
                    targetLimitMinutes = prefs.getInt("${packageName}_target_limit", 0),
                    currentLimitMinutes = prefs.getInt("${packageName}_current_limit", 0)
                )
            } catch (e: PackageManager.NameNotFoundException) {
                // アプリがアンインストールされている場合は除外
                null
            }
        }
        
        _monitoredApps.value = monitoredAppsList
    }
    
    /**
     * インストール済みアプリ一覧のFlowを取得
     */
    fun getInstalledAppsFlow(): StateFlow<List<AppInfo>> = _installedApps
    
    /**
     * インストール済みアプリ一覧を読み込み
     */
    fun loadInstalledApps() {
        android.util.Log.d("MonitoredAppRepository", "loadInstalledApps() called")
        val apps = getInstalledApps()
        android.util.Log.d("MonitoredAppRepository", "Setting ${apps.size} apps to StateFlow")
        _installedApps.value = apps
        android.util.Log.d("MonitoredAppRepository", "StateFlow updated, current value size: ${_installedApps.value.size}")
    }
    
    /**
     * 全ての監視対象アプリデータをクリア（デバッグ用）
     */
    fun clearAllData() {
        android.util.Log.d("MonitoredAppRepository", "Clearing all monitored app data")
        prefs.edit().clear().apply()
        _monitoredApps.value = emptyList()
        android.util.Log.d("MonitoredAppRepository", "All data cleared")
    }
} 