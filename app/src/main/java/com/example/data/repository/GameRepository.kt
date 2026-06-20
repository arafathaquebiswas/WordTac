package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.MatchHistoryEntity
import com.example.data.local.PlayerStatsEntity
import com.example.data.local.WordTacDao
import com.example.domain.scoring.ScoringEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "wordtac_settings")

data class WordQuestion(
    val word: String,
    val definition: String,
    val synonym: String,
    val distractorDefinitions: List<String>,
    val distractorSynonyms: List<String>,
    val simpleExplanation: String
)

class GameRepository(
    private val context: Context,
    private val dao: WordTacDao
) {
    // Keys for Preferences DataStore
    private val KEY_SOUND = booleanPreferencesKey("sound_enabled")
    private val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
    private val KEY_THEME = stringPreferencesKey("theme_mode")

    val soundEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOUND] ?: true
    }

    val vibrationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_VIBRATION] ?: true
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: "system"
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOUND] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VIBRATION] = enabled
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme
        }
    }

    // Room operations
    val allMatches: Flow<List<MatchHistoryEntity>> = dao.getAllMatches()

    val playerStats: Flow<PlayerStatsEntity> = dao.getPlayerStatsFlow().map { stats ->
        stats ?: PlayerStatsEntity()
    }

    /**
     * Saves a completed match, evaluates final scores, and updates cumulative stats in a single transaction path.
     */
    suspend fun saveMatch(
        mode: String,
        difficulty: String,
        opponentType: String,
        player1Outcomes: List<ScoringEngine.Outcome>,
        player2Outcomes: List<ScoringEngine.Outcome>,
        result: String, // "Player 1 Wins", "Player 2 (or CPU) Wins", "Draw"
        longestStreak: Int
    ): MatchHistoryEntity {
        val isPlayer1Winner = result == "Player 1 Wins"

        // Calculate final scores using deterministic scoring engine
        val p1ScoreBreakdown = ScoringEngine.calculateScore(player1Outcomes, isPlayer1Winner)
        val p2ScoreBreakdown = ScoringEngine.calculateScore(player2Outcomes, result == "Player 2 Wins" || result == "CPU Wins")

        val match = MatchHistoryEntity(
            mode = mode,
            difficulty = difficulty,
            opponentType = opponentType,
            date = System.currentTimeMillis(),
            player1Score = p1ScoreBreakdown.totalScore,
            player2Score = p2ScoreBreakdown.totalScore,
            result = result,
            longestStreak = longestStreak
        )

        // Pre-save match Record
        dao.insertMatchHistory(match)

        // Update Cumulative Statistics
        val currentStats = dao.getPlayerStatsSync() ?: PlayerStatsEntity()
        val isWin = result == "Player 1 Wins"
        val isLoss = result == "Player 2 Wins" || result == "CPU Wins"
        val isDraw = result == "Draw"

        val updatedStats = PlayerStatsEntity(
            id = 1,
            totalScore = currentStats.totalScore + p1ScoreBreakdown.totalScore,
            totalCorrect = currentStats.totalCorrect + p1ScoreBreakdown.correctCount,
            totalQuestions = currentStats.totalQuestions + (p1ScoreBreakdown.correctCount + p1ScoreBreakdown.wrongCount),
            winCount = currentStats.winCount + (if (isWin) 1 else 0),
            lossCount = currentStats.lossCount + (if (isLoss) 1 else 0),
            drawCount = currentStats.drawCount + (if (isDraw) 1 else 0),
            longestStreakEver = maxOf(currentStats.longestStreakEver, longestStreak)
        )

        dao.insertPlayerStats(updatedStats)
        return match
    }

    /**
     * Reads difficulty specific word pools from JSON assets.
     */
    fun loadWordsForDifficulty(difficulty: String): List<WordQuestion> {
        val filename = when (difficulty.lowercase()) {
            "easy" -> "easy_words.json"
            "medium" -> "medium_words.json"
            "hard" -> "hard_words.json"
            else -> "easy_words.json"
        }

        val jsonString = try {
            context.assets.open(filename).use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        }

        val list = mutableListOf<WordQuestion>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val distDefs = mutableListOf<String>()
                val arrDefs = obj.getJSONArray("distractor_definitions")
                for (j in 0 until arrDefs.length()) {
                    distDefs.add(arrDefs.getString(j))
                }

                val distSyns = mutableListOf<String>()
                val arrSyns = obj.getJSONArray("distractor_synonyms")
                for (j in 0 until arrSyns.length()) {
                    distSyns.add(arrSyns.getString(j))
                }

                list.add(
                    WordQuestion(
                        word = obj.getString("word"),
                        definition = obj.getString("definition"),
                        synonym = obj.getString("synonym"),
                        distractorDefinitions = distDefs,
                        distractorSynonyms = distSyns,
                        simpleExplanation = obj.optString("simple_explanation", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
