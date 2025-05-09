package com.example.handballconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.handballconnect.data.storage.ImageStorageManager
import com.example.handballconnect.ui.auth.AuthViewModel
import com.example.handballconnect.ui.auth.LoginScreen
import com.example.handballconnect.ui.auth.RegisterScreen
import com.example.handballconnect.ui.main.MainScreen
import com.example.handballconnect.ui.message.ChatScreen
import com.example.handballconnect.ui.message.ChatViewModel
import com.example.handballconnect.ui.profile.ProfileScreen
import com.example.handballconnect.ui.splash.SplashScreen
import com.example.handballconnect.ui.theme.HandballConnectTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imageStorageManager: ImageStorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HandballConnectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HandballConnectApp(
                        imageStorageManager = imageStorageManager
                    )
                }
            }
        }
    }
}

@Composable
fun HandballConnectApp(
    imageStorageManager: ImageStorageManager
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("login") {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("register") {
            RegisterScreen(navController = navController, authViewModel = authViewModel)
        }
        composable("main") {
            MainScreen(navController = navController, authViewModel = authViewModel, imageStorageManager = imageStorageManager)
        }
        composable("profile") {
            ProfileScreen(
                authViewModel = authViewModel,
                navController = navController,
                navigateBack = {navController.popBackStack()},
                imageStorageManager = imageStorageManager
            )
        }
        composable(
            route = "chat/{conversationId}",
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val chatViewModel = hiltViewModel<ChatViewModel>()

            ChatScreen(
                conversationId = conversationId,
                navigateBack = {
                    navController.popBackStack()
                },
                imageStorageManager = imageStorageManager,
                chatViewModel = chatViewModel
            )
        }
    }
}