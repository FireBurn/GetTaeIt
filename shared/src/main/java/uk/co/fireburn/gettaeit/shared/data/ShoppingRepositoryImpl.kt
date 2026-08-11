package uk.co.fireburn.gettaeit.shared.data

import kotlinx.coroutines.flow.Flow
import uk.co.fireburn.gettaeit.shared.domain.ShoppingRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepositoryImpl @Inject constructor(private val dao: ShoppingItemDao) : ShoppingRepository {
    override fun observeItems(): Flow<List<ShoppingItemEntity>> = dao.observeAll()
    override suspend fun add(title: String, category: String, supermarket: String?) {
        if (title.isNotBlank()) dao.insert(ShoppingItemEntity(title = title.trim(), category = category, supermarket = supermarket?.trim()?.ifBlank { null }))
    }
    override suspend fun toggleBought(item: ShoppingItemEntity) = dao.update(item.copy(isBought = !item.isBought))
    override suspend fun delete(id: UUID) = dao.delete(id)
    override suspend fun clearBought() = dao.clearBought()
}
