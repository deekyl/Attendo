package com.example.attendo.ui.screen.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.attendo.data.model.user.User
import com.example.attendo.ui.theme.PurplePrimary
import com.example.attendo.ui.viewmodel.user.UserManagementViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    onUserSelected: (User) -> Unit,
    onCreateUser: () -> Unit,
    viewModel: UserManagementViewModel = koinViewModel()
) {
    val allUsers by viewModel.allUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showSuccessMessage by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(operationResult) {
        operationResult?.let {
            when (it) {
                is UserManagementViewModel.OperationResult.Success -> {
                    showSuccessMessage = true
                    delay(3000)
                    showSuccessMessage = false
                    viewModel.clearOperationResult()
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
                title = { Text("Gestionar usuarios") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver atrás"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCreateUser) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Crear usuario"
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
            if (isLoading && allUsers.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (allUsers.isEmpty()) {
                EmptyUsersMessage(onCreateUser = onCreateUser)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        UsersSummaryCard(
                            totalUsers = allUsers.size,
                            activeUsers = allUsers.count { it.isActive },
                            adminUsers = allUsers.count { it.isAdmin }
                        )
                    }

                    items(allUsers) { user ->
                        UserManagementCard(
                            user = user,
                            onUserClick = { onUserSelected(user) },
                            onToggleStatus = { isActive ->
                                coroutineScope.launch {
                                    viewModel.toggleUserStatus(user.userId, isActive)
                                }
                            },
                            onDeleteUser = { showDeleteDialog = user }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showSuccessMessage,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                SuccessNotification(
                    message = (operationResult as? UserManagementViewModel.OperationResult.Success)?.message ?: ""
                )
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

    showDeleteDialog?.let { user ->
        DeleteUserDialog(
            user = user,
            onConfirm = {
                coroutineScope.launch {
                    viewModel.deleteUser(user.userId)
                }
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }
}

@Composable
fun UsersSummaryCard(
    totalUsers: Int,
    activeUsers: Int,
    adminUsers: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(
                count = totalUsers,
                label = "Total",
                icon = Icons.Default.People,
                color = MaterialTheme.colorScheme.primary
            )
            SummaryItem(
                count = activeUsers,
                label = "Activos",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50)
            )
            SummaryItem(
                count = adminUsers,
                label = "Admins",
                icon = Icons.Default.AdminPanelSettings,
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun SummaryItem(
    count: Int,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UserManagementCard(
    user: User,
    onUserClick: () -> Unit,
    onToggleStatus: (Boolean) -> Unit,
    onDeleteUser: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (user.isActive)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (user.isActive) 2.dp else 0.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (user.isActive)
                            PurplePrimary.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 2.dp,
                        color = if (user.isActive) PurplePrimary else Color.Gray,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (user.profileImage.isNotEmpty()) {
                    AsyncImage(
                        model = user.profileImage,
                        contentDescription = "Avatar de ${user.fullName}",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (user.isActive) PurplePrimary else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (user.isActive)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (user.isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFFF9800).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Admin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF9800),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (user.isActive)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (user.isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (user.isActive) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (user.isActive) "Activo" else "Inactivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (user.isActive) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Switch(
                    checked = user.isActive,
                    onCheckedChange = { onToggleStatus(it) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                IconButton(
                    onClick = onDeleteUser,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar usuario",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyUsersMessage(onCreateUser: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.People,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No hay usuarios registrados",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Crea el primer usuario para comenzar",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateUser,
            colors = ButtonDefaults.buttonColors(
                containerColor = PurplePrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crear usuario")
        }
    }
}

@Composable
fun DeleteUserDialog(
    user: User,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFF44336)
            )
        },
        title = {
            Text(
                text = "Eliminar usuario",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("¿Estás seguro de que quieres eliminar al usuario?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = user.fullName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Esta acción no se puede deshacer.",
                    color = Color(0xFFF44336),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Text("Eliminar")
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
fun SuccessNotification(message: String) {
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
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ErrorNotification(message: String) {
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
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}