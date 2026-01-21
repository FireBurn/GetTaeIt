package uk.co.fireburn.gettaeit.shared.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferenceKeys {
        val USER_PREFERENCES = stringPreferencesKey("user_preferences")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val json = preferences[PreferenceKeys.USER_PREFERENCES]
        if (json != null) {
            Gson().fromJson(json, UserPreferences::class.java)
        } else {
            UserPreferences(officeLocation = null, homeLocation = null, workSsid = null) // Default value
        }
    }

    suspend fun updateUserPreferences(userPreferences: UserPreferences) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.USER_PREFERENCES] = Gson().toJson(userPreferences)
        }
    }
}
