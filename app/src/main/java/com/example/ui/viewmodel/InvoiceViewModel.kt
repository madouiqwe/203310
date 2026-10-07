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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SalesStatistics(
    val totalRevenue: Double = 0.0,
    val totalInvoicesCount: Int = 0,
    val paidCount: Int = 0,
    val unpaidCount: Int = 0,
    val totalUnpaidAmount: Double = 0.0
)

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InvoiceRepository(application)

    val allProducts: StateFlow<List<ProductItem>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeInvoices: StateFlow<List<InvoiceEntity>> = repository.activeInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashInvoices: StateFlow<List<InvoiceEntity>> = repository.trashInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<CompanySettings> = repository.settings

    val salesStats: StateFlow<SalesStatistics> = repository.activeInvoices.map { list ->
        val totalRev = list.filter { it.isPaid }.fold(0.0) { sum, i -> sum + i.total }
        val unpaidRev = list.filter { !it.isPaid }.fold(0.0) { sum, i -> sum + i.total }
        val paid = list.count { it.isPaid }
        val unpaid = list.count { !it.isPaid }
        SalesStatistics(
            totalRevenue = totalRev,
            totalInvoicesCount = list.size,
            paidCount = paid,
            unpaidCount = unpaid,
            totalUnpaidAmount = unpaidRev
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalesStatistics())

    fun addProduct(
        name: String,
        price: Double,
        category: String = "عام",
        barcode: String = "",
        onComplete: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val id = repository.addProduct(name, price, category, barcode)
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

    fun addPresetsPack(type: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val presets = when (type) {
                "drinks" -> listOf(
                    ProductItem(name = "عصير برتقال طبيعي (1 لتر)", defaultPrice = 280.0, category = "مشروبات"),
                    ProductItem(name = "مشروب غازي كولا (330 مل)", defaultPrice = 90.0, category = "مشروبات"),
                    ProductItem(name = "مشروب طاقة (250 مل)", defaultPrice = 180.0, category = "مشروبات"),
                    ProductItem(name = "شاي أخضر صيني فاخر", defaultPrice = 320.0, category = "مشروبات"),
                    ProductItem(name = "نسكافيه جولد (100 غ)", defaultPrice = 750.0, category = "مشروبات")
                )
                "sweets" -> listOf(
                    ProductItem(name = "شوكولاتة بالحليب (100 غ)", defaultPrice = 220.0, category = "حلويات"),
                    ProductItem(name = "بسكويت بالشوكولاتة", defaultPrice = 140.0, category = "حلويات"),
                    ProductItem(name = "كيك إسفنجي بالفانيلا", defaultPrice = 160.0, category = "حلويات"),
                    ProductItem(name = "حلوى جيلي الفواكه", defaultPrice = 90.0, category = "حلويات")
                )
                else -> listOf(
                    ProductItem(name = "معكرونة إيطالية (500 غ)", defaultPrice = 95.0, category = "مواد غذائية"),
                    ProductItem(name = "طماطم مصبرة (400 غ)", defaultPrice = 120.0, category = "معلبات"),
                    ProductItem(name = "تونة بالزيت النباتي (160 غ)", defaultPrice = 210.0, category = "معلبات"),
                    ProductItem(name = "ملح طعام باليود (1 كغ)", defaultPrice = 40.0, category = "توابل")
                )
            }
            repository.insertPresetProducts(presets)
            onComplete?.invoke()
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
