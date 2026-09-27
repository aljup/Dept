package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.notification.DebtReminderScheduler
import com.example.notification.NotificationHelper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Debt
import com.example.data.model.Person
import com.example.ui.screens.admin.AdminPanelScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.PinUnlockScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.auth.WelcomeScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.debt.AddEditDebtScreen
import com.example.ui.screens.debt.DebtDetailsScreen
import com.example.ui.screens.persons.PersonDetailsScreen
import com.example.ui.theme.DuyuniTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.DebtViewModel
import com.example.viewmodel.PersonViewModel

sealed interface Screen {
    data object Welcome : Screen
    data object Login : Screen
    data object Register : Screen
    data object PinUnlock : Screen
    data object Dashboard : Screen
    data object AdminPanel : Screen
    data object AddDebt : Screen
    data class EditDebt(val debt: Debt) : Screen
    data class DebtDetails(val debtId: Long) : Screen
    data class PersonDetails(val person: Person) : Screen
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val extraDebtId = intent?.getLongExtra("EXTRA_DEBT_ID", -1L)?.takeIf { it != -1L }

        NotificationHelper.createNotificationChannel(this)
        DebtReminderScheduler.scheduleDailyPeriodicCheck(this)
        lifecycleScope.launch(Dispatchers.IO) {
            DebtReminderScheduler.rescheduleAllActiveDebts(this@MainActivity)
        }

        setContent {
            DuyuniTheme {
                // Ensure RTL layout support for optimal Arabic reading flow
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    DebtManagerApp(initialDebtId = extraDebtId)
                }
            }
        }
    }
}

@Composable
fun DebtManagerApp(
    initialDebtId: Long? = null,
    authViewModel: AuthViewModel = viewModel(),
    debtViewModel: DebtViewModel = viewModel(),
    personViewModel: PersonViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Welcome) }

    // Navigate to debt details if app opened via notification deep link
    LaunchedEffect(authState.isLoggedIn, authState.requiresPinUnlock, initialDebtId) {
        if (authState.isLoggedIn && !authState.requiresPinUnlock && initialDebtId != null) {
            currentScreen = Screen.DebtDetails(initialDebtId)
        }
    }

    // Synchronize initial auth state with navigation destination
    LaunchedEffect(authState.isCheckingSession, authState.isLoggedIn, authState.requiresPinUnlock) {
        if (!authState.isCheckingSession) {
            when {
                authState.requiresPinUnlock -> currentScreen = Screen.PinUnlock
                authState.isLoggedIn -> currentScreen = Screen.Dashboard
                else -> {
                    // Only reset to Welcome if not already in Login/Register
                    if (currentScreen != Screen.Login && currentScreen != Screen.Register) {
                        currentScreen = Screen.Welcome
                    }
                }
            }
        }
    }

    // Sync current user ID with PersonViewModel
    LaunchedEffect(authState.currentUser?.id) {
        authState.currentUser?.id?.let { userId ->
            personViewModel.refreshUser(userId)
        }
    }

    // Collect snackbar events from DebtViewModel
    LaunchedEffect(debtViewModel) {
        debtViewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Collect snackbar events from PersonViewModel
    LaunchedEffect(personViewModel) {
        personViewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Collect snackbar events from AdminViewModel
    LaunchedEffect(adminViewModel) {
        adminViewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Collect success/error messages from AuthViewModel
    LaunchedEffect(authState.successMessage, authState.errorMessage) {
        authState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
        authState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearMessages()
        }
    }

    // Loading Splash while checking session
    if (authState.isCheckingSession) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Screen Switching with Animated Transitions
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            (slideInHorizontally { width -> width / 3 } + fadeIn())
                .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut())
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            Screen.Welcome -> {
                WelcomeScreen(
                    onNavigateToLogin = { currentScreen = Screen.Login },
                    onNavigateToRegister = { currentScreen = Screen.Register },
                    onQuickAdminLogin = {
                        authViewModel.quickLoginAsAdmin {
                            currentScreen = Screen.Dashboard
                        }
                    }
                )
            }

            Screen.Login -> {
                BackHandler { currentScreen = Screen.Welcome }
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateBack = { currentScreen = Screen.Welcome },
                    onNavigateToRegister = { currentScreen = Screen.Register },
                    onLoginSuccess = { currentScreen = Screen.Dashboard }
                )
            }

            Screen.Register -> {
                BackHandler { currentScreen = Screen.Welcome }
                RegisterScreen(
                    viewModel = authViewModel,
                    onNavigateBack = { currentScreen = Screen.Welcome },
                    onNavigateToLogin = { currentScreen = Screen.Login },
                    onRegisterSuccess = { currentScreen = Screen.Dashboard }
                )
            }

            Screen.PinUnlock -> {
                PinUnlockScreen(
                    viewModel = authViewModel,
                    onPinSuccess = { currentScreen = Screen.Dashboard },
                    onUsePasswordInstead = {
                        authViewModel.logout {
                            currentScreen = Screen.Login
                        }
                    }
                )
            }

            Screen.Dashboard -> {
                DashboardScreen(
                    user = authState.currentUser,
                    debtViewModel = debtViewModel,
                    personViewModel = personViewModel,
                    onAddDebtClick = {
                        debtViewModel.initFormForAdd()
                        currentScreen = Screen.AddDebt
                    },
                    onDebtClick = { debtId ->
                        currentScreen = Screen.DebtDetails(debtId)
                    },
                    onViewPersonStatement = { person ->
                        currentScreen = Screen.PersonDetails(person)
                    },
                    onOpenAdmin = {
                        currentScreen = Screen.AdminPanel
                    },
                    onLockApp = {
                        authViewModel.lockApp()
                        currentScreen = Screen.PinUnlock
                    },
                    onLogout = {
                        authViewModel.logout {
                            currentScreen = Screen.Welcome
                        }
                    },
                    snackbarHostState = snackbarHostState
                )
            }

            Screen.AdminPanel -> {
                BackHandler { currentScreen = Screen.Dashboard }
                AdminPanelScreen(
                    adminViewModel = adminViewModel,
                    currentUser = authState.currentUser,
                    onNavigateBack = { currentScreen = Screen.Dashboard },
                    onEditDebt = { debtToEdit ->
                        debtViewModel.initFormForEdit(debtToEdit)
                        currentScreen = Screen.EditDebt(debtToEdit)
                    }
                )
            }

            Screen.AddDebt -> {
                BackHandler { currentScreen = Screen.Dashboard }
                AddEditDebtScreen(
                    debtViewModel = debtViewModel,
                    onNavigateBack = { currentScreen = Screen.Dashboard },
                    currency = authState.currentUser?.currency ?: debtViewModel.getCurrency()
                )
            }

            is Screen.EditDebt -> {
                BackHandler { currentScreen = Screen.DebtDetails(screen.debt.id) }
                AddEditDebtScreen(
                    debtViewModel = debtViewModel,
                    onNavigateBack = { currentScreen = Screen.DebtDetails(screen.debt.id) },
                    currency = authState.currentUser?.currency ?: debtViewModel.getCurrency()
                )
            }

            is Screen.DebtDetails -> {
                BackHandler { currentScreen = Screen.Dashboard }
                DebtDetailsScreen(
                    debtId = screen.debtId,
                    debtViewModel = debtViewModel,
                    onNavigateBack = { currentScreen = Screen.Dashboard },
                    onNavigateToEdit = { debtToEdit ->
                        debtViewModel.initFormForEdit(debtToEdit)
                        currentScreen = Screen.EditDebt(debtToEdit)
                    },
                    currency = authState.currentUser?.currency ?: debtViewModel.getCurrency()
                )
            }

            is Screen.PersonDetails -> {
                BackHandler { currentScreen = Screen.Dashboard }
                PersonDetailsScreen(
                    person = screen.person,
                    personViewModel = personViewModel,
                    currency = authState.currentUser?.currency ?: debtViewModel.getCurrency(),
                    onNavigateBack = { currentScreen = Screen.Dashboard },
                    onDebtClick = { debtId -> currentScreen = Screen.DebtDetails(debtId) },
                    onAddDebtForPerson = { personName ->
                        debtViewModel.initFormForAdd(initialName = personName)
                        currentScreen = Screen.AddDebt
                    }
                )
            }
        }
    }
}
