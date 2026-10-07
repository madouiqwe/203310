package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CompanySettings
import com.example.data.model.InvoiceEntity
import com.example.data.model.ProductItem
import com.example.data.repository.InvoiceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InvoiceRepository(application)

    val allProducts: StateFlow<List<ProductItem>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeInvoices: StateFlow<List<InvoiceEntity>> = repository.activeInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashInvoices: StateFlow<List<InvoiceEntity>> = repository.trashInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<CompanySettings> = repository.settings

    fun addProduct(name: String, price: Double, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.addProduct(name, price)
            onComplete?.invoke(id)
        }
    }

    fun updateProduct(product: ProductItem) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(product: ProductItem) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun saveInvoice(invoice: InvoiceEntity, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveInvoice(invoice)
            onComplete?.invoke()
        }
    }

    fun updateInvoice(invoice: InvoiceEntity, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateInvoice(invoice)
            onComplete?.invoke()
        }
    }

    fun toggleInvoicePaidStatus(invoice: InvoiceEntity) {
        viewModelScope.launch {
            val updated = invoice.copy(status = if (invoice.isPaid) "unpaid" else "paid")
            repository.updateInvoice(updated)
        }
    }

    fun softDeleteInvoice(id: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.softDeleteInvoice(id)
            onComplete?.invoke()
        }
    }

    fun restoreInvoice(id: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.restoreInvoice(id)
            onComplete?.invoke()
        }
    }

    fun deletePermanently(id: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteInvoicePermanently(id)
            onComplete?.invoke()
        }
    }

    fun emptyTrash(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.emptyTrash()
            onComplete?.invoke()
        }
    }

    fun updateSettings(newSettings: CompanySettings) {
        repository.updateSettings(newSettings)
    }

    suspend fun exportJson(): String {
        return repository.exportJson()
    }

    suspend fun importJson(jsonStr: String): Int {
        return repository.importJson(jsonStr)
    }

    fun clearAllData(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.clearAllData()
            onComplete?.invoke()
        }
    }
}
