package com.example.smartcard_reader

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SpringBootService(private val baseUrl: String) {

    companion object {
        private const val TAG = "SpringBootService"
        private const val API_KEY = "my-secure-key-123456"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->  // ✅ เพิ่ม Interceptor
            val request = chain.request().newBuilder()
                .addHeader("X-API-KEY", API_KEY)  // หรือใช้ "Authorization", "Bearer $API_KEY"
                .build()
            chain.proceed(request)
        }
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null

    var onCommandReceived: ((String) -> Unit)? = null

    // ส่งข้อมูลบัตรไปยัง Spring Boot
    suspend fun sendCardData(
        cardData: Map<String, String>,
        photoBase64: String?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("type", "card_data")
                put("timestamp", System.currentTimeMillis())
                put("data", JSONObject(cardData))
                put("photo", photoBase64)
            }

            val url = "$baseUrl/api/card/update"
            Log.d(TAG, "📤 Sending card data to: $url")
            Log.d(TAG, "📦 Payload size: ${payload.toString().length} bytes")

            val requestBody = payload.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseCode = response.code
            val responseBody = response.body?.string()

            if (response.isSuccessful) {
                Log.d(TAG, "✅ Data sent successfully")
                true
            } else {
                Log.e(TAG, "❌ Failed with code: $responseCode")
                Log.e(TAG, "❌ Response: $responseBody")
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "❌ Network error: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending data: ${e.message}", e)
            false
        }
    }

    // ส่งสถานะ
    suspend fun sendStatus(status: String) = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("type", "status")
                put("timestamp", System.currentTimeMillis())
                put("message", status)
            }

            val url = "$baseUrl/api/card/update"
            Log.d(TAG, "📊 Status: $status")

            val requestBody = payload.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d(TAG, "✅ Status sent successfully")
            } else {
                Log.w(TAG, "⚠️ Status send failed: ${response.code}")
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error sending status: ${e.message}")
        }
    }

    // เริ่มฟังคำสั่งจากเว็บ (Polling)
    fun startListeningForCommands() {
        Log.d(TAG, "👂 Starting to listen for commands...")
        Log.d(TAG, "🌐 Polling URL: $baseUrl/api/command/latest")

        pollingJob = scope.launch {
            while (isActive) {
                checkForCommands()
                delay(500) // เช็คทุก 500ms
            }
        }
        
        Log.d(TAG, "✅ Polling job started")
    }

    // หยุดฟังคำสั่ง
    fun stopListeningForCommands() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "🛑 Stopped listening for commands")
    }

    // เช็คว่ามีคำสั่งใหม่หรือไม่
    private suspend fun checkForCommands() = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/command/latest"            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseCode = response.code
            val body = response.body?.string()
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ HTTP Error $responseCode")
                return@withContext
            }
            if (body.isNullOrBlank()) {
                return@withContext
            }
            val json = JSONObject(body)
            Log.d(TAG, "📦 Parsed JSON: $json")
            if (json.optBoolean("hasCommand", false)) {
                val command = json.getString("command")
                Log.d(TAG, "📨 Received command: $command")

                withContext(Dispatchers.Main) {
                    Log.d(TAG, "🎯 Invoking callback for: $command")
                    onCommandReceived?.invoke(command)
                }

                acknowledgeCommand(command)
            } 
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}", e)
        }
    }

    // ยืนยันว่ารับคำสั่งแล้ว
    private suspend fun acknowledgeCommand(command: String) = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("command", command)
            }

            val requestBody = payload.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/api/command/acknowledge")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d(TAG, "✅ Command acknowledged: $command")
            } else {
                Log.w(TAG, "⚠️ Acknowledge failed: ${response.code}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error acknowledging command: ${e.message}")
        }
    }

    // ทดสอบการเชื่อมต่อ
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧪 Testing connection to: $baseUrl/api/command/pending")
            
            val request = Request.Builder()
                .url("$baseUrl/api/command/pending")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            
            if (success) {
                val body = response.body?.string()
                Log.d(TAG, "🧪 Test SUCCESS: $body")
            } else {
                Log.e(TAG, "🧪 Test FAILED: code ${response.code}")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Connection test error: ${e.message}", e)
            false
        }
    }

    // ดึงข้อมูลล่าสุด
    suspend fun getLatestCardData(): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/card/latest")
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                // แปลง JSONObject เป็น Map
                json.keys().asSequence().associateWith { json.get(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting latest data: ${e.message}")
            null
        }
    }

    // Cleanup
    fun dispose() {
        stopListeningForCommands()
        scope.cancel()
        Log.d(TAG, "🔒 Service disposed")
    }
}