package uk.co.fireburn.gettaeit.shared.domain

import kotlinx.coroutines.flow.Flow
import uk.co.fireburn.gettaeit.shared.data.ShoppingItemEntity
import java.util.UUID

interface ShoppingRepository {
    fun observeItems(): Flow<List<ShoppingItemEntity>>
    suspend fun add(title: String, category: String, supermarket: String?)
    suspend fun toggleBought(item: ShoppingItemEntity)
    suspend fun delete(id: UUID)
    suspend fun clearBought()
}
