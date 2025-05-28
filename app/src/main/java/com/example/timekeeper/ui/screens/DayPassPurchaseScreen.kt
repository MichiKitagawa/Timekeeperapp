package com.example.timekeeper.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.timekeeper.viewmodel.DayPassPurchaseViewModel
import com.example.timekeeper.ui.navigation.TimekeeperRoutes
import kotlin.math.pow

@Composable
fun DayPassPurchaseScreen(
    onPurchaseSuccess: () -> Unit,
    onCancelClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
    onPurchaseDaypassClick: (Int?) -> Unit = {},
    viewModel: DayPassPurchaseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences("TimekeeperPrefs", Context.MODE_PRIVATE)
    }
    val unlockCount = sharedPreferences.getInt("UNLOCK_COUNT", 0)

    // 価格計算: ¥(200×1.2^unlock_count)
    val price = (200 * 1.2.pow(unlockCount.toDouble())).toInt()

    var isLoading by remember { mutableStateOf(false) }
    
    // ViewModelの状態を監視
    val purchaseState by viewModel.purchaseState.collectAsState()
    
    // 購入状態の変化を監視
    LaunchedEffect(purchaseState) {
        when (purchaseState) {
            is DayPassPurchaseViewModel.PurchaseState.Success -> {
                isLoading = false
                // ダッシュボードに戻る
                navController.navigate(TimekeeperRoutes.DASHBOARD) {
                    popUpTo(TimekeeperRoutes.DAY_PASS_PURCHASE) { inclusive = true }
                }
            }
            is DayPassPurchaseViewModel.PurchaseState.Error -> {
                isLoading = false
                // エラー処理（必要に応じてSnackbarなどで表示）
            }
            is DayPassPurchaseViewModel.PurchaseState.Loading -> {
                isLoading = true
            }
            else -> {
                isLoading = false
            }
        }
    }
    
    // ローカル環境でのテスト用：直接決済成功をシミュレート
    val handlePurchaseAttempt: () -> Unit = {
        viewModel.purchaseDayPassMock()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "デイパス購入",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "今日1日だけ制限解除",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "¥$price",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // ローカルテスト用の注意書き
        Text(
            text = "※ ローカル環境でのテスト用\n実際の決済は行われません",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = handlePurchaseAttempt,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "今すぐアンロック（テスト）",
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "キャンセル",
                fontSize = 18.sp
            )
        }
    }
}

 