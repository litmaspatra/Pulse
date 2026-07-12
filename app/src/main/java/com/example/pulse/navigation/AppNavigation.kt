// FILE: app/src/main/java/com/example/pulse/navigation/AppNavigation.kt
@file:Suppress("NewApi")
// The NewApi/Matcher#start(String) warning reported in this file comes from
// androidx.navigation's inline route-matching helpers (it compiles route
// templates like "journal_editor/{fileName}" into a regex with named groups).
// Because those helpers are `inline`, their bytecode is inlined directly into
// this file at the composable(...) call sites, so lint attributes the API 26
// requirement to us even though Navigation Compose already guards it
// internally and supports minSdk 21+. Suppressed at file level rather than
// raising minSdk, since that would drop Android 7.0/7.1 (API 24-25) support
// for no real runtime benefit.

package com.example.pulse.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import com.example.pulse.data.LockTimeout
import com.example.pulse.data.PinStorage
import com.example.pulse.screens.ArchiveScreen
import com.example.pulse.screens.CalendarScreen
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
import com.example.pulse.screens.TrashScreen
import com.example.pulse.viewmodel.ChatViewModel
import com.example.pulse.viewmodel.ChatViewModelFactory
import com.example.pulse.viewmodel.PinType
import com.example.pulse.viewmodel.PinViewModel
import com.example.pulse.viewmodel.PinViewModelFactory

private val lockExemptRoutes = setOf(
    Routes.SPLASH, Routes.PIN, Routes.CREATE_PIN, Routes.CONFIRM_PIN,
    Routes.FORGOT_PIN, Routes.RESET_PIN, Routes.RESET_CONFIRM_PIN,
    Routes.SECURITY_QUESTION_SETUP, Routes.CHANGE_PIN, Routes.CHANGE_PIN_CONFIRM
)

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val context = LocalContext.current

    val pinViewModel: PinViewModel = viewModel(factory = PinViewModelFactory(context))
    val pinStorage = remember { PinStorage(context) }
    val lockTimeoutName by pinStorage.lockTimeoutFlow.collectAsStateWithLifecycle(initialValue = null)

    fun openJournalLandingScreen() {
        navController.navigate(Routes.JOURNAL) { popUpTo(0) { inclusive = true } }
    }

    var shouldRelock by remember { mutableStateOf(false) }
    var backgroundedAtMillis by remember { mutableStateOf(0L) }

    // rememberUpdatedState so the DisposableEffect's lifecycle observer
    // (which is only ever created ONCE, since its key is Unit) still reads
    // the LATEST lock-timeout preference each time it fires, instead of
    // whatever value happened to exist the moment the effect was set up.
    val currentLockTimeoutName by rememberUpdatedState(lockTimeoutName)

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    if (currentRoute != null && currentRoute !in lockExemptRoutes) {
                        shouldRelock = true
                        backgroundedAtMillis = System.currentTimeMillis()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (shouldRelock) {
                        val timeout = LockTimeout.fromName(currentLockTimeoutName)
                        val elapsed = System.currentTimeMillis() - backgroundedAtMillis
                        if (elapsed >= timeout.millis) {
                            shouldRelock = false
                            navController.navigate(Routes.PIN) { popUpTo(0) { inclusive = true } }
                        } else {
                            shouldRelock = false
                        }
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

        composable(Routes.CHANGE_PIN) {
            CreatePinScreen { pin ->
                pinViewModel.startPinCreation(pin)
                navController.navigate(Routes.CHANGE_PIN_CONFIRM)
                true
            }
        }

        composable(Routes.CHANGE_PIN_CONFIRM) {
            ConfirmPinScreen { pin ->
                val success = pinViewModel.confirmPin(pin)
                if (success) {
                    navController.popBackStack(Routes.SETTINGS, inclusive = false)
                }
                success
            }
        }

        composable(Routes.JOURNAL) {
            JournalScreen(
                onEntryClick = { fileName -> navController.navigate(Routes.journalEditor(fileName)) },
                onNewEntry = { fileName -> navController.navigate(Routes.journalEditor(fileName)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                onOpenArchive = { navController.navigate(Routes.ARCHIVE) },
                onOpenTrash = { navController.navigate(Routes.TRASH) }
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

        composable(Routes.CALENDAR) {
            CalendarScreen(
                onBack = { navController.popBackStack() },
                onEntryClick = { fileName -> navController.navigate(Routes.journalEditor(fileName)) }
            )
        }

        composable(Routes.ARCHIVE) {
            ArchiveScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.TRASH) {
            TrashScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSetupSecondPin = { navController.navigate(Routes.CREATE_SECOND_PIN) },
                onChangePrimaryPin = { navController.navigate(Routes.CHANGE_PIN) }
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
            ChatScreen(
                onOpenSettings = { navController.navigate(Routes.CHAT_SETTINGS) },
                onBack = { navController.navigate(Routes.PIN) { popUpTo(0) { inclusive = true } } }
            )
        }

        composable(Routes.CHAT_SETTINGS) {
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModelFactory(PinStorage(context))
            )

            ChatSettingsScreen(
                onBack = { navController.popBackStack() },
                onDisconnect = {
                    chatViewModel.disconnect {
                        navController.navigate(Routes.PIN) { popUpTo(0) { inclusive = true } }
                    }
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