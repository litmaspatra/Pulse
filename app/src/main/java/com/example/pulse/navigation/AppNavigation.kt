package com.example.pulse.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pulse.screens.ChatScreen
import com.example.pulse.screens.LoginScreen
import com.example.pulse.screens.TrackerScreen
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pulse.viewmodel.PinViewModel
import com.example.pulse.viewmodel.PinViewModelFactory
import com.example.pulse.screens.CreatePinScreen
import com.example.pulse.screens.ConfirmPinScreen


@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val context = LocalContext.current

    val pinViewModel: PinViewModel = viewModel(
        factory = PinViewModelFactory(context)
    )

    NavHost(
        navController = navController,
        startDestination = Routes.CREATE_PIN
    ) {
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

                    navController.popBackStack(
                        Routes.PIN,
                        inclusive = false
                    )

                }

                success
            }

        }
        composable(Routes.PIN) {
            LoginScreen (
                onPinEntered = { enteredPin ->

                    val success = pinViewModel.validatePin(enteredPin)

                    if (success) {
                        navController.navigate(Routes.CHAT)
                    }

                    success
                }
            )

        }

        composable(Routes.CHAT) {
            ChatScreen()
        }
        composable(Routes.TRACKER) {
            TrackerScreen()
        }
    }
}