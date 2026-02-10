package com.example.smartcard_reader.ui.components

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcard_reader.viewmodel.CardReaderViewModel
import okhttp3.internal.wait

@Composable
fun StatusCard(
    connectionStatus: CardReaderViewModel.ConnectionStatus,
    statusMessage: String,
    onReconnect: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val backgroundColor: Color
    val iconColor: Color
    val icon: ImageVector
    val statusText: String
    var scale by remember { mutableFloatStateOf(0.8f) }

    LaunchedEffect(connectionStatus) {
        animate(
            initialValue = 0.9f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { value, _ ->
            scale = value
        }
    }


    when (connectionStatus) {
        CardReaderViewModel.ConnectionStatus.READY -> {
            if(statusMessage.contains("ไม่พบบัตรประชาชน")){
                backgroundColor = Color(0xFFFFFFFF)
                iconColor = Color(0xFFFF9800)
                icon = Icons.Default.Warning
                statusText = "พร้อมใช้งาน"
            }else{
                backgroundColor = Color(0xFFFFFFFF)
                iconColor = Color(0xFF4CAF50)
                icon = Icons.Default.CheckCircle
                statusText = "พร้อมใช้งาน"
            }

        }

        CardReaderViewModel.ConnectionStatus.CONNECTED -> {
            backgroundColor = Color(0xFFFFFFFF)
            iconColor = Color(0xFFFF9800)
            icon = Icons.Default.Info
            statusText = "กำลังเริ่มต้น"
        }
        CardReaderViewModel.ConnectionStatus.CONNECTING -> {
            backgroundColor = Color(0xFFFFFFFF)
            iconColor = Color(0xFF2196F3)
            icon = Icons.Default.Refresh
            statusText = "กำลังเชื่อมต่อ"
        }
        CardReaderViewModel.ConnectionStatus.DISCONNECTED -> {
            backgroundColor = Color(0xFFFFFFFF)
            iconColor = Color(0xFFF44336)
            icon = Icons.Default.Warning
            statusText = "ไม่ได้เชื่อมต่อ"
        }
    }

    // Compact status display
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF616161)
            )
        }

        if (connectionStatus == CardReaderViewModel.ConnectionStatus.DISCONNECTED) {
            FilledTonalIconButton(
                onClick = onReconnect,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reconnect"
                )
            }
        }
    }
}
