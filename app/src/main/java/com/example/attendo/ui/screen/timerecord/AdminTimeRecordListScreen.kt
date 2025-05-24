package com.example.attendo.ui.screen.timerecord

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.attendo.data.model.attendance.TimeRecord
import com.example.attendo.data.model.attendance.TimeRecordFilter
import com.example.attendo.data.model.style.RecordStyle
import com.example.attendo.data.model.user.User
import com.example.attendo.ui.viewmodel.timerecord.AdminTimeRecordListViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTimeRecordListScreen(
    adminUser: User,
    onBack: () -> Unit,
    viewModel: AdminTimeRecordListViewModel = koinViewModel { parametersOf(adminUser.userId) }
) {
    Log.d("Attendo", "Iniciando pantalla con usuario admin: ${adminUser.userId}")

    // Estados del ViewModel
    val timeRecords by viewModel.filteredRecords.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filterState by viewModel.filter.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val selectedUser by viewModel.selectedUser.collectAsState()
    val breakTypesMap by viewModel.breakTypesMap.collectAsState()

    // Efecto de lanzamiento para inicializar el ViewModel con el usuario admin
    LaunchedEffect(key1 = adminUser.userId) {
        viewModel.initialize(adminUser)
    }

    // Para gestionar el datepicker
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Para gestionar el panel de filtros
    var filtersExpanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    var selectedRecord by remember { mutableStateOf<TimeRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administración de fichajes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver atrás"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { filtersExpanded = !filtersExpanded }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtros"
                        )
                        Badge(
                            modifier = Modifier.offset(x = (-6).dp, y = 6.dp)
                        ) {
                            val activeFilters = countActiveFilters(filterState)
                            if (activeFilters > 0) {
                                Text(activeFilters.toString())
                            }
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
        ) {
            AnimatedVisibility(
                visible = filtersExpanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 300, easing = EaseOutQuad)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 300, easing = EaseInQuad)
                )
            ) {
                AdminFilterPanel(
                    filterState = filterState,
                    selectedUser = selectedUser,
                    allUsers = allUsers,
                    onFilterChanged = { newFilter ->
                        coroutineScope.launch {
                            viewModel.updateFilter(newFilter)
                        }
                    },
                    onUserSelected = { user ->
                        coroutineScope.launch {
                            viewModel.selectUser(user)
                        }
                    },
                    onApplyFilters = {
                        coroutineScope.launch {
                            viewModel.loadTimeRecords()
                        }
                    },
                    onShowStartDatePicker = { showStartDatePicker = true },
                    onShowEndDatePicker = { showEndDatePicker = true }
                )
            }

            if (!isLoading && timeRecords.isNotEmpty()) {
                SummaryCard(
                    recordCount = timeRecords.size,
                    dateRange = getPrettyDateRange(filterState),
                    actionType = filterState.actionType,
                    userName = selectedUser?.fullName ?: "Todos los usuarios"
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (timeRecords.isEmpty()) {
                    EmptyStateMessage(filterState)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(timeRecords) { record ->
                            TimeRecordCard(
                                record = record,
                                getBreakTypeDescription = { breakId ->
                                    breakTypesMap[breakId]?.description ?: "Pausa $breakId"
                                },
                                onEditClick = { selectedRecord = it }
                            )
                        }
                    }
                    selectedRecord?.let { record ->
                        EditTimeRecordDialog(
                            record = record,
                            onDismiss = { selectedRecord = null },
                            onSave = { updatedRecord ->
                                coroutineScope.launch {
                                    viewModel.updateTimeRecord(updatedRecord)
                                    selectedRecord = null
                                }
                            },
                            getBreakTypeDescription = { breakId ->
                                breakTypesMap[breakId]?.description ?: "Pausa $breakId"
                            }
                        )
                    }
                }
            }
        }
    }

    // DatePicker para fecha inicio
    if (showStartDatePicker) {
        ShowDatePicker(
            initialDate = filterState.startDate,
            title = "Selecciona fecha inicio",
            onDateSelected = { newDate ->
                coroutineScope.launch {
                    viewModel.updateFilter(filterState.copy(startDate = newDate))
                }
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    // DatePicker para fecha fin
    if (showEndDatePicker) {
        ShowDatePicker(
            initialDate = filterState.endDate,
            title = "Selecciona fecha fin",
            onDateSelected = { newDate ->
                coroutineScope.launch {
                    viewModel.updateFilter(filterState.copy(endDate = newDate))
                }
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

@Composable
fun AdminFilterPanel(
    filterState: TimeRecordFilter,
    selectedUser: User?,
    allUsers: List<User>,
    onFilterChanged: (TimeRecordFilter) -> Unit,
    onUserSelected: (User) -> Unit,
    onApplyFilters: () -> Unit,
    onShowStartDatePicker: () -> Unit,
    onShowEndDatePicker: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Filtros",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Selector de usuario (específico para admin)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Usuario",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var showUserDropdown by remember { mutableStateOf(false) }

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { showUserDropdown = true },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedUser?.fullName ?: "Seleccionar usuario",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = showUserDropdown,
                    onDismissRequest = { showUserDropdown = false },
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    allUsers.forEach { user ->
                        DropdownMenuItem(
                            text = { Text(user.fullName) },
                            onClick = {
                                onUserSelected(user)
                                showUserDropdown = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fechas (en una fila)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Fecha Inicio
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Desde",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .clickable { onShowStartDatePicker() },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = filterState.startDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                    ?: "Seleccionar",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Fecha Fin
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hasta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                            .clickable { onShowEndDatePicker() },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = filterState.endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                    ?: "Seleccionar",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tipo de acción - Chips seleccionables
            Text(
                text = "Tipo de acción",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionTypeChip(
                    text = "Todos",
                    selected = filterState.actionType == TimeRecordFilter.ActionType.ALL,
                    onClick = { onFilterChanged(filterState.copy(actionType = TimeRecordFilter.ActionType.ALL)) }
                )
                ActionTypeChip(
                    text = "Entradas",
                    selected = filterState.actionType == TimeRecordFilter.ActionType.ENTRY,
                    onClick = { onFilterChanged(filterState.copy(actionType = TimeRecordFilter.ActionType.ENTRY)) }
                )
                ActionTypeChip(
                    text = "Salidas",
                    selected = filterState.actionType == TimeRecordFilter.ActionType.EXIT,
                    onClick = { onFilterChanged(filterState.copy(actionType = TimeRecordFilter.ActionType.EXIT)) }
                )
                ActionTypeChip(
                    text = "Pausas",
                    selected = filterState.actionType == TimeRecordFilter.ActionType.BREAK,
                    onClick = { onFilterChanged(filterState.copy(actionType = TimeRecordFilter.ActionType.BREAK)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fila de opciones adicionales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Límite de registros
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Máx. registros",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = filterState.limit.toString(),
                        onValueChange = { newValue ->
                            val limit = newValue.toIntOrNull() ?: 20
                            onFilterChanged(filterState.copy(limit = limit))
                        },
                        modifier = Modifier
                            .width(100.dp)
                            .padding(end = 8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true
                    )
                }

                // Orden (Ascendente/Descendente)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Orden",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = filterState.ascending,
                            onCheckedChange = {
                                onFilterChanged(filterState.copy(ascending = it))
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (filterState.ascending) "Ascendente" else "Descendente",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para aplicar filtros
            Button(
                onClick = onApplyFilters,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aplicar filtros")
            }
        }
    }
}

@Composable
fun TimeRecordCard(
    record: TimeRecord,
    getBreakTypeDescription: (Int) -> String = { "Pausa: $it" }, // Función para obtener descripción de la pausa
    onEditClick: (TimeRecord) -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val (dateStr, timeStr) = try {
        val dateTime =
           LocalDateTime.parse(record.time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        Pair(dateTime.format(dateFormatter), dateTime.format(timeFormatter))
    } catch (_: Exception) {
        try {
            val dateTime =
                LocalDateTime.parse(record.time, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            Pair(dateTime.format(dateFormatter), dateTime.format(timeFormatter))
        } catch (_: Exception) {
            Pair(record.time, "")
        }
    }

    val recordStyle = when {
        // Entrada normal
        record.isEntry && record.breakTypeId == null ->
            RecordStyle(
                icon = Icons.AutoMirrored.Filled.Login,
                backgroundColor = Color(0xFFE8F5E9), // Verde claro
                textColor = Color(0xFF1B5E20), // Verde oscuro
                actionText = "Entrada",
                iconTint = Color(0xFF4CAF50) // Verde
            )

        // Salida normal
        !record.isEntry && record.breakTypeId == null ->
            RecordStyle(
                icon = Icons.AutoMirrored.Filled.Logout,
                backgroundColor = Color(0xFFFFEBEE), // Rojo claro
                textColor = Color(0xFFB71C1C), // Rojo oscuro
                actionText = "Salida",
                iconTint = Color(0xFFF44336) // Rojo
            )

        // Inicio de pausa (con motivo)
        !record.isEntry && record.breakTypeId != null ->
            RecordStyle(
                icon = Icons.Default.Coffee,
                backgroundColor = Color(0xFFFFF8E1), // Amarillo claro
                textColor = Color(0xFFF57F17), // Amarillo oscuro
                actionText = "Pausa",
                iconTint = Color(0xFFFF9800) // Naranja
            )

        // Fin de pausa (con motivo)
        record.isEntry && record.breakTypeId != null ->
            RecordStyle(
                icon = Icons.Default.WorkOutline,
                backgroundColor = Color(0xFFE3F2FD), // Azul claro
                textColor = Color(0xFF0D47A1), // Azul oscuro
                actionText = "Fin Pausa",
                iconTint = Color(0xFF2196F3) // Azul
            )

        // Caso desconocido
        else ->
            RecordStyle(
                icon = Icons.Default.Info,
                backgroundColor = Color(0xFFF5F5F5), // Gris claro
                textColor = Color(0xFF424242), // Gris oscuro
                actionText = "Desconocido",
                iconTint = Color.Gray
            )
    }

    val icon = recordStyle.icon
    val backgroundColor = recordStyle.backgroundColor
    val textColor = recordStyle.textColor
    val actionText = recordStyle.actionText
    val iconTint = recordStyle.iconTint

    // Si hay un break_type_id, obtener la descripción
    val breakDescription = if (record.breakTypeId != null) {
        getBreakTypeDescription(record.breakTypeId)
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onEditClick(record) },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono circular
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = iconTint.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                    .border(
                        width = 0.5.dp,
                        color = iconTint.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = actionText,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del registro
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Título con la descripción de la pausa, si existe
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (record.breakTypeId != null) {
                        // Si es una pausa, mostrar "Pausa: [Descripción]"
                        Text(
                            text = "Pausa: ",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                        // Descripción de la pausa con texto recortado si es muy largo
                        Text(
                            text = breakDescription ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Si no es pausa, mostrar solo el tipo de acción
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }

                if (record.location != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = record.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }

                if (record.isManual) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFFFF5722).copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Registro manual",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF5722).copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTimeRecordDialog(
    record: TimeRecord,
    onDismiss: () -> Unit,
    onSave: (TimeRecord) -> Unit,
    getBreakTypeDescription: (Int) -> String
) {
    var editedTime by remember { mutableStateOf(record.time) }

    // Para manejar fecha y hora separadas
    var dateTime = try {
        LocalDateTime.parse(record.time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    } catch (_: Exception) {
        try {
            LocalDateTime.parse(record.time, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (_: Exception) {
            LocalDateTime.now() // Valor por defecto
        }
    }

    var selectedDate by remember { mutableStateOf(dateTime.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(dateTime.toLocalTime()) }

    // Estados para los pickers
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Título
                Text(
                    text = "Editar registro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tipo de acción (solo mostrar, no editable)
                val actionText = when {
                    record.isEntry && record.breakTypeId == null -> "Entrada"
                    !record.isEntry && record.breakTypeId == null -> "Salida"
                    !record.isEntry && record.breakTypeId != null ->
                        "Inicio de pausa: ${getBreakTypeDescription(record.breakTypeId)}"

                    record.isEntry && record.breakTypeId != null -> "Fin de pausa"
                    else -> "Desconocido"
                }

                OutlinedTextField(
                    value = actionText,
                    onValueChange = { },
                    label = { Text("Tipo de registro") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    readOnly = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de fecha
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Fecha",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Selector de hora
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Hora",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de ubicación
                var location by remember { mutableStateOf(record.location ?: "") }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ubicación") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // Combinar fecha y hora seleccionadas
                            val newDateTime = LocalDateTime.of(selectedDate, selectedTime)
                            val formattedDateTime =
                                newDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

                            // Crear el objeto TimeRecord actualizado
                            val updatedRecord = record.copy(
                                time = formattedDateTime,
                                location = location,
                                isManual = true
                            )

                            onSave(updatedRecord)
                            onDismiss()
                        }
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }

    // DatePicker
    if (showDatePicker) {
        ShowDatePicker(
            initialDate = selectedDate,
            title = "Seleccionar fecha",
            onDateSelected = { newDate ->
                selectedDate = newDate
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // TimePicker
    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                selectedTime = LocalTime.of(hour, minute, 0)
                showTimePicker = false
            }
        )
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(12) }
    var selectedMinute by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Seleccionar hora",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(8.dp)
                )

                // Selector de hora
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Horas
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(onClick = {
                            if (selectedHour < 23) selectedHour++ else selectedHour = 0
                        }) {
                            Icon(Icons.Default.KeyboardArrowUp, null)
                        }
                        Text(
                            text = String.format("%02d", selectedHour),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        IconButton(onClick = {
                            if (selectedHour > 0) selectedHour-- else selectedHour = 23
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    }

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minutos
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(onClick = {
                            if (selectedMinute < 59) selectedMinute++ else selectedMinute = 0
                        }) {
                            Icon(Icons.Default.KeyboardArrowUp, null)
                        }
                        Text(
                            text = String.format("%02d", selectedMinute),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        IconButton(onClick = {
                            if (selectedMinute > 0) selectedMinute-- else selectedMinute = 59
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    }
                }

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        onConfirm(selectedHour, selectedMinute)
                    }) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}