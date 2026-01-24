package uk.co.fireburn.gettaeit.shared.domain.ai

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uk.co.fireburn.gettaeit.shared.BuildConfig
import uk.co.fireburn.gettaeit.shared.data.TaskContext
import uk.co.fireburn.gettaeit.shared.domain.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SubTask(
    val title: String,
    val description: String? = null
)

@Serializable
data class ParsedTask(
    val title: String,
    val context: TaskContext = TaskContext.ANY
)

@Singleton
class GeminiService @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    // Replace with your actual API key
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private suspend fun getGenerativeModel(): GenerativeModel {
        val modelName = getModelName()
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey
        )
    }

    private suspend fun getModelName(): String {
        return userPreferencesRepository.getUserPreferences().first().geminiModel ?: "gemini-pro"
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun breakdownTask(prompt: String): List<SubTask> {
        val fullPrompt = """
            You are a helpful assistant for people with ADHD. A user has given you a task.
            Break it down into small, manageable sub-tasks.
            Return the result as a JSON array of objects, where each object has a "title" and an optional "description".
            Do NOT include any other text or markdown in your response, only the JSON array.

            Example Task: "Clean the kitchen"
            Example Response:
            [
                {"title": "Empty the dishwasher"},
                {"title": "Wipe down all surfaces"},
                {"title": "Sweep the floor"},
                {"title": "Take out the trash"}
            ]

            Task to break down: "$prompt"
        """.trimIndent()

        try {
            val response = getGenerativeModel().generateContent(fullPrompt)
            return response.text?.let {
                val cleanedJson = it.trim().removePrefix("```json").removeSuffix("```").trim()
                json.decodeFromString<List<SubTask>>(cleanedJson)
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    suspend fun parseVoiceInput(prompt: String): List<ParsedTask> {
        val fullPrompt = """
            You are a helpful assistant for people with ADHD. A user has given you a voice command.
            Extract one or more tasks from the command.
            For each task, determine if it is a WORK task or a PERSONAL task. Default to ANY if unsure.
            Return the result as a JSON array of objects, where each object has a "title" and a "context".
            The context must be one of "WORK", "PERSONAL", or "ANY".
            Do NOT include any other text or markdown in your response, only the JSON array.

            Example Command: "Remind me to email Dave when I get to work and also I need to buy milk"
            Example Response:
            [
                {"title": "Email Dave", "context": "WORK"},
                {"title": "Buy milk", "context": "PERSONAL"}
            ]

            Command to parse: "$prompt"
        """.trimIndent()

        try {
            val response = getGenerativeModel().generateContent(fullPrompt)
            return response.text?.let {
                val cleanedJson = it.trim().removePrefix("```json").removeSuffix("```").trim()
                json.decodeFromString<List<ParsedTask>>(cleanedJson)
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}
