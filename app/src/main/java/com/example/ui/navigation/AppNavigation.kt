package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItem
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InvoiceDetailScreen
import com.example.ui.screens.InvoiceScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.QuickCalcScreen
import com.example.ui.screens.SavedInvoicesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.InvoiceViewModel

object AppRoutes {
    const val HOME = "home"
    const val PRODUCTS = "products"
    const val INVOICE = "invoice"
    const val QUICK_CALC = "quick_calc"
    const val SAVED_INVOICES = "saved_invoices"
    const val INVOICE_DETAIL = "invoice_detail"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(viewModel: InvoiceViewModel) {
    val navController = rememberNavController()

    var transferredItems by remember { mutableStateOf<List<InvoiceItem>?>(null) }
    var selectedInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME
    ) {
        composable(AppRoutes.HOME) {
            HomeScreen(
                onNavigateToQuickCalc = { navController.navigate(AppRoutes.QUICK_CALC) },
                onNavigateToInvoice = {
                    transferredItems = null
                    navController.navigate(AppRoutes.INVOICE)
                },
                onNavigateToSavedInvoices = { navController.navigate(AppRoutes.SAVED_INVOICES) },
                onNavigateToProducts = { navController.navigate(AppRoutes.PRODUCTS) },
                onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) }
            )
        }

        composable(AppRoutes.PRODUCTS) {
            ProductsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.INVOICE) {
            InvoiceScreen(
                viewModel = viewModel,
                initialItems = transferredItems,
                onNavigateBack = {
                    transferredItems = null
                    navController.popBackStack()
                },
                onNavigateToSaved = { navController.navigate(AppRoutes.SAVED_INVOICES) }
            )
        }

        composable(AppRoutes.QUICK_CALC) {
            QuickCalcScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFullInvoice = { items ->
                    transferredItems = items
                    navController.navigate(AppRoutes.INVOICE)
                }
            )
        }

        composable(AppRoutes.SAVED_INVOICES) {
            SavedInvoicesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSelectInvoice = { inv ->
                    selectedInvoice = inv
                    navController.navigate(AppRoutes.INVOICE_DETAIL)
                }
            )
        }

        composable(AppRoutes.INVOICE_DETAIL) {
            selectedInvoice?.let { inv ->
                InvoiceDetailScreen(
                    invoice = inv,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } ?: run {
                navController.popBackStack()
            }
        }

        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
