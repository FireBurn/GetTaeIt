package uk.co.fireburn.gettaeit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.fireburn.gettaeit.shared.data.ShoppingItemEntity
import uk.co.fireburn.gettaeit.shared.domain.ShoppingRepository
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel @Inject constructor(private val repository: ShoppingRepository) : ViewModel() {
    val items = repository.observeItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun add(title: String, category: String, supermarket: String) = viewModelScope.launch { repository.add(title, category, supermarket) }
    fun toggle(item: ShoppingItemEntity) = viewModelScope.launch { repository.toggleBought(item) }
    fun delete(item: ShoppingItemEntity) = viewModelScope.launch { repository.delete(item.id) }
    fun clearBought() = viewModelScope.launch { repository.clearBought() }
}
