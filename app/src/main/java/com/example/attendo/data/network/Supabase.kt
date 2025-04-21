package com.example.attendo.data.network

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import com.example.attendo.BuildConfig
import io.github.jan.supabase.postgrest.Postgrest

object Supabase {
    val spClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ){
        install(Auth)
        install(Postgrest)
    }
}