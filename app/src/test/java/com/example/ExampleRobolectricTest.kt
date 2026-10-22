package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("فاتورة الدفع", appName)
  }

  @Test
  fun `invoice calculation and post completion edit verification`() {
    val item1 = InvoiceItem(name = "استشارة تقنية", quantity = 2.0, unitPrice = 500.0)
    val item2 = InvoiceItem(name = "تطوير لوحة تحكم", quantity = 1.0, unitPrice = 2000.0)

    val completedInvoice = Invoice(
      invoiceNumber = "INV-TEST-01",
      customerName = "عميل تجريبي",
      status = InvoiceStatus.COMPLETED,
      items = listOf(item1, item2),
      discountAmount = 100.0,
      isDiscountPercentage = false,
      taxPercentage = 15.0,
      paidAmount = 3335.0
    )

    // Subtotal: 1000 + 2000 = 3000
    assertEquals(3000.0, completedInvoice.subtotal, 0.01)
    // After discount: 3000 - 100 = 2900
    // Tax 15%: 2900 * 0.15 = 435
    // Grand Total: 2900 + 435 = 3335
    assertEquals(3335.0, completedInvoice.grandTotal, 0.01)
    assertEquals(0.0, completedInvoice.remainingBalance, 0.01)

    // User edits the completed invoice and adds another item:
    val item3 = InvoiceItem(name = "دعم فني إضافي", quantity = 1.0, unitPrice = 500.0)
    val editedInvoice = completedInvoice.copy(
      items = completedInvoice.items + item3,
      isEditedAfterCompletion = true
    )

    // Subtotal: 3000 + 500 = 3500
    // After discount: 3500 - 100 = 3400
    // Tax 15%: 3400 * 0.15 = 510
    // Grand Total: 3400 + 510 = 3910
    assertEquals(3500.0, editedInvoice.subtotal, 0.01)
    assertEquals(3910.0, editedInvoice.grandTotal, 0.01)
    assertTrue(editedInvoice.isEditedAfterCompletion)
    // Remaining balance updated automatically
    assertEquals(3910.0 - 3335.0, editedInvoice.remainingBalance, 0.01)
  }

  @Test
  fun `item packageInfo field and json converter persistence verification`() {
    val converters = com.example.data.Converters()
    val itemWithPackage = InvoiceItem(
      name = "منتج تجاري",
      packageInfo = "حزمة 12 حبة",
      quantity = 3.0,
      unitPrice = 45.0,
      description = "كرتون معبأ"
    )

    val jsonString = converters.fromInvoiceItemsList(listOf(itemWithPackage))
    assertTrue(jsonString.contains("حزمة 12 حبة"))

    val restoredList = converters.toInvoiceItemsList(jsonString)
    assertEquals(1, restoredList.size)
    assertEquals("حزمة 12 حبة", restoredList[0].packageInfo)
    assertEquals(135.0, restoredList[0].total, 0.01)
  }

  @Test
  fun `automatic item total and invoice total calculation when quantity or unit price updates`() {
    val initialItem = InvoiceItem(
      id = "test-item-1",
      name = "منتج عصير طبيعي",
      quantity = 2.0,
      unitPrice = 15.0
    )
    // Initially: 2 * 15 = 30.0
    assertEquals(30.0, initialItem.total, 0.001)

    // Update quantity from 2 to 5 -> total automatically becomes 75.0
    val updatedQuantityItem = initialItem.copy(quantity = 5.0)
    assertEquals(75.0, updatedQuantityItem.total, 0.001)

    // Update unit price from 15 to 20 -> total automatically becomes 100.0
    val updatedPriceItem = updatedQuantityItem.copy(unitPrice = 20.0)
    assertEquals(100.0, updatedPriceItem.total, 0.001)

    val invoice = Invoice(
      invoiceNumber = "INV-AUTO-01",
      customerName = "عميل التجربة",
      items = listOf(updatedPriceItem),
      discountAmount = 0.0,
      taxPercentage = 15.0
    )

    // Subtotal: 100.0
    assertEquals(100.0, invoice.subtotal, 0.001)
    // Tax (15%): 15.0
    assertEquals(15.0, invoice.calculatedTax, 0.001)
    // Grand Total: 115.0
    assertEquals(115.0, invoice.grandTotal, 0.001)
  }

  @Test
  fun `package calculation with units per package times packages count times price`() {
    // حزمة فيها 12 × 5 حزمات × 10 ر.س سعر القطعة = 600 ر.س
    val packageItem = InvoiceItem(
      id = "pkg-item-1",
      name = "كرتون علب عصير",
      unitsPerPackage = 12.0, // كم في الحزمة
      packagesCount = 5.0,    // عدد الحزمات
      quantity = 12.0 * 5.0,  // 60 قطعة
      unitPrice = 10.0        // سعر القطعة
    )

    // Total = 12 * 5 * 10 = 600.0
    assertEquals(60.0, packageItem.quantity, 0.001)
    assertEquals(600.0, packageItem.total, 0.001)
    // Package price = 12 * 10 = 120.0
    assertEquals(120.0, packageItem.packagePrice, 0.001)

    // Converters persistence check
    val converters = com.example.data.Converters()
    val json = converters.fromInvoiceItemsList(listOf(packageItem))
    val parsed = converters.toInvoiceItemsList(json)
    assertEquals(1, parsed.size)
    assertEquals(12.0, parsed[0].unitsPerPackage, 0.001)
    assertEquals(5.0, parsed[0].packagesCount, 0.001)
    assertEquals(60.0, parsed[0].quantity, 0.001)
    assertEquals(600.0, parsed[0].total, 0.001)
  }
}
