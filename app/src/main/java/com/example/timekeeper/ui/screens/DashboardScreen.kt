package com.example.timekeeper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MonitoredApp(
    val appName: String,
    val usedMinutes: Int,
    val limitMinutes: Int
)

@Composable
fun DashboardScreen(
    onAppLimitExceeded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // サンプルデータ（実際の実装ではRoomから取得）
    val monitoredApps = listOf(
        MonitoredApp("Sample App 1", 45, 60),
        MonitoredApp("Sample App 2", 30, 45),
        MonitoredApp("Sample App 3", 60, 60) // 制限時間に達している
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ダッシュボード",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(monitoredApps) { app ->
                AppUsageCard(
                    app = app,
                    onLimitExceeded = { onAppLimitExceeded(app.appName) }
                )
            }
        }
    }
}

@Composable
fun AppUsageCard(
    app: MonitoredApp,
    onLimitExceeded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingMinutes = app.limitMinutes - app.usedMinutes
    val progress = app.usedMinutes.toFloat() / app.limitMinutes.toFloat()
    val progressColor = when {
        progress >= 1.0f -> Color.Red
        progress >= 0.8f -> Color(0xFFFF9800) // Orange
        else -> Color.Green
    }
    
    // 制限時間に達した場合の処理
    LaunchedEffect(remainingMinutes) {
        if (remainingMinutes <= 0) {
            onLimitExceeded()
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = app.appName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "${app.usedMinutes} / ${app.limitMinutes} 分",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor,
            )
            
            Text(
                text = if (remainingMinutes > 0) "残り ${remainingMinutes} 分" else "制限時間に達しました",
                fontSize = 12.sp,
                color = if (remainingMinutes > 0) Color.Gray else Color.Red,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
} 