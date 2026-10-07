package com.example.data

import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import com.example.model.PaymentMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class InvoiceRepository(private val invoiceDao: InvoiceDao) {

    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getInvoiceById(id: Long): Invoice? {
        return invoiceDao.getInvoiceById(id)?.toDomain()
    }

    suspend fun saveInvoice(invoice: Invoice): Long {
        val now = System.currentTimeMillis()
        val isNew = invoice.id == 0L

        // Determine completion state and post-completion edits
        val isCompleted = invoice.status == InvoiceStatus.COMPLETED
        val completedTimestamp = when {
            isCompleted && invoice.completedAt == null -> now
            !isCompleted -> null
            else -> invoice.completedAt
        }

        // If it was already completed before, mark as edited after completion
        val editedAfterCompletion = if (!isNew && (invoice.completedAt != null || invoice.isEditedAfterCompletion)) {
            true
        } else {
            invoice.isEditedAfterCompletion
        }

        val toSave = invoice.copy(
            updatedAt = now,
            completedAt = completedTimestamp,
            isEditedAfterCompletion = editedAfterCompletion
        )

        return invoiceDao.insertInvoice(InvoiceEntity.fromDomain(toSave))
    }

    suspend fun deleteInvoice(id: Long) {
        invoiceDao.deleteInvoiceById(id)
    }

    suspend fun duplicateInvoice(originalId: Long): Long? {
        val original = getInvoiceById(originalId) ?: return null
        val now = System.currentTimeMillis()
        val copy = original.copy(
            id = 0L,
            invoiceNumber = generateInvoiceNumber("INV-COPY"),
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            status = InvoiceStatus.PENDING,
            paidAmount = 0.0,
            isEditedAfterCompletion = false
        )
        return invoiceDao.insertInvoice(InvoiceEntity.fromDomain(copy))
    }

    suspend fun toggleCompletedStatus(invoice: Long): Boolean {
        val current = getInvoiceById(invoice) ?: return false
        val now = System.currentTimeMillis()
        val newStatus = if (current.status == InvoiceStatus.COMPLETED) {
            InvoiceStatus.PENDING
        } else {
            InvoiceStatus.COMPLETED
        }
        val newPaidAmount = if (newStatus == InvoiceStatus.COMPLETED) {
            current.grandTotal
        } else {
            current.paidAmount
        }

        val updated = current.copy(
            status = newStatus,
            paidAmount = newPaidAmount,
            updatedAt = now,
            completedAt = if (newStatus == InvoiceStatus.COMPLETED) now else null
        )
        invoiceDao.updateInvoice(InvoiceEntity.fromDomain(updated))
        return true
    }

    suspend fun populateSampleDataIfEmpty() {
        val count = invoiceDao.getInvoiceCount()
        if (count > 0) return

        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        // 1. A completed invoice that user can edit anytime!
        val completedInvoice = Invoice(
            id = 0L,
            invoiceNumber = "INV-2026-101",
            customerName = "شركة الأفق للتقنية",
            customerPhone = "+966 55 123 4567",
            customerEmail = "info@alofooq-tech.sa",
            issueDate = now - (3 * dayMillis),
            dueDate = now - (1 * dayMillis),
            status = InvoiceStatus.COMPLETED,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            items = listOf(
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "تصميم واجهة متجر إلكتروني",
                    quantity = 1.0,
                    unitPrice = 3500.0,
                    description = "تصميم تجربة وواجهة المستخدم كاملة مع الهوية"
                ),
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "تطوير لوحة تحكم متقدمة",
                    quantity = 1.0,
                    unitPrice = 2800.0,
                    description = "ربط واجهات برمجة التطبيقات مع تقارير المبيعات"
                ),
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "دعم فني وصيانة شهرية",
                    quantity = 2.0,
                    unitPrice = 600.0,
                    description = "دعم على مدار الساعة ومراقبة الأداء"
                )
            ),
            discountAmount = 300.0,
            isDiscountPercentage = false,
            taxPercentage = 15.0,
            paidAmount = 8280.0, // Fully paid
            notes = "تم سداد الفاتورة بالكامل عبر تحويل سريع إلى مصرف الراجحي.",
            terms = "الفاتورة صالحة ومعتمدة. تخضع لسياسة الضمان لمدة 3 أشهر.",
            createdAt = now - (3 * dayMillis),
            updatedAt = now - (2 * dayMillis),
            completedAt = now - (2 * dayMillis),
            isEditedAfterCompletion = false
        )

        // 2. A pending invoice
        val pendingInvoice = Invoice(
            id = 0L,
            invoiceNumber = "INV-2026-102",
            customerName = "مؤسسة النور للتجارة",
            customerPhone = "+966 50 987 6543",
            customerEmail = "alnoor.trading@gmail.com",
            issueDate = now - (1 * dayMillis),
            dueDate = now + (5 * dayMillis),
            status = InvoiceStatus.PENDING,
            paymentMethod = PaymentMethod.CARD,
            items = listOf(
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "تركيب أنظمة كاميرات المراقبة",
                    quantity = 4.0,
                    unitPrice = 450.0,
                    description = "كاميرات بدقة 4K خارجية مقاومة للطقس"
                ),
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "جهاز تسجيل شبكي NVR 8 قنوات",
                    quantity = 1.0,
                    unitPrice = 1200.0,
                    description = "سعة تخزينية 4 تيرابايت مع ضبط الشبكة"
                )
            ),
            discountAmount = 10.0,
            isDiscountPercentage = true,
            taxPercentage = 15.0,
            paidAmount = 0.0,
            notes = "يرجى تسديد الدفعة قبل موعد الاستحقاق لبدء التشغيل الفعلي.",
            terms = "الدفع خلال 5 أيام عمل من تاريخ الفاتورة.",
            createdAt = now - (1 * dayMillis),
            updatedAt = now - (1 * dayMillis),
            completedAt = null,
            isEditedAfterCompletion = false
        )

        // 3. A partially paid invoice
        val partialInvoice = Invoice(
            id = 0L,
            invoiceNumber = "INV-2026-103",
            customerName = "مكتب المهندس خالد العتيبي",
            customerPhone = "+966 54 321 0000",
            customerEmail = "khaled.arch@outlook.com",
            issueDate = now,
            dueDate = now + (10 * dayMillis),
            status = InvoiceStatus.PARTIALLY_PAID,
            paymentMethod = PaymentMethod.CASH,
            items = listOf(
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "استشارات هندسية ومعاينة موقع",
                    quantity = 3.0,
                    unitPrice = 800.0,
                    description = "معاينة هندسية وتقارير فحص التربة"
                )
            ),
            discountAmount = 0.0,
            isDiscountPercentage = false,
            taxPercentage = 15.0,
            paidAmount = 1500.0, // Partial payment
            notes = "تم استلام دفعة مقدمة نقداً وقدرها 1,500 ريال والمتبقي عند تسليم التقرير.",
            terms = "تسليم المخططات مشروط باستكمال كامل المبلغ المتبقي.",
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            isEditedAfterCompletion = false
        )

        invoiceDao.insertInvoice(InvoiceEntity.fromDomain(completedInvoice))
        invoiceDao.insertInvoice(InvoiceEntity.fromDomain(pendingInvoice))
        invoiceDao.insertInvoice(InvoiceEntity.fromDomain(partialInvoice))
    }

    fun generateInvoiceNumber(prefix: String = "INV"): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US)
        return "$prefix-${dateFormat.format(Date())}"
    }
}
