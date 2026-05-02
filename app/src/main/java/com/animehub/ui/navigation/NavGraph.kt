package com.animehub.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.animehub.ui.screens.detail.DetailScreen
import com.animehub.ui.screens.home.HomeScreen
import com.animehub.ui.screens.player.PlayerScreen
import com.animehub.ui.screens.search.SearchScreen
import com.animehub.ui.screens.settings.SettingsScreen
import com.animehub.ui.screens.sources.SourcesScreen
import com.animehub.ui.screens.sources.WebDavBrowserScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Filled.Home)
    data object Search : Screen("search", "搜索", Icons.Filled.Search)
    data object Sources : Screen("sources", "源", Icons.Filled.Storage)
    data object Settings : Screen("settings", "设置", Icons.Filled.Settings)
}

val bottomNavItems = listOf(Screen.Home, Screen.Search, Screen.Sources, Screen.Settings)

object DetailNav {
    const val DETAIL = "detail/{animeId}/{sourceId}"
    const val PLAYER = "player/{episodeId}/{animeId}/{sourceId}"
    const val WEBDAV_BROWSER = "webdav_browser/{sourceId}"

    fun detailRoute(animeId: String, sourceId: String): String {
        return "detail/${Uri.encode(animeId)}/${Uri.encode(sourceId)}"
    }

    fun playerRoute(episodeId: String, animeId: String, sourceId: String): String {
        return "player/${Uri.encode(episodeId)}/${Uri.encode(animeId)}/${Uri.encode(sourceId)}"
    }

    fun webdavBrowserRoute(sourceId: String): String {
        return "webdav_browser/${Uri.encode(sourceId)}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeHubNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToDetail = { animeId, sourceId ->
                        navController.navigate(DetailNav.detailRoute(animeId, sourceId))
                    },
                    onNavigateToPlayer = { episodeId, animeId, sourceId ->
                        navController.navigate(DetailNav.playerRoute(episodeId, animeId, sourceId))
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateToDetail = { animeId, sourceId ->
                        navController.navigate(DetailNav.detailRoute(animeId, sourceId))
                    }
                )
            }

            composable(Screen.Sources.route) {
                SourcesScreen(
                    onNavigateToBrowser = { sourceId ->
                        navController.navigate(DetailNav.webdavBrowserRoute(sourceId))
                    }
                )
            }

            composable(
                route = DetailNav.WEBDAV_BROWSER,
                arguments = listOf(
                    navArgument("sourceId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val sourceId = Uri.decode(backStackEntry.arguments?.getString("sourceId") ?: return@composable)
                WebDavBrowserScreen(
                    sourceId = sourceId,
                    onBack = { navController.popBackStack() },
                    onNavigateToPlayer = { episodeId, animeId, srcId ->
                        navController.navigate(DetailNav.playerRoute(episodeId, animeId, srcId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = DetailNav.DETAIL,
                arguments = listOf(
                    navArgument("animeId") { type = NavType.StringType },
                    navArgument("sourceId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val animeId = Uri.decode(backStackEntry.arguments?.getString("animeId") ?: return@composable)
                val sourceId = Uri.decode(backStackEntry.arguments?.getString("sourceId") ?: return@composable)
                DetailScreen(
                    animeId = animeId,
                    sourceId = sourceId,
                    onNavigateToPlayer = { episodeId, animeId, srcId ->
                        navController.navigate(DetailNav.playerRoute(episodeId, animeId, srcId))
                    }
                )
            }

            composable(
                route = DetailNav.PLAYER,
                arguments = listOf(
                    navArgument("episodeId") { type = NavType.StringType },
                    navArgument("animeId") { type = NavType.StringType },
                    navArgument("sourceId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val episodeId = Uri.decode(backStackEntry.arguments?.getString("episodeId") ?: return@composable)
                val animeId = Uri.decode(backStackEntry.arguments?.getString("animeId") ?: return@composable)
                val sourceId = Uri.decode(backStackEntry.arguments?.getString("sourceId") ?: return@composable)
                PlayerScreen(
                    episodeId = episodeId,
                    animeId = animeId,
                    sourceId = sourceId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
