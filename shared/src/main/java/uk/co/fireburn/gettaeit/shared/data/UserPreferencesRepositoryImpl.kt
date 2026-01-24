package uk.co.fireburn.gettaeit.shared.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import uk.co.fireburn.gettaeit.shared.domain.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val userPreferencesDao: UserPreferencesDao
) : UserPreferencesRepository {

    override fun getUserPreferences(): Flow<UserPreferences> {
        return userPreferencesDao.getUserPreferences().map { it ?: UserPreferences() }
    }

    override suspend fun setGeminiModel(model: String) {
        val current = userPreferencesDao.getUserPreferences().firstOrNull() ?: UserPreferences()
        userPreferencesDao.updateUserPreferences(current.copy(geminiModel = model))
    }

    override suspend fun updateUserPreferences(userPreferences: UserPreferences) {
        userPreferencesDao.updateUserPreferences(userPreferences)
    }
}
