package com.example.smartcard_reader

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.smartcard_reader.ui.screens.CardReaderScreen
import com.example.smartcard_reader.ui.theme.CardReaderTheme
import com.example.smartcard_reader.util.UsbPermissionHandler
import com.example.smartcard_reader.viewmodel.CardReaderViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.core.content.ContextCompat
import com.example.smartcard_reader.service.CardReaderBackgroundService

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CardReaderViewModel
    private lateinit var usbHandler: UsbPermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start background service
        val intent = Intent(this, CardReaderBackgroundService::class.java)
        ContextCompat.startForegroundService(this, intent)

        // สร้าง ViewModel
        viewModel = ViewModelProvider(this)[CardReaderViewModel::class.java]
        viewModel.initialize(this)

        // ตั้งค่า callback สำหรับปุ่ม reconnect
        viewModel.onReconnectRequested = {
            requestUsbPermissionAgain()
        }

        // จัดการ USB Permission
        requestUsbPermissionAgain()

        // ตั้งค่า UI
        setContent {
            CardReaderTheme {
                CardReaderScreen(viewModel = viewModel)
            }
        }
    }

    private fun requestUsbPermissionAgain() {
        usbHandler = UsbPermissionHandler(this) { granted ->
            if (granted) {
                viewModel.onUsbPermissionGranted()
            } else {
                viewModel.onUsbPermissionDenied()
            }
        }
        usbHandler.requestPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::viewModel.isInitialized) {
            viewModel.cleanup()
        }
        if (::usbHandler.isInitialized) {
            usbHandler.cleanup()
        }
    }
}





































//การเช็คเรื่อง Device ID เพื่ออนุญาตให้ใช้งานแอปพลิเคชัน
// class MainActivity : ComponentActivity() {

//     private lateinit var viewModel: CardReaderViewModel
//     private lateinit var usbHandler: UsbPermissionHandler

//     // กำหนด Android ID ที่อนุญาตให้ใช้งาน 
//     // c4b4dd27cdb5aba7 คือ Device ID ของอุปกรณ์ที่ได้รับอนุญาต
//     private val ALLOWED_ANDROID_ID = "c4b4dd27cdb5aba7"

//     override fun onCreate(savedInstanceState: Bundle?) {
//         super.onCreate(savedInstanceState)

//         // ตรวจสอบ Device ID
//         if (!isDeviceAuthorized()) {
//             showUnauthorizedScreen()
//             return
//         }

//         // สร้าง ViewModel
//         viewModel = ViewModelProvider(this)[CardReaderViewModel::class.java]
//         viewModel.initialize(this)

//         // ตั้งค่า callback สำหรับปุ่ม reconnect
//         viewModel.onReconnectRequested = {
//             requestUsbPermissionAgain()
//         }

//         // จัดการ USB Permission
//         requestUsbPermissionAgain()

//         // ตั้งค่า UI
//         setContent {
//             CardReaderTheme {
//                 CardReaderScreen(viewModel = viewModel)
//             }
//         }
//     }

//     private fun isDeviceAuthorized(): Boolean {
//         val androidId = Settings.Secure.getString(
//             contentResolver,
//             Settings.Secure.ANDROID_ID
//         )
        
//         Log.d("DeviceAuth", "Current Android ID: $androidId")
//         Log.d("DeviceAuth", "Allowed Android ID: $ALLOWED_ANDROID_ID")
        
//         val isAuthorized = androidId == ALLOWED_ANDROID_ID
//         Log.d("DeviceAuth", "Device Authorized: $isAuthorized")
        
//         return isAuthorized
//     }

//     private fun showUnauthorizedScreen() {
//         setContent {
//             CardReaderTheme {
//                 UnauthorizedScreen()
//             }
//         }
//     }

//     @Composable
//     private fun UnauthorizedScreen() {
//         Surface(
//             modifier = Modifier.fillMaxSize(),
//             color = MaterialTheme.colorScheme.background
//         ) {
//             Column(
//                 modifier = Modifier
//                     .fillMaxSize()
//                     .padding(24.dp),
//                 horizontalAlignment = Alignment.CenterHorizontally,
//                 verticalArrangement = Arrangement.Center
//             ) {
//                 Icon(
//                     imageVector = androidx.compose.material.icons.Icons.Default.Lock,
//                     contentDescription = "Unauthorized",
//                     modifier = Modifier.size(80.dp),
//                     tint = MaterialTheme.colorScheme.error
//                 )
                
//                 Spacer(modifier = Modifier.height(24.dp))
                
//                 Text(
//                     text = "ไม่ได้รับอนุญาต",
//                     style = MaterialTheme.typography.headlineMedium,
//                     color = MaterialTheme.colorScheme.error
//                 )
                
//                 Spacer(modifier = Modifier.height(16.dp))
                
//                 Text(
//                     text = "อุปกรณ์นี้ไม่ได้รับอนุญาตให้ใช้งานแอปพลิเคชัน",
//                     style = MaterialTheme.typography.bodyLarge,
//                     textAlign = TextAlign.Center,
//                     color = MaterialTheme.colorScheme.onBackground
//                 )
                
//                 Spacer(modifier = Modifier.height(8.dp))
                
//                 val currentId = Settings.Secure.getString(
//                     contentResolver,
//                     Settings.Secure.ANDROID_ID
//                 )
                
//                 Text(
//                     text = "Device ID: $currentId",
//                     style = MaterialTheme.typography.bodySmall,
//                     textAlign = TextAlign.Center,
//                     color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
//                 )
                
//                 Spacer(modifier = Modifier.height(24.dp))
                
//                 Button(
//                     onClick = { finish() },
//                     colors = ButtonDefaults.buttonColors(
//                         containerColor = MaterialTheme.colorScheme.error
//                     )
//                 ) {
//                     Text("ปิดแอป")
//                 }
//             }
//         }
//     }

//     private fun requestUsbPermissionAgain() {
//         usbHandler = UsbPermissionHandler(this) { granted ->
//             if (granted) {
//                 viewModel.onUsbPermissionGranted()
//             } else {
//                 viewModel.onUsbPermissionDenied()
//             }
//         }
//         usbHandler.requestPermission()
//     }

//     override fun onDestroy() {
//         super.onDestroy()
//         if (::viewModel.isInitialized) {
//             viewModel.cleanup()
//         }
//         if (::usbHandler.isInitialized) {
//             usbHandler.cleanup()
//         }
//     }
// }