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
import com.example.attendo.data.repositories.AuthRepository
import com.example.attendo.data.repositories.AuthRepositoryImpl
import com.example.attendo.ui.screen.auth.LoginScreen
import com.example.attendo.ui.theme.AttendoTheme
import io.github.jan.supabase.SupabaseClient
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import com.example.attendo.data.model.auth.AuthUiState
import com.example.attendo.data.network.Supabase.spClient
import com.example.attendo.ui.viewmodel.auth.login.LoginViewModel

class MainActivity : ComponentActivity() {

    lateinit var client: SupabaseClient
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initVars()
        enableEdgeToEdge()

        setContent {
            AttendoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthNavigation(authRepository, client)
                }
            }
        }
    }


    private fun initVars() {
        client = spClient
        authRepository = AuthRepositoryImpl(client)
    }
}

@Composable
fun AuthNavigation(authRepository: AuthRepository, client: SupabaseClient) {
    val navController = rememberNavController()

    var startDestination by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        //val currentUser = authRepository.getCurrentUser()
        val currentUser = null
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
                val viewModel: LoginViewModel = viewModel(
                    factory = LoginViewModel.LoginViewModelFactory(client)
                )

                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState) {
                    if (uiState is AuthUiState.Success) {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }

                LoginScreen(
                    viewModel = viewModel,
                    onLogin = { email, password ->
                        viewModel.login(email, password)
                    },
                    onRegister = {
                        navController.navigate("register")
                    }
                )
            }

            composable("register") {

            }

            composable("dashboard") {

            }
        }
    }
}


