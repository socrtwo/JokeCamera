package com.jokecamera.app.data

import com.jokecamera.app.model.*
import com.jokecamera.app.platform.SettingsStore
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Cross-platform joke manager. Manages the collection of jokes,
 * tracks which have been told, and supports custom joke import/export.
 */
class JokeRepository(
    private val settings: SettingsStore
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val builtInJokes: List<Joke> = generateBuiltInJokes()
    private var customJokes: List<Joke> = loadCustomJokes()
    private val toldJokeIds: MutableSet<Int> = loadToldJokes()

    companion object {
        private const val KEY_TOLD_JOKES = "told_jokes"
        private const val KEY_CUSTOM_JOKES = "custom_jokes"
        private const val KEY_REPLACED_BUILTIN = "replaced_builtin"
        private const val CUSTOM_ID_OFFSET = 100000
    }

    private val allJokes: List<Joke>
        get() = if (settings.getBoolean(KEY_REPLACED_BUILTIN, false)) {
            customJokes
        } else {
            builtInJokes + customJokes
        }

    private fun getFilteredJokes(): List<Joke> {
        val enabledTypes = getEnabledCategories()
        return if (enabledTypes.isEmpty()) allJokes
        else allJokes.filter { it.type in enabledTypes }
    }

    fun getEnabledCategories(): Set<JokeType> {
        val enabled = mutableSetOf<JokeType>()
        if (settings.getBoolean(AppSettings.KEY_CATEGORY_DAD, true)) enabled.add(JokeType.DAD)
        if (settings.getBoolean(AppSettings.KEY_CATEGORY_GENERAL, true)) enabled.add(JokeType.GENERAL)
        if (settings.getBoolean(AppSettings.KEY_CATEGORY_KNOCK_KNOCK, true)) enabled.add(JokeType.KNOCK_KNOCK)
        if (settings.getBoolean(AppSettings.KEY_CATEGORY_PROGRAMMING, true)) enabled.add(JokeType.PROGRAMMING)
        if (settings.getBoolean(AppSettings.KEY_CATEGORY_CUSTOM, true)) enabled.add(JokeType.CUSTOM)
        return enabled
    }

    fun getNextJoke(): Joke {
        val jokes = getFilteredJokes()
        if (jokes.isEmpty()) {
            return Joke(0, JokeType.GENERAL, "Why did the chicken cross the road?", "To get to the other side!")
        }

        val availableJokes = jokes.filter { it.id !in toldJokeIds }
        if (availableJokes.isEmpty()) {
            val filteredIds = jokes.map { it.id }.toSet()
            toldJokeIds.removeAll(filteredIds)
            saveToldJokes()
            return jokes.random().also {
                toldJokeIds.add(it.id)
                saveToldJokes()
            }
        }

        val joke = availableJokes.random()
        toldJokeIds.add(joke.id)
        saveToldJokes()
        return joke
    }

    fun getRemainingCount(): Int = getFilteredJokes().count { it.id !in toldJokeIds }
    fun getTotalCount(): Int = getFilteredJokes().size
    fun getAllJokesCount(): Int = allJokes.size
    fun getBuiltInCount(): Int = builtInJokes.size
    fun getCustomCount(): Int = customJokes.size
    fun getCountByType(type: JokeType): Int = allJokes.count { it.type == type }
    fun isBuiltInReplaced(): Boolean = settings.getBoolean(KEY_REPLACED_BUILTIN, false)

    fun resetToldJokes() {
        toldJokeIds.clear()
        saveToldJokes()
    }

    fun importJokes(jsonString: String, replaceExisting: Boolean): Int {
        val jokeJsonList: List<JokeJson> = json.decodeFromString(jsonString)
        val newJokes = jokeJsonList.mapIndexed { i, j -> j.toJoke(CUSTOM_ID_OFFSET + i) }

        if (replaceExisting) {
            settings.putBoolean(KEY_REPLACED_BUILTIN, true)
        }

        customJokes = newJokes
        saveCustomJokes()
        resetToldJokes()
        return newJokes.size
    }

    fun clearCustomJokes() {
        customJokes = emptyList()
        settings.remove(KEY_CUSTOM_JOKES)
        settings.putBoolean(KEY_REPLACED_BUILTIN, false)
        resetToldJokes()
    }

    fun exportJokesAsJson(): String {
        val jokeJsonList = allJokes.map { JokeJson(it.type.toJsonString(), it.setup, it.punchline) }
        return json.encodeToString(jokeJsonList)
    }

    fun getTemplateJson(): String = """[
  {
    "type": "general",
    "setup": "Why did the chicken cross the road?",
    "punchline": "To get to the other side!"
  },
  {
    "type": "programming",
    "setup": "Why do programmers prefer dark mode?",
    "punchline": "Because light attracts bugs!"
  },
  {
    "type": "dad",
    "setup": "I'm reading a book about anti-gravity.",
    "punchline": "It's impossible to put down!"
  },
  {
    "type": "knock-knock",
    "setup": "Knock knock.\nWho's there?\nBoo.\nBoo who?",
    "punchline": "Don't cry, it's just a joke!"
  }
]"""

    private fun loadToldJokes(): MutableSet<Int> {
        val stored = settings.getStringSet(KEY_TOLD_JOKES, emptySet())
        return stored.mapNotNull { it.toIntOrNull() }.toMutableSet()
    }

    private fun saveToldJokes() {
        settings.putStringSet(KEY_TOLD_JOKES, toldJokeIds.map { it.toString() }.toSet())
    }

    private fun loadCustomJokes(): List<Joke> {
        val jsonString = settings.getString(KEY_CUSTOM_JOKES, "")
        if (jsonString.isBlank()) return emptyList()
        return try {
            val jokeJsonList: List<JokeJson> = json.decodeFromString(jsonString)
            jokeJsonList.mapIndexed { i, j -> j.toJoke(CUSTOM_ID_OFFSET + i) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCustomJokes() {
        val jokeJsonList = customJokes.map { JokeJson(it.type.toJsonString(), it.setup, it.punchline) }
        settings.putString(KEY_CUSTOM_JOKES, json.encodeToString(jokeJsonList))
    }
}
