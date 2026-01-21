package uk.co.fireburn.gettaeit.auto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.assisted.AssistedFactory
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import javax.inject.Inject

class AutoViewModelFactory @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AutoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AutoViewModel(taskRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
