package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceJsonAdapter
import com.example.data.model.ProductItem
import com.example.ui.viewmodel.InvoiceViewModel
import com.example.util.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InvoiceScreen(
    viewModel: InvoiceViewModel,
    initialItems: List<InvoiceItem>? = null,
    onNavigateBack: () -> Unit,
    onNavigateToSaved: () -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.allProducts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var customerName by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf(FormatUtils.generateInvoiceNumber()) }
    var invoiceDate by remember { mutableStateOf(FormatUtils.formatDate()) }
    var discountText by remember { mutableStateOf("0") }
    var taxRateText by remember { mutableStateOf("0") }
    var isPaid by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("نقداً") } // "نقداً", "بطاقة", "تحويل", "آجل"
    var paidAmountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val invoiceItems = remember {
        mutableStateListOf<InvoiceItem>().apply {
            if (!initialItems.isNullOrEmpty()) {
                addAll(initialItems)
            }
        }
    }

    var showItemDialog by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var itemToDeleteIndex by remember { mutableStateOf<Int?>(null) }

    val subtotal = invoiceItems.fold(0.0) { sum, it -> sum + it.total }
    val discount = discountText.toDoubleOrNull() ?: 0.0
    val taxRate = taxRateText.toDoubleOrNull() ?: 0.0
    val taxAmount = subtotal * (taxRate / 100.0)
    val grandTotal = (subtotal - discount + taxAmount).coerceAtLeast(0.0)

    val paidAmount = paidAmountText.toDoubleOrNull() ?: (if (isPaid) grandTotal else 0.0)
    val changeAmount = (paidAmount - grandTotal).coerceAtLeast(0.0)
    val remainingAmount = if (isPaid) 0.0 else (grandTotal - paidAmount).coerceAtLeast(0.0)

    fun resetInvoice() {
        customerName = ""
        invoiceNumber = FormatUtils.generateInvoiceNumber()
        invoiceDate = FormatUtils.formatDate()
        discountText = "0"
        taxRateText = "0"
        isPaid = false
        paymentMethod = "نقداً"
        paidAmountText = ""
        notes = ""
        invoiceItems.clear()
    }

    fun buildInvoiceEntity(): InvoiceEntity {
        return InvoiceEntity(
            id = UUID.randomUUID().toString(),
            invoiceNumber = invoiceNumber.ifBlank { FormatUtils.generateInvoiceNumber() },
            customer = customerName.ifBlank { "عميل نقدي" },
            date = invoiceDate,
            itemsJson = InvoiceJsonAdapter.toJson(invoiceItems),
            discount = discount,
            taxRate = taxRate,
            status = if (isPaid) "paid" else "unpaid",
            paymentMethod = paymentMethod,
            paidAmount = paidAmount,
            notes = notes
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("فاتورة كاملة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("invoice_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { resetInvoice() }, modifier = Modifier.testTag("new_invoice_button")) {
                        Icon(imageVector = Icons.Default.NoteAdd, contentDescription = "فاتورة جديدة")
                    }
                    IconButton(onClick = onNavigateToSaved, modifier = Modifier.testTag("saved_invoices_button")) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "المحفوظة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            InvoiceBottomActionBar(
                subtotal = subtotal,
                discount = discount,
                taxRate = taxRate,
                taxAmount = taxAmount,
                grandTotal = grandTotal,
                currencySymbol = settings.currencySymbol,
                onAddItem = {
                    editingItemIndex = null
                    showItemDialog = true
                },
                onSave = {
                    if (invoiceItems.isEmpty()) {
                        scope.launch { snackbarHostState.showSnackbar("أضف صنفاً واحداً على الأقل") }
                        return@InvoiceBottomActionBar
                    }
                    val invoice = buildInvoiceEntity()
                    viewModel.saveInvoice(invoice) {
                        scope.launch { snackbarHostState.showSnackbar("تم حفظ الفاتورة بنجاح") }
                    }
                },
                onPrint = {
                    if (invoiceItems.isEmpty()) {
                        scope.launch { snackbarHostState.showSnackbar("أضف صنفاً واحداً على الأقل") }
                        return@InvoiceBottomActionBar
                    }
                    FormatUtils.printInvoice(context, buildInvoiceEntity(), settings)
                },
                onShare = {
                    if (invoiceItems.isEmpty()) {
                        scope.launch { snackbarHostState.showSnackbar("أضف صنفاً واحداً على الأقل") }
                        return@InvoiceBottomActionBar
                    }
                    FormatUtils.shareInvoiceAsText(context, buildInvoiceEntity(), settings)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("اسم العميل") },
                        placeholder = { Text("عميل نقدي") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = invoiceNumber,
                            onValueChange = { invoiceNumber = it },
                            label = { Text("رقم الفاتورة") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("invoice_number_input")
                        )

                        OutlinedTextField(
                            value = invoiceDate,
                            onValueChange = { invoiceDate = it },
                            label = { Text("التاريخ") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("invoice_date_input")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = { Text("الخصم (${settings.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("discount_input")
                        )

                        OutlinedTextField(
                            value = taxRateText,
                            onValueChange = { taxRateText = it },
                            label = { Text("الضريبة %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tax_rate_input")
                        )
                    }

                    // Payment Method & Status
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "طريقة الدفع:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("نقداً", "بطاقة", "تحويل", "آجل").forEach { method ->
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = { paymentMethod = method },
                                    label = { Text(method) },
                                    leadingIcon = {
                                        when (method) {
                                            "نقداً" -> Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                            "بطاقة" -> Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                            else -> Icon(Icons.Default.Money, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Payment Status selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حالة الفاتورة:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp)
                        )

                        FilterChip(
                            selected = !isPaid,
                            onClick = { isPaid = false },
                            label = { Text("غير مدفوعة") },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("status_unpaid_chip")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilterChip(
                            selected = isPaid,
                            onClick = { isPaid = true },
                            label = { Text("مدفوعة") },
                            leadingIcon = {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.testTag("status_paid_chip")
                        )
                    }

                    // Cash received & Change calculator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = paidAmountText,
                            onValueChange = { paidAmountText = it },
                            label = { Text("المبلغ المستلم (${settings.currencySymbol})") },
                            placeholder = { Text(if (isPaid) grandTotal.toString() else "0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (changeAmount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (changeAmount > 0) "الباقي للعميل:" else "المتبقي على العميل:",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(if (changeAmount > 0) changeAmount else remainingAmount, settings.currencySymbol),
                                    fontWeight = FontWeight.Bold,
                                    color = if (changeAmount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Quick add from catalog chips
            if (products.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "إضافة سريعة من دليل السلع (اضغط للإضافة):",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            products.take(14).forEach { prod ->
                                AssistChip(
                                    onClick = {
                                        val existingIndex = invoiceItems.indexOfFirst { it.name == prod.name && it.price == prod.defaultPrice }
                                        if (existingIndex >= 0) {
                                            val old = invoiceItems[existingIndex]
                                            invoiceItems[existingIndex] = old.copy(qty = old.qty + 1.0)
                                        } else {
                                            invoiceItems.add(InvoiceItem(name = prod.name, qty = 1.0, price = prod.defaultPrice))
                                        }
                                        scope.launch { snackbarHostState.showSnackbar("أضيف: ${prod.name}") }
                                    },
                                    label = {
                                        Text("${prod.name} • ${FormatUtils.formatCurrency(prod.defaultPrice, settings.currencySymbol)}")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)
                                )
                            }
                        }
                    }
                }
            }

            // Items Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الأصناف (${invoiceItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = {
                        editingItemIndex = null
                        showItemDialog = true
                    },
                    modifier = Modifier.testTag("add_item_inline_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة صنف")
                }
            }

            // Items List
            if (invoiceItems.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد أصناف في الفاتورة بعد",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    invoiceItems.forEachIndexed { index, item ->
                        InvoiceItemCard(
                            index = index + 1,
                            item = item,
                            currencySymbol = settings.currencySymbol,
                            onEdit = {
                                editingItemIndex = index
                                showItemDialog = true
                            },
                            onDelete = {
                                itemToDeleteIndex = index
                            }
                        )
                    }
                }
            }

            // Notes input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات وشروط الفاتورة (اختياري)") },
                placeholder = { Text("مثال: البضاعة المباعة ترد أو تستبدل خلال 3 أيام...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Add / Edit Item Dialog with Enhanced Autocomplete
    if (showItemDialog) {
        val editingItem = editingItemIndex?.let { invoiceItems.getOrNull(it) }
        InvoiceItemAutocompleteDialogEnhanced(
            initialItem = editingItem,
            products = products,
            currencySymbol = settings.currencySymbol,
            onDismiss = {
                showItemDialog = false
                editingItemIndex = null
            },
            onSave = { newItem, saveToProductsCatalog, category ->
                if (editingItemIndex != null && editingItemIndex!! in invoiceItems.indices) {
                    invoiceItems[editingItemIndex!!] = newItem
                } else {
                    invoiceItems.add(newItem)
                }

                if (saveToProductsCatalog) {
                    viewModel.addProduct(newItem.name, newItem.price, category)
                }

                showItemDialog = false
                editingItemIndex = null
            }
        )
    }

    // Delete Item Confirmation
    if (itemToDeleteIndex != null) {
        AlertDialog(
            onDismissRequest = { itemToDeleteIndex = null },
            title = { Text("حذف الصنف") },
            text = { Text("هل تريد حذف هذا الصنف من الفاتورة؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToDeleteIndex?.let { invoiceItems.removeAt(it) }
                        itemToDeleteIndex = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteIndex = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Autocomplete item dialog satisfying:
 * "بمجرد كتابة أول حرف من اسم السلعة داخل الفاتورة، تظهر السلع المطابقة فوراً في قائمة منسدلة.
 * عند الضغط على السلعة، يُعبّأ اسمها وسعرها تلقائياً مع نقل المؤشر للكمية دون الحاجة لإعادة كتابة البيانات."
 */
@Composable
fun InvoiceItemAutocompleteDialogEnhanced(
    initialItem: InvoiceItem?,
    products: List<ProductItem>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (item: InvoiceItem, saveToCatalog: Boolean, category: String) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var qtyText by remember { mutableStateOf(initialItem?.let { if (it.qty % 1 == 0.0) it.qty.toInt().toString() else it.qty.toString() } ?: "1") }
    var priceText by remember { mutableStateOf(initialItem?.let { if (it.price % 1 == 0.0) it.price.toInt().toString() else it.price.toString() } ?: "") }
    var itemCategory by remember { mutableStateOf("عام") }
    var saveToCatalog by remember { mutableStateOf(true) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var qtyError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    var dropdownExpanded by remember { mutableStateOf(false) }
    val qtyFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    // Matching products for autocomplete: triggers immediately from the first character!
    val matchingProducts = remember(name, products) {
        if (name.isBlank()) {
            emptyList()
        } else {
            val q = name.trim()
            val starts = products.filter { it.name.startsWith(q) || it.barcode.startsWith(q) }
            val contains = products.filter {
                (!it.name.startsWith(q) && !it.barcode.startsWith(q)) &&
                        (it.name.contains(q, ignoreCase = true) || it.barcode.contains(q))
            }
            starts + contains
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialItem == null) "إضافة صنف للفاتورة" else "تعديل الصنف",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name Input with Autocomplete Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                            dropdownExpanded = it.isNotBlank() && matchingProducts.isNotEmpty()
                        },
                        label = { Text("اسم السلعة / الصنف أو الباركود") },
                        placeholder = { Text("اكتب حرفاً للبحث التلقائي...") },
                        isError = nameError != null,
                        supportingText = { nameError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item_name_input")
                    )

                    // Autocomplete Dropdown Menu
                    DropdownMenu(
                        expanded = dropdownExpanded && matchingProducts.isNotEmpty(),
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .heightIn(max = 250.dp)
                    ) {
                        matchingProducts.forEach { product ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = product.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (product.category.isNotBlank() && product.category != "عام") {
                                                Text(
                                                    text = product.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                        Text(
                                            text = FormatUtils.formatCurrency(product.defaultPrice, currencySymbol),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    // 1. Auto-fill name and price
                                    name = product.name
                                    priceText = if (product.defaultPrice % 1 == 0.0) {
                                        product.defaultPrice.toInt().toString()
                                    } else {
                                        product.defaultPrice.toString()
                                    }
                                    itemCategory = product.category
                                    dropdownExpanded = false

                                    // 2. Transfer focus to Quantity field smoothly!
                                    scope.launch {
                                        delay(100)
                                        try {
                                            qtyFocusRequester.requestFocus()
                                        } catch (_: Exception) {}
                                    }
                                },
                                modifier = Modifier.testTag("autocomplete_option_${product.id}")
                            )
                        }
                    }
                }

                // Quantity Input
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = {
                        qtyText = it
                        qtyError = null
                    },
                    label = { Text("الكمية") },
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = qtyError != null,
                    supportingText = { qtyError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(qtyFocusRequester)
                        .testTag("item_qty_input")
                )

                // Price Input
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        priceError = null
                    },
                    label = { Text("السعر الإفرادي ($currencySymbol)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = priceError != null,
                    supportingText = { priceError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_price_input")
                )

                // Save to catalog checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { saveToCatalog = !saveToCatalog },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = saveToCatalog,
                        onCheckedChange = { saveToCatalog = it },
                        modifier = Modifier.testTag("save_to_catalog_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حفظ الاسم والسعر في الدليل للفواتير القادمة",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    val parsedQty = qtyText.toDoubleOrNull()
                    val parsedPrice = priceText.toDoubleOrNull()

                    var valid = true
                    if (trimmedName.isEmpty()) {
                        nameError = "الرجاء إدخال اسم السلعة"
                        valid = false
                    }
                    if (parsedQty == null || parsedQty <= 0) {
                        qtyError = "كمية غير صحيحة"
                        valid = false
                    }
                    if (parsedPrice == null || parsedPrice < 0) {
                        priceError = "سعر غير صحيح"
                        valid = false
                    }

                    if (valid && parsedQty != null && parsedPrice != null) {
                        onSave(
                            InvoiceItem(name = trimmedName, qty = parsedQty, price = parsedPrice),
                            saveToCatalog,
                            itemCategory
                        )
                    }
                },
                modifier = Modifier.testTag("save_item_dialog_button")
            ) {
                Text("حفظ", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun InvoiceItemCard(
    index: Int,
    item: InvoiceItem,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                val qtyText = if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else item.qty.toString()
                Text(
                    text = "$qtyText × ${FormatUtils.formatCurrency(item.price, currencySymbol)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Text(
                text = FormatUtils.formatCurrency(item.total, currencySymbol),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun InvoiceBottomActionBar(
    subtotal: Double,
    discount: Double,
    taxRate: Double,
    taxAmount: Double,
    grandTotal: Double,
    currencySymbol: String,
    onAddItem: () -> Unit,
    onSave: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Totals breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "المجموع الفرعي:", style = MaterialTheme.typography.bodyMedium)
                Text(text = FormatUtils.formatCurrency(subtotal, currencySymbol), style = MaterialTheme.typography.bodyMedium)
            }

            if (discount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "الخصم:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Text(text = "-${FormatUtils.formatCurrency(discount, currencySymbol)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }

            if (taxRate > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "الضريبة ($taxRate%):", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "+${FormatUtils.formatCurrency(taxAmount, currencySymbol)}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = DividerDefaults.color.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الإجمالي:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = FormatUtils.formatCurrency(grandTotal, currencySymbol),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddItem,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_item_bottom_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة")
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_invoice_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حفظ")
                }

                Button(
                    onClick = onPrint,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("print_invoice_button")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("طباعة")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_invoice_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مشاركة الفاتورة")
            }
        }
    }
}
