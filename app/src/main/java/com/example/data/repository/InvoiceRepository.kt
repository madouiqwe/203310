package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.AppDatabase
import com.example.data.model.CompanySettings
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceJsonAdapter
import com.example.data.model.ProductItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class InvoiceRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val productDao = db.productDao()
    private val invoiceDao = db.invoiceDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("quick_invoice_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings = _settings.asStateFlow()

    // Products
    val allProducts: Flow<List<ProductItem>> = productDao.getAllProducts()

    fun searchProducts(query: String): Flow<List<ProductItem>> = productDao.searchProducts(query)

    suspend fun addProduct(name: String, price: Double): Long = withContext(Dispatchers.IO) {
        val existing = productDao.getProductByName(name.trim())
        if (existing != null) {
            val updated = existing.copy(name = name.trim(), defaultPrice = price)
            productDao.updateProduct(updated)
            existing.id
        } else {
            productDao.insertProduct(ProductItem(name = name.trim(), defaultPrice = price))
        }
    }

    suspend fun updateProduct(product: ProductItem) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductItem) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun deleteProductById(id: Long) = withContext(Dispatchers.IO) {
        productDao.deleteById(id)
    }

    // Invoices
    val activeInvoices: Flow<List<InvoiceEntity>> = invoiceDao.getActiveInvoices()
    val trashInvoices: Flow<List<InvoiceEntity>> = invoiceDao.getTrashInvoices()

    suspend fun getInvoiceById(id: String): InvoiceEntity? = withContext(Dispatchers.IO) {
        invoiceDao.getInvoiceById(id)
    }

    suspend fun saveInvoice(invoice: InvoiceEntity) = withContext(Dispatchers.IO) {
        invoiceDao.insertInvoice(invoice)
    }

    suspend fun updateInvoice(invoice: InvoiceEntity) = withContext(Dispatchers.IO) {
        invoiceDao.updateInvoice(invoice)
    }

    suspend fun softDeleteInvoice(id: String) = withContext(Dispatchers.IO) {
        invoiceDao.softDelete(id, System.currentTimeMillis())
    }

    suspend fun restoreInvoice(id: String) = withContext(Dispatchers.IO) {
        invoiceDao.restore(id)
    }

    suspend fun deleteInvoicePermanently(id: String) = withContext(Dispatchers.IO) {
        invoiceDao.deletePermanently(id)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        invoiceDao.emptyTrash()
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        invoiceDao.deleteAll()
    }

    // Settings
    private fun loadSettings(): CompanySettings {
        return CompanySettings(
            companyName = prefs.getString("company_name", "اسم شركتك") ?: "اسم شركتك",
            companyPhone = prefs.getString("company_phone", "0555 55 55 55") ?: "0555 55 55 55",
            companyAddress = prefs.getString("company_address", "العنوان") ?: "العنوان",
            currencySymbol = prefs.getString("currency_symbol", "د.ج") ?: "د.ج",
            themeMode = prefs.getString("theme_mode", "light") ?: "light",
            primaryColorIndex = prefs.getInt("primary_color_index", 0)
        )
    }

    fun updateSettings(newSettings: CompanySettings) {
        prefs.edit()
            .putString("company_name", newSettings.companyName)
            .putString("company_phone", newSettings.companyPhone)
            .putString("company_address", newSettings.companyAddress)
            .putString("currency_symbol", newSettings.currencySymbol)
            .putString("theme_mode", newSettings.themeMode)
            .putInt("primary_color_index", newSettings.primaryColorIndex)
            .apply()
        _settings.value = newSettings
    }

    // Backup & Restore
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val invoices = invoiceDao.getAllInvoices()
        val root = JSONObject()
        root.put("version", 1)
        root.put("exported_at", System.currentTimeMillis())
        val invArray = JSONArray()
        for (inv in invoices) {
            val obj = JSONObject()
            obj.put("id", inv.id)
            obj.put("invoiceNumber", inv.invoiceNumber)
            obj.put("customer", inv.customer)
            obj.put("date", inv.date)
            obj.put("itemsJson", inv.itemsJson)
            obj.put("discount", inv.discount)
            obj.put("taxRate", inv.taxRate)
            obj.put("status", inv.status)
            obj.put("notes", inv.notes)
            obj.put("createdAt", inv.createdAt)
            if (inv.deletedAt != null) obj.put("deletedAt", inv.deletedAt)
            invArray.put(obj)
        }
        root.put("invoices", invArray)
        root.toString(2)
    }

    suspend fun importJson(jsonStr: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonStr)
        val array = root.optJSONArray("invoices") ?: JSONArray()
        val list = mutableListOf<InvoiceEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                InvoiceEntity(
                    id = obj.getString("id"),
                    invoiceNumber = obj.optString("invoiceNumber", ""),
                    customer = obj.optString("customer", "عميل نقدي"),
                    date = obj.optString("date", ""),
                    itemsJson = obj.optString("itemsJson", "[]"),
                    discount = obj.optDouble("discount", 0.0),
                    taxRate = obj.optDouble("taxRate", 0.0),
                    status = obj.optString("status", "unpaid"),
                    notes = obj.optString("notes", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    deletedAt = if (obj.has("deletedAt")) obj.optLong("deletedAt") else null
                )
            )
        }
        if (list.isNotEmpty()) {
            invoiceDao.insertAll(list)
        }
        list.size
    }
}
