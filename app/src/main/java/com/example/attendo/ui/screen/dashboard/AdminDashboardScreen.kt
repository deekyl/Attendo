package com.example.attendo.ui.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.attendo.data.model.user.User
import com.example.attendo.ui.components.LocationErrorCard
import com.example.attendo.ui.components.LocationPermissionDialog
import com.example.attendo.ui.components.LocationSettingsDialog
import com.example.attendo.ui.components.ProfileHeader
import com.example.attendo.ui.components.timerecord.TimeRecordStatusCard
import com.example.attendo.ui.components.timerecord.TodayTimeRecordsSection
import com.example.attendo.ui.viewmodel.timerecord.TimeRecordViewModel
import com.example.attendo.ui.viewmodel.user.UserViewModel
import com.example.attendo.utils.rememberLocationPermissionState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    user: User,
    onLogout: () -> Unit,
    onTimeRecordListClick: (User) -> Unit,
    onAddManualTimeRecordClick: (User) -> Unit,
    onManageBreakTypesClick: () -> Unit,
    onManageUsersClick: () -> Unit,
    onProfileClick: (User) -> Unit,
    timeRecordViewModel: TimeRecordViewModel = koinViewModel { parametersOf(user.userId) },
    userViewModel: UserViewModel = koinViewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val timeRecordState by timeRecordViewModel.timeRecordState.collectAsState()
    val todayRecords by timeRecordViewModel.todayRecords.collectAsState()
    val breakTypes by timeRecordViewModel.breakTypes.collectAsState()
    val profileImageUrl by userViewModel.profileImageUrl.collectAsState()

    val isGettingLocation by timeRecordViewModel.isGettingLocation.collectAsState()
    val locationError by timeRecordViewModel.locationError.collectAsState()
    val locationPermissionState = rememberLocationPermissionState()
    var showLocationSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(locationPermissionState.hasPermission) {
        if (locationPermissionState.hasPermission && !timeRecordViewModel.isLocationEnabled()) {
            showLocationSettingsDialog = true
        }
    }

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
                            DropdownMenuItem(
                                text = { Text("Gestionar usuarios") },
                                onClick = {
                                    showMenu = false
                                    onManageUsersClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null
                                    )
                                }
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

                ProfileHeader(
                    userName = user.fullName,
                    profileImageUrl = profileImageUrl,
                    onProfileClick = { onProfileClick(user) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                LocationErrorCard(
                    error = locationError,
                    isGettingLocation = isGettingLocation,
                    onRetry = {
                        timeRecordViewModel.clearLocationError()
                    },
                    onDismiss = {
                        timeRecordViewModel.clearLocationError()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

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

                // Componente reutilizable del historial del día
                TodayTimeRecordsSection(
                    todayRecords = todayRecords,
                    getBreakTypeDescription = { timeRecordViewModel.getBreakTypeDescription(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de acción específicos para el administrador
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

    LocationPermissionDialog(
        show = locationPermissionState.showRationale,
        onDismiss = locationPermissionState.dismissRationale,
        onRequestPermission = locationPermissionState.requestPermission,
        message = "Attendo necesita acceso a tu ubicación para registrar automáticamente dónde realizas tus fichajes como administrador."
    )

    LocationSettingsDialog(
        show = showLocationSettingsDialog,
        onDismiss = { showLocationSettingsDialog = false },
        message = "Los servicios de ubicación están deshabilitados. Para registrar la ubicación de tus fichajes, activa la ubicación en los ajustes."
    )

}