package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.MatchHistoryEntity
import com.example.domain.scoring.ScoringEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedMatchDetail by remember { mutableStateOf<MatchHistoryEntity?>(null) }

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF020617)
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp))
                        Text(
                            text = "ACADEMIC STATS",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 15.sp,
                            color = Color.White
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(bgBrush)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile & Rank Header
            item {
                ProfileHeaderCard(
                    rank = uiState.rank,
                    totalScore = uiState.playerStats.totalScore,
                    level = uiState.level,
                    levelName = uiState.levelName,
                    levelProgress = uiState.levelProgress,
                    xpNeeded = uiState.xpNeeded
                )
            }

            // Stats Matrix Card
            item {
                StatsMatrixCard(
                    winCount = uiState.playerStats.winCount,
                    lossCount = uiState.playerStats.lossCount,
                    drawCount = uiState.playerStats.drawCount,
                    accuracyPercent = uiState.accuracyPercent,
                    longestStreak = uiState.playerStats.longestStreakEver
                )
            }

            // Historical Match Records segment
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MATCH LOG HISTORY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${uiState.matches.size} Plays",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF60A5FA)
                    )
                }
            }

            if (uiState.matches.isEmpty()) {
                item {
                    EmptyHistoryPlaceholder()
                }
            } else {
                items(uiState.matches) { match ->
                    MatchHistoryRowItem(
                        match = match,
                        onClick = { selectedMatchDetail = match }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal popup to inspect direct match details
    selectedMatchDetail?.let { match ->
        MatchDetailPopup(
            match = match,
            onDismiss = { selectedMatchDetail = null }
        )
    }
}

@Composable
fun ProfileHeaderCard(
    rank: ScoringEngine.Rank,
    totalScore: Int,
    level: Int,
    levelName: String,
    levelProgress: Float,
    xpNeeded: Int
) {
    val badgeColor = when (rank) {
        ScoringEngine.Rank.BRONZE -> Color(0xFFCD7F32)
        ScoringEngine.Rank.IRON -> Color(0xFF708090)
        ScoringEngine.Rank.SILVER -> Color(0xFFC0C0C0)
        ScoringEngine.Rank.GOLD -> Color(0xFFFFD700)
        ScoringEngine.Rank.PLATINUM -> Color(0xFF94A3B8)
        ScoringEngine.Rank.TITAN -> Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CURRENT LEVEL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Lvl $level ($levelName)",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Rank Badge representation
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(1.5.dp, badgeColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = badgeColor, modifier = Modifier.size(16.dp))
                        Text(
                            text = rank.displayName.uppercase(),
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar towards next level thresholds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$totalScore XP Total",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                if (level < 3) {
                    Text(
                        text = "$xpNeeded XP to level up",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    Text(
                        text = "MAX LEVEL REACHED",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { levelProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}

@Composable
fun StatsMatrixCard(
    winCount: Int,
    lossCount: Int,
    drawCount: Int,
    accuracyPercent: Float,
    longestStreak: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "CUMULATIVE PERFORMANCE METRICS",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            // Grid column matrices cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    IndicatorMatrixItem("Matches Won", "$winCount", Icons.Default.EmojiEvents, Color(0xFF10B981))
                }
                Box(modifier = Modifier.weight(1f)) {
                    IndicatorMatrixItem("Matches Lost", "$lossCount", Icons.Default.Cancel, Color(0xFFEF4444))
                }
                Box(modifier = Modifier.weight(1f)) {
                    IndicatorMatrixItem("Matches Drawn", "$drawCount", Icons.Default.Handshake, MaterialTheme.colorScheme.primary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Accuracy rate calculation
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { accuracyPercent / 100f },
                                modifier = Modifier.size(52.dp),
                                color = Color(0xFF10B981),
                                trackColor = Color.White.copy(alpha = 0.08f)
                            )
                            val cleanFormat = String.format(Locale.US, "%.0f%%", accuracyPercent)
                            Text(text = cleanFormat, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        Column {
                            Text("Solve Accuracy", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("All-time correct response rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }

                // Longest Streak Box
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                            Text("Peak Streak", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFF97316))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$longestStreak in a row", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun IndicatorMatrixItem(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(12.dp)
    ) {
        Column {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(text = title, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun MatchHistoryRowItem(
    match: MatchHistoryEntity,
    onClick: () -> Unit
) {
    val dateStr = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(match.date))
    } catch (e: Exception) {
        "Unknown"
    }

    val itemColor = when (match.result) {
        "Player 1 Wins" -> Color(0xFF2563EB)
        "CPU Wins", "Player 2 Wins" -> Color(0xFFDC2626)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.02f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = match.mode.uppercase() + " (${match.difficulty})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Opponent: ${if (match.opponentType == "vs Computer") "VS Engine" else "Local Duo"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = match.result.uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = itemColor,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${match.player1Score} - ${match.player2Score} XP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = Icons.Default.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
            Text(
                text = "Welcome onboard! No matches saved yet.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MatchDetailPopup(
    match: MatchHistoryEntity,
    onDismiss: () -> Unit
) {
    val dateLongName = try {
        SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.US).format(Date(match.date))
    } catch (e: Exception) {
        "Unknown"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Match Review Detail",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close match details")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MatchDetailItem("Match Outcome", match.result)
                    MatchDetailItem("Match Date", dateLongName)
                    MatchDetailItem("Match Mode", match.mode.uppercase())
                    MatchDetailItem("Game Difficulty", match.difficulty)
                    MatchDetailItem("Opponent Played", if (match.opponentType == "vs Computer") "VS Engine (CPU)" else "Pass-and-Play (2 Players)")
                    MatchDetailItem("Player 1 Score", "${match.player1Score} XP")
                    MatchDetailItem("Player 2 / CPU Score", "${match.player2Score} XP")
                    MatchDetailItem("Peak Match Streak", "${match.longestStreak} Correct Answers")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK, GOT IT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MatchDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
