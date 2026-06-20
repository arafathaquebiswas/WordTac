package com.example.domain.scoring

/**
 * Pure, deterministic scoring and ranking engine for WordTac.
 */
object ScoringEngine {

    enum class Outcome {
        CORRECT, WRONG
    }

    data class ScoreBreakdown(
        val baseScore: Int = 0,
        val streakBonuses: Int = 0,
        val penalties: Int = 0,
        val winBonus: Int = 0,
        val totalScore: Int = 0,
        val correctCount: Int = 0,
        val wrongCount: Int = 0,
        val currentStreak: Int = 0,
        val longestStreak: Int = 0,
        val consecutiveMistakes: Int = 0
    )

    /**
     * Calculates the entire score breakdown based on sequential answer outcomes in a match.
     */
    fun calculateScore(outcomes: List<Outcome>, isWinner: Boolean): ScoreBreakdown {
        var baseScore = 0
        var streakBonuses = 0
        var penalties = 0
        var correctCount = 0
        var wrongCount = 0
        var currentStreak = 0
        var longestStreak = 0
        var consecutiveMistakes = 0

        for (outcome in outcomes) {
            if (outcome == Outcome.CORRECT) {
                correctCount++
                currentStreak++
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak
                }
                consecutiveMistakes = 0

                // Base scoring
                baseScore += 5

                // Streak bonuses (one-time upon reaching threshold)
                if (currentStreak == 3) {
                    streakBonuses += 15
                } else if (currentStreak == 5) {
                    streakBonuses += 100
                }
            } else {
                wrongCount++
                currentStreak = 0
                consecutiveMistakes++

                // Consecutive mistakes penalization
                if (consecutiveMistakes == 3) {
                    penalties += 15
                } else if (consecutiveMistakes == 6) {
                    penalties += 30
                } else if (consecutiveMistakes == 9) {
                    penalties += 45
                }
            }
        }

        val winBonus = if (isWinner) 50 else 0
        val totalScore = (baseScore + streakBonuses - penalties + winBonus).coerceAtLeast(0)

        return ScoreBreakdown(
            baseScore = baseScore,
            streakBonuses = streakBonuses,
            penalties = penalties,
            winBonus = winBonus,
            totalScore = totalScore,
            correctCount = correctCount,
            wrongCount = wrongCount,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            consecutiveMistakes = consecutiveMistakes
        )
    }

    /**
     * Level System
     * Level 1 -> 0–999 XP
     * Level 2 -> 1000–2499 XP
     * Level 3 ("Titan") -> 2500+ XP
     */
    fun getLevel(xp: Int): Int {
        return when {
            xp < 1000 -> 1
            xp < 2500 -> 2
            else -> 3
        }
    }

    fun getLevelName(level: Int): String {
        return when (level) {
            1 -> "Novice"
            2 -> "Adept"
            3 -> "Titan"
            else -> "Titan"
        }
    }

    fun getLevelProgress(xp: Int): Float {
        return when {
            xp < 1000 -> xp / 1000f
            xp < 2500 -> (xp - 1000) / 1500f
            else -> 1f
        }
    }

    fun getXPNeededForNextLevel(xp: Int): Int {
        return when {
            xp < 1000 -> 1000 - xp
            xp < 2500 -> 2500 - xp
            else -> 0
        }
    }

    fun getNextLevelThreshold(xp: Int): Int {
        return when {
            xp < 1000 -> 1000
            xp < 2500 -> 2500
            else -> 2500
        }
    }

    /**
     * Rank System
     * Bronze -> 0–499
     * Iron -> 500–999
     * Silver -> 1000–1499
     * Gold -> 1500–1999
     * Platinum -> 2000–2499
     * Titan -> 2500+
     */
    enum class Rank(val displayName: String, val minXp: Int) {
        BRONZE("Bronze", 0),
        IRON("Iron", 500),
        SILVER("Silver", 1000),
        GOLD("Gold", 1500),
        PLATINUM("Platinum", 2000),
        TITAN("Titan", 2500);

        companion object {
            fun fromXp(xp: Int): Rank {
                return values().lastOrNull { xp >= it.minXp } ?: BRONZE
            }
        }
    }
}
