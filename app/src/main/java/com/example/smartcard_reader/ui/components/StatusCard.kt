package com.example.smartcard_reader.ui.components

import android.R
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcard_reader.viewmodel.CardReaderViewModel

@Composable
fun StatusCard(
    connectionStatus: CardReaderViewModel.ConnectionStatus,
    statusMessage: String,
    onReconnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionStatus) {
                CardReaderViewModel.ConnectionStatus.READY -> Color(0xFFE8F5E9)
                CardReaderViewModel.ConnectionStatus.CONNECTED -> Color(0xFFFFF3E0)
                CardReaderViewModel.ConnectionStatus.CONNECTING -> Color(0xFFE3F2FD)
                CardReaderViewModel.ConnectionStatus.DISCONNECTED -> Color(0xFFFFEBEE)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (connectionStatus) {
                    CardReaderViewModel.ConnectionStatus.READY -> Icons.Default.CheckCircle
                    CardReaderViewModel.ConnectionStatus.CONNECTED,
                    CardReaderViewModel.ConnectionStatus.CONNECTING -> Icons.Default.Info
                    CardReaderViewModel.ConnectionStatus.DISCONNECTED -> Icons.Default.Warning
                },
                contentDescription = null,
                tint = when (connectionStatus) {
                    CardReaderViewModel.ConnectionStatus.READY -> Color(0xFF4CAF50)
                    CardReaderViewModel.ConnectionStatus.CONNECTED -> Color(0xFFFF9800)
                    CardReaderViewModel.ConnectionStatus.CONNECTING -> MaterialTheme.colorScheme.primary
                    CardReaderViewModel.ConnectionStatus.DISCONNECTED -> Color(0xFFF44336)
                },
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (connectionStatus) {
                        CardReaderViewModel.ConnectionStatus.READY -> "✅ พร้อมใช้งาน"
                        CardReaderViewModel.ConnectionStatus.CONNECTED -> "🔄 กำลังเริ่มต้น"
                        CardReaderViewModel.ConnectionStatus.CONNECTING -> "⏳ กำลังเชื่อมต่อ"
                        CardReaderViewModel.ConnectionStatus.DISCONNECTED -> "❌ ไม่ได้เชื่อมต่อ"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
            if (connectionStatus == CardReaderViewModel.ConnectionStatus.DISCONNECTED) {
                IconButton(onClick = onReconnect) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "เชื่อมต่อใหม่",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}