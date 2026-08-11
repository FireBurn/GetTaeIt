package uk.co.fireburn.gettaeit.shared.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items ORDER BY isBought ASC, category ASC, title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItemEntity)

    @Update suspend fun update(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE id = :id") suspend fun delete(id: UUID)
    @Query("DELETE FROM shopping_items WHERE isBought = 1") suspend fun clearBought()
}
