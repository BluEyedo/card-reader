package com.example.smartcard_reader.util

object Constants {
    const val DEFAULT_SPRING_BOOT_URL = "http://192.168.1.139:8080"
    const val PREFS_NAME = "smartcard_prefs"
    const val KEY_SERVER_URL = "server_url"
    const val KEY_DEVICE_ID = "device_id"
    const val SPRING_BOOT_URL = DEFAULT_SPRING_BOOT_URL
    const val MAX_RETRY_COUNT = 3
    const val RETRY_DELAY_MS = 1000L
    const val AUTO_READ_DELAY_MS = 500L
}