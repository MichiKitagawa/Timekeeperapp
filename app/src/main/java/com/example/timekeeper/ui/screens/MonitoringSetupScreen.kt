package com.example.timekeeper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.timekeeper.utils.ErrorHandler

data class AppInfo(
    val packageName: String,
    val appName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringSetupScreen(
    onSetupComplete: () -> Unit,
    navController: NavController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var initialTime by remember { mutableStateOf("") }
    var targetTime by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    // サンプルアプリリスト（実際の実装では OSから取得）
    val sampleApps = listOf(
        AppInfo("com.example.app1", "Sample App 1"),
        AppInfo("com.example.app2", "Sample App 2"),
        AppInfo("com.example.app3", "Sample App 3")
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "監視対象設定",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        // アプリ選択
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedApp?.appName ?: "",
                onValueChange = { },
                readOnly = true,
                label = { Text("アプリを選択") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                sampleApps.forEach { app ->
                    DropdownMenuItem(
                        text = { Text(app.appName) },
                        onClick = {
                            selectedApp = app
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // 初期時間入力
        OutlinedTextField(
            value = initialTime,
            onValueChange = { initialTime = it },
            label = { Text("初期時間（分）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        // 目標時間入力
        OutlinedTextField(
            value = targetTime,
            onValueChange = { targetTime = it },
            label = { Text("目標時間（分）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = {
                try {
                    // バリデーション
                    val initialTimeInt = initialTime.toIntOrNull()
                    val targetTimeInt = targetTime.toIntOrNull()
                    
                    if (initialTimeInt == null || targetTimeInt == null) {
                        throw IllegalArgumentException("時間は数値で入力してください")
                    }
                    
                    if (targetTimeInt >= initialTimeInt) {
                        throw IllegalArgumentException("目標時間は初期時間より小さく設定してください")
                    }
                    
                    // 設定完了
                    onSetupComplete()
                } catch (e: Exception) {
                    ErrorHandler.handleException(navController, e)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedApp != null && initialTime.isNotBlank() && targetTime.isNotBlank()
        ) {
            Text(
                text = "追加/更新",
                fontSize = 18.sp
            )
        }
    }
} 