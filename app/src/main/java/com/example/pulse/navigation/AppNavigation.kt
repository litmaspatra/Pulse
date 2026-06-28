package com.example.pulse.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pulse.screens.ChatScreen
import com.example.pulse.screens.ConfirmPinScreen
import com.example.pulse.screens.CreatePinScreen
import com.example.pulse.screens.LoginScreen
import com.example.pulse.screens.SplashScreen
import com.example.pulse.screens.TrackerScreen
import com.example.pulse.viewmodel.PinViewModel
import com.example.pulse.viewmodel.PinViewModelFactory

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val context = LocalContext.current

    val pinViewModel: PinViewModel = viewModel(
        factory = PinViewModelFactory(context)
    )

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        composable(Routes.SPLASH) {
            val savedPin by pinViewModel.savedPin.collectAsStateWithLifecycle()

            SplashScreen(
                onSplashComplete = {
                    val destination = if (!savedPin.isNullOrEmpty()) {
                        Routes.PIN
                    } else {
                        Routes.CREATE_PIN
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CREATE_PIN) {
            CreatePinScreen { pin ->
                pinViewModel.startPinCreation(pin)
                navController.navigate(Routes.CONFIRM_PIN)
                true
            }
        }

        composable(Routes.CONFIRM_PIN) {
            ConfirmPinScreen { pin ->
                val success = pinViewModel.confirmPin(pin)
                if (success) {
                    navController.navigate(Routes.CHAT) {
                        popUpTo(Routes.CREATE_PIN) { inclusive = true }
                    }
                }
                success
            }
        }

        composable(Routes.PIN) {
            LoginScreen(
                onPinEntered = { enteredPin ->
                    val success = pinViewModel.validatePin(enteredPin)
                    if (success) {
                        navController.navigate(Routes.CHAT) {
                            popUpTo(Routes.PIN) { inclusive = true }
                        }
                    }
                    success
                }
            )
        }

        composable(Routes.CHAT) {
            ChatScreen(
                onDisconnected = {
                    // Stay on the chat route — ChatScreen itself will fall back
                    // to showing PairingScreen since isPaired becomes false.
                }
            )
        }

        composable(Routes.TRACKER) {
            TrackerScreen()
        }
    }
}