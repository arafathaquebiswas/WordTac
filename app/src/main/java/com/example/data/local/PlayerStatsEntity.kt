package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalScore: Int = 0,
    val totalCorrect: Int = 0,
    val totalQuestions: Int = 0,
    val winCount: Int = 0,
    val lossCount: Int = 0,
    val drawCount: Int = 0,
    val longestStreakEver: Int = 0
)
