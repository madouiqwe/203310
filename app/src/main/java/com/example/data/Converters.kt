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
            val effectivePackages = if (item.unitsPerPackage > 1.0) item.packagesCount else item.quantity
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("packageInfo", item.packageInfo)
                put("unitsPerPackage", item.unitsPerPackage)
                put("packagesCount", effectivePackages)
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
                val rawQty = obj.optDouble("quantity", 1.0)
                val units = obj.optDouble("unitsPerPackage", 1.0)
                val packs = obj.optDouble("packagesCount", if (units > 1.0) (rawQty / units) else rawQty)
                val finalQty = if (units > 1.0) (units * packs) else rawQty
                val finalPacks = if (units > 1.0) packs else rawQty

                val item = InvoiceItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    name = obj.optString("name", ""),
                    packageInfo = obj.optString("packageInfo", ""),
                    unitsPerPackage = units,
                    packagesCount = finalPacks,
                    quantity = finalQty,
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
