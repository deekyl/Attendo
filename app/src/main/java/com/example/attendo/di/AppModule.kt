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
import com.example.attendo.data.dao.implementation.BreakTypeImplSupabase
import com.example.attendo.data.dao.interfaces.BreakTypeDao
import com.example.attendo.ui.viewmodel.timerecord.AdminTimeRecordListViewModel
import com.example.attendo.ui.viewmodel.timerecord.ManualTimeRecordViewModel
import com.example.attendo.ui.viewmodel.timerecord.UserTimeRecordListViewModel
import com.example.attendo.ui.viewmodel.breaktype.BreakTypeViewModel

val appModule = module {
    single { Supabase.spClient }

    // DAos
    single<UserDao> { UserDaoImplSupabase(get()) }
    single<TimeRecordDao> { TimeRecordImplSupabase(get()) }
    single<BreakTypeDao> { BreakTypeImplSupabase(get()) }

    // Repositorios
    single<AuthRepository> { AuthRepositoryImplSupabase(get(), get()) }

    // ViewModels
    viewModel { LoginViewModel(get()) }
    viewModel { UserViewModel(get()) }
    viewModel { parameters ->
        TimeRecordViewModel(
            timeRecordDao = get(),
            breakTypeDao = get(),
            userId = parameters.get()
        )
    }
    viewModel { parameters ->
        UserTimeRecordListViewModel(
            timeRecordDao = get(),
            breakTypeDao = get(),
            userId = parameters.get()
        )
    }

    viewModel { parameters ->
        AdminTimeRecordListViewModel(
            timeRecordDao = get(),
            breakTypeDao = get(),
            userDao = get(),
            adminUserId = parameters.get()
        )
    }

    viewModel { parameters ->
        ManualTimeRecordViewModel(
            timeRecordDao = get(),
            breakTypeDao = get(),
            userDao = get(),
            adminUserId = parameters.get()
        )
    }

    viewModel { BreakTypeViewModel(get()) }
}