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
}
