package com.example.smartcard_reader.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcard_reader.SmartCardReader
import com.example.smartcard_reader.SpringBootService
import com.example.smartcard_reader.data.model.CardData
import com.example.smartcard_reader.service.CardReaderForegroundService
import com.example.smartcard_reader.util.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CardReaderViewModel : ViewModel() {

    // State
    val cardData = mutableStateOf<CardData?>(null)
    val photoBase64 = mutableStateOf<String?>(null)
    val statusMessage = mutableStateOf("⏳ กำลังเริ่มต้นระบบ...\nกรุณารอสักครู่")
    val isLoading = mutableStateOf(false)
    val isReadingPhoto = mutableStateOf(false)
    val connectionStatus = mutableStateOf(ConnectionStatus.DISCONNECTED)
    val autoReadEnabled = mutableStateOf(false)
    val serverUrl = mutableStateOf("")

    // Services
    private var springService: SpringBootService? = null
    private lateinit var cardReader: SmartCardReader
    private var readerInitialized = false
    private lateinit var appContext: Context

    // Callback for reconnect
    var onReconnectRequested: (() -> Unit)? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext

        // Load saved URL or use default
        val savedUrl = getSavedServerUrl()
        serverUrl.value = savedUrl

        setupSpringService(savedUrl)
        setupCardReader(context)
    }

    private fun getSavedServerUrl(): String {
        val prefs = appContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Constants.KEY_SERVER_URL, Constants.DEFAULT_SPRING_BOOT_URL)
            ?: Constants.DEFAULT_SPRING_BOOT_URL
    }

    private fun saveServerUrl(url: String) {
        appContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(Constants.KEY_SERVER_URL, url)
            .apply()
    }

    private fun setupCardReader(context: Context) {
        cardReader = SmartCardReader(context).apply {
            initialize()
            onReaderReady = { handleReaderReady() }
            onReaderDisconnected = { handleReaderDisconnected() }
        }
    }

    private fun setupSpringService(url: String) {
        springService?.dispose()
        springService = SpringBootService(baseUrl = url)

        viewModelScope.launch {
            Log.d(TAG, "🔌 Testing connection to: $url")
            val connected = springService?.testConnection() == true

            if (connected) {
                Log.d(TAG, "✅ Connection OK - Starting command listener")
                springService?.apply {
                    onCommandReceived = { command ->
                        Log.d(TAG, "📥 Callback triggered with: $command")
                        handleCommand(command)
                    }
                    startListeningForCommands()
                }
            } else {
                Log.e(TAG, "❌ Connection FAILED - Cannot start listener")
                statusMessage.value = "❌ ไม่สามารถเชื่อมต่อ Spring Boot\nตรวจสอบ URL: $url"
            }
        }
    }

    fun updateServerUrl(newUrl: String) {
        val formattedUrl = formatUrl(newUrl.trim())
        serverUrl.value = formattedUrl
        saveServerUrl(formattedUrl)
        setupSpringService(formattedUrl)
    }

    private fun formatUrl(url: String): String {
        return if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "http://$url"
        } else {
            url
        }
    }

    fun onUsbPermissionGranted() {
        Log.d(TAG, "🔐 USB Permission granted, opening reader...")
        connectionStatus.value = ConnectionStatus.CONNECTED
        openReader()
    }

    fun onUsbPermissionDenied() {
        Log.e(TAG, "❌ USB Permission denied")
        connectionStatus.value = ConnectionStatus.DISCONNECTED
        statusMessage.value = "❌ ไม่ได้รับอนุญาตใช้งาน USB\nกรุณาอนุญาตและกดปุ่ม \"เชื่อมต่อใหม่\""
    }

    private fun handleReaderReady() {
        Log.d(TAG, "✅ Card Reader is ready!")
        readerInitialized = true
        connectionStatus.value = ConnectionStatus.READY
        statusMessage.value = "✅ พร้อมใช้งาน\nเสียบบัตรเพื่อเริ่มอ่านข้อมูล"

        viewModelScope.launch {
            springService?.sendStatus("เชื่อมต่อสำเร็จ")

            if (autoReadEnabled.value) {
                delay(Constants.AUTO_READ_DELAY_MS)
                readCard()
            }
        }
    }

    private fun handleReaderDisconnected() {
        viewModelScope.launch {
            Log.e(TAG, "🔌 Reader disconnected!")
            readerInitialized = false
            connectionStatus.value = ConnectionStatus.DISCONNECTED
            statusMessage.value = "ไม่เชื่อมต่อ\nกรุณาเสียบเครื่องอ่านบัตรใหม่\nและกดปุ่ม \nเชื่อมต่อใหม่\n"
            springService?.sendStatus("ไม่เชื่อมต่อ")
        }
    }

    private fun openReader() {
        viewModelScope.launch {
            statusMessage.value = "⏳ กำลังเชื่อมต่อเครื่องอ่านบัตร..."
            springService?.sendStatus("กำลังเชื่อมต่อเครื่องอ่านบัตร...")

            val success = cardReader.openReader()

            if (success) {
                handleReaderOpenSuccess()
            } else {
                handleReaderOpenFailure()
            }
        }
    }

    private suspend fun handleReaderOpenSuccess() {
        Log.d(TAG, "✅ Reader opened successfully")
        readerInitialized = true
        connectionStatus.value = ConnectionStatus.READY
        statusMessage.value = "✅ พร้อมใช้งาน\nเสียบบัตรเพื่อเริ่มอ่านข้อมูล"
        springService?.sendStatus("เชื่อมต่อสำเร็จ")
    }

    private suspend fun handleReaderOpenFailure() {
        Log.e(TAG, "❌ Failed to open reader")
        readerInitialized = false
        connectionStatus.value = ConnectionStatus.DISCONNECTED
        statusMessage.value = "❌ ไม่สามารถเชื่อมต่อได้\n" +
                "กรุณาเสียบเครื่องอ่านบัตรใหม่\n" +
                "และตรวจสอบว่ามีบัตรอยู่ในเครื่อง\n" +
                "แล้วกดปุ่ม \"เชื่อมต่อใหม่\""
        springService?.sendStatus("เชื่อมต่อล้มเหลว")
    }

    fun reconnectReader() {
        Log.d(TAG, "🔄 Reconnecting reader...")
        viewModelScope.launch {
            try {
                cardReader.close()
                readerInitialized = false
                delay(RECONNECT_DELAY_MS)

                connectionStatus.value = ConnectionStatus.CONNECTING
                statusMessage.value = "⏳ กำลังเชื่อมต่อใหม่...\nกรุณารอสักครู่"

                cardReader.initialize()
                delay(RECONNECT_DELAY_MS)

                onReconnectRequested?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Error reconnecting", e)
                connectionStatus.value = ConnectionStatus.DISCONNECTED
                statusMessage.value = "❌ เกิดข้อผิดพลาด\n${e.message}"
            }
        }
    }

    private fun handleCommand(command: String) {
        Log.d(TAG, "🎮 Handling command: $command")
        viewModelScope.launch {
            when (command) {
                COMMAND_READ_CARD -> readCard()
                COMMAND_CLEAR_DATA -> clearData()
                COMMAND_SAVE_DATA -> saveData()
                else -> Log.w(TAG, "⚠️ Unknown command: $command")
            }
        }
    }

    fun startService(context: Context) {
        val intent = Intent(context, CardReaderForegroundService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopService(context: Context) {
        val intent = Intent(context, CardReaderForegroundService::class.java)
        context.stopService(intent)
    }

    fun readCard() {
        if (!validateReaderConnection()) return

        viewModelScope.launch {
            try {
                isLoading.value = true
                updateReadingStatus()

                val data = readCardDataWithRetry()

                if (data.isNullOrEmpty() || data["CID"].isNullOrEmpty()) {
                    handleReadFailure()
                    return@launch
                }

                processCardData(data)
            } catch (e: Exception) {
                handleReadError(e)
            } finally {
                isLoading.value = false
                isReadingPhoto.value = false
            }
        }
    }

    private fun validateReaderConnection(): Boolean {
        if (!cardReader.isDeviceConnected()) {
            Log.e(TAG, "❌ Device not connected physically")
            readerInitialized = false
            connectionStatus.value = ConnectionStatus.DISCONNECTED
            statusMessage.value = "ไม่เชื่อมต่อ\n" +
                    "กรุณาเสียบเครื่องอ่านบัตรใหม่\n" +
                    "และกดปุ่ม \nเชื่อมต่อใหม่\n"
            return false
        }

        if (!readerInitialized) {
            statusMessage.value = "❌ เครื่องอ่านบัตรไม่พร้อม\nกรุณากดปุ่ม \"เชื่อมต่อใหม่\""
            return false
        }

        return true
    }

    private suspend fun updateReadingStatus() {
        statusMessage.value = "⏳ กำลังอ่านข้อมูลบัตร...\nกรุณารอสักครู่"
        springService?.sendStatus("กำลังอ่านบัตร...")
        springService?.startListeningForCommands()
    }

    private suspend fun readCardDataWithRetry(): Map<String, String>? {
        var data: Map<String, String>? = null
        var retryCount = 0

        while (data == null && retryCount < Constants.MAX_RETRY_COUNT) {
            if (retryCount > 0) {
                statusMessage.value = "⏳ กำลังลองอีกครั้ง ($retryCount/${Constants.MAX_RETRY_COUNT})..."
                delay(Constants.RETRY_DELAY_MS)
            }
            data = cardReader.readBasicInfo()
            retryCount++
        }

        return data
    }

    private suspend fun handleReadFailure() {
        statusMessage.value = if (!cardReader.isDeviceConnected()) {
            "ไม่เชื่อมต่อ\nกรุณาเสียบเครื่องอ่านบัตรใหม่\nและกดปุ่ม \nเชื่อมต่อใหม่\n"
        } else {
            "ไม่พบบัตรประชาชน\nกรุณาเสียบบัตรให้แน่น"
        }
        springService?.sendStatus("️️ไม่พบบัตรประชาชน")
        isLoading.value = false
    }

    private suspend fun processCardData(data: Map<String, String>) {
        cardData.value = CardData.fromMap(data)

        val photo = readCardPhoto()

        val success = springService?.sendCardData(data, photo) == true
        updateFinalStatus(success)
    }

    private suspend fun readCardPhoto(): String? {
        isReadingPhoto.value = true
        statusMessage.value = "⏳ กำลังอ่านรูปภาพ...\n(ประมาณ 5-10 วินาที)"
        springService?.sendStatus("กำลังอ่านรูปภาพ...")

        val photo = cardReader.readPhoto()
        if (photo != null) photoBase64.value = photo

        isReadingPhoto.value = false
        return photo
    }

    private suspend fun updateFinalStatus(success: Boolean) {
        if (success) {
            statusMessage.value = "สำเร็จ!\nถอดบัตรออกแล้วเสียบใหม่เพื่ออ่านต่อ"
            springService?.sendStatus("อ่านบัตรสำเร็จ")
        } else {
            statusMessage.value = "⚠️ อ่านได้แต่ส่งข้อมูลไม่สำเร็จ\nตรวจสอบ IP: ${serverUrl.value}"
            springService?.sendStatus("ส่งข้อมูลล้มเหลว")
        }
    }

    private suspend fun handleReadError(e: Exception) {
        Log.e(TAG, "❌ Error reading card", e)
        statusMessage.value = "❌ เกิดข้อผิดพลาด\n${e.message}"
        springService?.sendStatus("❌ เกิดข้อผิดพลาด")
    }

    fun clearData() {
        viewModelScope.launch {
            cardData.value = null
            photoBase64.value = null
            statusMessage.value = "✅ เคลียข้อมูลแล้ว"
            springService?.sendStatus("เคลียข้อมูลแล้ว")

            if (autoReadEnabled.value) {
                delay(AUTO_READ_AFTER_CLEAR_DELAY_MS)
                readCard()
            }
        }
    }

    fun saveData() {
        viewModelScope.launch {
            statusMessage.value = "💾 บันทึกข้อมูลเรียบร้อย"
            springService?.sendStatus("บันทึกข้อมูลเรียบร้อย")
        }
    }

    fun toggleAutoRead() {
        autoReadEnabled.value = !autoReadEnabled.value
    }

    fun cleanup() {
        springService?.dispose()
        cardReader.close()
    }

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        READY
    }

    companion object {
        private const val TAG = "CardReaderViewModel"
        private const val RECONNECT_DELAY_MS = 500L
        private const val AUTO_READ_AFTER_CLEAR_DELAY_MS = 1000L

        // Command constants
        private const val COMMAND_READ_CARD = "read_card"
        private const val COMMAND_CLEAR_DATA = "clear_data"
        private const val COMMAND_SAVE_DATA = "save_data"
    }
}