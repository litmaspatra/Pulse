package com.example.pulse.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pulse.data.JournalRepository
import com.example.pulse.data.PinStorage
import com.example.pulse.screens.ChatScreen
import com.example.pulse.screens.ChatSettingsScreen
import com.example.pulse.screens.ConfirmPinScreen
import com.example.pulse.screens.CreatePinScreen
import com.example.pulse.screens.ForgotPinScreen
import com.example.pulse.screens.JournalEditorScreen
import com.example.pulse.screens.JournalScreen
import com.example.pulse.screens.LoginScreen
import com.example.pulse.screens.SecurityQuestionSetupScreen
import com.example.pulse.screens.SettingsScreen
import com.example.pulse.screens.SplashScreen
import com.example.pulse.screens.TrackerScreen
import com.example.pulse.viewmodel.ChatViewModel
import com.example.pulse.viewmodel.ChatViewModelFactory
import com.example.pulse.viewmodel.PinType
import com.example.pulse.viewmodel.PinViewModel
import com.example.pulse.viewmodel.PinViewModelFactory

private val lockExemptRoutes = setOf(
    Routes.SPLASH, Routes.PIN, Routes.CREATE_PIN, Routes.CONFIRM_PIN,
    Routes.FORGOT_PIN, Routes.RESET_PIN, Routes.RESET_CONFIRM_PIN,
    Routes.SECURITY_QUESTION_SETUP
)

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val context = LocalContext.current

    val pinViewModel: PinViewModel = viewModel(factory = PinViewModelFactory(context))
    val journalRepository = remember { JournalRepository(context) }

    fun openJournalLandingScreen() {
        navController.navigate(Routes.JOURNAL) { popUpTo(0) { inclusive = true } }
        navController.navigate(Routes.journalEditor(journalRepository.todayFileName()))
    }

    var shouldRelock by remember { mutableStateOf(false) }

    // Forces the PIN screen back up whenever the app is backgrounded (not just killed).
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    if (currentRoute != null && currentRoute !in lockExemptRoutes) {
                        shouldRelock = true
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (shouldRelock) {
                        shouldRelock = false
                        navController.navigate(Routes.PIN) { popUpTo(0) { inclusive = true } }
                    }
                }
                else -> {}
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = { fadeIn(tween(120)) },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(120)) },
        popExitTransition = { fadeOut(tween(120)) }
    ) {

        composable(Routes.SPLASH) {
            val primaryPin by pinViewModel.primaryPin.collectAsStateWithLifecycle()

            SplashScreen(
                onSplashComplete = {
                    val destination = if (!primaryPin.isNullOrEmpty()) Routes.PIN else Routes.CREATE_PIN
                    navController.navigate(destination) { popUpTo(Routes.SPLASH) { inclusive = true } }
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
                    navController.navigate(Routes.SECURITY_QUESTION_SETUP) {
                        popUpTo(Routes.CREATE_PIN) { inclusive = true }
                    }
                }
                success
            }
        }

        composable(Routes.SECURITY_QUESTION_SETUP) {
            SecurityQuestionSetupScreen(
                onCompleted = { question, answer ->
                    pinViewModel.saveSecurityQuestion(question, answer)
                    openJournalLandingScreen()
                }
            )
        }

        composable(Routes.PIN) {
            LoginScreen(
                onPinEntered = { enteredPin ->
                    when (pinViewModel.validatePin(enteredPin)) {
                        PinType.PRIMARY -> { openJournalLandingScreen(); true }
                        PinType.SECOND -> {
                            navController.navigate(Routes.CHAT) { popUpTo(Routes.PIN) { inclusive = true } }
                            true
                        }
                        PinType.INVALID -> false
                    }
                },
                onForgotPin = { navController.navigate(Routes.FORGOT_PIN) }
            )
        }

        composable(Routes.FORGOT_PIN) {
            val question by pinViewModel.securityQuestion.collectAsStateWithLifecycle()
            ForgotPinScreen(
                question = question ?: "No security question set up.",
                onAnswerSubmit = { answer -> pinViewModel.verifySecurityAnswer(answer) },
                onVerified = { navController.navigate(Routes.RESET_PIN) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RESET_PIN) {
            CreatePinScreen { pin ->
                pinViewModel.startPinCreation(pin)
                navController.navigate(Routes.RESET_CONFIRM_PIN)
                true
            }
        }

        composable(Routes.RESET_CONFIRM_PIN) {
            ConfirmPinScreen { pin ->
                val success = pinViewModel.confirmPin(pin)
                if (success) openJournalLandingScreen()
                success
            }
        }

        composable(Routes.JOURNAL) {
            JournalScreen(
                onEntryClick = { fileName -> navController.navigate(Routes.journalEditor(fileName)) },
                onNewEntry = { fileName -> navController.navigate(Routes.journalEditor(fileName)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.JOURNAL_EDITOR,
            arguments = listOf(navArgument("fileName") { type = NavType.StringType })
        ) { backStackEntry ->
            val fileName = backStackEntry.arguments?.getString("fileName") ?: return@composable
            JournalEditorScreen(
                fileName = fileName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSetupSecondPin = { navController.navigate(Routes.CREATE_SECOND_PIN) }
            )
        }

        composable(Routes.CREATE_SECOND_PIN) {
            CreatePinScreen { pin ->
                pinViewModel.startSecondPinCreation(pin)
                navController.navigate(Routes.CONFIRM_SECOND_PIN)
                true
            }
        }

        composable(Routes.CONFIRM_SECOND_PIN) {
            ConfirmPinScreen { pin ->
                val success = pinViewModel.confirmSecondPin(pin)
                if (success) {
                    navController.popBackStack(Routes.CREATE_SECOND_PIN, inclusive = true)
                }
                success
            }
        }

        composable(Routes.CHAT) {
            ChatScreen(onOpenSettings = { navController.navigate(Routes.CHAT_SETTINGS) })
        }

        composable(Routes.CHAT_SETTINGS) {
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModelFactory(PinStorage(context))
            )

            ChatSettingsScreen(
                onBack = { navController.popBackStack() },
                onDisconnect = {
                    chatViewModel.disconnect()
                    navController.navigate(Routes.PIN) { popUpTo(0) { inclusive = true } }
                },
                onVerifySecondPin = { code -> pinViewModel.validateSecondPinEntry(code) },
                onDisableChat = {
                    pinViewModel.clearSecondPin()
                    navController.navigate(Routes.PIN) { popUpTo(0) { inclusive = true } }
                },
                onChangePin = { navController.navigate(Routes.CREATE_SECOND_PIN) }
            )
        }

        composable(Routes.TRACKER) {
            TrackerScreen()
        }
    }
}