package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.WordTacDatabase
import com.example.data.repository.GameRepository
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.dashboard.DashboardViewModelFactory
import com.example.ui.game.GameScreen
import com.example.ui.game.GameViewModel
import com.example.ui.game.GameViewModelFactory
import com.example.ui.home.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize SQLite Room database & DataStore preferences repository
        val database = WordTacDatabase.getDatabase(this)
        repository = GameRepository(this, database.wordTacDao())

        setContent {
            // Live observe Theme preference setting from user profile
            val themeMode by repository.themeFlow.collectAsState(initial = "system")
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        // 1. Home destination
                        composable("home") {
                            HomeScreen(
                                repository = repository,
                                onStartGame = { mode, diff, opponent ->
                                    navController.navigate("game/$mode/$diff/$opponent")
                                },
                                onNavigateToDashboard = {
                                    navController.navigate("dashboard")
                                }
                            )
                        }

                        // 2. Play game match route
                        composable(
                            route = "game/{mode}/{difficulty}/{opponentType}",
                            arguments = listOf(
                                navArgument("mode") { type = NavType.StringType },
                                navArgument("difficulty") { type = NavType.StringType },
                                navArgument("opponentType") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val mode = backStackEntry.arguments?.getString("mode") ?: "meaning"
                            val diff = backStackEntry.arguments?.getString("difficulty") ?: "Medium"
                            val opponent = backStackEntry.arguments?.getString("opponentType") ?: "vs Computer"

                            // Load VM utilizing application scoped context provider
                            val gameViewModel: GameViewModel = viewModel(
                                factory = GameViewModelFactory(application, repository)
                            )

                            GameScreen(
                                viewModel = gameViewModel,
                                mode = mode,
                                difficulty = diff,
                                opponentType = opponent,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 3. User Dashboard stats screen route
                        composable("dashboard") {
                            val dashboardViewModel: DashboardViewModel = viewModel(
                                factory = DashboardViewModelFactory(repository)
                            )

                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
