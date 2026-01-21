package uk.co.fireburn.gettaeit.shared.domain.ai

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uk.co.fireburn.gettaeit.shared.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SubTask(
    val title: String,
    val description: String? = null
)

@Singleton
class GeminiService @Inject constructor() {
    // Replace with your actual API key
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

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
            val response = generativeModel.generateContent(fullPrompt)
            return response.text?.let {
                // Clean the response to ensure it's valid JSON
                val cleanedJson = it.trim().removePrefix("```json").removeSuffix("```").trim()
                json.decodeFromString<List<SubTask>>(cleanedJson)
            } ?: emptyList()
        } catch (e: Exception) {
            // Log the exception or handle it appropriately
            e.printStackTrace()
            return emptyList()
        }
    }
}
