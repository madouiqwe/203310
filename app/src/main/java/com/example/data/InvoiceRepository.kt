package com.example.data

import com.example.model.Invoice
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.SaleUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val productDao: ProductDao
) {

    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices().map { entities ->
        entities.map { it.toDomain() }
    }

    val allProducts: Flow<List<Product>> = productDao.getAllProducts().map { entities ->
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

    suspend fun toggleCompletedStatus(invoiceId: Long): Boolean {
        val current = getInvoiceById(invoiceId) ?: return false
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

    suspend fun saveProduct(product: Product) {
        productDao.insertProduct(ProductEntity.fromDomain(product))
    }

    suspend fun deleteProduct(productId: String) {
        productDao.deleteProductById(productId)
    }

    suspend fun populateSampleDataIfEmpty() {
        populateSampleProductsIfEmpty()

        val count = invoiceDao.getInvoiceCount()
        if (count > 0) return

        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        // 1. A completed invoice with variable package capacities (e.g. 24 and 10)
        val completedInvoice = Invoice(
            id = 0L,
            invoiceNumber = "INV-2026-101",
            customerName = "سوبرماركت التوفير الحديث",
            customerPhone = "+966 55 123 4567",
            customerEmail = "orders@tawfeer-market.sa",
            issueDate = now - (3 * dayMillis),
            dueDate = now - (1 * dayMillis),
            status = InvoiceStatus.COMPLETED,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            items = listOf(
                // Product with 24 units per package (كرتونة عصير)
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "عصير برتقال طبيعي 250 مل",
                    packageInfo = "كرتونة (24 حبة)",
                    unitsPerPackage = 24.0,
                    packagesCount = 5.0,
                    singlePiecesCount = 0.0,
                    saleUnit = SaleUnit.PACKAGE,
                    quantity = 120.0,
                    unitPrice = 2.50,
                    description = "عصير طازج معبأ - كرتونة سعة 24 حبة"
                ),
                // Product with 10 units per package (شد معجون طماطم)
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "معجون طماطم لونا 135 جم",
                    packageInfo = "شد (10 حبات)",
                    unitsPerPackage = 10.0,
                    packagesCount = 4.0,
                    singlePiecesCount = 0.0,
                    saleUnit = SaleUnit.PACKAGE,
                    quantity = 40.0,
                    unitPrice = 3.00,
                    description = "معجون طماطم مركز - شد 10 حبات"
                ),
                // Product sold by SINGLE PIECE (حبة مفردة) with defined package capacity of 12
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "زيت زيتون بكر ممتاز 500 مل",
                    packageInfo = "كرتونة (12 حبة)",
                    unitsPerPackage = 12.0,
                    packagesCount = 0.0,
                    singlePiecesCount = 3.0,
                    saleUnit = SaleUnit.PIECE,
                    quantity = 3.0,
                    unitPrice = 25.00,
                    description = "بيع قطاعي بالحبة المفردة"
                )
            ),
            discountAmount = 15.0,
            isDiscountPercentage = false,
            taxPercentage = 15.0,
            paidAmount = 552.0, // Fully paid
            notes = "تم تسليم الطلبية كاملة وسداد القيمة عبر تحويل بنكي فوري.",
            terms = "شكراً لتعاملكم معنا. البضاعة المباعة تخضع لسياسة الفحص عند الاستلام.",
            createdAt = now - (3 * dayMillis),
            updatedAt = now - (2 * dayMillis),
            completedAt = now - (2 * dayMillis),
            isEditedAfterCompletion = false
        )

        // 2. A pending invoice with water bottles (40 per carton) and biscuits (18 per box)
        val pendingInvoice = Invoice(
            id = 0L,
            invoiceNumber = "INV-2026-102",
            customerName = "مؤسسة النور للتوريدات",
            customerPhone = "+966 50 987 6543",
            customerEmail = "alnoor.supply@gmail.com",
            issueDate = now - (1 * dayMillis),
            dueDate = now + (5 * dayMillis),
            status = InvoiceStatus.PENDING,
            paymentMethod = PaymentMethod.CARD,
            items = listOf(
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "مياه شرب نقية 330 مل",
                    packageInfo = "كرتونة (40 حبة)",
                    unitsPerPackage = 40.0,
                    packagesCount = 10.0,
                    singlePiecesCount = 0.0,
                    saleUnit = SaleUnit.PACKAGE,
                    quantity = 400.0,
                    unitPrice = 0.85,
                    description = "كراتين مياه سعة 40 حبة"
                ),
                InvoiceItem(
                    id = UUID.randomUUID().toString(),
                    name = "بسكويت شاي فاخر",
                    packageInfo = "صندوق (18 باكيت)",
                    unitsPerPackage = 18.0,
                    packagesCount = 3.0,
                    singlePiecesCount = 0.0,
                    saleUnit = SaleUnit.PACKAGE,
                    quantity = 54.0,
                    unitPrice = 2.00,
                    description = "صندوق 18 حبة"
                )
            ),
            discountAmount = 5.0,
            isDiscountPercentage = true,
            taxPercentage = 15.0,
            paidAmount = 0.0,
            notes = "يرجى سداد المبلغ قبل موعد الاستحقاق الموضح أعلاه.",
            terms = "الدفع خلال 5 أيام عمل من تاريخ الفاتورة.",
            createdAt = now - (1 * dayMillis),
            updatedAt = now - (1 * dayMillis),
            completedAt = null,
            isEditedAfterCompletion = false
        )

        invoiceDao.insertInvoice(InvoiceEntity.fromDomain(completedInvoice))
        invoiceDao.insertInvoice(InvoiceEntity.fromDomain(pendingInvoice))
    }

    private suspend fun populateSampleProductsIfEmpty() {
        if (productDao.getProductCount() > 0) return

        val sampleCatalog = listOf(
            Product(
                id = "p-juice-24",
                name = "عصير برتقال طبيعي 250 مل",
                defaultUnitPrice = 2.50,
                unitsPerPackage = 24.0,
                packageTypeName = "كرتونة",
                description = "كرتونة تحتوي على 24 علبة عصير"
            ),
            Product(
                id = "p-tomato-10",
                name = "معجون طماطم لونا 135 جم",
                defaultUnitPrice = 3.00,
                unitsPerPackage = 10.0,
                packageTypeName = "شد",
                description = "شد مغلّف يحتوي على 10 حبات"
            ),
            Product(
                id = "p-water-40",
                name = "مياه شرب نقية 330 مل",
                defaultUnitPrice = 0.85,
                unitsPerPackage = 40.0,
                packageTypeName = "كرتونة",
                description = "كرتونة اقتصادية تحتوي على 40 قارورة"
            ),
            Product(
                id = "p-milk-12",
                name = "حليب نادك كامل الدسم 1 لتر",
                defaultUnitPrice = 5.50,
                unitsPerPackage = 12.0,
                packageTypeName = "صندوق",
                description = "صندوق 12 عبوة تترا بريك"
            ),
            Product(
                id = "p-biscuit-18",
                name = "بسكويت شاي فاخر",
                defaultUnitPrice = 2.00,
                unitsPerPackage = 18.0,
                packageTypeName = "صندوق",
                description = "صندوق يحتوي على 18 باكيت"
            ),
            Product(
                id = "p-tissue-6",
                name = "مناديل فاخرة معقمة",
                defaultUnitPrice = 4.50,
                unitsPerPackage = 6.0,
                packageTypeName = "ربطة",
                description = "ربطة تحتوي على 6 علب مناديل"
            ),
            Product(
                id = "p-oil-12",
                name = "زيت زيتون بكر ممتاز 500 مل",
                defaultUnitPrice = 25.00,
                unitsPerPackage = 12.0,
                packageTypeName = "كرتونة",
                description = "كرتونة زجاجات زيت زيتون 12 حبة"
            ),
            Product(
                id = "p-softdrink-30",
                name = "مشروب غازي كانز 330 مل",
                defaultUnitPrice = 3.00,
                unitsPerPackage = 30.0,
                packageTypeName = "بلتة",
                description = "بلتة كانز 30 حبة"
            )
        )

        productDao.insertAll(sampleCatalog.map { ProductEntity.fromDomain(it) })
    }

    fun generateInvoiceNumber(prefix: String = "INV"): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US)
        return "$prefix-${dateFormat.format(Date())}"
    }
}
