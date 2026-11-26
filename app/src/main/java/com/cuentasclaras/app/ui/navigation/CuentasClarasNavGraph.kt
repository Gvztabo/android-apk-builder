package com.cuentasclaras.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cuentasclaras.app.data.repository.FinanceRepository
import com.cuentasclaras.app.ui.screens.AccountDetailScreen
import com.cuentasclaras.app.ui.screens.AccountsScreen
import com.cuentasclaras.app.ui.screens.InvoiceEditorScreen
import com.cuentasclaras.app.ui.screens.InvoicesScreen
import com.cuentasclaras.app.ui.screens.SettingsScreen
import com.cuentasclaras.app.ui.viewmodel.AccountDetailViewModel
import com.cuentasclaras.app.ui.viewmodel.AccountsViewModel
import com.cuentasclaras.app.ui.viewmodel.InvoiceEditorViewModel
import com.cuentasclaras.app.ui.viewmodel.InvoicesViewModel
import com.cuentasclaras.app.ui.viewmodel.SettingsViewModel

@Composable
fun CuentasClarasNavGraph(repository: FinanceRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val accountsViewModel: AccountsViewModel = viewModel(
        factory = AccountsViewModel.Factory(repository)
    )

    LaunchedEffect(accountsViewModel) {
        accountsViewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val showBottomBar =
        currentRoute == Screen.Accounts.route ||
            currentRoute == Screen.Invoices.route ||
            currentRoute == Screen.Settings.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        val navColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        NavigationBarItem(
                            selected = currentRoute == Screen.Accounts.route,
                            onClick = {
                                navController.navigate(Screen.Accounts.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = navColors,
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = "Cuentas"
                                )
                            },
                            label = {
                                Text(
                                    text = "Cuentas",
                                    fontWeight = if (currentRoute == Screen.Accounts.route) FontWeight.SemiBold else FontWeight.Medium
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Screen.Invoices.route,
                            onClick = {
                                navController.navigate(Screen.Invoices.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = navColors,
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.ReceiptLong,
                                    contentDescription = "Facturas"
                                )
                            },
                            label = {
                                Text(
                                    text = "Facturas",
                                    fontWeight = if (currentRoute == Screen.Invoices.route) FontWeight.SemiBold else FontWeight.Medium
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Screen.Settings.route,
                            onClick = {
                                navController.navigate(Screen.Settings.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = navColors,
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Configuración"
                                )
                            },
                            label = {
                                Text(
                                    text = "Ajustes",
                                    fontWeight = if (currentRoute == Screen.Settings.route) FontWeight.SemiBold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Accounts.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Accounts.route) {
                AccountsScreen(
                    viewModel = accountsViewModel,
                    onAccountClick = { cuentaId ->
                        navController.navigate(Screen.AccountDetail.createRoute(cuentaId))
                    }
                )
            }

            composable(
                route = Screen.AccountDetail.route,
                arguments = listOf(navArgument("cuentaId") { type = NavType.LongType })
            ) { backStackEntry ->
                val cuentaId = backStackEntry.arguments?.getLong("cuentaId") ?: return@composable
                val detailViewModel: AccountDetailViewModel = viewModel(
                    factory = AccountDetailViewModel.Factory(cuentaId, repository)
                )
                AccountDetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                    onInvoiceClick = { facturaId ->
                        navController.navigate(Screen.InvoiceEditor.createRoute(facturaId))
                    }
                )
            }

            composable(Screen.Invoices.route) {
                val invoicesViewModel: InvoicesViewModel = viewModel(
                    factory = InvoicesViewModel.Factory(repository)
                )
                InvoicesScreen(
                    viewModel = invoicesViewModel,
                    snackbarHostState = snackbarHostState,
                    onInvoiceClick = { facturaId ->
                        navController.navigate(Screen.InvoiceEditor.createRoute(facturaId))
                    }
                )
            }

            composable(
                route = Screen.InvoiceEditor.route,
                arguments = listOf(navArgument("facturaId") { type = NavType.LongType })
            ) { backStackEntry ->
                val facturaId = backStackEntry.arguments?.getLong("facturaId") ?: return@composable
                val editorViewModel: InvoiceEditorViewModel = viewModel(
                    factory = InvoiceEditorViewModel.Factory(facturaId, repository)
                )
                InvoiceEditorScreen(
                    viewModel = editorViewModel,
                    snackbarHostState = snackbarHostState,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(repository)
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}
