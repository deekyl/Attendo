package com.example.attendo.di

import com.example.attendo.data.network.Supabase
import com.example.attendo.data.repositories.AuthRepository
import com.example.attendo.data.repositories.AuthRepositoryImpl
import com.example.attendo.ui.viewmodel.auth.login.LoginViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { Supabase.spClient }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    viewModel { LoginViewModel(get()) }
}