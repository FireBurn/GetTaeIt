package uk.co.fireburn.gettaeit.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    var showMap by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    val userPreferences by viewModel.userPreferences.collectAsState()
    val context = LocalContext.current
    var showPermissionRationale by remember { mutableStateOf(false) }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    ) { permissionsResult ->
        // This callback is triggered after the user responds to the permission request.
        if (permissionsResult.values.all { it }) {
            // Only show the map if permissions were just granted.
            showMap = true
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Location Permission Needed") },
            text = { Text("To automatically track tasks at your workplace, the app needs access to your location. This helps switch to 'Work' mode when you arrive.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    locationPermissions.launchMultiplePermissionRequest()
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Not now")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Changed to Top for better layout
    ) {
        if (showMap) {
            Box(Modifier.fillMaxSize()) {
                val cameraPositionState = rememberCameraPositionState {
                    position = userPreferences.workLocation?.let {
                        CameraPosition.fromLatLngZoom(it, 15f)
                    } ?: CameraPosition.fromLatLngZoom(LatLng(55.9533, -3.1883), 10f) // Edinburgh
                }
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng -> selectedLocation = latLng }
                ) {
                    selectedLocation?.let {
                        Marker(state = MarkerState(position = it), title = "Work Location")
                    }
                }
                Button(
                    onClick = {
                        selectedLocation?.let {
                            viewModel.setWorkLocation(it)
                            Toast.makeText(
                                context,
                                "Sorted! Work location saved.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showMap = false
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    enabled = selectedLocation != null
                ) {
                    Text("Save Work Location")
                }
            }
        } else {
            // This is the default view when not picking a location
            userPreferences.workLocation?.let { location ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Work Location Set (Geofence Active)")
                        Text(
                            "Lat: ${
                                String.format(
                                    "%.4f",
                                    location.latitude
                                )
                            }, Lng: ${String.format("%.4f", location.longitude)}"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.clearWorkLocation() }) {
                            Text("Clear Location")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            val buttonText =
                if (userPreferences.workLocation == null) "Set Work Location" else "Change Work Location"
            Button(onClick = {
                when {
                    locationPermissions.allPermissionsGranted -> showMap = true
                    locationPermissions.shouldShowRationale -> showPermissionRationale = true
                    else -> locationPermissions.launchMultiplePermissionRequest()
                }
            }) {
                Text(buttonText)
            }

            // Handle permanently denied case
            if (!locationPermissions.allPermissionsGranted && !locationPermissions.shouldShowRationale) {
                val revokedPermissions =
                    locationPermissions.permissions.filter { it.status != PermissionStatus.Granted }
                if (revokedPermissions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("You've permanently denied location permission. To use this feature, please enable it in the app settings.")
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.fromParts("package", context.packageName, null)
                        context.startActivity(intent)
                    }) {
                        Text("Open Settings")
                    }
                }
            }
        }
    }
}
