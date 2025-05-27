package com.example.attendo.ui.screen.breaktype

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.attendo.data.model.attendance.BreakType
import com.example.attendo.ui.viewmodel.breaktype.BreakTypeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakTypeManagementScreen(
    onBack: () -> Unit,
    viewModel: BreakTypeViewModel = koinViewModel()
) {
    val breakTypes by viewModel.breakTypes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Estados para el diálogo de creación/edición
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingBreakType by remember { mutableStateOf<BreakType?>(null) }

    // Estado para notificaciones temporales
    var showSuccessMessage by remember { mutableStateOf(false) }

    // Efecto para manejar resultados de operaciones
    LaunchedEffect(operationResult) {
        operationResult?.let {
            when (it) {
                is BreakTypeViewModel.OperationResult.Success -> {
                    showSuccessMessage = true
                    delay(3000)
                    showSuccessMessage = false
                    viewModel.clearOperationResult()
                }
                is BreakTypeViewModel.OperationResult.Error -> {
                    delay(5000)
                    viewModel.clearOperationResult()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar tipos de pausa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver atrás"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingBreakType = null // Aseguramos que estamos creando uno nuevo
                        showAddEditDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Añadir tipo de pausa"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && breakTypes.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (breakTypes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay tipos de pausa definidos",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pulsa el botón + para añadir un nuevo tipo de pausa",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(breakTypes) { breakType ->
                        BreakTypeItem(
                            breakType = breakType,
                            onEdit = {
                                editingBreakType = breakType
                                showAddEditDialog = true
                            },
                            onToggleStatus = { isActive ->
                                coroutineScope.launch {
                                    viewModel.toggleBreakTypeStatus(breakType.breakId, isActive)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Overlay para mensajes de éxito
            AnimatedVisibility(
                visible = showSuccessMessage,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = (operationResult as? BreakTypeViewModel.OperationResult.Success)?.message ?: "",
                            color = Color.White
                        )
                    }
                }
            }

            // Overlay para mensajes de error
            AnimatedVisibility(
                visible = operationResult is BreakTypeViewModel.OperationResult.Error,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = (operationResult as? BreakTypeViewModel.OperationResult.Error)?.message ?: "",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Diálogo para añadir/editar tipos de pausa
    if (showAddEditDialog) {
        BreakTypeDialog(
            breakType = editingBreakType,
            onDismiss = { showAddEditDialog = false },
            onSave = { description, computesAs ->
                if (editingBreakType != null) {
                    // Actualizar
                    coroutineScope.launch {
                        viewModel.updateBreakType(
                            editingBreakType!!.copy(
                                description = description,
                                computesAs = computesAs
                            )
                        )
                    }
                } else {
                    // Crear nuevo
                    coroutineScope.launch {
                        viewModel.createBreakType(description, computesAs)
                    }
                }
                showAddEditDialog = false
            }
        )
    }
}

@Composable
fun BreakTypeItem(
    breakType: BreakType,
    onEdit: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    val isActive = breakType.isActive

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de tipo de pausa
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BreakfastDining,
                    contentDescription = null,
                    tint = if (isActive)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del tipo de pausa
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = breakType.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isActive)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Text(
                    text = if (breakType.computesAs) "Computa como trabajo" else "No computa como trabajo",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // Switch para activar/desactivar
            Switch(
                checked = isActive,
                onCheckedChange = { onToggleStatus(it) },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun BreakTypeDialog(
    breakType: BreakType?,
    onDismiss: () -> Unit,
    onSave: (description: String, computesAs: Boolean) -> Unit
) {
    val isEditing = breakType != null
    var description by remember { mutableStateOf(breakType?.description ?: "") }
    var computesAs by remember { mutableStateOf(breakType?.computesAs ?: false) }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (isEditing) "Editar tipo de pausa" else "Nuevo tipo de pausa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Campo de descripción
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        if (it.isNotEmpty()) descriptionError = null
                    },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = descriptionError != null,
                    supportingText = {
                        if (descriptionError != null) {
                            Text(
                                text = descriptionError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null
                        )
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Checkbox para "computa como trabajo"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { computesAs = !computesAs }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = computesAs,
                        onCheckedChange = { computesAs = it }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Computa como trabajo",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Si está activo, el tiempo de pausa se considera tiempo trabajado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (description.isBlank()) {
                                descriptionError = "La descripción es obligatoria"
                            } else {
                                onSave(description.trim(), computesAs)
                            }
                        }
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}