package com.example.timekeeper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
    navController: NavController,
    viewModel: MonitoringSetupViewModel = hiltViewModel()
) {
    val installedApps by viewModel.installedApps.collectAsState()
    val monitoredApps by viewModel.monitoredApps.collectAsState()
    
    // デバッグログ
    LaunchedEffect(installedApps) {
        android.util.Log.d("MonitoringSetupScreen", "Installed apps updated: ${installedApps.size} apps")
        installedApps.take(3).forEach { app ->
            android.util.Log.d("MonitoringSetupScreen", "App: ${app.appName}")
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
                                onRemove = { viewModel.removeMonitoredApp(app.packageName) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // アプリ追加ボタン
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("アプリを追加")
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
                    // アプリ選択
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedApp?.let { packageName ->
                                installedApps.find { it.packageName == packageName }?.appName
                            } ?: "",
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
                            installedApps.forEach { app ->
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
    onRemove: () -> Unit
) {
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
                    text = "現在: ${app.currentLimitMinutes}分 → 目標: ${app.targetLimitMinutes}分",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
} 