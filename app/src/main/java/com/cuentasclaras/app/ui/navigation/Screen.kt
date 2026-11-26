package com.cuentasclaras.app.ui.navigation

sealed class Screen(val route: String) {
    data object Accounts : Screen("accounts")
    data object AccountDetail : Screen("account/{cuentaId}") {
        fun createRoute(cuentaId: Long) = "account/$cuentaId"
    }
    data object Invoices : Screen("invoices")
    data object InvoiceEditor : Screen("invoice/{facturaId}") {
        fun createRoute(facturaId: Long) = "invoice/$facturaId"
    }
    data object Settings : Screen("settings")
}
