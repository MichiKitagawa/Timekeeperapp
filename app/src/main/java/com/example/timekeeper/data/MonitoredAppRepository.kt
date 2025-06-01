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
     * アプリの現在の制限時間を取得
     */
    fun getCurrentLimit(packageName: String): Int {
        // 監視対象アプリかどうかをチェック
        if (!isAppMonitored(packageName)) {
            android.util.Log.d("MonitoredAppRepository", 
                "App $packageName is not monitored, returning -1")
            return -1
        }
        
        val currentLimit = prefs.getInt("${packageName}_current_limit", -1)
        android.util.Log.d("MonitoredAppRepository", 
            "Current limit for $packageName: $currentLimit minutes")
        return currentLimit
    }
    
    /**
     * 監視対象アプリを追加（更新は無効）
     * 
     * 注意: 一度設定したアプリの制限時間は変更できません。
     * 重複登録も防止されます。
     */
    fun addOrUpdateMonitoredApp(
        packageName: String,
        initialLimitMinutes: Int,
        targetLimitMinutes: Int
    ): Boolean {
        // 既に監視対象アプリとして登録されているかチェック
        if (isAppMonitored(packageName)) {
            android.util.Log.w("MonitoredAppRepository", 
                "App $packageName is already monitored. Updates are not allowed.")
            return false
        }
        
        // バリデーション: target_limit < initial_limit
        if (targetLimitMinutes >= initialLimitMinutes) {
            android.util.Log.w("MonitoredAppRepository", 
                "Invalid limits: target=$targetLimitMinutes >= initial=$initialLimitMinutes")
            return false
        }
        
        // バリデーション: 値が正の数であること
        if (initialLimitMinutes <= 0 || targetLimitMinutes <= 0) {
            android.util.Log.w("MonitoredAppRepository", 
                "Invalid limits: values must be positive. initial=$initialLimitMinutes, target=$targetLimitMinutes")
            return false
        }
        
        val monitoredApps = prefs.getStringSet("monitored_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
        monitoredApps.add(packageName)
        
        android.util.Log.d("MonitoredAppRepository", 
            "Adding new app $packageName: initial=$initialLimitMinutes, target=$targetLimitMinutes, current=$initialLimitMinutes")
        
        prefs.edit()
            .putStringSet("monitored_apps", monitoredApps)
            .putInt("${packageName}_initial_limit", initialLimitMinutes)
            .putInt("${packageName}_target_limit", targetLimitMinutes)
            .putInt("${packageName}_current_limit", initialLimitMinutes) // 初期値は initial_limit
            .apply()
        
        android.util.Log.i("MonitoredAppRepository", 
            "App $packageName successfully added to monitoring")
        
        loadMonitoredApps()
        return true
    }
    
    /**
     * 監視対象アプリを削除（機能無効化）
     * 
     * 注意: 一度設定したアプリの制限は削除できません。
     * この機能は完全に無効化されています。
     */
    fun removeMonitoredApp(packageName: String) {
        android.util.Log.w("MonitoredAppRepository", 
            "Removing monitored app is disabled for security. App: $packageName")
        
        // 削除機能は無効化されています
        // 一度設定したアプリの制限は削除できません
        /*
        val monitoredApps = prefs.getStringSet("monitored_apps", emptySet())?.toMutableSet() ?: mutableSetOf()
        monitoredApps.remove(packageName)
        
        prefs.edit()
            .putStringSet("monitored_apps", monitoredApps)
            .remove("${packageName}_initial_limit")
            .remove("${packageName}_target_limit")
            .remove("${packageName}_current_limit")
            .apply()
        
        loadMonitoredApps()
        */
    }
    
    /**
     * 監視対象アプリの目標時間を更新
     * 
     * 注意: 目標時間は短縮のみ可能です（現在の目標時間より短い値のみ）
     */
    fun updateMonitoredAppTarget(packageName: String, newTargetLimit: Int): Boolean {
        // アプリが監視対象かチェック
        if (!isAppMonitored(packageName)) {
            android.util.Log.w("MonitoredAppRepository", 
                "App $packageName is not monitored, cannot update target")
            return false
        }
        
        // 現在の値を取得
        val currentTargetLimit = prefs.getInt("${packageName}_target_limit", -1)
        val currentLimit = prefs.getInt("${packageName}_current_limit", -1)
        
        // バリデーション
        when {
            newTargetLimit <= 0 -> {
                android.util.Log.w("MonitoredAppRepository", 
                    "Invalid target limit: $newTargetLimit must be positive")
                return false
            }
            newTargetLimit >= currentTargetLimit -> {
                android.util.Log.w("MonitoredAppRepository", 
                    "Target limit can only be reduced: $newTargetLimit >= current $currentTargetLimit")
                return false
            }
            newTargetLimit >= currentLimit -> {
                android.util.Log.w("MonitoredAppRepository", 
                    "Target limit must be less than current limit: $newTargetLimit >= $currentLimit")
                return false
            }
        }
        
        // 目標時間を更新
        prefs.edit()
            .putInt("${packageName}_target_limit", newTargetLimit)
            .apply()
        
        android.util.Log.i("MonitoredAppRepository", 
            "Updated target limit for $packageName from $currentTargetLimit to $newTargetLimit minutes")
        
        loadMonitoredApps()
        return true
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
        android.util.Log.i("MonitoredAppRepository", "=== loadMonitoredApps() called ===")
        
        val monitoredPackages = prefs.getStringSet("monitored_apps", emptySet()) ?: emptySet()
        val packageManager = context.packageManager
        
        android.util.Log.d("MonitoredAppRepository", "Monitored packages from SharedPreferences: $monitoredPackages")
        
        val monitoredAppsList = monitoredPackages.mapNotNull { packageName ->
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                
                val initialLimit = prefs.getInt("${packageName}_initial_limit", 0)
                val targetLimit = prefs.getInt("${packageName}_target_limit", 0)
                val currentLimit = prefs.getInt("${packageName}_current_limit", 0)
                
                android.util.Log.i("MonitoredAppRepository", 
                    "Loading app $appName ($packageName): initial=$initialLimit, target=$targetLimit, current=$currentLimit")
                
                MonitoredApp(
                    packageName = packageName,
                    appName = appName,
                    initialLimitMinutes = initialLimit,
                    targetLimitMinutes = targetLimit,
                    currentLimitMinutes = currentLimit
                )
            } catch (e: PackageManager.NameNotFoundException) {
                // アプリがアンインストールされている場合は除外
                android.util.Log.w("MonitoredAppRepository", "App $packageName not found, excluding from monitoring")
                null
            }
        }
        
        android.util.Log.i("MonitoredAppRepository", "Setting StateFlow: ${_monitoredApps.value.size} -> ${monitoredAppsList.size} apps")
        
        // 更新前の状態をログ出力
        _monitoredApps.value.forEach { app ->
            android.util.Log.d("MonitoredAppRepository", "Before update: ${app.appName} = ${app.currentLimitMinutes} minutes")
        }
        
        _monitoredApps.value = monitoredAppsList
        
        // 更新後の状態をログ出力
        _monitoredApps.value.forEach { app ->
            android.util.Log.i("MonitoredAppRepository", "After update: ${app.appName} = ${app.currentLimitMinutes} minutes")
        }
        
        android.util.Log.i("MonitoredAppRepository", "=== loadMonitoredApps() completed ===")
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
    
    /**
     * データを強制的に再読み込み（外部からの呼び出し用）
     */
    fun forceReload() {
        android.util.Log.d("MonitoredAppRepository", "Force reload requested")
        loadMonitoredApps()
    }
} 