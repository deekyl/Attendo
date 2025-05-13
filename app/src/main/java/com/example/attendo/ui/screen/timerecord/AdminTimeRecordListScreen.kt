
package com.example.attendo.ui.screen.timerecord

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.attendo.data.model.attendance.TimeRecord
import com.example.attendo.data.model.attendance.TimeRecordFilter
import com.example.attendo.data.model.user.User
import com.example.attendo.ui.viewmodel.timerecord.AdminTimeRecordListViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTimeRecordListScreen(
    adminUser: User,
    onBack: () -> Unit,
    viewModel: AdminTimeRecordListViewModel = koinViewModel()
) {
    Log.d("AdminTimeRecordListScreen", "Iniciando pantalla de administración de fichajes")

    LaunchedEffect(key1 = adminUser.userId) {
        try {
            // Inicializamos con el usuario admin
            viewModel.initialize(adminUser)
        } catch (e: Exception) {
            Log.e("AdminTimeRecordListScreen", "Error inicializando: ${e.message}", e)
        }
    }

    // Estados del ViewModel
    val timeRecords by viewModel.filteredRecords.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filterState by viewModel.filter.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val selectedUser by viewModel.selectedUser.collectAsState()

    // Efecto de lanzamiento para inicializar el ViewModel con el usuario admin
    LaunchedEffect(key1 = Unit) {
        viewModel.initialize(adminUser)
    }

    // Para gestionar el datepicker
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Para gestionar los menús desplegables
    var showActionDropdown by remember { mutableStateOf(false) }
    var showUserDropdown by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administración de fichajes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver atrás"
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
                .padding(horizontal = 16.dp)
        ) {
            // Sección de listado
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (timeRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron registros con los filtros aplicados",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Encabezado fijo (sticky header)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEEEEEE))
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Usuario",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(80.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fecha",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(100.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Acción",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(80.dp)
                            )
                            Text(
                                text = "Motivo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(80.dp)
                            )
                        }
                        HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                    }

                    // Lista de registros con un padding superior
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 41.dp)
                    ) {
                        items(timeRecords) { record ->
                            // Mostrar la fila de registro
                            AdminTimeRecordRow(
                                record = record,
                                userName = viewModel.getUserName(record.userId)
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sección de filtros
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "criterios selección",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Filtro de USUARIO
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "usuario",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedUser?.fullName ?: adminUser.fullName,
                                onValueChange = { },
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showUserDropdown = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Seleccionar usuario",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            DropdownMenu(
                                expanded = showUserDropdown,
                                onDismissRequest = { showUserDropdown = false },
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                allUsers.forEach { user ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = user.fullName,
                                                fontSize = 14.sp,
                                                maxLines = 1
                                            )
                                        },
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.selectUser(user)
                                            }
                                            showUserDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Primera fila de filtros de fecha
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Filtro DESDE fecha
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "desde",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            OutlinedTextField(
                                value = filterState.startDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
                                onValueChange = { },
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showStartDatePicker = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Seleccionar fecha inicio",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Filtro HASTA fecha
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "hasta",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            OutlinedTextField(
                                value = filterState.endDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
                                onValueChange = { },
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showEndDatePicker = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Seleccionar fecha fin",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Segunda fila de filtros
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Filtro por ACCIÓN
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "acción",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = when(filterState.actionType) {
                                        TimeRecordFilter.ActionType.ENTRY -> "entrada"
                                        TimeRecordFilter.ActionType.EXIT -> "salida"
                                        TimeRecordFilter.ActionType.BREAK -> "pausa"
                                        else -> "acción"
                                    },
                                    onValueChange = { },
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { showActionDropdown = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Seleccionar acción",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary
                                    )
                                )

                                DropdownMenu(
                                    expanded = showActionDropdown,
                                    onDismissRequest = { showActionDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("acción", fontSize = 14.sp) },
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.updateFilter(
                                                    filterState.copy(actionType = TimeRecordFilter.ActionType.ALL)
                                                )
                                            }
                                            showActionDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("entrada", fontSize = 14.sp) },
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.updateFilter(
                                                    filterState.copy(actionType = TimeRecordFilter.ActionType.ENTRY)
                                                )
                                            }
                                            showActionDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("salida", fontSize = 14.sp) },
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.updateFilter(
                                                    filterState.copy(actionType = TimeRecordFilter.ActionType.EXIT)
                                                )
                                            }
                                            showActionDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("pausa", fontSize = 14.sp) },
                                        onClick = {
                                            coroutineScope.launch {
                                                viewModel.updateFilter(
                                                    filterState.copy(actionType = TimeRecordFilter.ActionType.BREAK)
                                                )
                                            }
                                            showActionDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Registros
                        Column(
                            modifier = Modifier.width(70.dp)
                        ) {
                            Text(
                                text = "registros",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            OutlinedTextField(
                                value = filterState.limit.toString(),
                                onValueChange = { newValue ->
                                    val limit = newValue.toIntOrNull() ?: 20
                                    coroutineScope.launch {
                                        viewModel.updateFilter(
                                            filterState.copy(limit = limit)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // Orden ascendente/descendente
                        Column(
                            modifier = Modifier.width(50.dp)
                        ) {
                            Text(
                                text = "orden",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                                    .border(
                                        width = 1.dp,
                                        color = Color.LightGray,
                                        shape = RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.updateFilter(
                                                filterState.copy(ascending = !filterState.ascending)
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (filterState.ascending)
                                            Icons.Default.ArrowUpward
                                        else
                                            Icons.Default.ArrowDownward,
                                        contentDescription = "Cambiar orden",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón de filtrar
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.loadTimeRecords()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121))
                    ) {
                        Text(
                            "filtrar",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // DatePicker para fecha inicio
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = filterState.startDate?.atStartOfDay()
                ?.atZone(java.time.ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            coroutineScope.launch {
                                viewModel.updateFilter(
                                    filterState.copy(startDate = localDate)
                                )
                            }
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStartDatePicker = false }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("Selecciona fecha inicio") },
                showModeToggle = false
            )
        }
    }

    // DatePicker para fecha fin
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = filterState.endDate?.atStartOfDay()
                ?.atZone(java.time.ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            coroutineScope.launch {
                                viewModel.updateFilter(
                                    filterState.copy(endDate = localDate)
                                )
                            }
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEndDatePicker = false }
                ) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = { Text("Selecciona fecha fin") },
                showModeToggle = false
            )
        }
    }
}

@Composable
fun AdminTimeRecordRow(
    record: TimeRecord,
    userName: String
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")
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

    // Iconos y colores según el tipo de registro
    val (icon, iconColor, actionText) = when {
        record.isEntry && record.breakTypeId == null ->
            Triple(
                Icons.Default.Login,
                Color(0xFF4CAF50),
                "Entrada"
            )
        !record.isEntry && record.breakTypeId == null ->
            Triple(
                Icons.Default.Logout,
                Color(0xFFF44336),
                "Salida"
            )
        !record.isEntry && record.breakTypeId != null ->
            Triple(
                Icons.Default.Coffee,
                Color(0xFFFF9800),
                "Pausa"
            )
        record.isEntry && record.breakTypeId != null ->
            Triple(
                Icons.Default.WorkOutline,
                Color(0xFF2196F3),
                "Fin Pausa"
            )
        else ->
            Triple(
                Icons.Default.Info,
                Color.Gray,
                "Desconocido"
            )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Aquí mostramos el nombre del usuario en lugar de "Usuario"
        Text(
            text = userName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.width(100.dp)
        ) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier.width(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = actionText,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = record.breakTypeId?.toString() ?: "-",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.Center
        )
    }
}