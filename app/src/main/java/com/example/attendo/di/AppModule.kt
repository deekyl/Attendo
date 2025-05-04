package com.example.attendo.di

import com.example.attendo.data.dao.implementation.TimeRecordImplSupabase
import com.example.attendo.data.dao.implementation.UserDaoImplSupabase
import com.example.attendo.data.dao.interfaces.TimeRecordDao
import com.example.attendo.data.dao.interfaces.UserDao
import com.example.attendo.data.network.Supabase
import com.example.attendo.data.repositories.repository.AuthRepository
import com.example.attendo.data.repositories.implementation.supabase.AuthRepositoryImplSupabase
import com.example.attendo.ui.viewmodel.auth.login.LoginViewModel
import com.example.attendo.ui.viewmodel.timerecord.TimeRecordViewModel
import com.example.attendo.ui.viewmodel.user.UserViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { Supabase.spClient }

    // DAos
    single<UserDao> { UserDaoImplSupabase(get()) }
    single<TimeRecordDao> { TimeRecordImplSupabase(get()) }

    // Repositorios
    single<AuthRepository> { AuthRepositoryImplSupabase(get(), get()) }

    // ViewModels
    viewModel { LoginViewModel(get()) }
    viewModel { UserViewModel(get()) }
    viewModel { parameters -> TimeRecordViewModel(get(), parameters.get()) }
}