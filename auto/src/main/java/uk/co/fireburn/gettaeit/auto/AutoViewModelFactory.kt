package uk.co.fireburn.gettaeit.auto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import uk.co.fireburn.gettaeit.shared.domain.TaskRepository
import uk.co.fireburn.gettaeit.shared.domain.ai.HybridTaskService
import javax.inject.Inject

class AutoViewModelFactory @Inject constructor(
    private val taskRepository: TaskRepository,
    private val hybridTaskService: HybridTaskService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AutoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AutoViewModel(taskRepository, hybridTaskService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
