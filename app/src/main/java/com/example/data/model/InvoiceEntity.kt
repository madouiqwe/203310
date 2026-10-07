package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey
    val id: String,
    val invoiceNumber: String,
    val customer: String,
    val date: String,
    val itemsJson: String,
    val discount: Double = 0.0,
    val taxRate: Double = 0.0,
    val status: String = "unpaid", // "paid" or "unpaid"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
) {
    val isPaid: Boolean get() = status == "paid"
    val isDeleted: Boolean get() = deletedAt != null

    fun getItems(): List<InvoiceItem> = InvoiceJsonAdapter.fromJson(itemsJson)

    val subtotal: Double
        get() = getItems().fold(0.0) { acc, item -> acc + item.total }

    val taxAmount: Double
        get() = subtotal * (taxRate / 100.0)

    val total: Double
        get() = subtotal - discount + taxAmount
}

object InvoiceJsonAdapter {
    fun toJson(items: List<InvoiceItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("name", item.name)
            obj.put("qty", item.qty)
            obj.put("price", item.price)
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(json: String): List<InvoiceItem> {
        val list = mutableListOf<InvoiceItem>()
        if (json.isBlank()) return list
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    InvoiceItem(
                        name = obj.optString("name", ""),
                        qty = obj.optDouble("qty", 1.0),
                        price = obj.optDouble("price", 0.0)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
