package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Invoice
import com.example.ui.InvoiceViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InvoiceDetailScreen
import com.example.ui.screens.InvoiceEditScreen
import com.example.ui.theme.MyApplicationTheme

sealed interface Screen {
    data object Home : Screen
    data class Detail(val invoiceId: Long) : Screen
    data object Edit : Screen
}

class MainActivity : ComponentActivity() {

    private val viewModel: InvoiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InvoiceApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun InvoiceApp(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()

    when (val screen = currentScreen) {
        is Screen.Home -> {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = { id ->
                    currentScreen = Screen.Detail(id)
                },
                onNavigateToEdit = { invoice ->
                    viewModel.startEditingInvoice(invoice)
                    currentScreen = Screen.Edit
                },
                modifier = modifier
            )
        }

        is Screen.Detail -> {
            val invoice = allInvoices.find { it.id == screen.invoiceId }
            if (invoice != null) {
                InvoiceDetailScreen(
                    invoice = invoice,
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.Home },
                    onEdit = { inv ->
                        viewModel.startEditingInvoice(inv)
                        currentScreen = Screen.Edit
                    },
                    modifier = modifier
                )
            } else {
                currentScreen = Screen.Home
            }
        }

        is Screen.Edit -> {
            InvoiceEditScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = Screen.Home },
                onSaveSuccess = { savedId ->
                    currentScreen = Screen.Detail(savedId)
                },
                modifier = modifier
            )
        }
    }
}
