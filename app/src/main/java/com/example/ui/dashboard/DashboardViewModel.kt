package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.MatchHistoryEntity
import com.example.data.local.PlayerStatsEntity
import com.example.data.repository.GameRepository
import com.example.domain.scoring.ScoringEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val playerStats: PlayerStatsEntity = PlayerStatsEntity(),
    val matches: List<MatchHistoryEntity> = emptyList(),
    val level: Int = 1,
    val levelName: String = "Novice",
    val levelProgress: Float = 0f,
    val rank: ScoringEngine.Rank = ScoringEngine.Rank.BRONZE,
    val accuracyPercent: Float = 0f,
    val xpNeeded: Int = 1000
)

class DashboardViewModel(private val repository: GameRepository) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.playerStats,
        repository.allMatches
    ) { stats, matches ->
        val score = stats.totalScore
        val level = ScoringEngine.getLevel(score)
        val levelName = ScoringEngine.getLevelName(level)
        val progress = ScoringEngine.getLevelProgress(score)
        val rank = ScoringEngine.Rank.fromXp(score)
        val xpNeeded = ScoringEngine.getXPNeededForNextLevel(score)

        val accuracy = if (stats.totalQuestions > 0) {
            (stats.totalCorrect.toFloat() / stats.totalQuestions) * 100f
        } else {
            0f
        }

        DashboardUiState(
            playerStats = stats,
            matches = matches,
            level = level,
            levelName = levelName,
            levelProgress = progress,
            rank = rank,
            accuracyPercent = accuracy,
            xpNeeded = xpNeeded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}

class DashboardViewModelFactory(private val repository: GameRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
