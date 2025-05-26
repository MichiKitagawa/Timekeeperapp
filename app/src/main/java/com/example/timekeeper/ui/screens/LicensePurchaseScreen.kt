package com.example.timekeeper.ui.screens

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
import androidx.navigation.compose.rememberNavController
import com.example.timekeeper.data.api.ApiResponse
import com.example.timekeeper.data.api.TimekeeperRepository
import com.example.timekeeper.utils.ErrorHandler
import kotlinx.coroutines.launch

@Composable
fun LicensePurchaseScreen(
    onPurchaseClick: () -> Unit,
    navController: NavController = rememberNavController(),
    modifier: Modifier = Modifier
) {
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
                
                val response = repository.confirmLicense(deviceId, purchaseToken)
                
                when (response) {
                    is ApiResponse.Success -> {
                        onPurchaseClick()
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
            text = "¥10,000で利用開始",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
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
                    text = "購入",
                    fontSize = 18.sp
                )
            }
        }
    }
} 