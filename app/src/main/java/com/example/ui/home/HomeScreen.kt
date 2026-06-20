package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.repository.GameRepository
import kotlinx.coroutines.launch

data class GameModeItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: GameRepository,
    onStartGame: (mode: String, difficulty: String, opponent: String) -> Unit,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Preferences Settings States
    val soundEnabled by repository.soundEnabledFlow.collectAsState(initial = true)
    val vibrationEnabled by repository.vibrationEnabledFlow.collectAsState(initial = true)
    val themeMode by repository.themeFlow.collectAsState(initial = "system")

    // Match setups local states
    var selectedMode by remember { mutableStateOf("meaning") }
    var selectedDifficulty by remember { mutableStateOf("Medium") }
    var selectedOpponent by remember { mutableStateOf("vs Computer") }

    var showSettingsDialog by remember { mutableStateOf(false) }

    val modeItems = listOf(
        GameModeItem("meaning", "Meaning Arena", Icons.Default.MenuBook, "Identify correct semantic dictionary definitions"),
        GameModeItem("synonym", "Synonyms Match", Icons.Default.SyncAlt, "Match words with their lexical synonyms"),
        GameModeItem("spelling", "Sonic Spelling", Icons.Default.Hearing, "Listen to native audio and write spelling"),
        GameModeItem("engeng", "English-English Challenge", Icons.Default.Translate, "Immersive ESL mode designed for advanced learners"),
        GameModeItem("timed", "Timed Sudde-Death", Icons.Default.Timer, "20-second flash intervals. Maximum intensity")
    )

    // Vibrant cosmic deep-space gradient background
    val spaceBg = Brush.verticalGradient(
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
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF10B981))))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "WORDTAC",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Preferences Shortcut",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(spaceBg)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Creative Logo & Brand presentation banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.04f))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.wordtac_logo),
                    contentDescription = "WordTac Logo Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VOCABULARY ARENA",
                                color = Color(0xFF93C5FD),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "A Tic-Tac-Toe Vocabulary Quest",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Conquer cells by solving lexical puzzles",
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Game Mode Section
            GlassCard(
                title = "CHOOSE GAME MODE",
                titleColor = Color(0xFF3B82F6),
                icon = Icons.Default.SportsEsports
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    modeItems.forEach { item ->
                        val isSelected = selectedMode == item.id
                        val itemBg = if (isSelected) {
                            Brush.linearGradient(listOf(Color(0xFF1E3A8A).copy(alpha = 0.8f), Color(0xFF1E293B).copy(alpha = 0.8f)))
                        } else {
                            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.03f), Color.White.copy(alpha = 0.03f)))
                        }
                        val itemBorder = if (isSelected) {
                            BorderStroke(1.5.dp, Color(0xFF3B82F6))
                        } else {
                            BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        }

                        Card(
                            onClick = { selectedMode = item.id },
                            shape = RoundedCornerShape(16.dp),
                            border = itemBorder,
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(itemBg, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        text = item.description,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Difficulty & Opponent setup cards side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Difficulty Setting
                Box(modifier = Modifier.weight(1f)) {
                    GlassCard(
                        title = "DIFFICULTY",
                        titleColor = Color(0xFF10B981),
                        icon = Icons.Default.BarChart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Easy", "Medium", "Hard").forEach { diff ->
                                val isSelected = selectedDifficulty == diff
                                val buttonBg = if (isSelected) {
                                    Brush.linearGradient(listOf(Color(0xFF047857), Color(0xFF064E3B)))
                                } else {
                                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.05f)))
                                }
                                val borderStroke = if (isSelected) {
                                    BorderStroke(1.dp, Color(0xFF10B981))
                                } else {
                                    BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                }

                                Card(
                                    onClick = { selectedDifficulty = diff },
                                    shape = RoundedCornerShape(12.dp),
                                    border = borderStroke,
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .background(buttonBg, RoundedCornerShape(12.dp))
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = diff,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Opponent setting
                Box(modifier = Modifier.weight(1f)) {
                    GlassCard(
                        title = "OPPONENT",
                        titleColor = Color(0xFFEF4444),
                        icon = Icons.Default.Group
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("vs Computer", "Pass-and-play").forEach { opp ->
                                val isSelected = selectedOpponent == opp
                                val buttonBg = if (isSelected) {
                                    Brush.linearGradient(listOf(Color(0xFFB91C1C), Color(0xFF7F1D1D)))
                                } else {
                                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.05f)))
                                }
                                val borderStroke = if (isSelected) {
                                    BorderStroke(1.dp, Color(0xFFEF4444))
                                } else {
                                    BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                }

                                Card(
                                    onClick = { selectedOpponent = opp },
                                    shape = RoundedCornerShape(12.dp),
                                    border = borderStroke,
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .background(buttonBg, RoundedCornerShape(12.dp))
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (opp == "vs Computer") "VS Engine" else "Local Duo",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PRIMARY FIRE ACTION: Let's Play Button
            Button(
                onClick = { onStartGame(selectedMode, selectedDifficulty, selectedOpponent) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981))
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
                contentPadding = PaddingValues()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Text(
                        text = "START WORDTAC MATCH",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }

            // Dashboard Navigate Button
            OutlinedButton(
                onClick = onNavigateToDashboard,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.03f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF60A5FA))
                    Text(
                        text = "LIFETIME STATS & HISTORY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.7.sp,
                        color = Color(0xFF93C5FD)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Settings Configuration Panel Dialog
    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF1E293B),
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Preferences",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        IconButton(
                            onClick = { showSettingsDialog = false },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close settings", tint = Color.White)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA)
                                    )
                                }
                                Column {
                                    Text("Sound Effects", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text("Play matches beep guides", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                }
                            }
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = { scope.launch { repository.setSoundEnabled(it) } }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Vibration,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399)
                                    )
                                }
                                Column {
                                    Text("Haptic Feedback", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text("Tactile cell pulse", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                }
                            }
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = { scope.launch { repository.setVibrationEnabled(it) } }
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Visual Skin", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val themes = listOf("system" to "System", "light" to "Classic", "dark" to "Tactical Dark")
                                themes.forEach { (modeVal, label) ->
                                    val isSel = themeMode == modeVal
                                    Button(
                                        onClick = { scope.launch { repository.setTheme(modeVal) } },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSel) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.05f),
                                            contentColor = if (isSel) Color.White else Color.White.copy(alpha = 0.7f)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun GlassCard(
    title: String,
    icon: ImageVector,
    titleColor: Color = Color(0xFF10B981),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = titleColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title.uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = titleColor
                )
            }
            content()
        }
    }
}
