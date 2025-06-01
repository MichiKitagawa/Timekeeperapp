package com.example.timekeeper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.timekeeper.viewmodel.MonitoringSetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: MonitoringSetupViewModel = hiltViewModel()
) {
    val installedApps by viewModel.installedApps.collectAsState()
    val monitoredApps by viewModel.monitoredApps.collectAsState()
    
    // 既に監視対象のアプリを除外したリストを作成
    val availableApps = installedApps.filter { app ->
        !monitoredApps.any { monitored -> monitored.packageName == app.packageName }
    }
    
    // デバッグログ
    LaunchedEffect(installedApps, monitoredApps) {
        android.util.Log.d("MonitoringSetupScreen", "Installed apps: ${installedApps.size}, Monitored apps: ${monitoredApps.size}, Available apps: ${availableApps.size}")
        availableApps.take(3).forEach { app ->
            android.util.Log.d("MonitoringSetupScreen", "Available app: ${app.appName}")
        }
    }
    
    var selectedApp by remember { mutableStateOf<String?>(null) }
    var initialLimit by remember { mutableStateOf("") }
    var targetLimit by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "アプリ時間制限設定",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 監視対象アプリ一覧
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "監視対象アプリ",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (monitoredApps.isEmpty()) {
                    Text(
                        text = "監視対象アプリが設定されていません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn {
                        items(monitoredApps) { app ->
                            MonitoredAppItem(
                                app = app,
                                onUpdateTarget = { packageName, newTarget ->
                                    viewModel.updateMonitoredAppTarget(packageName, newTarget)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // アプリ追加ボタン - 追加可能なアプリがない場合は無効化
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = availableApps.isNotEmpty()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            if (availableApps.isEmpty()) {
                Text("全てのアプリが設定済みです")
            } else {
            Text("アプリを追加")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 完了ボタン
        Button(
            onClick = onSetupComplete,
            modifier = Modifier.fillMaxWidth(),
            enabled = monitoredApps.isNotEmpty()
        ) {
            Text("設定完了")
        }
    }

    // アプリ追加ダイアログ
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                selectedApp = null
                initialLimit = ""
                targetLimit = ""
                errorMessage = null
            },
            title = { Text("アプリを追加") },
            text = {
                Column {
                    // アプリ選択 - 利用可能なアプリのみ表示
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedApp?.let { packageName ->
                                availableApps.find { it.packageName == packageName }?.appName
                            } ?: "",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("アプリを選択") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            // 利用可能なアプリのみ表示（既に監視対象のアプリは除外）
                            availableApps.forEach { app ->
                                DropdownMenuItem(
                                    text = { Text(app.appName) },
                                    onClick = {
                                        selectedApp = app.packageName
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 初期制限時間
                    OutlinedTextField(
                        value = initialLimit,
                        onValueChange = { initialLimit = it },
                        label = { Text("初期制限時間（分）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 目標制限時間
                    OutlinedTextField(
                        value = targetLimit,
                        onValueChange = { targetLimit = it },
                        label = { Text("目標制限時間（分）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // エラーメッセージ
                    errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val packageName = selectedApp
                        val initial = initialLimit.toIntOrNull()
                        val target = targetLimit.toIntOrNull()

                        when {
                            packageName == null -> {
                                errorMessage = "アプリを選択してください"
                            }
                            initial == null || initial <= 0 -> {
                                errorMessage = "初期制限時間は1以上の数値を入力してください"
                            }
                            target == null || target <= 0 -> {
                                errorMessage = "目標制限時間は1以上の数値を入力してください"
                            }
                            target >= initial -> {
                                errorMessage = "目標制限時間は初期制限時間より短く設定してください"
                            }
                            else -> {
                                val success = viewModel.addMonitoredApp(packageName, initial, target)
                                if (success) {
                                    showAddDialog = false
                                    selectedApp = null
                                    initialLimit = ""
                                    targetLimit = ""
                                    errorMessage = null
                                } else {
                                    errorMessage = "アプリの追加に失敗しました"
                                }
                            }
                        }
                    }
                ) {
                    Text("追加")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAddDialog = false
                        selectedApp = null
                        initialLimit = ""
                        targetLimit = ""
                        errorMessage = null
                    }
                ) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun MonitoredAppItem(
    app: com.example.timekeeper.data.MonitoredAppRepository.MonitoredApp,
    onUpdateTarget: (String, Int) -> Unit = { _, _ -> } // 目標時間更新用コールバック
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var newTargetLimit by remember { mutableStateOf(app.targetLimitMinutes.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "初期: ${app.initialLimitMinutes}分（変更不可）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "現在: ${app.currentLimitMinutes}分 → 目標: ${app.targetLimitMinutes}分",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "※目標時間のみ短縮可能",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // 目標時間編集ボタン
            IconButton(onClick = { 
                showEditDialog = true
                newTargetLimit = app.targetLimitMinutes.toString()
                errorMessage = null
            }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "目標時間を編集",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // 目標時間編集ダイアログ
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { 
                showEditDialog = false
                errorMessage = null
            },
            title = { Text("目標時間の変更") },
            text = {
                Column {
                    Text(
                        text = "現在の目標時間: ${app.targetLimitMinutes}分",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = newTargetLimit,
                        onValueChange = { newTargetLimit = it },
                        label = { Text("新しい目標時間（分）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = "※現在の目標時間より短い値のみ設定できます",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    // エラーメッセージ
                    errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTarget = newTargetLimit.toIntOrNull()
                        when {
                            newTarget == null || newTarget <= 0 -> {
                                errorMessage = "1以上の数値を入力してください"
                            }
                            newTarget >= app.targetLimitMinutes -> {
                                errorMessage = "現在の目標時間（${app.targetLimitMinutes}分）より短く設定してください"
                            }
                            newTarget >= app.currentLimitMinutes -> {
                                errorMessage = "現在の制限時間（${app.currentLimitMinutes}分）より短く設定してください"
                            }
                            else -> {
                                onUpdateTarget(app.packageName, newTarget)
                                showEditDialog = false
                                errorMessage = null
                            }
                        }
                    }
                ) {
                    Text("変更")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showEditDialog = false
                        errorMessage = null
                    }
                ) {
                    Text("キャンセル")
                }
            }
        )
    }
} 