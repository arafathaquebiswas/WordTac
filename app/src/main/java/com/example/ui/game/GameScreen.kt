package com.example.ui.game
import com.example.data.repository.WordQuestion

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.scoring.ScoringEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    mode: String,
    difficulty: String,
    opponentType: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe flow states reactively
    val board by viewModel.board.collectAsState()
    val currentPlayer by viewModel.currentPlayer.collectAsState()
    val viewState by viewModel.viewState.collectAsState()
    val secondsRemaining by viewModel.secondsRemaining.collectAsState()
    val selectedCell by viewModel.selectedCell.collectAsState()
    val mcqOptions by viewModel.mcqOptions.collectAsState()
    val winningLine by viewModel.winningLine.collectAsState()
    val matchConcluded by viewModel.matchConcluded.collectAsState()
    val winnerLabel by viewModel.matchWinnerLabel.collectAsState()
    val gameLogs by viewModel.gameLogs.collectAsState()
    val isCpuThinking by viewModel.isCpuThinking.collectAsState()

    // Live score observations
    val p1Score by viewModel.p1TotalScore.collectAsState()
    val p2Score by viewModel.p2TotalScore.collectAsState()

    // Initialize/Setup match once upon entry
    LaunchedEffect(mode, difficulty, opponentType) {
        viewModel.setupMatch(mode, difficulty, opponentType)
    }

    // Color definitions
    val p1Color = Color(0xFF3B82F6) // Electric high-contrast Blue
    val p2Color = Color(0xFFEF4444) // Vibrant Crimson Red
    val activeColor = if (currentPlayer == 1) p1Color else p2Color

    val containerBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF020617)
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "WORDTAC ARENA",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${difficulty.uppercase()} • ${if (opponentType == "vs Computer") "VS ENGINE" else "LOCAL DUO"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(containerBg)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Stats Scoreboard Header
            ScoreboardHeader(
                p1Score = p1Score,
                p2Score = p2Score,
                currentPlayer = currentPlayer,
                opponentType = opponentType,
                p1Color = p1Color,
                p2Color = p2Color
            )

            // Dynamic Action Board Panel (Normal Board vs Question View)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                when (val state = viewState) {
                    is GameplayState.ACTIVE_BOARD -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "TurnPulse")
                            val blinkAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "TurnDotAlpha"
                            )

                            // Turn directive label
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = activeColor.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, activeColor.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val currentLabel = if (currentPlayer == 1) {
                                        "Player 1 (X) turn"
                                    } else {
                                        if (opponentType == "vs Computer") "CPU (O) turn..." else "Player 2 (O) turn"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(activeColor.copy(alpha = blinkAlpha), CircleShape)
                                    )
                                    Text(
                                        text = currentLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeColor
                                    )
                                }
                            }

                            // Interactive Board View
                            WordTacBoard(
                                board = board,
                                onCellClicked = { viewModel.onCellClicked(it) },
                                winningLine = winningLine,
                                p1Color = p1Color,
                                p2Color = p2Color,
                                isCpuThinking = isCpuThinking
                            )

                            if (isCpuThinking) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = p2Color
                                    )
                                    Text(
                                        text = "CPU is plotting the best move...",
                                        fontSize = 11.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = p2Color
                                    )
                                }
                            } else {
                                Text(
                                    text = "Tap any empty square to invoke a vocabulary challenge!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    is GameplayState.QUIZ_PROMPT -> {
                        QuizSection(
                            question = state.question,
                            mcqOptions = mcqOptions,
                            secondsRemaining = secondsRemaining,
                            activeColor = activeColor,
                            soundEnabled = viewModel.soundEnabled.value,
                            vibrationEnabled = viewModel.vibrationEnabled.value,
                            onSubmit = { viewModel.submitAnswer(it) },
                            onPlayTts = { viewModel.playTts(it) },
                            isSpellingMode = mode == "spelling" || (mode == "timed" && state.question.word.isNotEmpty() && mcqOptions.isEmpty()) // Helper guess spelling
                        )
                    }

                    is GameplayState.EXPLANATION_REVEAL -> {
                        LearningExplanationSection(
                            question = state.question,
                            isCorrect = state.isCorrect,
                            playerAnswer = state.playerAnswer,
                            correctAnswer = state.correctAnswer,
                            activeColor = activeColor,
                            onProceed = { viewModel.proceedToNextTurn() },
                            onPlayTts = { viewModel.playTts(it) }
                        )
                    }
                }
            }

            // Game logs visual list at bottom (glass card styling)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.03f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "CONSOLE LOGS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        gameLogs.takeLast(2).forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(activeColor, CircleShape)
                                )
                                Text(
                                    text = log,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Match Concluded outcomes Overhang Dialog
    if (matchConcluded) {
        AlertDialog(
            onDismissRequest = { /* Force explicit interact */ },
            title = {
                Text(
                    text = "MATCH RESULT",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val labelColor = when (winnerLabel) {
                        "Player 1 Wins" -> p1Color
                        "CPU Wins", "Player 2 Wins" -> p2Color
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Text(
                        text = winnerLabel?.uppercase() ?: "DRAW",
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                        color = labelColor,
                        textAlign = TextAlign.Center
                    )

                    // Draw celebratory icons or check
                    if (winnerLabel == "Player 1 Wins") {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Victory",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier
                                .size(64.dp)
                                .scale(1.2f)
                        )
                    } else if (winnerLabel == "Draw") {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = "Draw",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.HeartBroken,
                            contentDescription = "Defeat/Opponent Victory",
                            tint = p2Color,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    // Score summary box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Player 1 Final Score:", fontSize = 13.sp)
                                Text("$p1Score XP", fontWeight = FontWeight.Bold, color = p1Color, fontSize = 13.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val secondLabel = if (opponentType == "vs Computer") "CPU Final Score:" else "Player 2 Final Score:"
                                Text(secondLabel, fontSize = 13.sp)
                                Text("$p2Score XP", fontWeight = FontWeight.Bold, color = p2Color, fontSize = 13.sp)
                            }
                        }
                    }

                    val matchingOutcomes = if (winnerLabel == "Player 1") viewModel.p1Outcomes.value else viewModel.p2Outcomes.value
                    if (matchingOutcomes.isNotEmpty()) {
                        val maxStreak = ScoringEngine.calculateScore(matchingOutcomes, false).longestStreak
                        Text(
                            text = "Longest correct answering streak: $maxStreak",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("SAVE & CONTINUE TO HOME", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ScoreboardHeader(
    p1Score: Int,
    p2Score: Int,
    currentPlayer: Int,
    opponentType: String,
    p1Color: Color,
    p2Color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.04f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player 1 Header Card
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Circle, contentDescription = null, tint = p1Color, modifier = Modifier.size(8.dp))
                    Text("PLAYER 1 (X)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = p1Color)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("$p1Score XP", fontWeight = FontWeight.Black, fontSize = 18.sp, color = p1Color)
            }

            // VS accent divider
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("VS", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }

            // Player 2 / CPU Card
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val label = if (opponentType == "vs Computer") "CPU (O)" else "PLAYER 2 (O)"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Circle, contentDescription = null, tint = p2Color, modifier = Modifier.size(8.dp))
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = p2Color)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("$p2Score XP", fontWeight = FontWeight.Black, fontSize = 18.sp, color = p2Color)
            }
        }
    }
}

@Composable
fun WordTacBoard(
    board: List<String?>,
    onCellClicked: (Int) -> Unit,
    winningLine: List<Int>?,
    p1Color: Color,
    p2Color: Color,
    isCpuThinking: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in 0..2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0..2) {
                    val index = row * 3 + col
                    val cellMark = board[index]
                    val isWinningCell = winningLine?.contains(index) == true

                    // Scale animation if winning line claimed
                    val scale by animateFloatAsState(
                        targetValue = if (isWinningCell) 1.15f else 1.0f,
                        animationSpec = repeatallTweenSpec(),
                        label = "WinningGlow"
                    )

                    val cellBg = if (isWinningCell) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                        Color.White.copy(alpha = 0.04f)
                    }

                    val cellBorderColor = if (isWinningCell) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White.copy(alpha = 0.12f)
                    }

                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .scale(scale)
                            .clip(RoundedCornerShape(16.dp))
                            .background(cellBg)
                            .border(
                                width = if (isWinningCell) 2.dp else 1.dp,
                                color = cellBorderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(
                                enabled = cellMark == null && !isCpuThinking,
                                onClick = { onCellClicked(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cellMark != null) {
                            val symbolColor = if (cellMark == "X") p1Color else p2Color
                            Text(
                                text = cellMark,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = symbolColor,
                                modifier = Modifier.animateContentSize()
                            )
                        } else if (!isCpuThinking) {
                            // Subtle futuristic dot/plus indicating interaction possibility
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun repeatallTweenSpec(): AnimationSpec<Float> {
    return infiniteRepeatable(
        animation = durationBasedTweenSpec(),
        repeatMode = RepeatMode.Reverse
    )
}

private fun durationBasedTweenSpec(): DurationBasedAnimationSpec<Float> {
    return tween(durationMillis = 600, easing = FastOutSlowInEasing)
}

@Composable
fun QuizSection(
    question: WordQuestion,
    mcqOptions: List<String>,
    secondsRemaining: Int,
    activeColor: Color,
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSubmit: (String) -> Unit,
    onPlayTts: (String) -> Unit,
    isSpellingMode: Boolean
) {
    // Local text state for spelling typing
    var typedValue by remember { mutableStateOf("") }

    // Shaking modifier on extreme timers
    val isTimerCrucial = secondsRemaining <= 5
    val pulseScale by animateFloatAsState(
        targetValue = if (isTimerCrucial) 1.1f else 1.0f,
        animationSpec = repeatallTweenSpec(),
        label = "TimerPulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Countdowns indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUESTION TASK",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                // Timer pill
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isTimerCrucial) Color(0xFFDC2626) else activeColor)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.HourglassEmpty, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(
                            text = "${secondsRemaining}s",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Word Prompt Label (Blank in pure Spelling Mode)
            if (isSpellingMode) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(activeColor.copy(alpha = 0.1f))
                        .clickable { onPlayTts(question.word) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak pronunciation aloud",
                        tint = activeColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(
                    text = "Press the icon to hear the spelling word!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = question.word.uppercase(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // Sub-query type directives
            val hintText = if (isSpellingMode) "Spell the vocal word correctly:" else "Choose the correct matching definition:"
            Text(
                text = hintText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            // Multiple alternative lists or input textbox depending on mode
            if (isSpellingMode) {
                OutlinedTextField(
                    value = typedValue,
                    onValueChange = { typedValue = it },
                    placeholder = { Text("type spelling here...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onSubmit(typedValue) },
                    enabled = typedValue.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("SUBMIT SPELLING", fontWeight = FontWeight.Bold)
                }
            } else {
                mcqOptions.forEach { option ->
                    val optionBg = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.02f))
                    )
                    Card(
                        onClick = { onSubmit(option) },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(optionBg, RoundedCornerShape(16.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = option,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LearningExplanationSection(
    question: WordQuestion,
    isCorrect: Boolean,
    playerAnswer: String,
    correctAnswer: String,
    activeColor: Color,
    onProceed: () -> Unit,
    onPlayTts: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Indicator
            Icon(
                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.size(54.dp)
            )

            Text(
                text = if (isCorrect) "ACCURATE!" else "INCORRECT",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 1.sp,
                color = if (isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
            )

            // Spelling Audio/Word presentation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = question.word.uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { onPlayTts(question.word) }) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Pronounce again", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Divider(color = Color.White.copy(alpha = 0.08f))

            // Verification lists
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isCorrect) {
                    Text(
                        text = "Your Answer: ${if (playerAnswer == "TIMED_OUT") "Expired (Timed Out)" else playerAnswer}",
                        color = Color(0xFFF87171),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Correct Definition: $correctAnswer",
                    color = Color(0xFF34D399),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (question.synonym.isNotEmpty()) {
                    Text(
                        text = "Synonym: ${question.synonym}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Learning 1-liner explanation box (English ↔ English requirement)
            if (question.simpleExplanation.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "LEARNING GUIDE:\n${question.simpleExplanation}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Start
                    )
                }
            }

            // ONLY visible primary action button
            Button(
                onClick = onProceed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCorrect) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("PROCEED", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
