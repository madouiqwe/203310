package com.example

import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceJsonAdapter
import com.example.data.model.ProductItem
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuickInvoiceLogicTest {

    @Test
    fun testJsonAdapterSerialization() {
        val items = listOf(
            InvoiceItem(name = "قهوة عربية", qty = 2.0, price = 450.0),
            InvoiceItem(name = "شاي أسود", qty = 1.0, price = 200.0)
        )
        val json = InvoiceJsonAdapter.toJson(items)
        val decoded = InvoiceJsonAdapter.fromJson(json)

        assertEquals(2, decoded.size)
        assertEquals("قهوة عربية", decoded[0].name)
        assertEquals(2.0, decoded[0].qty, 0.001)
        assertEquals(450.0, decoded[0].price, 0.001)
        assertEquals(900.0, decoded[0].total, 0.001)
    }

    @Test
    fun testFormatCurrency() {
        val formatted = FormatUtils.formatCurrency(1500.0, "د.ج")
        assertTrue(formatted.contains("1,500.00"))
        assertTrue(formatted.contains("د.ج"))
    }

    @Test
    fun testAutocompleteFilterLogic() {
        val catalog = listOf(
            ProductItem(name = "قهوة عربية", defaultPrice = 450.0),
            ProductItem(name = "قهوة تركية", defaultPrice = 300.0),
            ProductItem(name = "شاي أخضر", defaultPrice = 250.0),
            ProductItem(name = "سكر أبيض", defaultPrice = 110.0)
        )

        val query = "ق"
        val startsWith = catalog.filter { it.name.startsWith(query) }
        val contains = catalog.filter { !it.name.startsWith(query) && it.name.contains(query, ignoreCase = true) }
        val results = startsWith + contains

        assertEquals(2, results.size)
        assertEquals("قهوة عربية", results[0].name)
        assertEquals("قهوة تركية", results[1].name)
    }
}
