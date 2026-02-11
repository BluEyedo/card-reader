package com.example.smartcard_reader.ui.screens

import android.content.Context
import android.hardware.usb.UsbDevice
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartcard_reader.ui.components.*
import com.example.smartcard_reader.util.BatteryOptimizationUtil
import com.example.smartcard_reader.viewmodel.CardReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardReaderScreen(viewModel: CardReaderViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ICS Portal ID Card",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2)
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshDevices(); showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        viewModel.availableDevices.value.forEach { device ->
                            DropdownMenuItem(
                                text = { Text(device.deviceName) },
                                onClick = {
                                    viewModel.selectDevice(device)
                                    showMenu = false
                                }
                            )
                        }
                        if (viewModel.availableDevices.value.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No devices found") },
                                onClick = {}
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF5F7FA),
                            Color(0xFFE8EAF6)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Main Content Card - Shows Status, Loading, Success, or Empty State
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Dynamic Content Area
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Loading Indicator
                            this@Column.AnimatedVisibility(
                                visible = viewModel.isLoading.value,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                LoadingCard(isReadingPhoto = viewModel.isReadingPhoto.value)
                            }

                            // Success or Empty State
                            this@Column.AnimatedVisibility(
                                visible = !viewModel.isLoading.value,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                if (viewModel.cardData.value != null) {
                                    StatusCard(
                                        connectionStatus = viewModel.connectionStatus.value,
                                        statusMessage = viewModel.statusMessage.value,
                                        onReconnect = { viewModel.reconnectReader() }
                                    )
                                } else {
                                    EmptyStateContent()
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                ActionButtons(
                    connectionStatus = viewModel.connectionStatus.value,
                    isLoading = viewModel.isLoading.value,
                    hasData = viewModel.cardData.value != null,
                    onReadCard = { viewModel.readCard() },
                    onSaveData = { viewModel.saveData() },
                    onClearData = { viewModel.clearData() },
                    onReconnect = { viewModel.reconnectReader() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Battery Optimization Setting
                BatteryOptimizationSetting(context)
            }
        }
    }
}

@Composable
private fun BatteryOptimizationSetting(context: Context) {
    val isIgnoringBatteryOptimizations = remember { mutableStateOf(BatteryOptimizationUtil.isBatteryOptimizationIgnored(context)) }

    if (!isIgnoringBatteryOptimizations.value) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "การทำงานเบื้องหลังอาจถูกจำกัด",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFA000)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "เพื่อให้แอปทำงานในพื้นหลังได้อย่างถูกต้องบนอุปกรณ์บางรุ่น (เช่น Huawei) จำเป็นต้องปิดการจัดการแบตเตอรี่สำหรับแอปนี้",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color(0xFF616161)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { BatteryOptimizationUtil.requestIgnoreBatteryOptimizations(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                ) {
                    Text(text = "ไปที่การตั้งค่า")
                }
            }
        }
    }
}


@Composable
private fun SuccessContent() {
    var scale by remember { mutableFloatStateOf(0.8f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0.8f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { value, _ -> scale = value }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "อ่านข้อมูลสำเร็จ",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "ข้อมูลบัตรประชาชนพร้อมใช้งาน",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
private fun EmptyStateContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Color(0xFFFFF3E0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBox,
                contentDescription = "No Data",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "ยังไม่มีข้อมูล",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF616161)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "กรุณาวางบัตรประชาชนบนเครื่องอ่าน\nแล้วกดปุ่ม 'อ่านบัตร'",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
