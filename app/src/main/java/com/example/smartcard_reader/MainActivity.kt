package com.example.smartcard_reader

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.smartcard_reader.service.CardReaderForegroundService
import com.example.smartcard_reader.ui.screens.CardReaderScreen
import com.example.smartcard_reader.ui.theme.CardReaderTheme
import com.example.smartcard_reader.util.UsbPermissionHandler
import com.example.smartcard_reader.viewmodel.CardReaderViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CardReaderViewModel
    private lateinit var usbHandler: UsbPermissionHandler

    // Set to null to disable device authorization, or add allowed device IDs
    private val allowedDeviceIds: Set<String>? = null
    // Example: setOf("c4b4dd27cdb5aba7", "another_device_id")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check device authorization if enabled
        if (allowedDeviceIds != null && !isDeviceAuthorized()) {
            showUnauthorizedScreen()
            return
        }

        startForegroundService()
        initializeViewModel()
        requestUsbPermission()
        setupUI()
    }

    private fun isDeviceAuthorized(): Boolean {
        val androidId = getAndroidId()
        val isAuthorized = allowedDeviceIds?.contains(androidId) == true

        Log.d(TAG, "Current Android ID: $androidId")
        Log.d(TAG, "Device Authorized: $isAuthorized")

        return isAuthorized
    }

    private fun getAndroidId(): String {
        return Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: ""
    }

    private fun showUnauthorizedScreen() {
        setContent {
            CardReaderTheme {
                UnauthorizedScreen(
                    currentDeviceId = getAndroidId(),
                    onClose = ::finish
                )
            }
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, CardReaderForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Log.d(TAG, "Foreground service started")
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(this)[CardReaderViewModel::class.java]
        viewModel.initialize(this)
        viewModel.onReconnectRequested = ::requestUsbPermission
    }

    private fun setupUI() {
        setContent {
            CardReaderTheme {
                CardReaderScreen(viewModel = viewModel)
            }
        }
    }

    private fun requestUsbPermission() {
        usbHandler = UsbPermissionHandler(this) { granted ->
            handleUsbPermissionResult(granted)
        }
        usbHandler.requestPermission()
    }

    private fun handleUsbPermissionResult(granted: Boolean) {
        if (granted) {
            viewModel.onUsbPermissionGranted()
        } else {
            viewModel.onUsbPermissionDenied()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun cleanup() {
        if (::viewModel.isInitialized) {
            viewModel.cleanup()
        }
        if (::usbHandler.isInitialized) {
            usbHandler.cleanup()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@Composable
private fun UnauthorizedScreen(
    currentDeviceId: String,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Unauthorized",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ไม่ได้รับอนุญาต",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "อุปกรณ์นี้ไม่ได้รับอนุญาตให้ใช้งานแอปพลิเคชัน",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Device ID: $currentDeviceId",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("ปิดแอป")
            }
        }
    }
}