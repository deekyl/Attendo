package com.example.attendo.ui.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendo.data.model.attendance.BreakType
import com.example.attendo.data.model.attendance.TimeRecord
import com.example.attendo.data.model.user.User
import com.example.attendo.data.model.attendance.TimeRecordState
import com.example.attendo.ui.viewmodel.timerecord.TimeRecordViewModel
import kotlinx.coroutines.coroutineScope
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDashboardScreen(
    user: User,
    onLogout: () -> Unit,
    onTimeRecordListClick: (User) -> Unit, // Añadido el nuevo parámetro
    timeRecordViewModel: TimeRecordViewModel = koinViewModel { parametersOf(user.userId) }
) {
    val coroutineScope = rememberCoroutineScope()
    val timeRecordState by timeRecordViewModel.timeRecordState.collectAsState()
    val todayRecords by timeRecordViewModel.todayRecords.collectAsState()
    val breakTypes by timeRecordViewModel.breakTypes.collectAsState()

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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Bienvenido/a, ${user.fullName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Estado actual
                TimeRecordStatusCard(
                    timeRecordState = timeRecordState,
                    breakTypes = breakTypes,
                    onCheckIn = {
                        coroutineScope.launch {
                            timeRecordViewModel.checkIn()
                        }
                    },
                    onCheckOut = {
                        coroutineScope.launch {
                            timeRecordViewModel.checkOut()
                        }
                    },
                    onStartBreak = { breakTypeId ->
                        coroutineScope.launch {
                            timeRecordViewModel.startBreak(breakTypeId)
                        }
                    },
                    onEndBreak = {
                        coroutineScope.launch {
                            timeRecordViewModel.endBreak()
                        }
                    },
                    getBreakTypeDescription = { timeRecordViewModel.getBreakTypeDescription(it) }
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
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            LazyColumn {
                                items(todayRecords) { record ->
                                    TimeRecordItem(
                                        record = record,
                                        getBreakTypeDescription = {
                                            timeRecordViewModel.getBreakTypeDescription(
                                                it
                                            )
                                        }
                                    )

                                    if (todayRecords.indexOf(record) < todayRecords.size - 1) {
                                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón con el callback de navegación
            Button(
                onClick = { onTimeRecordListClick(user) }, // Usar el callback
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121))
            ) {
                Text(
                    "Consultar mis fichajes",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

@Composable
fun TimeRecordStatusCard(
    timeRecordState: TimeRecordState,
    breakTypes: List<BreakType>,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit,
    onStartBreak: (Int) -> Unit,
    onEndBreak: () -> Unit,
    getBreakTypeDescription: (Int) -> String = { "Descanso" }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
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
                                breakTypes.forEach { breakType ->
                                    DropdownMenuItem(
                                        text = { Text(breakType.description) },
                                        onClick = {
                                            onStartBreak(breakType.breakId)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

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
                        text = "En pausa: ${getBreakTypeDescription(breakTypeId)}",
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
fun TimeRecordItem(
    record: TimeRecord,
    getBreakTypeDescription: (Int) -> String = { "Descanso" }
) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val timestamp = try {
        // Parsear el ISO-8601 con offset (ej: 2025-05-05T15:23:36.646088+00:00)
        val dateTime = LocalDateTime.parse(record.time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        dateTime.format(formatter)
    } catch (e: Exception) {
        // Fallback: si el formato original falla, intentar con formato ISO_LOCAL_DATE_TIME
        try {
            val dateTime = LocalDateTime.parse(record.time, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            dateTime.format(formatter)
        } catch (e2: Exception) {
            // Si todo falla, mostrar el timestamp original
            record.time
        }
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
                "Inicio de pausa: ${getBreakTypeDescription(record.breakTypeId)}",
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
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                lineHeight = 16.sp
            )

            if (record.location != null) {
                Text(
                    text = "Ubicación: ${record.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }

            if (record.isManual) {
                Text(
                    text = "Registro manual",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF5722),
                    lineHeight = 16.sp
                )
            }
        }
    }
}