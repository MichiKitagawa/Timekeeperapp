package com.example.timekeeper.util

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class GapDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val heartbeatLogger: HeartbeatLogger
) {
    companion object {
        private const val TAG = "GapDetector"
        private const val NORMAL_INTERVAL = 5 * 60 * 1000L // 5分
        private const val SUSPICIOUS_GAP_THRESHOLD = 10 * 60 * 1000L // 10分以上で疑わしい
        private const val SECURITY_BREACH_THRESHOLD = 30 * 60 * 1000L // 30分以上でセキュリティ違反
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * セキュリティ違反データクラス
     */
    data class SecurityBreach(
        val type: String,
        val gapDuration: Long,
        val gapMinutes: Long,
        val lastSeen: Date,
        val detectedAt: Date,
        val severity: Severity
    ) {
        enum class Severity {
            SUSPICIOUS,
            SECURITY_BREACH
        }
    }

    /**
     * 疑わしいギャップをチェック
     */
    fun checkForSuspiciousGaps(): SecurityBreach? {
        val lastHeartbeat = heartbeatLogger.getLastHeartbeat()
        val currentTime = System.currentTimeMillis()
        
        // 初回起動時（ハートビート記録なし）は問題なし
        if (lastHeartbeat == 0L) {
            Log.d(TAG, "🔍 First launch detected - no heartbeat history")
            return null
        }
        
        val gap = currentTime - lastHeartbeat
        val gapMinutes = gap / (60 * 1000)
        val lastSeenTime = Date(lastHeartbeat)
        val currentTimeDate = Date(currentTime)
        
        Log.d(TAG, "🔍 Gap analysis: ${gapMinutes}分 " +
                "(Last: ${dateFormat.format(lastSeenTime)}, " +
                "Current: ${dateFormat.format(currentTimeDate)})")
        
        return when {
            gap >= SECURITY_BREACH_THRESHOLD -> {
                Log.w(TAG, "🚨 SECURITY BREACH: Major gap detected = ${gapMinutes}分")
                SecurityBreach(
                    type = "MAJOR_HEARTBEAT_GAP",
                    gapDuration = gap,
                    gapMinutes = gapMinutes,
                    lastSeen = lastSeenTime,
                    detectedAt = currentTimeDate,
                    severity = SecurityBreach.Severity.SECURITY_BREACH
                )
            }
            gap >= SUSPICIOUS_GAP_THRESHOLD -> {
                Log.w(TAG, "⚠️ SUSPICIOUS: Minor gap detected = ${gapMinutes}分")
                SecurityBreach(
                    type = "MINOR_HEARTBEAT_GAP",
                    gapDuration = gap,
                    gapMinutes = gapMinutes,
                    lastSeen = lastSeenTime,
                    detectedAt = currentTimeDate,
                    severity = SecurityBreach.Severity.SUSPICIOUS
                )
            }
            else -> {
                Log.d(TAG, "✅ Normal gap: ${gapMinutes}分 (正常)")
                null
            }
        }
    }

    /**
     * ハートビート履歴全体を分析して異常なパターンを検出
     */
    fun analyzeHeartbeatPattern(): List<SecurityBreach> {
        val history = heartbeatLogger.getHeartbeatHistory()
        val breaches = mutableListOf<SecurityBreach>()
        
        if (history.size < 2) {
            Log.d(TAG, "🔍 Insufficient history for pattern analysis")
            return breaches
        }
        
        Log.d(TAG, "🔍 Analyzing ${history.size} heartbeat entries for suspicious patterns")
        
        // 連続するハートビート間のギャップをチェック
        for (i in 1 until history.size) {
            val prevTime = history[i - 1]
            val currentTime = history[i]
            val gap = currentTime - prevTime
            val gapMinutes = gap / (60 * 1000)
            
            if (gap >= SECURITY_BREACH_THRESHOLD) {
                Log.w(TAG, "🚨 Historical breach found: ${gapMinutes}分間のギャップ")
                breaches.add(
                    SecurityBreach(
                        type = "HISTORICAL_GAP",
                        gapDuration = gap,
                        gapMinutes = gapMinutes,
                        lastSeen = Date(prevTime),
                        detectedAt = Date(currentTime),
                        severity = SecurityBreach.Severity.SECURITY_BREACH
                    )
                )
            }
        }
        
        Log.d(TAG, "🔍 Pattern analysis complete: ${breaches.size} breaches found in history")
        return breaches
    }

    /**
     * セキュリティ違反の詳細をログ出力
     */
    fun logSecurityBreach(breach: SecurityBreach) {
        val severityIcon = when (breach.severity) {
            SecurityBreach.Severity.SUSPICIOUS -> "⚠️"
            SecurityBreach.Severity.SECURITY_BREACH -> "🚨"
        }
        
        Log.w(TAG, "$severityIcon Security Breach Detected:")
        Log.w(TAG, "   Type: ${breach.type}")
        Log.w(TAG, "   Duration: ${breach.gapMinutes}分")
        Log.w(TAG, "   Last seen: ${dateFormat.format(breach.lastSeen)}")
        Log.w(TAG, "   Detected at: ${dateFormat.format(breach.detectedAt)}")
        Log.w(TAG, "   Severity: ${breach.severity}")
    }

    /**
     * 緊急度レベルの判定
     */
    fun getUrgencyLevel(gapMinutes: Long): String {
        return when {
            gapMinutes >= 120 -> "CRITICAL" // 2時間以上
            gapMinutes >= 60 -> "HIGH"     // 1時間以上
            gapMinutes >= 30 -> "MEDIUM"   // 30分以上
            gapMinutes >= 10 -> "LOW"      // 10分以上
            else -> "NORMAL"
        }
    }

    /**
     * 推奨アクションの取得
     */
    fun getRecommendedAction(breach: SecurityBreach): String {
        return when (breach.severity) {
            SecurityBreach.Severity.SECURITY_BREACH -> {
                "データ完全初期化を実行してください"
            }
            SecurityBreach.Severity.SUSPICIOUS -> {
                "監視を強化し、再発時はデータ初期化を実行します"
            }
        }
    }
} 