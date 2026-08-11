package uk.co.fireburn.gettaeit.shared.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val title: String,
    val category: String = "Other",
    val isBought: Boolean = false,
    val supermarket: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
