package com.example.attendo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.attendo.data.repositories.repository.AuthRepository
import com.example.attendo.ui.screen.auth.LoginScreen
import com.example.attendo.ui.theme.AttendoTheme
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import com.example.attendo.data.model.auth.AuthUiState
import com.example.attendo.data.model.user.UserState
import com.example.attendo.ui.screen.breaktype.BreakTypeManagementScreen
import com.example.attendo.ui.screen.dashboard.AdminDashboardScreen
import com.example.attendo.ui.screen.dashboard.UserDashboardScreen
import com.example.attendo.ui.screen.profile.ProfileScreen
import com.example.attendo.ui.screen.timerecord.AdminTimeRecordListScreen
import com.example.attendo.ui.screen.timerecord.ManualTimeRecordScreen
import com.example.attendo.ui.screen.timerecord.TimeRecordListScreen
import com.example.attendo.ui.screen.user.CreateUserScreen
import com.example.attendo.ui.screen.user.EditUserScreen
import com.example.attendo.ui.screen.user.UserManagementScreen
import com.example.attendo.ui.viewmodel.auth.login.LoginViewModel
import com.example.attendo.ui.viewmodel.timerecord.TimeRecordViewModel
import com.example.attendo.ui.viewmodel.user.UserManagementViewModel
import com.example.attendo.ui.viewmodel.user.UserViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AttendoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthNavigation()
                }
            }
        }
    }
}

@Composable
fun AuthNavigation() {
    val navController = rememberNavController()
    val authRepository: AuthRepository = koinInject()

    var startDestination by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        val currentUser = authRepository.getCurrentUser()
        startDestination = if (currentUser != null) {
            "dashboard"
        } else {
            "login"
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    startDestination?.let { destination: String ->
        NavHost(
            navController = navController,
            startDestination = destination
        ) {
            composable("login") {
                val viewModel = koinViewModel<LoginViewModel>()
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState) {
                    if (uiState is AuthUiState.Success) {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }

                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("dashboard") {
                val userViewModel = koinViewModel<UserViewModel>()
                val userState by userViewModel.userState.collectAsState()

                LaunchedEffect(navController.currentBackStackEntry) {
                    userViewModel.refreshUserData()
                }

                when (userState) {
                    is UserState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is UserState.Regular -> {
                        val timeRecordViewModel = koinViewModel<TimeRecordViewModel> {
                            parametersOf((userState as UserState.Regular).user.userId)
                        }

                        LaunchedEffect(navController.currentBackStackEntry) {
                            timeRecordViewModel.refreshData()
                        }

                        UserDashboardScreen(
                            user = (userState as UserState.Regular).user,
                            onLogout = {
                                userViewModel.logout {
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            },
                            onTimeRecordListClick = { user ->
                                navController.navigate("timeRecordList/${user.userId}")
                            },
                            onProfileClick = { user ->
                                navController.navigate("profile/${user.userId}") {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    is UserState.Admin -> {
                        val timeRecordViewModel = koinViewModel<TimeRecordViewModel> {
                            parametersOf((userState as UserState.Admin).user.userId)
                        }

                        LaunchedEffect(navController.currentBackStackEntry) {
                            timeRecordViewModel.refreshData()
                        }

                        AdminDashboardScreen(
                            user = (userState as UserState.Admin).user,
                            onLogout = {
                                userViewModel.logout {
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            },
                            onTimeRecordListClick = { user ->
                                navController.navigate("adminTimeRecordList") {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onProfileClick = { user ->
                                navController.navigate("profile/${user.userId}") {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onAddManualTimeRecordClick = { user ->
                                navController.navigate("addManualTimeRecord/${user.userId}") {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onManageBreakTypesClick = {
                                navController.navigate("manageBreakTypes") {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onManageUsersClick = {
                                navController.navigate("userManagement") {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }


                    is UserState.Inactive -> {
                        LaunchedEffect(Unit) {
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    }

                    is UserState.Error -> {
                        LaunchedEffect(Unit) {
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }
                    }
                }
            }
            composable("timeRecordList/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                val userViewModel = koinViewModel<UserViewModel>()
                val userState by userViewModel.userState.collectAsState()

                var showContent by remember { mutableStateOf(false) }

                LaunchedEffect(userState) {
                    when (userState) {
                        is UserState.Regular -> {
                            showContent = true
                        }

                        is UserState.Loading -> {
                            // Seguimos esperando
                        }

                        else -> {
                            // Solo navegamos atrás si no está cargando
                            if (userState !is UserState.Loading) {
                                navController.popBackStack()
                            }
                        }
                    }
                }

                if (showContent) {
                    val user = (userState as UserState.Regular).user
                    TimeRecordListScreen(
                        user = user,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    // Mostrar un indicador de carga mientras esperamos
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            composable("adminTimeRecordList") {
                val userViewModel = koinViewModel<UserViewModel>()
                val userState by userViewModel.userState.collectAsState()

                when (userState) {
                    is UserState.Admin -> {
                        val adminUser = (userState as UserState.Admin).user

                        if (adminUser != null) {
                            val rememberedUser = remember { adminUser }

                            AdminTimeRecordListScreen(
                                adminUser = rememberedUser,
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    is UserState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
            }

            composable("addManualTimeRecord/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                val userViewModel = koinViewModel<UserViewModel>()
                val userState by userViewModel.userState.collectAsState()

                when (userState) {
                    is UserState.Admin -> {
                        val adminUser = (userState as UserState.Admin).user

                        if (adminUser != null) {
                            ManualTimeRecordScreen(
                                adminUser = adminUser,
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    is UserState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
            }

            composable("manageBreakTypes") {
                val userViewModel = koinViewModel<UserViewModel>()
                val userState by userViewModel.userState.collectAsState()

                when (userState) {
                    is UserState.Admin -> {
                        BreakTypeManagementScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    is UserState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
            }
            composable("profile/{userId}") { backStackEntry ->
                val userId =
                    backStackEntry.arguments?.getString("userId") ?: return@composable
                val userViewModel = koinViewModel<UserViewModel>()
                val userState by userViewModel.userState.collectAsState()

                LaunchedEffect(Unit) {
                    userViewModel.refreshUserData()
                }


                when (userState) {
                    is UserState.Regular -> {
                        val user = (userState as UserState.Regular).user
                        if (user.userId == userId) {
                            ProfileScreen(
                                user = user,
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                            }
                        }
                    }

                    is UserState.Admin -> {
                        val user = (userState as UserState.Admin).user
                        if (user.userId == userId) {
                            ProfileScreen(
                                user = user,
                                onBack = { navController.popBackStack() }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                            }
                        }
                    }

                    is UserState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
            }

            composable("userManagement") {
                val userViewModel = koinViewModel<UserViewModel>()
                val userState by userViewModel.userState.collectAsState()
                val userManagementViewModel = koinViewModel<UserManagementViewModel>()

                LaunchedEffect(navController.currentBackStackEntry) {
                    userManagementViewModel.refreshUsers()
                }


                when (userState) {
                    is UserState.Admin -> {
                        UserManagementScreen(
                            onBack = { navController.popBackStack() },
                            onUserSelected = { user ->
                                navController.navigate("editUser/${user.userId}") {
                                    launchSingleTop = true
                                }
                            },
                            onCreateUser = {
                                navController.navigate("createUser") {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    is UserState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
            }

            composable("createUser") {
                val userViewModel = koinViewModel<UserViewModel>()
                val userState by userViewModel.userState.collectAsState()

                when (userState) {
                    is UserState.Admin -> {
                        CreateUserScreen(
                            onBack = { navController.popBackStack() },
                            onUserCreated = {
                                navController.popBackStack()
                            }
                        )
                    }

                    is UserState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
            }

            composable("editUser/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                val userViewModel = koinViewModel<UserViewModel>()
                val userState by userViewModel.userState.collectAsState()
                val userManagementViewModel = koinViewModel<UserManagementViewModel>()
                val allUsers by userManagementViewModel.allUsers.collectAsState()

                when (userState) {
                    is UserState.Admin -> {
                        val userToEdit = allUsers.find { it.userId == userId }

                        if (userToEdit != null) {
                            EditUserScreen(
                                user = userToEdit,
                                onBack = { navController.popBackStack() },
                                onUserUpdated = {
                                    navController.popBackStack()
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                navController.popBackStack()
                            }
                        }
                    }

                    is UserState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    else -> {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}