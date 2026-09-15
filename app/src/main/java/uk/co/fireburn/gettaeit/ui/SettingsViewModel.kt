package uk.co.fireburn.gettaeit.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import uk.co.fireburn.gettaeit.shared.data.UserPreferences
import uk.co.fireburn.gettaeit.shared.domain.GeofenceManager
import uk.co.fireburn.gettaeit.shared.domain.UserPreferencesRepository
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.co.fireburn.gettaeit.shared.data.AppDatabase
import uk.co.fireburn.gettaeit.shared.domain.AuthRepository
import uk.co.fireburn.gettaeit.shared.data.FirestoreTaskSync
import uk.co.fireburn.gettaeit.shared.data.TaskSyncStore
import com.google.gson.GsonBuilder
import android.content.Intent

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val geofenceManager: GeofenceManager,
    private val appDatabase: AppDatabase,
    private val authRepository: AuthRepository,
    private val firestoreTaskSync: FirestoreTaskSync,
    private val taskSyncStore: TaskSyncStore,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> =
        userPreferencesRepository.getUserPreferences().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    /** Capture the device's current GPS position and save it as the work location. */
    @SuppressLint("MissingPermission")
    fun captureCurrentLocationAsWork() {
        viewModelScope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
                val cts = CancellationTokenSource()
                val location: Location? = suspendCancellableCoroutine { cont ->
                    cont.invokeOnCancellation { cts.cancel() }
                    fusedClient
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { loc: Location? -> cont.resume(loc) }
                        .addOnFailureListener { e -> cont.resumeWithException(e) }
                }
                if (location != null) {
                    setWorkLocation(location.latitude, location.longitude)
                }
            } catch (_: Exception) {
                // Permission not granted or location unavailable — silently swallow
            }
        }
    }

    fun setWorkLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val updated = userPreferences.first().copy(
                workLocationString = "$latitude,$longitude"
            )
            userPreferencesRepository.updateUserPreferences(updated)
            geofenceManager.addWorkGeofence(latitude, longitude)
        }
    }

    fun clearWorkLocation() {
        viewModelScope.launch {
            val updated = userPreferences.first().copy(workLocationString = null)
            userPreferencesRepository.updateUserPreferences(updated)
            geofenceManager.removeWorkGeofence()
        }
    }

    fun setWorkSsid(ssid: String) {
        viewModelScope.launch {
            val updated = userPreferences.first().copy(workSsid = ssid.ifBlank { null })
            userPreferencesRepository.updateUserPreferences(updated)
        }
    }

    fun setWorkHours(startHour: Int, endHour: Int) {
        viewModelScope.launch {
            val current = userPreferences.first()
            val updated = current.copy(
                workSchedule = current.workSchedule.copy(
                    startHour = startHour,
                    endHour = endHour
                )
            )
            userPreferencesRepository.updateUserPreferences(updated)
        }
    }

    fun setWorkingDays(days: List<Int>) {
        viewModelScope.launch {
            val current = userPreferences.first()
            val updated = current.copy(
                workSchedule = current.workSchedule.copy(workingDays = days)
            )
            userPreferencesRepository.updateUserPreferences(updated)
        }
    }

    fun setVacationMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateUserPreferences(
                userPreferences.first().copy(isVacationMode = enabled)
            )
        }
    }

    fun setWearHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.updateUserPreferences(userPreferences.first().copy(wearHapticsEnabled = enabled)) }
    }

    fun exportData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val tasks = appDatabase.taskDao().getAllTasksOnce()
                    val prefs = userPreferences.first()
                    
                    val exportObj = mapOf(
                        "preferences" to prefs,
                        "tasks" to tasks
                    )
                    
                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val json = gson.toJson(exportObj)
                    
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Get Tae It Data Export")
                        putExtra(Intent.EXTRA_TEXT, json)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    
                    appContext.startActivity(Intent.createChooser(intent, "Export Data").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val taskIds = appDatabase.taskDao().getAllTasksOnce().map { it.id }
                // Best effort, while we still hold the sign-in. Offline, the cloud copy stays
                // until the account's data is removed from Firebase.
                firestoreTaskSync.deleteRemoteBackup()
                appDatabase.clearAllTables()
                // Tombstones hold no content, only ids and a time, and tell a paired watch to
                // clear its copy too instead of sending everything back.
                taskSyncStore.deleteLocally(taskIds, deletedAt = System.currentTimeMillis())
                authRepository.signOut()
            }
        }
    }
}
