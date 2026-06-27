package com.example.pulse.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pulse.screens.ChatScreen
import com.example.pulse.screens.PinScreen
import com.example.pulse.screens.TrackerScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.PIN
    ) {

        composable(Routes.PIN) {

            PinScreen(
                onPinEntered = { enteredPin ->

                    when (enteredPin) {

                        "1234" -> {
                            navController.navigate(Routes.CHAT)
                            true
                        }

                        "0000" -> {
                            navController.navigate(Routes.TRACKER)
                            true
                        }

                        else -> {
                            false
                        }

                    }

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