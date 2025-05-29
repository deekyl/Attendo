package com.example.attendo.ui.screen.user

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.attendo.data.model.user.User
import com.example.attendo.ui.theme.PurplePrimary
import com.example.attendo.ui.viewmodel.user.UserManagementViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(
    user: User,
    onBack: () -> Unit,
    onUserUpdated: () -> Unit,
    viewModel: UserManagementViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val selectedUser by viewModel.selectedUser.collectAsState()
    val profileImageUrl by viewModel.profileImageUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()

    // Estados del formulario
    var fullName by remember { mutableStateOf(user.fullName) }
    var email by remember { mutableStateOf(user.email) }
    var documentId by remember { mutableStateOf(user.documentId) }
    var address by remember { mutableStateOf(user.address) }
    var isAdmin by remember { mutableStateOf(user.isAdmin) }
    var isActive by remember { mutableStateOf(user.isActive) }

    // Estados de validación
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var documentIdError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }

    var showImagePickerDialog by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    // Launcher para seleccionar imagen
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    bytes?.let { imageBytes ->
                        viewModel.uploadProfileImage(user.userId, imageBytes)
                    }
                } catch (e: Exception) {
                    // Error manejado en el ViewModel
                }
            }
        }
    }

    // Función de validación
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



    // Cargar usuario al iniciar
    LaunchedEffect(user.userId) {
        viewModel.selectUser(user)
    }

    // Efecto para manejar resultados
    LaunchedEffect(operationResult) {
        operationResult?.let {
            when (it) {
                is UserManagementViewModel.OperationResult.Success -> {
                    showSuccess = true
                    delay(2000)
                    onUserUpdated()
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
                title = { Text("Editar usuario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver atrás"
                        )
                    }
                },
                actions = {
                    // Botón para desactivar/activar usuario
                    IconButton(
                        onClick = { showDeactivateDialog = true }
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.PersonOff else Icons.Default.Person,
                            contentDescription = if (isActive) "Desactivar usuario" else "Activar usuario",
                            tint = if (isActive) Color(0xFFF44336) else Color(0xFF4CAF50)
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
                // Sección de imagen de perfil
                ProfileImageSection(
                    profileImageUrl = profileImageUrl,
                    userName = selectedUser?.fullName ?: user.fullName,
                    isUploading = isUploadingImage,
                    onImageClick = { showImagePickerDialog = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Estado del usuario
                if (!isActive) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF44336).copy(alpha = 0.1f)
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
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Usuario inactivo",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFF44336),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Este usuario no puede acceder al sistema",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF44336)
                                )
                            }
                        }
                    }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Información del usuario",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            if (isAdmin) {
                                Surface(
                                    color = Color(0xFFFF9800).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Admin",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFFF9800),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

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
                        Text(
                            text = "Permisos del usuario",
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
                                        val updatedUser = user.copy(
                                            fullName = fullName.trim(),
                                            email = email.trim().lowercase(),
                                            documentId = documentId.trim(),
                                            address = address.trim(),
                                            isAdmin = isAdmin,
                                            isActive = isActive
                                        )
                                        viewModel.updateUser(updatedUser)
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
                                    Text("Guardar cambios")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Notificaciones
            AnimatedVisibility(
                visible = showSuccess,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                SuccessNotification()
            }

            AnimatedVisibility(
                visible = operationResult is UserManagementViewModel.OperationResult.Error,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                ErrorNotification(
                    message = (operationResult as? UserManagementViewModel.OperationResult.Error)?.message ?: ""
                )
            }
        }
    }

    // Diálogos
    if (showImagePickerDialog) {
        ImagePickerDialog(
            onDismiss = { showImagePickerDialog = false },
            onSelectFromGallery = {
                showImagePickerDialog = false
                imagePickerLauncher.launch("image/*")
            }
        )
    }

    if (showDeactivateDialog) {
        UserStatusDialog(
            user = selectedUser ?: user,
            currentStatus = isActive,
            onConfirm = { newStatus ->
                isActive = newStatus
                viewModel.toggleUserStatus(user.userId, newStatus)
                showDeactivateDialog = false
            },
            onDismiss = { showDeactivateDialog = false }
        )
    }
}

@Composable
fun ProfileImageSection(
    profileImageUrl: String?,
    userName: String,
    isUploading: Boolean,
    onImageClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(
                        width = 3.dp,
                        color = PurplePrimary,
                        shape = CircleShape
                    )
                    .clickable { onImageClick() },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUrl != null) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Perfil",
                        tint = PurplePrimary,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            if (!isUploading) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Cambiar foto",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-8).dp, y = (-8).dp)
                        .size(32.dp)
                        .background(
                            PurplePrimary,
                            CircleShape
                        )
                        .padding(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = userName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun UserStatusDialog(
    user: User,
    currentStatus: Boolean,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val newStatus = !currentStatus

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (newStatus) Icons.Default.Person else Icons.Default.PersonOff,
                contentDescription = null,
                tint = if (newStatus) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        },
        title = {
            Text(
                text = if (newStatus) "Activar usuario" else "Desactivar usuario",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = if (newStatus)
                        "¿Estás seguro de que quieres activar a este usuario?"
                    else
                        "¿Estás seguro de que quieres desactivar a este usuario?"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = user.fullName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (newStatus)
                        "El usuario podrá acceder al sistema nuevamente."
                    else
                        "El usuario no podrá acceder al sistema.",
                    color = if (newStatus) Color(0xFF4CAF50) else Color(0xFFF44336),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newStatus) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (newStatus) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            ) {
                Text(if (newStatus) "Activar" else "Desactivar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onSelectFromGallery: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                Text(
                    text = "Cambiar foto de perfil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFromGallery() },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Seleccionar de galería",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Elige una imagen del dispositivo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
fun SuccessNotification() {
    Card(
        modifier = Modifier.fillMaxWidth(0.9f),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                    text = "¡Usuario actualizado!",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Los cambios se han guardado correctamente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
