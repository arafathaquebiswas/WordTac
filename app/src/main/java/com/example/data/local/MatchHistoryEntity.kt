package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mode: String,
    val difficulty: String,
    val opponentType: String,
    val date: Long,
    val player1Score: Int,
    val player2Score: Int,
    val result: String, // "Player 1 Wins", "Player 2 Wins", "CPU Wins", "Draw"
    val longestStreak: Int
)
