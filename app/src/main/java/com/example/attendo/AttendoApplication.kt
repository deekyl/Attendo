package com.example.attendo

import android.app.Application
import com.example.attendo.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AttendoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@AttendoApplication)
            modules(appModule)
        }
    }
}