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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.attendo.ui.viewmodel.timerecord.TimeRecordViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.attendo.data.model.attendance.TimeRecord
import com.example.attendo.data.model.attendance.TimeRecordFilter
import com.example.attendo.data.model.user.User
import com.example.attendo.ui.viewmodel.timerecord.UserTimeRecordListViewModel
import androidx.compose.ui.text.style.TextOverflow

// Actualiza la import line y cómo usas los ViewModels en el Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRecordListScreen(
    user: User,
    onBack: () -> Unit,
    userTimeRecordListViewModel: UserTimeRecordListViewModel = koinViewModel { parametersOf(user.userId) },
    timeRecordViewModel: TimeRecordViewModel = koinViewModel { parametersOf(user.userId) }
) {
    Log.d("TimeRecordListScreen", "Iniciando screen con usuario: ${user.userId}")
    val timeRecords by userTimeRecordListViewModel.filteredRecords.collectAsState()
    val isLoading by userTimeRecordListViewModel.isLoading.collectAsState()
    val filterState by userTimeRecordListViewModel.filter.collectAsState()

    // Para gestionar el datepicker
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Para gestionar el panel de filtros
    var filtersExpanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Añadimos debug para ver qué descripciones tiene actualmente el TimeRecordViewModel
    LaunchedEffect(Unit) {
        // Log para depuración
        for (i in 1..5) {
            Log.d("BREAK_DEBUG", "ViewModel prueba - ID $i: ${timeRecordViewModel.getBreakTypeDescription(i)}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de fichajes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                            // Mostrar un contador si hay filtros activos
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
            // Panel de filtros animado que se expande y contrae
            AnimatedVisibility(
                visible = filtersExpanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 300, easing = EaseOutQuad)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 300, easing = EaseInQuad)
                )
            ) {
                FilterPanel(
                    filterState = filterState,
                    onFilterChanged = { newFilter ->
                        coroutineScope.launch {
                            userTimeRecordListViewModel.updateFilter(newFilter)
                        }
                    },
                    onApplyFilters = {
                        coroutineScope.launch {
                            userTimeRecordListViewModel.loadTimeRecords()
                        }
                    },
                    onShowStartDatePicker = { showStartDatePicker = true },
                    onShowEndDatePicker = { showEndDatePicker = true }
                )
            }

            // Summary Card - Muestra un resumen de los resultados
            if (!isLoading && timeRecords.isNotEmpty()) {
                SummaryCard(
                    recordCount = timeRecords.size,
                    dateRange = getPrettyDateRange(filterState),
                    actionType = filterState.actionType
                )
            }

            // Lista de registros
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
                    // Usamos el TimeRecordViewModel que ya funciona en dashboard para obtener las descripciones
                    TimeRecordsList(
                        records = timeRecords,
                        getBreakTypeDescription = { breakTypeId ->
                            timeRecordViewModel.getBreakTypeDescription(breakTypeId)
                        }
                    )
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
                    userTimeRecordListViewModel.updateFilter(filterState.copy(startDate = newDate))
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
                    userTimeRecordListViewModel.updateFilter(filterState.copy(endDate = newDate))
                }
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

@Composable
fun FilterPanel(
    filterState: TimeRecordFilter,
    onFilterChanged: (TimeRecordFilter) -> Unit,
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
                                text = filterState.startDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Seleccionar",
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
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha =.3f)
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
                                text = filterState.endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Seleccionar",
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
fun ActionTypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun SummaryCard(
    recordCount: Int,
    dateRange: String,
    actionType: TimeRecordFilter.ActionType,
    userName: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "$recordCount registros encontrados",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Periodo: $dateRange",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                val actionTypeText = when (actionType) {
                    TimeRecordFilter.ActionType.ALL -> "Todos los tipos"
                    TimeRecordFilter.ActionType.ENTRY -> "Solo entradas"
                    TimeRecordFilter.ActionType.EXIT -> "Solo salidas"
                    TimeRecordFilter.ActionType.BREAK -> "Solo pausas"
                }
                Text(
                    text = "Tipo: $actionTypeText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun TimeRecordsList(
    records: List<TimeRecord>,
    getBreakTypeDescription: (Int) -> String = { "Pausa: $it" }
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(records) { record ->
            TimeRecordCard(
                record = record,
                getBreakTypeDescription = getBreakTypeDescription
            )
        }
    }
}

@Composable
fun TimeRecordCard(
    record: TimeRecord,
    getBreakTypeDescription: (Int) -> String = { "Pausa: $it" } // Función para obtener descripción de la pausa
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val (dateStr, timeStr) = try {
        val dateTime = java.time.LocalDateTime.parse(record.time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        Pair(dateTime.format(dateFormatter), dateTime.format(timeFormatter))
    } catch (e: Exception) {
        try {
            val dateTime = java.time.LocalDateTime.parse(record.time, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            Pair(dateTime.format(dateFormatter), dateTime.format(timeFormatter))
        } catch (e2: Exception) {
            Pair(record.time, "")
        }
    }

    // Definir iconos, colores y textos según el tipo de registro
    // Crear un data class para mantener los 5 valores que necesitamos
    data class RecordStyle(
        val icon: ImageVector,
        val backgroundColor: Color,
        val textColor: Color,
        val actionText: String,
        val iconTint: Color
    )

    val recordStyle = when {
        // Entrada normal
        record.isEntry && record.breakTypeId == null ->
            RecordStyle(
                icon = Icons.Default.Login,
                backgroundColor = Color(0xFFE8F5E9), // Verde claro
                textColor = Color(0xFF1B5E20), // Verde oscuro
                actionText = "Entrada",
                iconTint = Color(0xFF4CAF50) // Verde
            )

        // Salida normal
        !record.isEntry && record.breakTypeId == null ->
            RecordStyle(
                icon = Icons.Default.Logout,
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
            ),
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

@Composable
fun EmptyStateMessage(filterState: TimeRecordFilter) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No se encontraron registros",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Prueba a modificar los filtros para ver más resultados",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDatePicker(
    initialDate: LocalDate?,
    title: String,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.atStartOfDay()
            ?.atZone(java.time.ZoneId.systemDefault())
            ?.toInstant()?.toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        onDateSelected(localDate)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = { Text(title) },
            showModeToggle = false
        )
    }
}

// Función para obtener un rango de fechas en formato legible
fun getPrettyDateRange(filterState: TimeRecordFilter): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val startDate = filterState.startDate?.format(formatter) ?: "Inicio"
    val endDate = filterState.endDate?.format(formatter) ?: "Fin"
    return "$startDate - $endDate"
}

// Función para contar los filtros activos
fun countActiveFilters(filterState: TimeRecordFilter): Int {
    var count = 0
    if (filterState.startDate != null) count++
    if (filterState.endDate != null) count++
    if (filterState.actionType != TimeRecordFilter.ActionType.ALL) count++
    if (filterState.limit != 20) count++
    if (!filterState.ascending) count++
    return count
}