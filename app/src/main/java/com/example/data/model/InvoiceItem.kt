package com.example.data.model

data class InvoiceItem(
    val name: String,
    val qty: Double = 1.0,
    val price: Double = 0.0
) {
    val total: Double get() = qty * price
}
