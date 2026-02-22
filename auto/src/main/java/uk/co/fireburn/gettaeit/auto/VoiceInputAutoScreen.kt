package uk.co.fireburn.gettaeit.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template

class VoiceInputAutoScreen(
    carContext: CarContext,
    private val viewModel: AutoViewModel
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val searchCallback = object : SearchTemplate.SearchCallback {
            override fun onSearchSubmitted(searchTerm: String) {
                // Send dictated text to the ViewModel to be parsed by AI
                viewModel.addTasksFromVoice(searchTerm)
                screenManager.pop() // Return to task list after parsing
            }
        }

        return SearchTemplate.Builder(searchCallback)
            .setShowKeyboardByDefault(false) // Triggers voice input natively in Android Auto
            .setHeaderAction(Action.BACK)
            .build()
    }
}
