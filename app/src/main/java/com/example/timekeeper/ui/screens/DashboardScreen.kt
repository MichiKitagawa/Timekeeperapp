package com.example.timekeeper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.timekeeper.utils.ErrorHandler
import com.example.timekeeper.viewmodel.DashboardViewModel
import com.example.timekeeper.ui.navigation.TimekeeperRoutes

@Composable
fun DashboardScreen(
    navController: NavController = rememberNavController(),
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    // 実際の監視対象アプリデータを取得
    val appUsageInfoList by viewModel.appUsageInfoList.collectAsState()
    
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
        
        if (appUsageInfoList.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "監視対象アプリが設定されていません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
                
                Button(
                    onClick = { 
                        navController.navigate(TimekeeperRoutes.MONITORING_SETUP)
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("アプリを追加")
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(appUsageInfoList) { app ->
                    AppUsageCard(
                        app = app,
                        onDayPassPurchase = {
                            try {
                                // 課金画面に遷移
                                navController.navigate(TimekeeperRoutes.DAY_PASS_PURCHASE)
                            } catch (e: Exception) {
                                ErrorHandler.handleException(navController, e)
                            }
                        }
                    )
                }
                
                // アプリ追加ボタンを最後に配置
                item {
                    Button(
                        onClick = { 
                            navController.navigate(TimekeeperRoutes.MONITORING_SETUP)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("アプリを追加")
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageCard(
    app: DashboardViewModel.AppUsageInfo,
    onDayPassPurchase: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val remainingMinutes = app.limitMinutes - app.usedMinutes
    val progress = app.usedMinutes.toFloat() / app.limitMinutes.toFloat()
    val progressColor = when {
        app.hasDayPass -> Color(0xFF4CAF50) // Green for day pass
        progress >= 1.0f -> Color.Red
        progress >= 0.8f -> Color(0xFFFF9800) // Orange
        else -> Color.Green
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.appName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                when {
                    app.hasDayPass -> {
                        Text(
                            text = "デイパス有効",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else -> {
                        Button(
                            onClick = { onDayPassPurchase(app.packageName) },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "アンロック",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    app.hasDayPass -> "無制限"
                    else -> "${app.usedMinutes} / ${app.limitMinutes} 分"
                },
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (!app.hasDayPass) {
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
            } else {
                Text(
                    text = "今日は無制限で使用できます",
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
} 