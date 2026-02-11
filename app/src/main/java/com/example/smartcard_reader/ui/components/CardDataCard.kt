package com.example.smartcard_reader.ui.components

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
import com.example.smartcard_reader.data.model.CardData

@Composable
fun CardDataCard(cardData: CardData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ข้อมูลบัตรประชาชน",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Divider(modifier = Modifier.padding(bottom = 16.dp))

            DataField("เลขบัตรประชาชน", cardData.cid, Icons.Default.AccountBox)
            DataField("ชื่อ-นามสกุล (ไทย)", cardData.nameTH, Icons.Default.Person)
            DataField("ชื่อ-นามสกุล (อังกฤษ)", cardData.nameEN, Icons.Default.Check)
            DataField("วันเกิด (พ.ศ.)", cardData.birthDate, Icons.Default.DateRange)
            DataField("วันเกิด (ค.ศ.)", cardData.birthDateEN, Icons.Default.DateRange)
            DataField("เพศ", cardData.gender, Icons.Default.Person)
            DataField("ที่อยู่", cardData.address, Icons.Default.Home, maxLines = 3)
            DataField("สำนักงานออกบัตร", cardData.issuer, Icons.Default.LocationOn)
            DataField("วันออกบัตร (พ.ศ.)", cardData.issueDate, Icons.Default.Check)
            DataField("วันออกบัตร (ค.ศ.)", cardData.issueDateEN, Icons.Default.Check)
            DataField("วันหมดอายุ (พ.ศ.)", cardData.expireDate, Icons.Default.Check)
            DataField("วันหมดอายุ (ค.ศ.)", cardData.expireDateEN, Icons.Default.Check)
        }
    }
}