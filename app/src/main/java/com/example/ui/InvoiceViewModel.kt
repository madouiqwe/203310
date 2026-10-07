package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.InvoiceRepository
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import com.example.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class InvoiceStatistics(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val partialCount: Int = 0,
    val totalAmount: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalRemaining: Double = 0.0
)

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: InvoiceRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = InvoiceRepository(database.invoiceDao())
        viewModelScope.launch {
            repository.populateSampleDataIfEmpty()
        }
    }

    val allInvoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<InvoiceStatus?>(null)
    val selectedStatusFilter: StateFlow<InvoiceStatus?> = _selectedStatusFilter.asStateFlow()

    val filteredInvoices: StateFlow<List<Invoice>> = combine(
        allInvoices,
        _searchQuery,
        _selectedStatusFilter
    ) { invoices, query, filter ->
        invoices.filter { invoice ->
            val matchesQuery = query.isBlank() ||
                    invoice.customerName.contains(query, ignoreCase = true) ||
                    invoice.invoiceNumber.contains(query, ignoreCase = true) ||
                    invoice.items.any { it.name.contains(query, ignoreCase = true) }

            val matchesFilter = filter == null || invoice.status == filter

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val statistics: StateFlow<InvoiceStatistics> = allInvoices.combine(_searchQuery) { invoices, _ ->
        var totalAmount = 0.0
        var totalCollected = 0.0
        var totalRemaining = 0.0
        var completed = 0
        var pending = 0
        var partial = 0

        for (inv in invoices) {
            totalAmount += inv.grandTotal
            totalCollected += inv.paidAmount
            totalRemaining += inv.remainingBalance
            when (inv.status) {
                InvoiceStatus.COMPLETED -> completed++
                InvoiceStatus.PENDING -> pending++
                InvoiceStatus.PARTIALLY_PAID -> partial++
                InvoiceStatus.CANCELLED -> {}
            }
        }

        InvoiceStatistics(
            totalCount = invoices.size,
            completedCount = completed,
            pendingCount = pending,
            partialCount = partial,
            totalAmount = totalAmount,
            totalCollected = totalCollected,
            totalRemaining = totalRemaining
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InvoiceStatistics()
    )

    // Current invoice being edited / created
    private val _draftInvoice = MutableStateFlow<Invoice?>(null)
    val draftInvoice: StateFlow<Invoice?> = _draftInvoice.asStateFlow()

    // Flag indicating whether the invoice being edited was previously completed
    val isDraftCompletedInitially: Boolean
        get() = _initialCompletionState

    private var _initialCompletionState: Boolean = false

    // User message/toast
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: InvoiceStatus?) {
        _selectedStatusFilter.value = filter
    }

    fun startNewInvoice() {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyMMdd-HHmm", Locale.US)
        val defaultNumber = "INV-${dateFormat.format(Date(now))}"

        _initialCompletionState = false
        _draftInvoice.value = Invoice(
            id = 0L,
            invoiceNumber = defaultNumber,
            customerName = "",
            customerPhone = "",
            customerEmail = "",
            issueDate = now,
            dueDate = now + (7L * 24 * 60 * 60 * 1000),
            status = InvoiceStatus.PENDING,
            paymentMethod = PaymentMethod.CASH,
            items = listOf(
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "خدمة / منتج رئيسي",
                    quantity = 1.0,
                    unitPrice = 100.0,
                    description = ""
                )
            ),
            discountAmount = 0.0,
            isDiscountPercentage = false,
            taxPercentage = 15.0,
            paidAmount = 0.0,
            notes = "",
            terms = "شكراً لتعاملكم معنا. الدفع خلال مدة الاستحقاق المحددة.",
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            isEditedAfterCompletion = false
        )
    }

    fun startEditingInvoice(invoice: Invoice) {
        // Record whether it was already completed so we can show helpful indicators
        _initialCompletionState = invoice.status == InvoiceStatus.COMPLETED || invoice.completedAt != null
        _draftInvoice.value = invoice.copy()
    }

    fun cancelDraft() {
        _draftInvoice.value = null
        _initialCompletionState = false
    }

    fun updateDraftCustomer(name: String, phone: String, email: String) {
        _draftInvoice.value = _draftInvoice.value?.copy(
            customerName = name,
            customerPhone = phone,
            customerEmail = email
        )
    }

    fun updateDraftInvoiceNumber(number: String) {
        _draftInvoice.value = _draftInvoice.value?.copy(
            invoiceNumber = number
        )
    }

    fun updateDraftDates(issueDate: Long, dueDate: Long) {
        _draftInvoice.value = _draftInvoice.value?.copy(
            issueDate = issueDate,
            dueDate = dueDate
        )
    }

    fun updateDraftStatus(status: InvoiceStatus, paidAmount: Double? = null) {
        val current = _draftInvoice.value ?: return
        val newPaid = paidAmount ?: when (status) {
            InvoiceStatus.COMPLETED -> current.grandTotal
            InvoiceStatus.PENDING -> 0.0
            InvoiceStatus.CANCELLED -> 0.0
            InvoiceStatus.PARTIALLY_PAID -> if (current.paidAmount > 0) current.paidAmount else (current.grandTotal / 2.0)
        }
        _draftInvoice.value = current.copy(
            status = status,
            paidAmount = newPaid
        )
    }

    fun updateDraftPaidAmount(amount: Double) {
        val current = _draftInvoice.value ?: return
        val total = current.grandTotal
        val status = when {
            amount >= total && total > 0 -> InvoiceStatus.COMPLETED
            amount > 0 -> InvoiceStatus.PARTIALLY_PAID
            else -> InvoiceStatus.PENDING
        }
        _draftInvoice.value = current.copy(
            paidAmount = amount,
            status = status
        )
    }

    fun updateDraftPaymentMethod(method: PaymentMethod) {
        _draftInvoice.value = _draftInvoice.value?.copy(paymentMethod = method)
    }

    fun updateDraftDiscount(amount: Double, isPercentage: Boolean) {
        _draftInvoice.value = _draftInvoice.value?.copy(
            discountAmount = amount,
            isDiscountPercentage = isPercentage
        )
    }

    fun updateDraftTax(percentage: Double) {
        _draftInvoice.value = _draftInvoice.value?.copy(taxPercentage = percentage)
    }

    fun updateDraftNotes(notes: String, terms: String) {
        _draftInvoice.value = _draftInvoice.value?.copy(
            notes = notes,
            terms = terms
        )
    }

    fun addDraftItem(item: InvoiceItem) {
        val current = _draftInvoice.value ?: return
        _draftInvoice.value = current.copy(items = current.items + item)
    }

    fun updateDraftItem(item: InvoiceItem) {
        val current = _draftInvoice.value ?: return
        val updated = current.items.map { if (it.id == item.id) item else it }
        _draftInvoice.value = current.copy(items = updated)
    }

    fun removeDraftItem(itemId: String) {
        val current = _draftInvoice.value ?: return
        _draftInvoice.value = current.copy(items = current.items.filterNot { it.id == itemId })
    }

    fun saveDraft(onSaved: (Long) -> Unit) {
        val current = _draftInvoice.value ?: return
        if (current.customerName.isBlank()) {
            _userMessage.value = "يرجى كتابة اسم العميل"
            return
        }
        if (current.items.isEmpty()) {
            _userMessage.value = "يرجى إضافة بند واحد على الأقل في الفاتورة"
            return
        }

        viewModelScope.launch {
            val wasInitiallyCompleted = _initialCompletionState
            val savedId = repository.saveInvoice(current)
            _draftInvoice.value = null
            _initialCompletionState = false

            if (wasInitiallyCompleted) {
                _userMessage.value = "تم تعديل وحفظ الفاتورة المكتملة بنجاح ✅"
            } else {
                _userMessage.value = "تم حفظ الفاتورة بنجاح ✅"
            }
            onSaved(savedId)
        }
    }

    fun duplicateInvoice(id: Long) {
        viewModelScope.launch {
            val newId = repository.duplicateInvoice(id)
            if (newId != null) {
                _userMessage.value = "تم تكرار الفاتورة كنسخة جديدة بنجاح"
            }
        }
    }

    fun deleteInvoice(id: Long) {
        viewModelScope.launch {
            repository.deleteInvoice(id)
            _userMessage.value = "تم حذف الفاتورة"
        }
    }

    fun toggleCompleted(id: Long) {
        viewModelScope.launch {
            val success = repository.toggleCompletedStatus(id)
            if (success) {
                _userMessage.value = "تم تحديث حالة الفاتورة"
            }
        }
    }
}
