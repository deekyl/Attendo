package com.example.attendo.data.network

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object Supabase {
    val spClient = createSupabaseClient(
        supabaseUrl = "https://izauesysdxyzkyhwdvdg.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Iml6YXVlc3lzZHh5emt5aHdkdmRnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDM2MDc2NzksImV4cCI6MjA1OTE4MzY3OX0.rZrDhFo_UhhcPXoUo0QWT_EYVW_hQIeL7xy6u1OmaLQ"
    ){
        install(Auth)
        install(Postgrest)
    }
}