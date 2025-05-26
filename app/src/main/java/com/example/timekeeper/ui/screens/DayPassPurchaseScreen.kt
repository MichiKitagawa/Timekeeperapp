package com.example.timekeeper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.timekeeper.data.api.ApiResponse
import com.example.timekeeper.data.api.TimekeeperRepository
import com.example.timekeeper.utils.ErrorHandler
import kotlinx.coroutines.launch
import kotlin.math.pow

@Composable
fun DayPassPurchaseScreen(
    unlockCount: Int,
    onPurchaseClick: () -> Unit,
    onCancelClick: () -> Unit,
    navController: NavController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    // 価格計算: ¥(200×1.2^unlock_count)
    val price = (200 * 1.2.pow(unlockCount.toDouble())).toInt()
    
    val scope = rememberCoroutineScope()
    val repository = remember { TimekeeperRepository() }
    var isLoading by remember { mutableStateOf(false) }
    
    val handlePurchase: () -> Unit = {
        scope.launch {
            isLoading = true
            try {
                // 実際の実装では、Stripe決済処理を行い、purchase_tokenを取得
                val deviceId = "sample_device_id" // 実際の実装ではSharedPreferencesから取得
                val purchaseToken = "sample_purchase_token" // Stripe決済から取得
                
                val response = repository.unlockDaypass(deviceId, purchaseToken)
                
                when (response) {
                    is ApiResponse.Success -> {
                        onPurchaseClick()
                    }
                    is ApiResponse.Error -> {
                        // Stripe決済成功後のFirestore更新失敗の場合
                        if (response.error == "payment_verification_failed") {
                            ErrorHandler.handlePaymentSuccessButUnlockFailed(navController)
                        } else {
                            ErrorHandler.handleApiError(navController, response)
                        }
                    }
                    else -> {
                        ErrorHandler.handleApiError(navController, response)
                    }
                }
            } catch (e: Exception) {
                ErrorHandler.handleException(navController, e)
            } finally {
                isLoading = false
            }
        }
        Unit
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
        
        Button(
            onClick = handlePurchase,
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
                    text = "今すぐアンロック",
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