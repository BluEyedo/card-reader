package com.example.smartcard_reader.service


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smartcard_reader.SmartCardReader
import com.example.smartcard_reader.SpringBootService
import com.example.smartcard_reader.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class CardReaderBackgroundService : Service() {

    companion object {
        const val ACTION_READ_CARD = "ACTION_READ_CARD"
        const val ACTION_CLEAR = "ACTION_CLEAR"
    }

    private lateinit var springService: SpringBootService
    private lateinit var cardReader: SmartCardReader

    override fun onCreate() {
        super.onCreate()

        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        springService = SpringBootService(Constants.DEFAULT_SPRING_BOOT_URL, deviceId)
        cardReader = SmartCardReader(this)

        cardReader.initialize()

        springService.onCommandReceived = { command ->
            when (command) {
                "read_card" -> readCard()
                "clear_data" -> clearData()
            }
        }

        springService.startListeningForCommands()
//        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_READ_CARD -> readCard()
            ACTION_CLEAR -> clearData()
        }
        return START_STICKY
    }

    private fun readCard() {
        CoroutineScope(Dispatchers.IO).launch {
            // ⬅️ انسخ من ViewModel نفس منطق readCard()
            val data = cardReader.readBasicInfo() ?: return@launch
            val photo = cardReader.readPhoto()
            springService.sendCardData(data, photo)
        }
    }

    private fun clearData() {
        // reset internal state if needed
    }

    override fun onBind(intent: Intent?) = null
}
