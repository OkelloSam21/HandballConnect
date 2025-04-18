package com.example.handballconnect.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.Feed
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Feed
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsHandball
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.handballconnect.data.storage.ImageStorageManager
import com.example.handballconnect.ui.admin.AdminScreen
import com.example.handballconnect.ui.admin.AdminViewModel
import com.example.handballconnect.ui.auth.AuthViewModel
import com.example.handballconnect.ui.feed.FeedScreen
import com.example.handballconnect.ui.feed.FeedViewModel
import com.example.handballconnect.ui.message.MessageViewModel
import com.example.handballconnect.ui.message.MessagesScreen
import com.example.handballconnect.ui.profile.ProfileScreen
import com.example.handballconnect.ui.tatctic.TacticsScreen
import com.example.handballconnect.ui.tatctic.TacticsViewModel

@Composable
fun MainScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    imageStorageManager: ImageStorageManager
) {
    val userData by authViewModel.userData.collectAsState()
    val isAdmin = userData?.isAdmin == true
    
    val mainNavController = rememberNavController()
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Define navigation items
    val navigationItems = remember(isAdmin) {
        mutableListOf(
            NavigationItem(
                route = "feed",
                title = "Feed",
                selectedIcon = Icons.AutoMirrored.Filled.Feed,
                unselectedIcon = Icons.AutoMirrored.Outlined.Feed
            ),
            NavigationItem(
                route = "messages",
                title = "Messages",
                selectedIcon = Icons.AutoMirrored.Filled.Message,
                unselectedIcon = Icons.AutoMirrored.Outlined.Message
            ),
            NavigationItem(
                route = "tactics",
                title = "Tactics",
                selectedIcon = Icons.Filled.SportsHandball,
                unselectedIcon = Icons.Outlined.SportsHandball
            ),
            NavigationItem(
                route = "profile",
                title = "Profile",
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person
            )
        ).apply {
            // Add admin tab if user is admin
            if (isAdmin) {
                add(
                    NavigationItem(
                        route = "admin",
                        title = "Admin",
                        selectedIcon = Icons.Filled.AdminPanelSettings,
                        unselectedIcon = Icons.Outlined.AdminPanelSettings
                    )
                )
            }
        }
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                navigationItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (currentDestination?.hierarchy?.any { it.route == item.route } == true) {
                                    item.selectedIcon
                                } else {
                                    item.unselectedIcon
                                },
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            mainNavController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(mainNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = mainNavController,
                startDestination = "feed"
            ) {
                composable("feed") {
                    val feedViewModel: FeedViewModel = hiltViewModel()
                    FeedScreen(
                        feedViewModel = feedViewModel,
                        imageStorageManager = imageStorageManager
                    )
                }
                
                composable("messages") {
                    val messageViewModel: MessageViewModel = hiltViewModel()
                    MessagesScreen(messageViewModel = messageViewModel)
                }
                
                composable("tactics") {
                    val tacticsViewModel: TacticsViewModel = hiltViewModel()
                    TacticsScreen(tacticsViewModel = tacticsViewModel)
                }
                
                composable("profile") {
                    ProfileScreen(
                        authViewModel = authViewModel,
                        navController = navController,
                        imageStorageManager  = imageStorageManager
                        )
                }
                
                if (isAdmin) {
                    composable("admin") {
                        val adminViewModel: AdminViewModel = hiltViewModel()
                        AdminScreen(adminViewModel = adminViewModel)
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)