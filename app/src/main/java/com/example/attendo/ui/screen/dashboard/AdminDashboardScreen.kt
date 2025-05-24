package com.example.attendo.ui.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.ui.text.style.TextAlign
import com.example.attendo.ui.components.ProfileHeader
import com.example.attendo.ui.viewmodel.user.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    user: User,
    onLogout: () -> Unit,
    onTimeRecordListClick: (User) -> Unit,
    onAddManualTimeRecordClick: (User) -> Unit,
    onManageBreakTypesClick: () -> Unit,
    timeRecordViewModel: TimeRecordViewModel = koinViewModel { parametersOf(user.userId) },
    userViewModel: UserViewModel = koinViewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val timeRecordState by timeRecordViewModel.timeRecordState.collectAsState()
    val todayRecords by timeRecordViewModel.todayRecords.collectAsState()
    val breakTypes by timeRecordViewModel.breakTypes.collectAsState()
    val profileImageUrl by userViewModel.profileImageUrl.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendo - Admin") },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menú opciones"
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Gestionar tipos de pausa") },
                            onClick = {
                                showMenu = false
                                onManageBreakTypesClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Gestionar usuarios") },
                        onClick = { showMenu = false },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null
                            )
                        },
                        enabled = false // BORRAR
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Cerrar sesión") },
                        onClick = {
                            showMenu = false
                            onLogout()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null
                            )
                        }
                    )
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

                ProfileHeader(
                    userName = user.fullName,
                    profileImageUrl = profileImageUrl,
                    onChangeProfileImage = {
                        // TODO: Implementar selector de imagen (siguiente paso)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Estado actual - Reutilizamos el componente de UserDashboardScreen
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

                // Historial de fichajes del día - Reutilizamos de UserDashboardScreen
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

            // Botones de acción para el administrador
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón para consultar fichajes
                Button(
                    onClick = { onTimeRecordListClick(user) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121))
                ) {
                    Text(
                        "Consultar más fichajes",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Botón adicional para fichajes manuales
                Button(
                    onClick = { onAddManualTimeRecordClick(user) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text(
                        "Añadir fichaje manual",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}