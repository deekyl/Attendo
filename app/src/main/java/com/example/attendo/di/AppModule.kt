package com.example.attendo.di

import com.example.attendo.data.dao.implementation.UserDaoImplSupabase
import com.example.attendo.data.dao.interfaces.UserDao
import com.example.attendo.data.network.Supabase
import com.example.attendo.data.repositories.repository.AuthRepository
import com.example.attendo.data.repositories.implementation.supabase.AuthRepositoryImplSupabase
import com.example.attendo.ui.viewmodel.auth.login.LoginViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { Supabase.spClient }
    single<UserDao> { UserDaoImplSupabase(get()) }
    single<AuthRepository> { AuthRepositoryImplSupabase(get(), get()) }
    viewModel { LoginViewModel(get()) }
}