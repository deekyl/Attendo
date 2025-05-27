package com.example.attendo.ui.screen.timerecord

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.attendo.data.model.attendance.BreakType
import com.example.attendo.data.model.user.User
import com.example.attendo.ui.theme.PurplePrimary
import com.example.attendo.ui.viewmodel.timerecord.ManualTimeRecordViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTimeRecordScreen(
    adminUser: User,
    onBack: () -> Unit,
    viewModel: ManualTimeRecordViewModel = koinViewModel { parametersOf(adminUser.userId) }
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val allUsers by viewModel.allUsers.collectAsState()
    val breakTypes by viewModel.breakTypes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    val selectedUser by viewModel.selectedUser.collectAsState()
    val lastTimeRecord by viewModel.lastTimeRecord.collectAsState()

    // Estados para los campos del formulario
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var selectedRecordType by remember { mutableStateOf(RecordType.ENTRY) }
    var selectedBreakType by remember { mutableStateOf<BreakType?>(null) }
    var location by remember { mutableStateOf("") }

    // Estados para los diálogos
    var showUserDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showBreakTypeDropdown by remember { mutableStateOf(false) }

    // Estado para mensajes de error de validación
    var userError by remember { mutableStateOf<String?>(null) }
    var breakTypeError by remember { mutableStateOf<String?>(null) }

    // Estado para controlar notificaciones temporales
    var showSuccess by remember { mutableStateOf(false) }

    // Efecto para gestionar el resultado de la operación
    LaunchedEffect(operationResult) {
        operationResult?.let {
            when (it) {
                is ManualTimeRecordViewModel.OperationResult.Success -> {
                    // Mostrar notificación de éxito
                    showSuccess = true
                    delay(3000) // Mostrar por 3 segundos
                    showSuccess = false
                    viewModel.clearOperationResult()

                    // Resetear formulario
                    selectedDate = LocalDate.now()
                    selectedTime = LocalTime.now()
                    location = ""
                    // Mantenemos el usuario y tipo de registro para facilitar múltiples entradas
                }

                is ManualTimeRecordViewModel.OperationResult.Error -> {
                    // La notificación de error se muestra automáticamente
                    delay(5000)
                    viewModel.clearOperationResult()
                }
            }
        }
    }

    // Efecto para ajustar el tipo de pausa cuando es una vuelta de pausa
    LaunchedEffect(selectedUser, selectedRecordType) {
        if (selectedRecordType == RecordType.ENTRY && viewModel.isReturnFromBreak()) {
            // Si es una entrada y el último fichaje fue inicio de pausa, obtener el mismo tipo
            val breakTypeId = viewModel.getLastBreakTypeId()
            selectedBreakType = breakTypes.find { it.breakId == breakTypeId }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Añadir fichaje manual") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver atrás"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Fondo con gradiente
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    )
            )

            // Contenido principal
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título con icono
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Nuevo fichaje manual",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Formulario con diseño moderno
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Sección de usuario
                        FormSection(
                            title = "USUARIO",
                            icon = Icons.Outlined.Person
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showUserDropdown = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.3f
                                        )
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(PurplePrimary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Person,
                                                contentDescription = null,
                                                tint = PurplePrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = selectedUser?.fullName
                                                    ?: "Seleccionar usuario",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (selectedUser != null) {
                                                Text(
                                                    text = selectedUser?.email ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showUserDropdown,
                                    onDismissRequest = { showUserDropdown = false },
                                    modifier = Modifier
                                        .heightIn(max = 300.dp)
                                        .width(IntrinsicSize.Max)
                                ) {
                                    allUsers.forEach { user ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = user.fullName,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Text(
                                                        text = user.email,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectUser(user)
                                                userError = null
                                                showUserDropdown = false
                                            },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(PurplePrimary.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Person,
                                                        contentDescription = null,
                                                        tint = PurplePrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Mostrar error de usuario si existe
                            AnimatedVisibility(
                                visible = userError != null,
                                enter = fadeIn(animationSpec = tween(200)),
                                exit = fadeOut(animationSpec = tween(200))
                            ) {
                                userError?.let {
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = it,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sección de tipo de registro
                        FormSection(
                            title = "TIPO DE REGISTRO",
                            icon = Icons.AutoMirrored.Outlined.FactCheck
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RecordTypeButton(
                                    text = "Entrada",
                                    icon = Icons.Outlined.Login,
                                    selected = selectedRecordType == RecordType.ENTRY,
                                    color = Color(0xFF4CAF50),
                                    onClick = {
                                        selectedRecordType = RecordType.ENTRY
                                        if (viewModel.isReturnFromBreak()) {
                                            val breakTypeId = viewModel.getLastBreakTypeId()
                                            selectedBreakType =
                                                breakTypes.find { it.breakId == breakTypeId }
                                        } else {
                                            selectedBreakType = null
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                RecordTypeButton(
                                    text = "Salida",
                                    icon = Icons.AutoMirrored.Outlined.Logout,
                                    selected = selectedRecordType == RecordType.EXIT,
                                    color = Color(0xFFF44336),
                                    onClick = {
                                        selectedRecordType = RecordType.EXIT
                                        selectedBreakType = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                RecordTypeButton(
                                    text = "Pausa",
                                    icon = Icons.Outlined.Coffee,
                                    selected = selectedRecordType == RecordType.BREAK,
                                    color = Color(0xFFFF9800),
                                    onClick = { selectedRecordType = RecordType.BREAK },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Selector de tipo de pausa (siempre visible pero desactivado si no es BREAK)
                        FormSection(
                            title = "TIPO DE PAUSA",
                            icon = Icons.Outlined.Coffee
                        ) {
                            val isBreakType = selectedRecordType == RecordType.BREAK
                            val isReturnFromBreak =
                                selectedRecordType == RecordType.ENTRY && viewModel.isReturnFromBreak()

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = isBreakType || isReturnFromBreak) {
                                            if (isBreakType || isReturnFromBreak) {
                                                showBreakTypeDropdown = true
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = if (isBreakType || isReturnFromBreak)
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val iconTint = if (isBreakType || isReturnFromBreak)
                                            Color(0xFFFF9800)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(iconTint.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.BreakfastDining,
                                                contentDescription = null,
                                                tint = iconTint,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = selectedBreakType?.description
                                                ?: "Seleccionar tipo de pausa",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isBreakType || isReturnFromBreak)
                                                MaterialTheme.colorScheme.onSurface
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.5f
                                                )
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = if (isBreakType || isReturnFromBreak)
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.5f
                                                )
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showBreakTypeDropdown,
                                    onDismissRequest = { showBreakTypeDropdown = false },
                                    modifier = Modifier.heightIn(max = 300.dp)
                                ) {
                                    breakTypes.forEach { breakType ->
                                        DropdownMenuItem(
                                            text = { Text(breakType.description) },
                                            onClick = {
                                                selectedBreakType = breakType
                                                breakTypeError = null
                                                showBreakTypeDropdown = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.BreakfastDining,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFF9800)
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            // Mostrar error de tipo de pausa si existe
                            AnimatedVisibility(
                                visible = breakTypeError != null,
                                enter = fadeIn(animationSpec = tween(200)),
                                exit = fadeOut(animationSpec = tween(200))
                            ) {
                                breakTypeError?.let {
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = it,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            if (selectedRecordType == RecordType.ENTRY && viewModel.isReturnFromBreak()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Esta entrada es una vuelta de pausa",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Fecha y hora
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Selector de fecha
                            Column(modifier = Modifier.weight(1f)) {
                                FormSection(
                                    title = "FECHA",
                                    icon = Icons.Outlined.CalendarMonth
                                ) {
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showDatePicker = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.outlinedCardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.3f
                                            )
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            Icon(
                                                imageVector = Icons.Outlined.CalendarMonth,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = selectedDate.format(
                                                    DateTimeFormatter.ofPattern(
                                                        "dd/MM/yyyy"
                                                    )
                                                ),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                FormSection(
                                    title = "HORA",
                                    icon = Icons.Outlined.Schedule
                                ) {
                                    OutlinedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showTimePicker = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.outlinedCardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.3f
                                            )
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = selectedTime.format(
                                                    DateTimeFormatter.ofPattern(
                                                        "HH:mm"
                                                    )
                                                ),
                                                style = MaterialTheme.typography.bodyLarge  ,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Campo de ubicación
                        FormSection(
                            title = "UBICACIÓN (OPCIONAL)",
                            icon = Icons.Outlined.LocationOn
                        ) {
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                placeholder = { Text("Introducir ubicación") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Botones de acción
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cancelar")
                                }
                            }

                            Button(
                                onClick = {
                                    // Validación básica
                                    if (selectedUser == null) {
                                        userError = "Debes seleccionar un usuario"
                                        return@Button
                                    }

                                    if (selectedRecordType == RecordType.BREAK && selectedBreakType == null) {
                                        breakTypeError = "Debes seleccionar un tipo de pausa"
                                        return@Button
                                    }

                                    // Crear fecha y hora combinadas
                                    val localDateTime = LocalDateTime.of(selectedDate, selectedTime)

                                    // Determinar tipo de entrada, salida o pausa
                                    val isEntry = when (selectedRecordType) {
                                        RecordType.ENTRY -> true
                                        RecordType.EXIT -> false
                                        RecordType.BREAK -> false
                                    }

                                    // Determinar si tiene tipo de pausa
                                    val breakTypeId = when {
                                        selectedRecordType == RecordType.BREAK -> selectedBreakType?.breakId
                                        selectedRecordType == RecordType.ENTRY && viewModel.isReturnFromBreak() -> selectedBreakType?.breakId
                                        else -> null
                                    }

                                    // Crear registro
                                    coroutineScope.launch {
                                        viewModel.insertManualRecord(
                                            userId = selectedUser!!.userId,
                                            isEntry = isEntry,
                                            breakTypeId = breakTypeId,
                                            localDateTime = localDateTime,
                                            location = if (location.isNotEmpty()) location else null
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PurplePrimary
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Save,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Guardar")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Banner de éxito (flotante en la parte superior)
            AnimatedVisibility(
                visible = showSuccess,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡Fichaje guardado!",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "El fichaje se ha registrado correctamente",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Banner de error (parte superior)
            AnimatedVisibility(
                visible = operationResult is ManualTimeRecordViewModel.OperationResult.Error,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = (operationResult as? ManualTimeRecordViewModel.OperationResult.Error)?.message
                                    ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }

    // DatePicker Dialog
    if (showDatePicker) {
        ShowDatePicker(
            initialDate = selectedDate,
            title = "Seleccionar fecha",
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // TimePicker Dialog
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
fun FormSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        content()
    }
}

@Composable
fun RecordTypeButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor =
        if (selected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.3f
        )
    val borderColor = if (selected) color else Color.Transparent
    val contentColor = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class RecordType {
    ENTRY, EXIT, BREAK
}
