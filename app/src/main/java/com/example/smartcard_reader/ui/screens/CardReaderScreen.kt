package com.example.smartcard_reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smartcard_reader.ui.components.*
import com.example.smartcard_reader.viewmodel.CardReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardReaderScreen(viewModel: CardReaderViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📇 ID Card Reader", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Status Card
            StatusCard(
                connectionStatus = viewModel.connectionStatus.value,
                statusMessage = viewModel.statusMessage.value,
                onReconnect = { viewModel.reconnectReader() }
            )

            // Auto-Read Toggle
            //if (viewModel.connectionStatus.value == CardReaderViewModel.ConnectionStatus.READY) {
            //AutoReadToggle(
            //enabled = viewModel.autoReadEnabled.value,
            //onToggle = { viewModel.toggleAutoRead() }
            //)
            //}

            // Action Buttons
            Column(
                Modifier.padding(vertical = 16.dp)
            ){
                ActionButtons(
                    connectionStatus = viewModel.connectionStatus.value,
                    isLoading = viewModel.isLoading.value,
                    hasData = viewModel.cardData.value != null,
                    onReadCard = { viewModel.readCard() },
                    onSaveData = { viewModel.saveData() },
                    onClearData = { viewModel.clearData() },
                    onReconnect = { viewModel.reconnectReader() }
                )
            }


            // Loading Indicator
            if (viewModel.isLoading.value) {
                LoadingCard(isReadingPhoto = viewModel.isReadingPhoto.value)
            }

            // Photo Card
//            if (!viewModel.isLoading.value && viewModel.photoBase64.value != null) {
//                PhotoCard(photoBase64 = viewModel.photoBase64.value!!)
//            }

            // Card Data
            if (!viewModel.isLoading.value && viewModel.cardData.value != null) {
                //CardDataCard(cardData = viewModel.cardData.value!!)
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ){
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                        ){
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "CheckCircle",
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("อ่านข้อมูลบัตรเรียบร้อยแล้ว",textAlign = androidx.compose.ui.text.style.TextAlign.Center );
                        }


                    }
                }
            }

            if(!viewModel.isLoading.value && viewModel.cardData.value == null){
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(){
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = Color.Red
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ไม่มีข้อมูลอยู่ กรุณาอ่านบัตร", textAlign = androidx.compose.ui.text.style.TextAlign.Center );
                        }
                    }

                }
            }
    }
}
}