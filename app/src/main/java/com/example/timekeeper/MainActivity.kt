package com.example.timekeeper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.timekeeper.ui.navigation.TimekeeperNavigation
import com.example.timekeeper.ui.theme.TimekeeperTheme
import com.example.timekeeper.ui.payment.StripeCheckoutActivity
import com.example.timekeeper.viewmodel.StripeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sharedPreferences by lazy {
        getSharedPreferences("TimekeeperPrefs", Context.MODE_PRIVATE)
    }

    private val stripeViewModel: StripeViewModel by viewModels()

    private fun retrieveAppSpecificDeviceId(): String {
        var deviceId = sharedPreferences.getString("DEVICE_ID", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPreferences.edit().putString("DEVICE_ID", deviceId).apply()
        }
        return deviceId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appSpecificDeviceId = retrieveAppSpecificDeviceId()
        stripeViewModel.setDeviceId(appSpecificDeviceId)
        Log.d("MainActivity", "Device ID: $appSpecificDeviceId")

        handleDeepLink(intent)

        setContent {
            TimekeeperTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                val checkoutUrl by stripeViewModel.checkoutUrlFlow.collectAsState()
                LaunchedEffect(checkoutUrl) {
                    checkoutUrl?.let { url ->
                        Log.i("MainActivity", "Opening Stripe Checkout URL: $url")
                        val intent = StripeCheckoutActivity.createIntent(this@MainActivity, url)
                        startActivity(intent)
                        stripeViewModel.consumeCheckoutUrl()
                    }
                }

                val paymentUiState by stripeViewModel.paymentUiState.collectAsState()
                LaunchedEffect(paymentUiState) {
                    when (val state = paymentUiState) {
                        is com.example.timekeeper.viewmodel.PaymentUiState.Success -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        }
                        is com.example.timekeeper.viewmodel.PaymentUiState.Error -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> { /* Idle or Loading - no action needed */ }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TimekeeperNavigation(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        onPurchaseLicenseClick = {
                            stripeViewModel.startStripeCheckout("license", null)
                        },
                        onPurchaseDaypassClick = { unlockCount ->
                            stripeViewModel.startStripeCheckout("daypass", unlockCount ?: 1)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent called with: $intent")
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            Log.i("MainActivity", "Handling deep link: $uri")
            if (uri.scheme == "app" && uri.host == "com.example.timekeeper") {
                val pathSegments = uri.pathSegments
                if (pathSegments.contains("checkout-success")) {
                    val sessionId = uri.getQueryParameter("session_id")
                    val productType = uri.getQueryParameter("product_type")
                    if (sessionId != null && productType != null) {
                        Log.i("MainActivity", "Deep link: Checkout successful! Session ID: $sessionId, Product Type: $productType")
                        stripeViewModel.confirmStripePayment(sessionId, productType)
                    } else {
                        Log.e("MainActivity", "Deep link: Session ID or Product Type not found in success URL")
                    }
                } else if (pathSegments.contains("checkout-cancel")) {
                    Log.i("MainActivity", "Deep link: Checkout cancelled.")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TimekeeperTheme {
        Greeting("Android")
    }
}