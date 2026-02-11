package com.example.smartcard_reader

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SpringBootService(private val baseUrl: String, private val deviceId: String) {

    companion object {
        private const val TAG = "SpringBootService"
        private const val API_KEY = "my-secure-key-123456"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-API-KEY", API_KEY)
                .addHeader("X-DEVICE-ID", deviceId) // ✅ Identify which device is making the request
                .build()
            chain.proceed(request)
        }
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null

    var onCommandReceived: ((String) -> Unit)? = null

    // ทดสอบการเชื่อมต่อ
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/command/latest?deviceId=$deviceId"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Connection test failed: ${e.message}")
            false
        }
    }

    // ส่งข้อมูลบัตรไปยัง Spring Boot
    suspend fun sendCardData(
        cardData: Map<String, String>,
        photoBase64: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("type", "card_data")
                put("deviceId", deviceId) // ✅ Include device ID in payload
                put("timestamp", System.currentTimeMillis())
                put("data", JSONObject(cardData))
                put("photo", photoBase64)
            }

            val url = "$baseUrl/api/card/update"
            Log.d(TAG, "📤 Sending card data to: $url")

            val requestBody = payload.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending data: ${e.message}")
            false
        }
    }

    // ส่งสถานะ
    suspend fun sendStatus(status: String) = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("type", "status")
                put("deviceId", deviceId) // ✅ Include device ID
                put("timestamp", System.currentTimeMillis())
                put("message", status)
            }

            val url = "$baseUrl/api/card/update"
            val requestBody = payload.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                // Just execute, don't necessarily need to check response for status updates
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error sending status: ${e.message}")
        }
    }

    // เริ่มฟังคำสั่งจากเว็บ (Polling) - URL now includes deviceId
    fun startListeningForCommands() {
        pollingJob = scope.launch {
            while (isActive) {
                checkForCommands()
                delay(500)
            }
        }
    }

    // หยุดฟังคำสั่ง
    fun stopListeningForCommands() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun checkForCommands() = withContext(Dispatchers.IO) {
        try {
            // ✅ Server can now filter commands for this specific device
            val url = "$baseUrl/api/command/latest?deviceId=$deviceId"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()

                if (response.isSuccessful && !body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    if (json.optBoolean("hasCommand", false)) {
                        val command = json.getString("command")
                        withContext(Dispatchers.Main) {
                            onCommandReceived?.invoke(command)
                        }
                        acknowledgeCommand(command)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}")
        }
    }

    private suspend fun acknowledgeCommand(command: String) = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("command", command)
                put("deviceId", deviceId) // ✅ Acknowledge for this device
            }

            val request = Request.Builder()
                .url("$baseUrl/api/command/acknowledge")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error acknowledging: ${e.message}")
        }
    }

    fun dispose() {
        stopListeningForCommands()
        scope.cancel()
    }
}
