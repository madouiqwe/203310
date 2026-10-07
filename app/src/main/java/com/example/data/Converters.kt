package com.example.data

import androidx.room.TypeConverter
import com.example.model.InvoiceItem
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromInvoiceItemsList(items: List<InvoiceItem>?): String {
        if (items.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("quantity", item.quantity)
                put("unitPrice", item.unitPrice)
                put("description", item.description)
            }
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toInvoiceItemsList(data: String?): List<InvoiceItem> {
        if (data.isNullOrBlank()) return emptyList()
        val list = mutableListOf<InvoiceItem>()
        try {
            val array = JSONArray(data)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val item = InvoiceItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    name = obj.optString("name", ""),
                    quantity = obj.optDouble("quantity", 1.0),
                    unitPrice = obj.optDouble("unitPrice", 0.0),
                    description = obj.optString("description", "")
                )
                list.add(item)
            }
        } catch (_: Exception) {
            // Return empty list on parse failure
        }
        return list
    }
}
