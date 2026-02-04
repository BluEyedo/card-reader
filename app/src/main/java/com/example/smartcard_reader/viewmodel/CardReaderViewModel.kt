package com.example.smartcard_reader.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcard_reader.data.model.CardData
import com.example.smartcard_reader.SmartCardReader
import com.example.smartcard_reader.SpringBootService
import com.example.smartcard_reader.util.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CardReaderViewModel : ViewModel() {

    companion object {
        private const val TAG = "CardReaderViewModel"
    }
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

    // ✅ เพิ่ม callback สำหรับ reconnect
    var onReconnectRequested: (() -> Unit)? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        
        // Load saved URL or use default
        val prefs = appContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val savedUrl = prefs.getString(Constants.KEY_SERVER_URL, Constants.DEFAULT_SPRING_BOOT_URL) ?: Constants.DEFAULT_SPRING_BOOT_URL
        serverUrl.value = savedUrl
        
        setupSpringService(savedUrl)

        // สร้าง Card Reader
        cardReader = SmartCardReader(context)
        cardReader.initialize()

        cardReader.onReaderReady = {
            onReaderReady()
        }
        
        cardReader.onReaderDisconnected = {
            viewModelScope.launch {
                Log.e(TAG, "🔌 Reader disconnected!")
                readerInitialized = false
                connectionStatus.value = ConnectionStatus.DISCONNECTED
                statusMessage.value = "ไม่เชื่อมต่อ\n\nกรุณาเสียบเครื่องอ่านบัตรใหม่\nและกดปุ่ม \nเชื่อมต่อใหม่\n"
                springService?.sendStatus("ไม่เชื่อมต่อ")
            }
        }
    }

    private fun setupSpringService(url: String) {
        springService?.dispose()
        springService = SpringBootService(baseUrl = url)

        viewModelScope.launch {
            Log.d(TAG, "🔌 Testing connection to: $url")
            val connected = springService?.testConnection() ?: false
            
            if (connected) {
                Log.d(TAG, "✅ Connection OK - Starting command listener")
                springService?.onCommandReceived = { command ->
                    Log.d(TAG, "📥 Callback triggered with: $command")
                    handleCommand(command)
                }
                springService?.startListeningForCommands()
            } else {
                Log.e(TAG, "❌ Connection FAILED - Cannot start listener")
                statusMessage.value = "❌ ไม่สามารถเชื่อมต่อ Spring Boot\nตรวจสอบ URL: $url"
            }
        }
    }

    fun updateServerUrl(newUrl: String) {
        var formattedUrl = newUrl.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "http://$formattedUrl"
        }
        
        serverUrl.value = formattedUrl
        
        // Save to preferences
        val prefs = appContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.KEY_SERVER_URL, formattedUrl).apply()
        
        // Re-initialize service
        setupSpringService(formattedUrl)
    }

    fun onUsbPermissionGranted() {
        Log.d(TAG, "🔐 USB Permission granted, opening reader...")
        connectionStatus.value = ConnectionStatus.CONNECTED
        openReader()
    }

    fun onUsbPermissionDenied() {
        Log.e(TAG, "❌ USB Permission denied")
        connectionStatus.value = ConnectionStatus.DISCONNECTED
        statusMessage.value = "❌ ไม่ได้รับอนุญาตใช้งาน USB\n\nกรุณาอนุญาตและกดปุ่ม \"เชื่อมต่อใหม่\""
    }

    private fun onReaderReady() {
        Log.d(TAG, "✅ Card Reader is ready!")
        readerInitialized = true
        connectionStatus.value = ConnectionStatus.READY
        statusMessage.value = "✅ พร้อมใช้งาน\nเสียบบัตรเพื่อเริ่มอ่านข้อมูล"

        // ✅ ส่งสถานะเมื่อเชื่อมต่อสำเร็จโดยอัตโนมัติ
        viewModelScope.launch {
            springService?.sendStatus("เชื่อมต่อสำเร็จ")
        }

        if (autoReadEnabled.value) {
            viewModelScope.launch {
                delay(Constants.AUTO_READ_DELAY_MS)
                readCard()
            }
        }
    }

    private fun openReader() {
        viewModelScope.launch {
            statusMessage.value = "⏳ กำลังเชื่อมต่อเครื่องอ่านบัตร..."
            springService?.sendStatus("กำลังเชื่อมต่อเครื่องอ่านบัตร...")

            val success = cardReader.openReader()

            if (success) {
                Log.d(TAG, "✅ Reader opened successfully")
                readerInitialized = true
                connectionStatus.value = ConnectionStatus.READY
                statusMessage.value = "✅ พร้อมใช้งาน\nเสียบบัตรเพื่อเริ่มอ่านข้อมูล"
                springService?.sendStatus("เชื่อมต่อสำเร็จ")
            } else {
                Log.e(TAG, "❌ Failed to open reader")
                readerInitialized = false
                connectionStatus.value = ConnectionStatus.DISCONNECTED
                statusMessage.value = "❌ ไม่สามารถเชื่อมต่อได้\n\nกรุณาเสียบเครื่องอ่านบัตรใหม่\nและตรวจสอบว่ามีบัตรอยู่ในเครื่อง\nแล้วกดปุ่ม \"เชื่อมต่อใหม่\""
                springService?.sendStatus("เชื่อมต่อล้มเหลว")
            }
        }
    }

    fun reconnectReader() {
        Log.d(TAG, "🔄 Reconnecting reader...")
        viewModelScope.launch {
            try {
                cardReader.close()
                readerInitialized = false
                delay(500)
                connectionStatus.value = ConnectionStatus.CONNECTING
                statusMessage.value = "⏳ กำลังเชื่อมต่อใหม่...\nกรุณารอสักครู่"
                cardReader.initialize()
                delay(500)
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
        when (command) {
            "read_card" -> viewModelScope.launch { readCard() }
            "clear_data" -> viewModelScope.launch { clearData() }
            "save_data" -> viewModelScope.launch { saveData() }
            else -> Log.w(TAG, "⚠️ Unknown command: $command")
        }
    }

    fun readCard() {
        // ✅ เพิ่มการตรวจสอบการเชื่อมต่อก่อนเริ่มอ่าน
        if (!cardReader.isDeviceConnected()) {
            Log.e(TAG, "❌ Device not connected physically")
            readerInitialized = false
            connectionStatus.value = ConnectionStatus.DISCONNECTED
            statusMessage.value = "ไม่เชื่อมต่อ\n\nกรุณาเสียบเครื่องอ่านบัตรใหม่\nและกดปุ่ม \nเชื่อมต่อใหม่\n"
            return
        }

        if (!readerInitialized) {
            statusMessage.value = "❌ เครื่องอ่านบัตรไม่พร้อม\nกรุณากดปุ่ม \"เชื่อมต่อใหม่\""
            return
        }

        viewModelScope.launch {
            try {
                isLoading.value = true
                statusMessage.value = "⏳ กำลังอ่านข้อมูลบัตร...\nกรุณารอสักครู่"
                springService?.sendStatus("กำลังอ่านบัตร...")

                var data: Map<String, String>? = null
                var retryCount = 0
                val maxRetries = Constants.MAX_RETRY_COUNT

                while (data == null && retryCount < maxRetries) {
                    if (retryCount > 0) {
                        statusMessage.value = "⏳ กำลังลองอีกครั้ง (${retryCount}/$maxRetries)..."
                        delay(Constants.RETRY_DELAY_MS)
                    }
                    data = cardReader.readBasicInfo()
                    retryCount++
                }

                if (data == null || data["CID"].isNullOrEmpty()) {
                    // ตรวจสอบอีกครั้งว่าหลุดระหว่างอ่านหรือไม่
                    if (!cardReader.isDeviceConnected()) {
                        statusMessage.value = "ไม่เชื่อมต่อ\n\nกรุณาเสียบเครื่องอ่านบัตรใหม่\nและกดปุ่ม \nเชื่อมต่อใหม่\n"
                    } else {
                        statusMessage.value = "⚠️ ไม่พบบัตรประชาชน\n\nกรุณาเสียบบัตรให้แน่น"
                    }
                    springService?.sendStatus("️️ไม่พบบัตรประชาชน")
                    isLoading.value = false
                    return@launch
                }

                cardData.value = CardData.fromMap(data)
                isReadingPhoto.value = true
                statusMessage.value = "⏳ กำลังอ่านรูปภาพ...\n(ประมาณ 5-10 วินาที)"
                springService?.sendStatus("กำลังอ่านรูปภาพ...")

                val photo = cardReader.readPhoto()
                if (photo != null) photoBase64.value = photo
                isReadingPhoto.value = false

                val success = springService?.sendCardData(data, photo) ?: false
                if (success) {
                    statusMessage.value = "✅ สำเร็จ!\n\nถอดบัตรออกแล้วเสียบใหม่เพื่ออ่านต่อ"
                    springService?.sendStatus("อ่านบัตรสำเร็จ")
                } else {
                    statusMessage.value = "⚠️ อ่านได้แต่ส่งข้อมูลไม่สำเร็จ\nตรวจสอบ IP: ${serverUrl.value}"
                    springService?.sendStatus("ส่งข้อมูลล้มเหลว")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error reading card", e)
                statusMessage.value = "❌ เกิดข้อผิดพลาด\n${e.message}"
                springService?.sendStatus("❌ เกิดข้อผิดพลาด")
            } finally {
                isLoading.value = false
                isReadingPhoto.value = false
            }
        }
    }

    fun clearData() {
        viewModelScope.launch {
            cardData.value = null
            photoBase64.value = null
            statusMessage.value = "✅ เคลียข้อมูลแล้ว"
            springService?.sendStatus("เคลียข้อมูลแล้ว")
            if (autoReadEnabled.value) {
                delay(1000)
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
}
