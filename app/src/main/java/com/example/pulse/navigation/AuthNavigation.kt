package com.example.pulse.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pulse.screens.ConfirmPinScreen
import com.example.pulse.screens.CreatePinScreen
import com.example.pulse.screens.ForgotPinScreen
import com.example.pulse.screens.LoginScreen
import com.example.pulse.screens.SecurityQuestionSetupScreen
import com.example.pulse.screens.SplashScreen
import com.example.pulse.viewmodel.PinType
import com.example.pulse.viewmodel.PinViewModel
import com.example.pulse.viewmodel.PinViewModelFactory

private const val SPLASH = "auth_splash"
private const val PIN = "auth_pin"
private const val CREATE_PIN = "auth_create_pin"
private const val CONFIRM_PIN = "auth_confirm_pin"
private const val SECURITY_Q = "auth_security_q"
private const val FORGOT_PIN = "auth_forgot_pin"
private const val RESET_PIN = "auth_reset_pin"
private const val RESET_CONFIRM = "auth_reset_confirm"

@Composable
fun AuthNavigation(
    onUnlocked: () -> Unit,
    onOpenChat: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val pinViewModel: PinViewModel = viewModel(factory = PinViewModelFactory(context))
    val primaryPin by pinViewModel.primaryPin.collectAsStateWithLifecycle()

    fun goToJournal() { onUnlocked() }

    NavHost(
        navController = navController,
        startDestination = SPLASH,
        enterTransition = { fadeIn(tween(120)) },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(120)) },
        popExitTransition = { fadeOut(tween(120)) }
    ) {
        composable(SPLASH) {
            SplashScreen(
                onSplashComplete = {
                    val dest = if (!primaryPin.isNullOrEmpty()) PIN else CREATE_PIN
                    navController.navigate(dest) { popUpTo(SPLASH) { inclusive = true } }
                }
            )
        }

        composable(CREATE_PIN) {
            CreatePinScreen { pin ->
                pinViewModel.startPinCreation(pin)
                navController.navigate(CONFIRM_PIN)
                true
            }
        }

        composable(CONFIRM_PIN) {
            ConfirmPinScreen { pin ->
                val success = pinViewModel.confirmPin(pin)
                if (success) {
                    navController.navigate(SECURITY_Q) { popUpTo(CREATE_PIN) { inclusive = true } }
                }
                success
            }
        }

        composable(SECURITY_Q) {
            SecurityQuestionSetupScreen(
                onCompleted = { question, answer ->
                    pinViewModel.saveSecurityQuestion(question, answer)
                    goToJournal()
                }
            )
        }

        composable(PIN) {
            LoginScreen(
                onPinEntered = { enteredPin ->
                    when (pinViewModel.validatePin(enteredPin)) {
                        PinType.PRIMARY -> { goToJournal(); true }
                        PinType.SECOND -> { onOpenChat(); true }
                        PinType.INVALID -> false
                    }
                },
                onForgotPin = { navController.navigate(FORGOT_PIN) }
            )
        }

        composable(FORGOT_PIN) {
            val question by pinViewModel.securityQuestion.collectAsStateWithLifecycle()
            ForgotPinScreen(
                question = question ?: "No security question set up.",
                onAnswerSubmit = { answer -> pinViewModel.verifySecurityAnswer(answer) },
                onVerified = { navController.navigate(RESET_PIN) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(RESET_PIN) {
            CreatePinScreen { pin ->
                pinViewModel.startPinCreation(pin)
                navController.navigate(RESET_CONFIRM)
                true
            }
        }

        composable(RESET_CONFIRM) {
            ConfirmPinScreen { pin ->
                val success = pinViewModel.confirmPin(pin)
                if (success) goToJournal()
                success
            }
        }
    }
}