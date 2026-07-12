package com.example.pulse.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pulse.screens.ArchiveScreen
import com.example.pulse.screens.CalendarScreen
import com.example.pulse.screens.JournalEditorScreen
import com.example.pulse.screens.JournalScreen
import com.example.pulse.screens.SettingsAppearanceScreen
import com.example.pulse.screens.SettingsBackupScreen
import com.example.pulse.screens.SecurityScreen
import com.example.pulse.screens.SettingsScreen
import com.example.pulse.screens.TrackerScreen
import com.example.pulse.screens.TrashScreen
import com.example.pulse.viewmodel.PinViewModel
import com.example.pulse.viewmodel.PinViewModelFactory

@Composable
fun MainNavigation(onEnterChat: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val pinViewModel: PinViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = PinViewModelFactory(context)
    )

    NavHost(
        navController = navController,
        startDestination = Routes.JOURNAL,
        enterTransition = { fadeIn(tween(120)) },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(120)) },
        popExitTransition = { fadeOut(tween(120)) }
    ) {
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

        composable(Routes.TRACKER) {
            TrackerScreen()
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAppearance = { navController.navigate(Routes.SETTINGS_APPEARANCE) },
                onOpenSecurity = { navController.navigate(Routes.SETTINGS_SECURITY) },
                onOpenBackup = { navController.navigate(Routes.SETTINGS_BACKUP) },
                onEnterChat = onEnterChat,
                onSetupSecondPin = { navController.navigate(Routes.CREATE_SECOND_PIN) },
                onChangePrimaryPin = { navController.navigate(Routes.CHANGE_PIN) }
            )
        }

        composable(Routes.SETTINGS_APPEARANCE) {
            SettingsAppearanceScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS_SECURITY) {
            SecurityScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS_BACKUP) {
            SettingsBackupScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CHANGE_PIN) {
            com.example.pulse.screens.CreatePinScreen { pin ->
                pinViewModel.startPinCreation(pin)
                navController.navigate(Routes.CHANGE_PIN_CONFIRM)
                true
            }
        }

        composable(Routes.CHANGE_PIN_CONFIRM) {
            com.example.pulse.screens.ConfirmPinScreen { pin ->
                val success = pinViewModel.confirmPin(pin)
                if (success) navController.popBackStack(Routes.SETTINGS, inclusive = false)
                success
            }
        }

        composable(Routes.CREATE_SECOND_PIN) {
            com.example.pulse.screens.CreatePinScreen { pin ->
                pinViewModel.startSecondPinCreation(pin)
                navController.navigate(Routes.CONFIRM_SECOND_PIN)
                true
            }
        }

        composable(Routes.CONFIRM_SECOND_PIN) {
            com.example.pulse.screens.ConfirmPinScreen { pin ->
                val success = pinViewModel.confirmSecondPin(pin)
                if (success) navController.popBackStack(Routes.SETTINGS, inclusive = false)
                success
            }
        }
    }
}