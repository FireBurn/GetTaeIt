package uk.co.fireburn.gettaeit.shared.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uk.co.fireburn.gettaeit.shared.domain.RoutineTemplate
import uk.co.fireburn.gettaeit.shared.domain.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val userPreferencesDao: UserPreferencesDao
) : UserPreferencesRepository {
    private val gson = Gson()

    override fun getUserPreferences(): Flow<UserPreferences> =
        userPreferencesDao.getUserPreferences().map { it ?: UserPreferences() }

    override suspend fun updateUserPreferences(userPreferences: UserPreferences) {
        userPreferencesDao.updateUserPreferences(userPreferences)
    }

    override fun getUserRoutineTemplates(): Flow<List<RoutineTemplate>> =
        getUserPreferences().map { prefs ->
            runCatching {
                gson.fromJson<List<RoutineTemplate>>(
                    prefs.routineTemplatesJson,
                    object : TypeToken<List<RoutineTemplate>>() {}.type
                ) ?: emptyList()
            }.getOrDefault(emptyList())
        }

    override suspend fun saveUserRoutineTemplate(template: RoutineTemplate) {
        val prefs = getUserPreferences().first()
        val templates = getUserRoutineTemplates().first()
        updateUserPreferences(prefs.copy(routineTemplatesJson = gson.toJson(templates + template)))
    }

    override suspend fun addXp(amount: Int) {
        val prefs = getUserPreferences().first()
        updateUserPreferences(prefs.copy(xp = prefs.xp + amount))
    }

    override suspend fun updateSpoons(spoons: Int) {
        val prefs = getUserPreferences().first()
        updateUserPreferences(prefs.copy(dailySpoons = spoons, lastSpoonUpdateDate = System.currentTimeMillis()))
    }

    override suspend fun unlockSticker(stickerId: String) {
        val prefs = getUserPreferences().first()
        val currentStickers = runCatching {
            gson.fromJson<List<String>>(prefs.unlockedStickersJson, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        }.getOrDefault(emptyList())
        if (!currentStickers.contains(stickerId)) {
            val updatedStickers = currentStickers + stickerId
            updateUserPreferences(prefs.copy(unlockedStickersJson = gson.toJson(updatedStickers)))
        }
    }
}
