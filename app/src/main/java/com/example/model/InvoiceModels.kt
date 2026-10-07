package com.example.model

import java.util.UUID

enum class InvoiceStatus(val labelAr: String, val labelEn: String) {
    COMPLETED("مكتملة / مدفوعة", "Completed / Paid"),
    PENDING("قيد الانتظار", "Pending"),
    PARTIALLY_PAID("مدفوعة جزئياً", "Partially Paid"),
    CANCELLED("ملغاة", "Cancelled")
}

enum class PaymentMethod(val labelAr: String, val labelEn: String) {
    CASH("نقداً", "Cash"),
    BANK_TRANSFER("تحويل بنكي", "Bank Transfer"),
    CARD("بطاقة مدى / فيزا", "Card"),
    CHEQUE("شيك", "Cheque")
}

data class InvoiceItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val description: String = ""
) {
    val total: Double
        get() = quantity * unitPrice
}

data class Invoice(
    val id: Long = 0L,
    val invoiceNumber: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val issueDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000), // Default 7 days
    val status: InvoiceStatus = InvoiceStatus.PENDING,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val items: List<InvoiceItem> = emptyList(),
    val discountAmount: Double = 0.0,
    val isDiscountPercentage: Boolean = false,
    val taxPercentage: Double = 15.0, // Default 15% VAT standard
    val paidAmount: Double = 0.0,
    val notes: String = "",
    val terms: String = "شكراً لتعاملكم معنا.",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isEditedAfterCompletion: Boolean = false
) {
    val subtotal: Double
        get() = items.sumOf { it.total }

    val calculatedDiscount: Double
        get() = if (isDiscountPercentage) {
            (subtotal * (discountAmount / 100.0)).coerceAtLeast(0.0)
        } else {
            discountAmount.coerceAtLeast(0.0)
        }

    val amountAfterDiscount: Double
        get() = (subtotal - calculatedDiscount).coerceAtLeast(0.0)

    val calculatedTax: Double
        get() = if (taxPercentage > 0) {
            amountAfterDiscount * (taxPercentage / 100.0)
        } else 0.0

    val grandTotal: Double
        get() = (amountAfterDiscount + calculatedTax).coerceAtLeast(0.0)

    val remainingBalance: Double
        get() = (grandTotal - paidAmount).coerceAtLeast(0.0)
}
