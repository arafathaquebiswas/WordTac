package com.example.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.GameRepository
import com.example.data.repository.WordQuestion
import com.example.domain.scoring.ScoringEngine
import com.example.ui.util.SoundVibeHelper
import com.example.ui.util.TtsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface GameplayState {
    object ACTIVE_BOARD : GameplayState
    data class QUIZ_PROMPT(val question: WordQuestion) : GameplayState
    data class EXPLANATION_REVEAL(
        val question: WordQuestion,
        val isCorrect: Boolean,
        val playerAnswer: String,
        val correctAnswer: String
    ) : GameplayState
}

class GameViewModel(
    application: Application,
    private val repository: GameRepository
) : AndroidViewModel(application) {

    // Helper references for Audio/TTS feedback
    private val soundVibeHelper = SoundVibeHelper(application)
    private val ttsHelper = TtsHelper(application)

    // User settings flows cache
    val soundEnabled = MutableStateFlow(true)
    val vibrationEnabled = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            soundEnabled.value = repository.soundEnabledFlow.first()
            vibrationEnabled.value = repository.vibrationEnabledFlow.first()
        }
    }

    // Match Parameters
    private var mode: String = "meaning"
    private var difficulty: String = "Easy"
    private var opponentType: String = "Pass-and-play"

    // Raw Words Library list
    private var questionPool: List<WordQuestion> = emptyList()
    private var poolIndex = 0

    // Core Game States
    private val _board = MutableStateFlow<List<String?>>(List(9) { null })
    val board: StateFlow<List<String?>> = _board.asStateFlow()

    private val _currentPlayer = MutableStateFlow(1) // 1 = Blue (Player 1), 2 = Red (Player 2 or CPU)
    val currentPlayer: StateFlow<Int> = _currentPlayer.asStateFlow()

    private val _viewState = MutableStateFlow<GameplayState>(GameplayState.ACTIVE_BOARD)
    val viewState: StateFlow<GameplayState> = _viewState.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(20)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private val _selectedCell = MutableStateFlow<Int?>(null)
    val selectedCell: StateFlow<Int?> = _selectedCell.asStateFlow()

    private val _mcqOptions = MutableStateFlow<List<String>>(emptyList())
    val mcqOptions: StateFlow<List<String>> = _mcqOptions.asStateFlow()

    private val _winningLine = MutableStateFlow<List<Int>?>(null)
    val winningLine: StateFlow<List<Int>?> = _winningLine.asStateFlow()

    private val _matchConcluded = MutableStateFlow(false)
    val matchConcluded: StateFlow<Boolean> = _matchConcluded.asStateFlow()

    private val _matchWinnerLabel = MutableStateFlow<String?>(null) // "Player 1 Wins", "CPU Wins", "Draw", etc.
    val matchWinnerLabel: StateFlow<String?> = _matchWinnerLabel.asStateFlow()

    private val _gameLogs = MutableStateFlow<List<String>>(listOf("Match started. Player 1 (X) chooses first!"))
    val gameLogs: StateFlow<List<String>> = _gameLogs.asStateFlow()

    // Answer Tracking for Scoring Engine formulas
    val p1Outcomes = MutableStateFlow<List<ScoringEngine.Outcome>>(emptyList())
    val p2Outcomes = MutableStateFlow<List<ScoringEngine.Outcome>>(emptyList())

    // Score Caches calculated live per answer
    val p1TotalScore = MutableStateFlow(0)
    val p2TotalScore = MutableStateFlow(0)

    // CPU decision taking state
    private val _isCpuThinking = MutableStateFlow(false)
    val isCpuThinking: StateFlow<Boolean> = _isCpuThinking.asStateFlow()

    // Active Timer coroutine reference
    private var timerJob: Job? = null

    /**
     * Initializes match configurations and loads correct difficulty vocab assets.
     */
    fun setupMatch(mode: String, difficulty: String, opponentType: String) {
        this.mode = mode
        this.difficulty = difficulty
        this.opponentType = opponentType

        // Load words
        questionPool = repository.loadWordsForDifficulty(difficulty).shuffled()
        poolIndex = 0

        // Reset game board
        _board.value = List(9) { null }
        _currentPlayer.value = 1
        _viewState.value = GameplayState.ACTIVE_BOARD
        _selectedCell.value = null
        _winningLine.value = null
        _matchConcluded.value = false
        _matchWinnerLabel.value = null
        _gameLogs.value = listOf("WordTac standard match started! (${difficulty} | ${opponentType})")

        p1Outcomes.value = emptyList()
        p2Outcomes.value = emptyList()
        p1TotalScore.value = 0
        p2TotalScore.value = 0
        _isCpuThinking.value = false
        timerJob?.cancel()
    }

    /**
     * Triggered when any player taps an un-claimed grid box.
     */
    fun onCellClicked(index: Int) {
        if (_matchConcluded.value || _board.value[index] != null || _isCpuThinking.value) return
        if (_viewState.value != GameplayState.ACTIVE_BOARD) return

        _selectedCell.value = index
        presentQuestion()
    }

    private fun presentQuestion() {
        val q = nextQuestionFromLibrary() ?: return
        _viewState.value = GameplayState.QUIZ_PROMPT(q)

        // Shuffling alternatives
        val optionsList = mutableListOf<String>()
        val currentActiveMode = getCurrentQuestionMode()
        if (currentActiveMode == "synonym") {
            optionsList.add(q.synonym)
            optionsList.addAll(q.distractorSynonyms.take(3))
        } else {
            // "meaning", "engeng", etc. use definitions
            optionsList.add(q.definition)
            optionsList.addAll(q.distractorDefinitions.take(3))
        }
        _mcqOptions.value = optionsList.shuffled()

        // Handle TTS speak aloud if spelling mode is active
        if (currentActiveMode == "spelling") {
            ttsHelper.speak(q.word)
        }

        startTimer()
    }

    private fun getCurrentQuestionMode(): String {
        return if (mode == "timed") {
            // Cycle randomly for sudden death timed battle mode questions
            listOf("meaning", "synonym", "spelling", "engeng").random()
        } else {
            mode
        }
    }

    private fun nextQuestionFromLibrary(): WordQuestion? {
        if (questionPool.isEmpty()) return null
        if (poolIndex >= questionPool.size) {
            questionPool = questionPool.shuffled()
            poolIndex = 0
        }
        return questionPool.getOrNull(poolIndex++)
    }

    /**
     * Checks user responses and rewards grid cells reactively.
     */
    fun submitAnswer(playerAnswer: String) {
        val currentPrompt = _viewState.value as? GameplayState.QUIZ_PROMPT ?: return
        val question = currentPrompt.question
        timerJob?.cancel()

        val activeMode = getCurrentQuestionMode()
        val isCorrect: Boolean
        val correctAnswerString: String

        if (activeMode == "spelling") {
            isCorrect = playerAnswer.trim().lowercase() == question.word.trim().lowercase()
            correctAnswerString = question.word
        } else if (activeMode == "synonym") {
            isCorrect = playerAnswer == question.synonym
            correctAnswerString = question.synonym
        } else {
            isCorrect = playerAnswer == question.definition
            correctAnswerString = question.definition
        }

        resolveCurrentCellOutcome(isCorrect, playerAnswer, correctAnswerString, question)
    }

    private fun resolveCurrentCellOutcome(
        isCorrect: Boolean,
        playerAnswer: String,
        correctAnswerString: String,
        question: WordQuestion
    ) {
        val playerFlag = _currentPlayer.value
        val cellIndex = _selectedCell.value ?: return

        // Outlive score evaluations
        val outcome = if (isCorrect) ScoringEngine.Outcome.CORRECT else ScoringEngine.Outcome.WRONG
        if (playerFlag == 1) {
            val newList = p1Outcomes.value + outcome
            p1Outcomes.value = newList
            p1TotalScore.value = ScoringEngine.calculateScore(newList, false).totalScore
        } else {
            val newList = p2Outcomes.value + outcome
            p2Outcomes.value = newList
            p2TotalScore.value = ScoringEngine.calculateScore(newList, false).totalScore
        }

        // Apply cell claim if correct
        if (isCorrect) {
            soundVibeHelper.playCorrectSound(soundEnabled.value)
            soundVibeHelper.vibrate(vibrationEnabled.value, 150)

            val mark = if (playerFlag == 1) "X" else "O"
            val newBoard = _board.value.toMutableList()
            newBoard[cellIndex] = mark
            _board.value = newBoard

            addLogEntry("Player $playerFlag answered correctly and claimed cell ${cellIndex + 1}!")
        } else {
            soundVibeHelper.playWrongSound(soundEnabled.value)
            soundVibeHelper.vibrate(vibrationEnabled.value, 350)
            addLogEntry("Player $playerFlag got the question wrong! Cell ${cellIndex + 1} remains vacant.")
        }

        // Show learning correction details
        _viewState.value = GameplayState.EXPLANATION_REVEAL(
            question = question,
            isCorrect = isCorrect,
            playerAnswer = playerAnswer,
            correctAnswer = correctAnswerString
        )
    }

    fun proceedToNextTurn() {
        if (_matchConcluded.value) return
        _viewState.value = GameplayState.ACTIVE_BOARD
        _selectedCell.value = null

        // Check if game has ended
        if (evaluateGameEnd()) {
            return
        }

        // Flip Turn
        _currentPlayer.value = if (_currentPlayer.value == 1) 2 else 1

        // Evaluate if CPU player triggers automation
        if (opponentType == "vs Computer" && _currentPlayer.value == 2) {
            triggerCpuTurn()
        }
    }

    private fun triggerCpuTurn() {
        if (_matchConcluded.value) return
        _isCpuThinking.value = true
        addLogEntry("CPU is analyzing board choices...")

        viewModelScope.launch {
            // Simulate realistic human decision thought times (1.5 seconds)
            delay(1500)

            // Select priority-driven cell
            val cpuCell = selectCpuCell(_board.value, "O", "X")
            if (cpuCell != -1) {
                _selectedCell.value = cpuCell
                presentQuestion()

                // Simulating CPU answering with difficulty based rolling
                delay(1200)

                val roll = (1..100).random()
                val accuracyThreshold = when (difficulty.lowercase()) {
                    "easy" -> 50
                    "medium" -> 65
                    "hard" -> 80
                    else -> 50
                }

                val currentPrompt = _viewState.value as? GameplayState.QUIZ_PROMPT
                if (currentPrompt != null) {
                    val isCpuCorrect = roll <= accuracyThreshold
                    val question = currentPrompt.question
                    val correctAnswer = if (getCurrentQuestionMode() == "synonym") question.synonym else question.definition

                    resolveCurrentCellOutcome(
                        isCorrect = isCpuCorrect,
                        playerAnswer = if (isCpuCorrect) correctAnswer else "wrong_gpu_guess",
                        correctAnswerString = correctAnswer,
                        question = question
                    )
                }
            }
            _isCpuThinking.value = false
        }
    }

    private fun selectCpuCell(board: List<String?>, cpuMark: String, playerMark: String): Int {
        // Priority 1: Winning cell
        for (i in 0..8) {
            if (board[i] == null) {
                val test = board.toMutableList()
                test[i] = cpuMark
                if (checkWinningLine(test, cpuMark) != null) return i
            }
        }
        // Priority 2: Block Player's win
        for (i in 0..8) {
            if (board[i] == null) {
                val test = board.toMutableList()
                test[i] = playerMark
                if (checkWinningLine(test, playerMark) != null) return i
            }
        }
        // Priority 3: Take Center
        if (board[4] == null) return 4

        // Priority 4: Take Corner
        val corners = listOf(0, 2, 6, 8).shuffled()
        for (c in corners) {
            if (board[c] == null) return c
        }

        // Priority 5: Remainder
        val rem = (0..8).filter { board[it] == null }.shuffled()
        if (rem.isNotEmpty()) return rem.first()

        return -1
    }

    private fun checkWinningLine(board: List<String?>, mark: String): List<Int>? {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (line in lines) {
            if (board[line[0]] == mark && board[line[1]] == mark && board[line[2]] == mark) {
                return line
            }
        }
        return null
    }

    private fun evaluateGameEnd(): Boolean {
        // Check Player 1 (X)
        val p1WinningIndices = checkWinningLine(_board.value, "X")
        if (p1WinningIndices != null) {
            _winningLine.value = p1WinningIndices
            concludeMatch("Player 1 Wins")
            return true
        }

        // Check Player 2 (O)
        val p2WinningIndices = checkWinningLine(_board.value, "O")
        if (p2WinningIndices != null) {
            _winningLine.value = p2WinningIndices
            val outcomeLabel = if (opponentType == "vs Computer") "CPU Wins" else "Player 2 Wins"
            concludeMatch(outcomeLabel)
            return true
        }

        // Check Draw (Full Board)
        if (_board.value.all { it != null }) {
            concludeMatch("Draw")
            return true
        }

        return false
    }

    private fun concludeMatch(winnerLabel: String) {
        _matchConcluded.value = true
        _matchWinnerLabel.value = winnerLabel
        soundVibeHelper.playVictorySound(soundEnabled.value)
        addLogEntry("Match ended! Outcome: $winnerLabel")

        // Persist records to database inside repository in a clean coroutine scope
        viewModelScope.launch {
            val p1FinalOutcomes = p1Outcomes.value
            val p2FinalOutcomes = p2Outcomes.value

            val p1Breakdown = ScoringEngine.calculateScore(p1FinalOutcomes, winnerLabel == "Player 1 Wins")
            val p2Breakdown = ScoringEngine.calculateScore(p2FinalOutcomes, winnerLabel == "Player 2 Wins" || winnerLabel == "CPU Wins")

            p1TotalScore.value = p1Breakdown.totalScore
            p2TotalScore.value = p2Breakdown.totalScore

            val longestStreakThisMatch = maxOf(p1Breakdown.longestStreak, p2Breakdown.longestStreak)

            repository.saveMatch(
                mode = mode,
                difficulty = difficulty,
                opponentType = opponentType,
                player1Outcomes = p1FinalOutcomes,
                player2Outcomes = p2FinalOutcomes,
                result = winnerLabel,
                longestStreak = longestStreakThisMatch
            )
        }
    }

    private fun handleTimeout() {
        val qState = _viewState.value as? GameplayState.QUIZ_PROMPT ?: return
        val incorrectAnswerString = if (getCurrentQuestionMode() == "synonym") qState.question.synonym else qState.question.definition

        addLogEntry("Player ${_currentPlayer.value} ran out of time!")
        resolveCurrentCellOutcome(
            isCorrect = false,
            playerAnswer = "TIMED_OUT",
            correctAnswerString = incorrectAnswerString,
            question = qState.question
        )
    }

    private fun startTimer() {
        timerJob?.cancel()
        _secondsRemaining.value = 20
        timerJob = viewModelScope.launch {
            while (_secondsRemaining.value > 0) {
                delay(1000)
                _secondsRemaining.value = _secondsRemaining.value - 1
                if (_secondsRemaining.value <= 5 && _secondsRemaining.value > 0) {
                    soundVibeHelper.playTickSound(soundEnabled.value)
                    soundVibeHelper.vibrate(vibrationEnabled.value, 70)
                }
            }
            handleTimeout()
        }
    }

    private fun addLogEntry(text: String) {
        _gameLogs.value = listOf(text) + _gameLogs.value.take(25)
    }

    fun playTts(word: String) {
        ttsHelper.speak(word)
    }

    override fun onCleared() {
        soundVibeHelper.release()
        ttsHelper.shutdown()
        timerJob?.cancel()
        super.onCleared()
    }
}

class GameViewModelFactory(
    private val application: Application,
    private val repository: GameRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
