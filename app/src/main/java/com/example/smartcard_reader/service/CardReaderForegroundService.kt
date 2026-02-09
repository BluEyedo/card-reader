package com.example.smartcard_reader.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.smartcard_reader.SmartCardReader
import com.example.smartcard_reader.SpringBootService
import com.example.smartcard_reader.data.model.CardData
import com.example.smartcard_reader.util.Constants
import kotlinx.coroutines.*

class CardReaderForegroundService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var cardReader: SmartCardReader
    private lateinit var springService: SpringBootService
    private val CHANNEL_ID = "CardReaderServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        cardReader = SmartCardReader(this)
        cardReader.initialize()

        // Load URL from Prefs
        val prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)
        val url = prefs.getString(Constants.KEY_SERVER_URL, Constants.DEFAULT_SPRING_BOOT_URL)!!

        springService = SpringBootService(url)
        springService.onCommandReceived = { command ->
            if (command == "read_card") {
                performBackgroundRead()
            }
        }
        springService.startListeningForCommands()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Card Reader Active")
            .setContentText("Listening for commands from website...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    private fun performBackgroundRead() {
        serviceScope.launch {
            if (cardReader.isDeviceConnected()) {
                val data = cardReader.readBasicInfo()
                if (data != null) {
                    val photo = cardReader.readPhoto()
                    springService.sendCardData(data, photo)
                    springService.sendStatus("Background Read Success")
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(CHANNEL_ID, "Card Reader Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceJob.cancel()
        cardReader.close()
        springService.dispose()
        super.onDestroy()
    }
}