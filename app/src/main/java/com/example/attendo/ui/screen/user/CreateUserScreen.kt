package com.example.attendo.ui.screen.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.attendo.ui.theme.PurplePrimary
import com.example.attendo.ui.viewmodel.user.UserManagementViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserScreen(
    onBack: () -> Unit,
    onUserCreated: () -> Unit,
    viewModel: UserManagementViewModel = koinViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isLoading by viewModel.isLoading.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var documentId by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var documentIdError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }

    var showSuccess by remember { mutableStateOf(false) }

    fun validateForm(): Boolean {
        var isValid = true

        fullNameError = when {
            fullName.isBlank() -> {
                isValid = false
                "El nombre completo es obligatorio"
            }
            fullName.length < 2 -> {
                isValid = false
                "El nombre debe tener al menos 2 caracteres"
            }
            else -> null
        }

        emailError = when {
            email.isBlank() -> {
                isValid = false
                "El email es obligatorio"
            }
            !viewModel.isValidEmail(email) -> {
                isValid = false
                "El formato del email no es válido"
            }
            else -> null
        }

        documentIdError = when {
            documentId.isBlank() -> {
                isValid = false
                "El documento es obligatorio"
            }
            documentId.length < 5 -> {
                isValid = false
                "El documento debe tener al menos 5 caracteres"
            }
            else -> null
        }

        addressError = when {
            address.isBlank() -> {
                isValid = false
                "La dirección es obligatoria"
            }
            address.length < 5 -> {
                isValid = false
                "La dirección debe tener al menos 5 caracteres"
            }
            else -> null
        }

        return isValid
    }

    LaunchedEffect(operationResult) {
        operationResult?.let {
            when (it) {
                is UserManagementViewModel.OperationResult.Success -> {
                    showSuccess = true
                    delay(2000)
                    onUserCreated()
                }
                is UserManagementViewModel.OperationResult.Error -> {
                    delay(5000)
                    viewModel.clearOperationResult()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear usuario") },
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Nuevo usuario",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Formulario
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
                        Text(
                            text = "Información del usuario",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Nombre completo
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                if (fullNameError != null) fullNameError = null
                            },
                            label = { Text("Nombre completo") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null
                                )
                            },
                            isError = fullNameError != null,
                            supportingText = {
                                fullNameError?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                if (emailError != null) emailError = null
                            },
                            label = { Text("Email") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null
                                )
                            },
                            isError = emailError != null,
                            supportingText = {
                                emailError?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Documento
                        OutlinedTextField(
                            value = documentId,
                            onValueChange = {
                                documentId = it
                                if (documentIdError != null) documentIdError = null
                            },
                            label = { Text("Documento de identidad") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Badge,
                                    contentDescription = null
                                )
                            },
                            isError = documentIdError != null,
                            supportingText = {
                                documentIdError?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dirección
                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it
                                if (addressError != null) addressError = null
                            },
                            label = { Text("Dirección") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null
                                )
                            },
                            isError = addressError != null,
                            supportingText = {
                                addressError?.let {
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        HorizontalDivider()

                        Spacer(modifier = Modifier.height(24.dp))

                        // Tipo de usuario
                        Text(
                            text = "Tipo de usuario",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (isAdmin) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isAdmin) "Administrador" else "Usuario regular",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isAdmin)
                                            "Acceso completo a todas las funciones"
                                        else
                                            "Acceso limitado a sus propios fichajes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Switch(
                                    checked = isAdmin,
                                    onCheckedChange = { isAdmin = it }
                                )
                            }
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Los administradores pueden gestionar usuarios y acceder a todas las funciones del sistema.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Botones
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading
                            ) {
                                Text("Cancelar")
                            }

                            Button(
                                onClick = {
                                    if (validateForm()) {
                                        coroutineScope.launch {
                                            viewModel.createUser(
                                                fullName = fullName.trim(),
                                                email = email.trim().lowercase(),
                                                documentId = documentId.trim(),
                                                address = address.trim(),
                                                isAdmin = isAdmin
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PurplePrimary
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Crear usuario")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Notificación de éxito
            AnimatedVisibility(
                visible = showSuccess,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f),
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
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡Usuario creado!",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "El usuario se ha creado correctamente",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Notificación de error
            AnimatedVisibility(
                visible = operationResult is UserManagementViewModel.OperationResult.Error,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f),
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
                            imageVector = Icons.Default.Error,
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
                                text = (operationResult as? UserManagementViewModel.OperationResult.Error)?.message ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}