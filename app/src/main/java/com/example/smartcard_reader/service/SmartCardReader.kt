package com.example.smartcard_reader

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Base64
import android.util.Log
import amlib.ccid.Reader
import amlib.hw.HWType
import amlib.hw.HardwareInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class SmartCardReader(private val context: Context) {

    companion object {
        private const val TAG = "SmartCardReader"
    }

    private var usbManager: UsbManager? = null
    private var usbDevice: UsbDevice? = null
    private var hardware: HardwareInterface? = null
    private var reader: Reader? = null

    var onReaderReady: (() -> Unit)? = null
    var onReaderDisconnected: (() -> Unit)? = null

    fun initialize() {
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        listUsbDevices()
    }

    private fun listUsbDevices() {
        val devices = usbManager?.deviceList?.values
        if (devices.isNullOrEmpty()) {
            usbDevice = null
            return
        }
        
        devices.forEach { device ->
            Log.d(TAG, "🔍 USB FOUND")
            Log.d(TAG, "  VendorId  = ${Integer.toHexString(device.vendorId)}")
            Log.d(TAG, "  ProductId = ${Integer.toHexString(device.productId)}")
            Log.d(TAG, "  Interfaces= ${device.interfaceCount}")
            usbDevice = device
        }
    }

    fun isDeviceConnected(): Boolean {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val devices = manager.deviceList.values
        if (devices.isNullOrEmpty()) return false
        
        val currentDevice = usbDevice ?: return false
        return devices.any { it.vendorId == currentDevice.vendorId && it.productId == currentDevice.productId }
    }

    fun openReader(): Boolean {
        try {
            val device = usbDevice
            if (device == null) {
                Log.e(TAG, "❌ No USB device found")
                return false
            }

            if (usbManager?.hasPermission(device) != true) {
                Log.e(TAG, "❌ No USB permission")
                return false
            }

            Log.d(TAG, "⚙️ Initializing reader...")

            hardware = HardwareInterface(HWType.eUSB, context)
            val ok = hardware?.Init(usbManager, device) ?: false
            Log.d(TAG, "Hardware.Init = $ok")

            if (!ok) {
                Log.e(TAG, "❌ Hardware init failed")
                return false
            }

            reader = Reader(hardware)
            val openStatus = reader?.open() ?: -1
            Log.d(TAG, "reader.open = $openStatus")

            if (openStatus != 0) {
                Log.e(TAG, "❌ reader.open failed")
                return false
            }

            reader?.setSlot(0.toByte())
            Log.d(TAG, "reader.setSlot(0)")

            val power = reader?.setPower(Reader.CCID_POWERON) ?: -1
            Log.d(TAG, "reader.setPower = $power")

            if (power == 0) {
                Log.d(TAG, "✅ Reader is READY!")
                onReaderReady?.invoke()
                return true
            } else {
                Log.e(TAG, "❌ setPower failed with code: $power")
                return false
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ openReader error", e)
            return false
        }
    }

    suspend fun readBasicInfo(): Map<String, String>? = withContext(Dispatchers.IO) {
        try {
            if (reader == null) {
                Log.e(TAG, "❌ Reader not initialized")
                return@withContext null
            }

            Log.d(TAG, "📋 Reading Thai ID card data...")

            val resp = ByteArray(300)
            val len = IntArray(1)

            Log.d(TAG, "🔌 Power ON card...")
            val power = reader?.setPower(Reader.CCID_POWERON) ?: -1
            if (power != 0) {
                Log.e(TAG, "❌ No card detected or power failed: $power")
                
                if (!isDeviceConnected()) {
                    Log.e(TAG, "❌ Device disconnected!")
                    onReaderDisconnected?.invoke()
                }
                
                return@withContext null
            }
            val selectApdu = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00,
                0x08,
                0xA0.toByte(), 0x00, 0x00, 0x00,
                0x54, 0x48, 0x00, 0x01
            )
            reader?.transmit(selectApdu, selectApdu.size, resp, len)

            if (len[0] >= 2 && resp[0] == 0x61.toByte()) {
                val getResp = byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, resp[1])
                reader?.transmit(getResp, getResp.size, resp, len)
            }

            if (len[0] < 2 || (resp[len[0] - 2] != 0x90.toByte() && resp[len[0] - 2] != 0x61.toByte())) {
                Log.e(TAG, "❌ Card not responding or not Thai ID card")
                return@withContext null
            }

            val fields = arrayOf(
                ThaiIDField("CID", "เลขบัตรประชาชน",
                    byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x00, 0x04, 0x02, 0x00, 0x0d)),
                ThaiIDField("NameTH", "ชื่อ-สกุล (ไทย)",
                    byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x00, 0x11, 0x02, 0x00, 0x64)),
                ThaiIDField("NameEN", "ชื่อ-สกุล (English)",
                    byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x00, 0x75, 0x02, 0x00, 0x64)),
                ThaiIDField("BirthDate", "วันเกิด",
                    byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x00, 0xD9.toByte(), 0x02, 0x00, 0x08)),
                ThaiIDField("Gender", "เพศ",
                    byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x00, 0xE1.toByte(), 0x02, 0x00, 0x01)),
                ThaiIDField("Issuer", "ผู้ออกบัตร",
                    byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x00, 0xF6.toByte(), 0x02, 0x00, 0x64)),
                ThaiIDField("IssueDate", "วันออกบัตร",
                    byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x01, 0x67, 0x02, 0x00, 0x08)),
                ThaiIDField("ExpireDate", "วันหมดอายุ",
                    byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x01, 0x6F, 0x02, 0x00, 0x08)),
                ThaiIDField("Address", "ที่อยู่",
                    byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x15, 0x79, 0x02, 0x00, 0x64))
            )

            val resultMap = mutableMapOf<String, String>()

            fields.forEach { field ->
                Log.d(TAG, "━━━ Reading: ${field.nameTH} ━━━")
                val data = readFieldDirect(field.command)

                if (!data.isNullOrEmpty()) {
                    val formattedData = when (field.key) {
                        "NameTH", "NameEN", "Address", "Issuer" -> data.replace("#", " ").trim()
                        "BirthDate", "IssueDate", "ExpireDate" -> formatDate(data)
                        "Gender" -> formatGender(data)
                        else -> data
                    }
                    
                    resultMap[field.key] = formattedData
                    
                    if (field.key in listOf("BirthDate", "IssueDate", "ExpireDate")) {
                        resultMap["${field.key}EN"] = formatDateEnglish(data)
                    }
                    
                    Log.d(TAG, "✅ ${field.nameTH} = $formattedData")
                } else {
                    resultMap[field.key] = ""
                    Log.d(TAG, "⚠️ ${field.nameTH} = (no data)")
                }
            }

            resultMap

        } catch (e: Exception) {
            Log.e(TAG, "❌ readBasicInfo error", e)
            if (!isDeviceConnected()) {
                onReaderDisconnected?.invoke()
            }
            null
        }
    }

    private fun formatDate(dateStr: String): String {
        if (dateStr.length != 8) return dateStr
        val year = dateStr.substring(0, 4)
        val month = dateStr.substring(4, 6)
        val day = dateStr.substring(6, 8)
        val monthNamesTH = arrayOf("", "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
            "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.")
        val monthInt = month.toIntOrNull() ?: 0
        val monthName = if (monthInt in 1..12) monthNamesTH[monthInt] else month
        return "$day $monthName $year"
    }

    private fun formatDateEnglish(dateStr: String): String {
        if (dateStr.length != 8) return dateStr
        val yearBE = dateStr.substring(0, 4).toIntOrNull() ?: return dateStr
        val yearCE = yearBE - 543
        val month = dateStr.substring(4, 6)
        val day = dateStr.substring(6, 8)
        val monthNamesEN = arrayOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthInt = month.toIntOrNull() ?: 0
        val monthName = if (monthInt in 1..12) monthNamesEN[monthInt] else month
        return "$day $monthName $yearCE"
    }

    private fun formatGender(genderCode: String): String {
        return when (genderCode) {
            "1" -> "ชาย"
            "2" -> "หญิง"
            else -> genderCode
        }
    }

    suspend fun readPhoto(): String? = withContext(Dispatchers.IO) {
        try {
            if (reader == null) {
                Log.e(TAG, "❌ Reader not initialized")
                return@withContext null
            }

            Log.d(TAG, "📸 Reading photo from Thai ID card...")

            val power = reader?.setPower(Reader.CCID_POWERON) ?: -1
            if (power != 0) {
                Log.e(TAG, "❌ Power ON failed")
                return@withContext null
            }

            val selectApdu = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00,
                0x08,
                0xA0.toByte(), 0x00, 0x00, 0x00,
                0x54, 0x48, 0x00, 0x01
            )
            val resp = ByteArray(300)
            val len = IntArray(1)

            reader?.transmit(selectApdu, selectApdu.size, resp, len)

            if (len[0] >= 2 && resp[0] == 0x61.toByte()) {
                val getResp = byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, resp[1])
                reader?.transmit(getResp, getResp.size, resp, len)
            }

            val photoCommands = arrayOf(
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x01, 0x7B, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x02, 0x7A, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x03, 0x79, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x04, 0x78, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x05, 0x77, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x06, 0x76, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x07, 0x75, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x08, 0x74, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x09, 0x73, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x0A, 0x72, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x0B, 0x71, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x0C, 0x70, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x0D, 0x6F, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x0E, 0x6E, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x0F, 0x6D, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x10, 0x6C, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x11, 0x6B, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x12, 0x6A, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x13, 0x69, 0x02, 0x00, 0xFF.toByte()),
                byteArrayOf(0x80.toByte(), 0xb0.toByte(), 0x14, 0x68, 0x02, 0x00, 0xFF.toByte())
            )

            val photoStream = ByteArrayOutputStream()

            photoCommands.forEachIndexed { index, command ->
                Log.d(TAG, "📦 Reading photo part ${index + 1}/20...")
                val photoData = readPhotoPartDirect(command)
                if (photoData != null && photoData.isNotEmpty()) {
                    photoStream.write(photoData)
                    Log.d(TAG, "✅ Part ${index + 1} OK (${photoData.size} bytes)")
                } else {
                    Log.e(TAG, "❌ Part ${index + 1} FAILED")
                }
            }

            val completePhoto = photoStream.toByteArray()
            Log.d(TAG, "📸 Total photo size: ${completePhoto.size} bytes")

            if (completePhoto.isNotEmpty()) {
                val base64Photo = Base64.encodeToString(completePhoto, Base64.DEFAULT)
                Log.d(TAG, "✅ Photo read successfully!")
                base64Photo
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ readPhoto error", e)
            null
        }
    }

    private fun readPhotoPartDirect(cmd: ByteArray): ByteArray? {
        try {
            val resp = ByteArray(300)
            val len = IntArray(1)

            reader?.transmit(cmd, cmd.size, resp, len)

            if (len[0] < 2) return null

            var sw1 = resp[len[0] - 2]
            var sw2 = resp[len[0] - 1]

            if (sw1 == 0x61.toByte()) {
                val getResp = byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, sw2)
                reader?.transmit(getResp, getResp.size, resp, len)

                if (len[0] < 2) return null
                sw1 = resp[len[0] - 2]
                sw2 = resp[len[0] - 1]
            }

            if (sw1 == 0x90.toByte() && sw2 == 0x00.toByte() && len[0] > 2) {
                val dataLen = len[0] - 2
                val photoData = ByteArray(dataLen)
                System.arraycopy(resp, 0, photoData, 0, dataLen)
                return photoData
            }

            return null

        } catch (e: Exception) {
            Log.e(TAG, "  ❌ Exception reading photo part: ${e.message}")
            return null
        }
    }

    private fun readFieldDirect(cmd: ByteArray): String? {
        try {
            val resp = ByteArray(300)
            val len = IntArray(1)

            reader?.transmit(cmd, cmd.size, resp, len)

            if (len[0] < 2) return null

            var sw1 = resp[len[0] - 2]
            var sw2 = resp[len[0] - 1]

            if (sw1 == 0x61.toByte()) {
                val getResp = byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, sw2)
                reader?.transmit(getResp, getResp.size, resp, len)

                if (len[0] < 2) return null
                sw1 = resp[len[0] - 2]
                sw2 = resp[len[0] - 1]
            }

            if (sw1 == 0x90.toByte() && sw2 == 0x00.toByte() && len[0] > 2) {
                val dataLen = len[0] - 2

                return try {
                    String(resp, 0, dataLen, charset("TIS-620")).trim().takeIf { it.isNotEmpty() }
                } catch (e: Exception) {
                    String(resp, 0, dataLen, Charsets.UTF_8).trim().takeIf { it.isNotEmpty() }
                }
            }

            return null

        } catch (e: Exception) {
            Log.e(TAG, "  ❌ Exception: ${e.message}")
            return null
        }
    }

    private data class ThaiIDField(
        val key: String,
        val nameTH: String,
        val command: ByteArray
    )

    fun close() {
        try {
            reader?.close()
            hardware?.Close()
            Log.d(TAG, "🔒 Reader closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing reader", e)
        }
    }
}
