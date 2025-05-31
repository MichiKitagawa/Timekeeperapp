package com.example.timekeeper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.timekeeper.R
import com.example.timekeeper.util.HeartbeatLogger
import com.example.timekeeper.util.GapDetector
import com.example.timekeeper.util.SecurityManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HeartbeatService : Service() {

    @Inject
    lateinit var heartbeatLogger: HeartbeatLogger
    
    @Inject
    lateinit var gapDetector: GapDetector
    
    @Inject
    lateinit var securityManager: SecurityManager

    private val handler = Handler(Looper.getMainLooper())
    private val heartbeatInterval = 1 * 60 * 1000L // 1分間隔
    private var isRunning = false

    companion object {
        private const val TAG = "HeartbeatService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "heartbeat_channel"
        private const val CHANNEL_NAME = "Timekeeper監視"
    }

    private val heartbeatTask = object : Runnable {
        override fun run() {
            if (isRunning) {
                try {
                    // 1. アクセシビリティサービス状態をチェック
                    if (!isAccessibilityServiceEnabled()) {
                        Log.w(TAG, "🚨 Accessibility service is DISABLED - triggering security reset")
                        securityManager.handleAccessibilityDisabled()
                        
                        // サービス停止（不正状態なので継続する意味がない）
                        stopSelf()
                        return
                    }
                    
                    // 2. ハートビートギャップをチェック（バックグラウンド検知）
                    val breach = gapDetector.checkForSuspiciousGaps()
                    if (breach != null && breach.severity == GapDetector.SecurityBreach.Severity.SECURITY_BREACH) {
                        Log.w(TAG, "🚨 Security breach detected in BACKGROUND: ${breach.gapMinutes} minutes")
                        securityManager.handleHeartbeatGap(breach.gapMinutes)
                        
                        // サービス停止（不正状態なので継続する意味がない）
                        stopSelf()
                        return
                    }
                    
                    // 3. 通常のハートビート記録
                    heartbeatLogger.recordHeartbeat()
                    
                    // 4. 次回実行をスケジュール
                    handler.postDelayed(this, heartbeatInterval)
                    
                    Log.d(TAG, "💓 Heartbeat recorded, next in ${heartbeatInterval / 1000}秒")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in heartbeat task", e)
                    
                    // エラーが発生しても継続実行
                    handler.postDelayed(this, heartbeatInterval)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "🔧 HeartbeatService created")
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "🚀 HeartbeatService starting...")
        
        // フォアグラウンドサービスとして開始
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // ハートビート記録を開始
        startHeartbeatRecording()
        
        // システムによる再起動を保証
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "🔧 HeartbeatService destroyed")
        
        stopHeartbeatRecording()
    }

    private fun startHeartbeatRecording() {
        if (!isRunning) {
            isRunning = true
            
            // 即座に最初のハートビートを記録
            heartbeatLogger.recordHeartbeat()
            
            // 定期実行を開始
            handler.postDelayed(heartbeatTask, heartbeatInterval)
            
            Log.i(TAG, "💓 Heartbeat recording started (interval: ${heartbeatInterval / 1000}秒)")
        }
    }

    private fun stopHeartbeatRecording() {
        if (isRunning) {
            isRunning = false
            handler.removeCallbacks(heartbeatTask)
            
            Log.i(TAG, "⏹️ Heartbeat recording stopped")
        }
    }
    
    /**
     * アクセシビリティサービスが有効かどうかをチェック
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = ComponentName(this, com.example.timekeeper.service.MyAccessibilityService::class.java)
        val accessibilityEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        if (accessibilityEnabled == 0) {
            return false
        }
        val settingValue = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (settingValue != null) {
            val splitter = TextUtils.SimpleStringSplitter(':')
            splitter.setString(settingValue)
            while (splitter.hasNext()) {
                val accessibilityService = splitter.next()
                if (accessibilityService.equals(service.flattenToString(), ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Timekeeperの監視状態を表示"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timekeeper監視中")
            .setContentText("アプリの正常動作を監視しています")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }
} 