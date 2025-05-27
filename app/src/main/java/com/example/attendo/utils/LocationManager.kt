package com.example.attendo.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)

class AttendoLocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder = if (Geocoder.isPresent()) {
        Geocoder(context, Locale.getDefault())
    } else null

    //Verifica si la aplicación tiene permisos de ubicación
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }


    // Verifica si los servicios de ubicación están habilitados
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    //  Obtiene la ubicación actual del usuario
    suspend fun getCurrentLocation(): Result<LocationData> {
        if (!hasLocationPermission()) {
            return Result.failure(Exception("Permisos de ubicación no concedidos"))
        }

        if (!isLocationEnabled()) {
            return Result.failure(Exception("Servicios de ubicación deshabilitados"))
        }

        return try {
            val location = getLastKnownLocation()
            if (location != null) {
                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = getAddressFromLocation(location.latitude, location.longitude)
                )
                Log.d("Attendo", "Ubicación obtenida: $locationData")
                Result.success(locationData)
            } else {
                Result.failure(Exception("No se pudo obtener la ubicación"))
            }
        } catch (e: Exception) {
            Log.e("Attendo", "Error obteniendo ubicación: ${e.message}", e)
            Result.failure(e)
        }
    }


     // Obtiene la última ubicación conocida
    private suspend fun getLastKnownLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            try {
                if (!hasLocationPermission()) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                // Primero intentamos obtener la última ubicación conocida
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            Log.d("Attendo", "Última ubicación conocida obtenida")
                            continuation.resume(location)
                        } else {
                            // Si no hay última ubicación, pedimos una nueva
                            Log.d("Attendo", "No hay última ubicación, solicitando nueva")
                            requestCurrentLocation(continuation)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(
                            "Attendo",
                            "Error obteniendo última ubicación: ${exception.message}",
                            exception
                        )
                        // Si falla, intentamos obtener una nueva ubicación
                        requestCurrentLocation(continuation)
                    }
            } catch (e: SecurityException) {
                Log.e("Attendo", "Error de seguridad: ${e.message}", e)
                continuation.resume(null)
            }
        }


    // Solicita la ubicación actual en tiempo real
    private fun requestCurrentLocation(continuation: kotlin.coroutines.Continuation<Location?>) {
        try {
            if (!hasLocationPermission()) {
                continuation.resume(null)
                return
            }

            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )
                .addOnSuccessListener { location: Location? ->
                    Log.d("Attendo", "Ubicación actual obtenida: $location")
                    continuation.resume(location)
                }
                .addOnFailureListener { exception ->
                    Log.e(
                        "Attendo",
                        "Error obteniendo ubicación actual: ${exception.message}",
                        exception
                    )
                    continuation.resume(null)
                }
        } catch (e: SecurityException) {
            Log.e(
                "Attendo",
                "Error de seguridad al solicitar ubicación actual: ${e.message}",
                e
            )
            continuation.resume(null)
        }
    }


     // Convierte coordenadas en una dirección
    private fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            if (geocoder == null) {
                Log.w("AttendoLocation", "Geocoder no disponible")
                return "Lat: ${String.format("%.6f", latitude)}, Lng: ${
                    String.format(
                        "%.6f",
                        longitude
                    )
                }"
            }

            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                buildString {
                    // Construir dirección
                    address.thoroughfare?.let { append("$it ") }
                    address.subThoroughfare?.let { append("$it, ") }
                    address.locality?.let { append("$it, ") }
                    address.adminArea?.let { append("$it") }
                }.trim().removeSuffix(",")
            } else {
                "Lat: ${String.format("%.6f", latitude)}, Lng: ${String.format("%.6f", longitude)}"
            }
        } catch (e: Exception) {
            Log.e("AttendoLocation", "Error obteniendo dirección: ${e.message}", e)
            "Lat: ${String.format("%.6f", latitude)}, Lng: ${String.format("%.6f", longitude)}"
        }
    }


    // Formatea la ubicación para mostrar al usuario
    fun formatLocationForDisplay(locationData: LocationData): String {
        return locationData.address ?: "Lat: ${
            String.format(
                "%.6f",
                locationData.latitude
            )
        }, Lng: ${String.format("%.6f", locationData.longitude)}"
    }
}


//Composable para gestionar permisos de ubicación

@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (!hasPermission) {
            showRationale = true
        }
    }

    return LocationPermissionState(
        hasPermission = hasPermission,
        showRationale = showRationale,
        requestPermission = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        dismissRationale = { showRationale = false }
    )
}

data class LocationPermissionState(
    val hasPermission: Boolean,
    val showRationale: Boolean,
    val requestPermission: () -> Unit,
    val dismissRationale: () -> Unit
)