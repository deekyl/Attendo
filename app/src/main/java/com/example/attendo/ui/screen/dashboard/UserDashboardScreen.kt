package com.example.attendo.ui.screen.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.attendo.data.model.attendance.TimeRecord
import com.example.attendo.data.model.user.User
import com.example.attendo.data.model.attendance.TimeRecordState
import com.example.attendo.ui.viewmodel.timerecord.TimeRecordViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    user: User,
    onLogout: () -> Unit,
    timeRecordViewModel: TimeRecordViewModel = koinViewModel { parametersOf(user.userId) }
) {
    val timeRecordState by timeRecordViewModel.timeRecordState.collectAsState()
    val todayRecords by timeRecordViewModel.todayRecords.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendo") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bienvenido/a, ${user.fullName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Estado actual
            TimeRecordStatusCard(
                timeRecordState = timeRecordState,
                onCheckIn = { timeRecordViewModel.checkIn() },
                onCheckOut = { timeRecordViewModel.checkOut() },
                onStartBreak = { breakTypeId -> timeRecordViewModel.startBreak(breakTypeId) },
                onEndBreak = { timeRecordViewModel.endBreak() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Historial de fichajes del día
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Fichajes de hoy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (todayRecords.isEmpty()) {
                        Text(
                            text = "No hay registros para hoy",
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn {
                            items(todayRecords) { record ->
                                TimeRecordItem(record = record)

                                if (todayRecords.indexOf(record) < todayRecords.size - 1) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeRecordStatusCard(
    timeRecordState: TimeRecordState,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onStartBreak: (Int) -> Unit,
    onEndBreak: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (timeRecordState) {
                is TimeRecordState.CheckedIn -> Color(0xFFE8F5E9)
                is TimeRecordState.OnBreak -> Color(0xFFFFF9C4)
                is TimeRecordState.CheckedOut -> Color(0xFFFFEBEE)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Estado actual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (timeRecordState) {
                is TimeRecordState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                }
                is TimeRecordState.CheckedIn -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Trabajando",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Menú para diferentes tipos de pausas
                        var expanded by remember { mutableStateOf(false) }

                        Box {
                            Button(
                                onClick = { expanded = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF9800)
                                )
                            ) {
                                Text("Iniciar pausa")
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Expandir"
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Pausa para comer") },
                                    onClick = {
                                        onStartBreak(1)
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pausa para café") },
                                    onClick = {
                                        onStartBreak(2)
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Asuntos personales") },
                                    onClick = {
                                        onStartBreak(3)
                                        expanded = false
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = onCheckOut,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            )
                        ) {
                            Text("Salir")
                        }
                    }
                }
                is TimeRecordState.OnBreak -> {
                    val breakTypeId = timeRecordState.breakTypeId

                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "En pausa: " + when (breakTypeId) {
                            1 -> "Comida"
                            2 -> "Café"
                            3 -> "Asuntos personales"
                            else -> "Descanso"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onEndBreak,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Finalizar pausa")
                    }
                }
                is TimeRecordState.CheckedOut -> {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "No registrado",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onCheckIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Registrar entrada")
                    }
                }
                is TimeRecordState.Error -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = timeRecordState.message,
                        color = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun TimeRecordItem(record: TimeRecord) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val timestamp = try {
        LocalDateTime.parse(record.time).format(formatter)
    } catch (e: Exception) {
        record.time
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, label, color) = when {
            record.isEntry && record.breakTypeId == null -> Triple(
                Icons.Default.Login,
                "Entrada",
                Color(0xFF4CAF50)
            )
            !record.isEntry && record.breakTypeId == null -> Triple(
                Icons.Default.Logout,
                "Salida",
                Color(0xFFF44336)
            )
            !record.isEntry && record.breakTypeId != null -> Triple(
                Icons.Default.PauseCircle,
                "Inicio de pausa: " + when (record.breakTypeId) {
                    1 -> "Comida"
                    2 -> "Café"
                    3 -> "Asuntos personales"
                    else -> "Descanso"
                },
                Color(0xFFFF9800)
            )
            record.isEntry && record.breakTypeId != null -> Triple(
                Icons.Default.PlayCircle,
                "Fin de pausa",
                Color(0xFF2196F3)
            )
            else -> Triple(
                Icons.Default.Info,
                "Desconocido",
                Color.Gray
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = label,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            if (record.location != null) {
                Text(
                    text = "Ubicación: ${record.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (record.isManual) {
                Text(
                    text = "Registro manual",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF5722)
                )
            }
        }
    }
}