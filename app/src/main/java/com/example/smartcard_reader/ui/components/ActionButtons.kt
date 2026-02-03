package com.example.smartcard_reader.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartcard_reader.viewmodel.CardReaderViewModel

@Composable
fun ActionButtons(
    connectionStatus: CardReaderViewModel.ConnectionStatus,
    isLoading: Boolean,
    hasData: Boolean,
    onReadCard: () -> Unit,
    onSaveData: () -> Unit,
    onClearData: () -> Unit,
    onReconnect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (connectionStatus != CardReaderViewModel.ConnectionStatus.READY) {
            Button(
                onClick = onReconnect,
                modifier = Modifier.weight(1f),colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
) {
Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
Spacer(modifier = Modifier.width(8.dp))
Text("เชื่อมต่อใหม่", fontWeight = FontWeight.Bold)
}
} else {
Button(
onClick = onReadCard,
modifier = Modifier.weight(1f),
enabled = !isLoading,
colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
) {
Icon(Icons.Filled.AccountBox, contentDescription = null, modifier = Modifier.size(20.dp))
Spacer(modifier = Modifier.width(8.dp))
Text("อ่านบัตร", fontWeight = FontWeight.Bold)
}
//        Button(
//            onClick = onSaveData,
//            modifier = Modifier.weight(1f),
//            enabled = !isLoading && hasData,
//            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
//        ) {
//            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
//            Spacer(modifier = Modifier.width(8.dp))
//            Text("บันทึก", fontWeight = FontWeight.Bold)
//        }

        Button(
            onClick = onClearData,
            modifier = Modifier.weight(1f),
            enabled = !isLoading && hasData,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("เคลียร์", fontWeight = FontWeight.Bold)
        }
    }
}
}