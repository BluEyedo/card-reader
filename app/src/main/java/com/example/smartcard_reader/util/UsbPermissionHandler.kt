package com.example.smartcard_reader.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity

class UsbPermissionHandler(
    private val activity: ComponentActivity,
    private val onPermissionResult: (Boolean) -> Unit
) {
    companion object {
        private const val TAG = "UsbPermissionHandler"
        private const val ACTION_USB_PERMISSION = "com.example.smartcard_reader.USB_PERMISSION"
    }

    private var usbManager: UsbManager? = null
    private var usbDevice: UsbDevice? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION != intent.action) return

            val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            Log.d(TAG, "🔐 USB permission = $granted")

            onPermissionResult(granted)

            try {
                activity.unregisterReceiver(this)
            } catch (e: Exception) {
                Log.w(TAG, "Receiver already unregistered")
            }
        }
    }

    fun requestPermission() {
        usbManager = activity.getSystemService(Context.USB_SERVICE) as UsbManager

        usbManager?.deviceList?.values?.forEach { device ->
            Log.d(TAG, "🔍 Found USB: ${Integer.toHexString(device.vendorId)}")
            usbDevice = device
        }

        val device = usbDevice
        if (device == null) {
            Log.e(TAG, "❌ No USB device found")
            onPermissionResult(false)
            return
        }

        if (usbManager?.hasPermission(device) == true) {
            Log.d(TAG, "✅ Already has USB permission")
            onPermissionResult(true)
            return
        }

        Log.d(TAG, "⏳ Requesting USB permission...")

        val permissionIntent = PendingIntent.getBroadcast(
            activity,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val filter = IntentFilter(ACTION_USB_PERMISSION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            activity.registerReceiver(usbReceiver, filter)
        }

        usbManager?.requestPermission(device, permissionIntent)
    }

    fun cleanup() {
        try {
            activity.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }
}
