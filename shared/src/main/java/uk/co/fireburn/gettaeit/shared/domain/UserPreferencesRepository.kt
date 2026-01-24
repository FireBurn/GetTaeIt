package uk.co.fireburn.gettaeit.shared.domain

import kotlinx.coroutines.flow.Flow
import uk.co.fireburn.gettaeit.shared.data.UserPreferences

interface UserPreferencesRepository {
    fun getUserPreferences(): Flow<UserPreferences>
    suspend fun setGeminiModel(model: String)
    suspend fun updateUserPreferences(userPreferences: UserPreferences)
}
