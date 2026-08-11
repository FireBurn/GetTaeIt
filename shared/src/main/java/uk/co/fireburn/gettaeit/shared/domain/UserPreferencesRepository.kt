package uk.co.fireburn.gettaeit.shared.domain

import kotlinx.coroutines.flow.Flow
import uk.co.fireburn.gettaeit.shared.data.UserPreferences
import uk.co.fireburn.gettaeit.shared.domain.RoutineTemplate

interface UserPreferencesRepository {
    fun getUserPreferences(): Flow<UserPreferences>
    suspend fun updateUserPreferences(userPreferences: UserPreferences)
    fun getUserRoutineTemplates(): Flow<List<RoutineTemplate>>
    suspend fun saveUserRoutineTemplate(template: RoutineTemplate)
}
