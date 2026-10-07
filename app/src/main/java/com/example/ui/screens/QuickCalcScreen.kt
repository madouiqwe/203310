package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceJsonAdapter
import com.example.ui.viewmodel.InvoiceViewModel
import com.example.util.FormatUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCalcScreen(
    viewModel: InvoiceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToFullInvoice: (List<InvoiceItem>) -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.allProducts.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("1") }
    var itemPrice by remember { mutableStateOf("") }
    var cashTenderText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var saveToSavedInvoices by remember { mutableStateOf(false) }

    var showKeypad by remember { mutableStateOf(false) }
    var calcExpression by remember { mutableStateOf("") }
    var calcResult by remember { mutableStateOf("0") }

    var discountPercent by remember { mutableDoubleStateOf(0.0) }
    var taxPercent by remember { mutableDoubleStateOf(0.0) }

    val items = remember { mutableStateListOf<InvoiceItem>() }

    val priceFocusRequester = remember { FocusRequester() }
    val qtyFocusRequester = remember { FocusRequester() }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val matchingProducts = remember(itemName, products) {
        if (itemName.isBlank()) {
            emptyList()
        } else {
            val q = itemName.trim()
            val starts = products.filter { it.name.startsWith(q) || it.barcode.startsWith(q) }
            val contains = products.filter {
                (!it.name.startsWith(q) && !it.barcode.startsWith(q)) &&
                        (it.name.contains(q, ignoreCase = true) || it.barcode.contains(q))
            }
            starts + contains
        }
    }

    val subtotal = items.fold(0.0) { sum, it -> sum + it.total }
    val discountAmount = subtotal * (discountPercent / 100.0)
    val taxAmount = (subtotal - discountAmount) * (taxPercent / 100.0)
    val total = (subtotal - discountAmount + taxAmount).coerceAtLeast(0.0)

    val cashTender = cashTenderText.toDoubleOrNull() ?: 0.0
    val changeDue = (cashTender - total).coerceAtLeast(0.0)

    fun addItem() {
        val price = itemPrice.toDoubleOrNull()
        if (price == null || price <= 0) {
            scope.launch { snackbarHostState.showSnackbar("أدخل سعراً صحيحاً") }
            return
        }
        val qty = itemQty.toDoubleOrNull() ?: 1.0
        if (qty <= 0) {
            scope.launch { snackbarHostState.showSnackbar("كمية غير صحيحة") }
            return
        }

        val name = itemName.ifBlank { "صنف ${items.size + 1}" }
        items.add(InvoiceItem(name = name, qty = qty, price = price))
        itemName = ""
        itemPrice = ""
        itemQty = "1"
        try {
            priceFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    fun handleCalcKey(key: String) {
        when (key) {
            "C" -> {
                calcExpression = ""
                calcResult = "0"
            }
            "←" -> {
                if (calcExpression.isNotEmpty()) {
                    calcExpression = calcExpression.dropLast(1)
                }
                calcResult = if (calcExpression.isEmpty()) "0" else calcExpression
            }
            "=" -> {
                try {
                    val result = evaluateMathExpression(calcExpression)
                    calcResult = result
                    calcExpression = result
                } catch (_: Exception) {
                    calcResult = "خطأ"
                }
            }
            "→" -> {
                val v = calcResult.toDoubleOrNull()
                if (v != null && v > 0) {
                    itemPrice = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.2f", v)
                    showKeypad = false
                    try {
                        priceFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
            }
            else -> {
                calcExpression += key
                calcResult = calcExpression
            }
        }
    }

    fun buildInvoiceEntity(): InvoiceEntity {
        return InvoiceEntity(
            id = UUID.randomUUID().toString(),
            invoiceNumber = FormatUtils.generateInvoiceNumber(),
            customer = "عميل نقدي",
            date = FormatUtils.formatDate(),
            itemsJson = InvoiceJsonAdapter.toJson(items),
            discount = discountAmount,
            taxRate = taxPercent,
            status = "paid",
            paymentMethod = "نقداً",
            paidAmount = if (cashTender > 0) cashTender else total,
            notes = notes
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("الوضع السريع", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("quick_calc_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showKeypad = !showKeypad },
                        modifier = Modifier.testTag("calculator_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (showKeypad) Icons.Default.KeyboardHide else Icons.Default.Calculate,
                            contentDescription = "آلة حاسبة"
                        )
                    }
                    IconButton(
                        onClick = {
                            if (items.isNotEmpty()) {
                                onNavigateToFullInvoice(items.toList())
                            }
                        },
                        enabled = items.isNotEmpty(),
                        modifier = Modifier.testTag("transfer_to_full_invoice_button")
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "نقل إلى فاتورة كاملة")
                    }
                    IconButton(
                        onClick = {
                            if (items.isNotEmpty()) {
                                items.clear()
                                notes = ""
                                cashTenderText = ""
                                discountPercent = 0.0
                                taxPercent = 0.0
                            }
                        },
                        enabled = items.isNotEmpty(),
                        modifier = Modifier.testTag("clear_all_items_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "مسح الكل")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Cash Received and Change due bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = cashTenderText,
                            onValueChange = { cashTenderText = it },
                            label = { Text("المبلغ المستلم") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        if (cashTender > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("الباقي للعميل:", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        FormatUtils.formatCurrency(changeDue, settings.currencySymbol),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveToSavedInvoices = !saveToSavedInvoices },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = saveToSavedInvoices,
                            onCheckedChange = { saveToSavedInvoices = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ تلقائي في سجل الفواتير", style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (items.isNotEmpty()) {
                                    items.removeAt(items.size - 1)
                                }
                            },
                            enabled = items.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("undo_last_item_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تراجع")
                        }

                        OutlinedButton(
                            onClick = {
                                if (items.isEmpty()) {
                                    scope.launch { snackbarHostState.showSnackbar("أضف صنفاً واحداً على الأقل") }
                                    return@OutlinedButton
                                }
                                val invoice = buildInvoiceEntity()
                                if (saveToSavedInvoices) {
                                    viewModel.saveInvoice(invoice)
                                }
                                FormatUtils.shareInvoiceAsText(context, invoice, settings)
                            },
                            enabled = items.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_share_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة")
                        }

                        Button(
                            onClick = {
                                if (items.isEmpty()) {
                                    scope.launch { snackbarHostState.showSnackbar("أضف صنفاً واحداً على الأقل") }
                                    return@Button
                                }
                                val invoice = buildInvoiceEntity()
                                if (saveToSavedInvoices) {
                                    viewModel.saveInvoice(invoice)
                                }
                                FormatUtils.printInvoice(context, invoice, settings)
                            },
                            enabled = items.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_print_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Big Total Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "الإجمالي",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = FormatUtils.formatCurrency(total, settings.currencySymbol),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${items.size} صنف",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        if (discountPercent > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "خصم ${discountPercent.toInt()}%",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        if (taxPercent > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "ضريبة ${taxPercent.toInt()}%",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Optional Calculator Pad
            if (showKeypad) {
                QuickCalculatorPad(
                    expression = calcExpression,
                    result = calcResult,
                    onKeyClick = { handleCalcKey(it) }
                )
            } else {
                // Quick Catalog Chips
                if (products.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item { Spacer(modifier = Modifier.width(10.dp)) }
                        items(products) { prod ->
                            AssistChip(
                                onClick = {
                                    items.add(InvoiceItem(name = prod.name, qty = 1.0, price = prod.defaultPrice))
                                },
                                label = { Text(prod.name) },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.width(10.dp)) }
                    }
                }

                // Autocomplete Item Input Row
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Name with Autocomplete
                            Box(modifier = Modifier.weight(2f)) {
                                OutlinedTextField(
                                    value = itemName,
                                    onValueChange = {
                                        itemName = it
                                        dropdownExpanded = it.isNotBlank() && matchingProducts.isNotEmpty()
                                    },
                                    label = { Text("الاسم") },
                                    placeholder = { Text("اكتب حرفاً...") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quick_item_name_input")
                                )

                                DropdownMenu(
                                    expanded = dropdownExpanded && matchingProducts.isNotEmpty(),
                                    onDismissRequest = { dropdownExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .heightIn(max = 200.dp)
                                ) {
                                    matchingProducts.forEach { product ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(product.name, fontWeight = FontWeight.Bold)
                                                    Text(FormatUtils.formatCurrency(product.defaultPrice, settings.currencySymbol))
                                                }
                                            },
                                            onClick = {
                                                itemName = product.name
                                                itemPrice = if (product.defaultPrice % 1 == 0.0) {
                                                    product.defaultPrice.toInt().toString()
                                                } else {
                                                    product.defaultPrice.toString()
                                                }
                                                dropdownExpanded = false

                                                scope.launch {
                                                    delay(100)
                                                    try {
                                                        qtyFocusRequester.requestFocus()
                                                    } catch (_: Exception) {}
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Quantity
                            OutlinedTextField(
                                value = itemQty,
                                onValueChange = { itemQty = it },
                                label = { Text("الكمية") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(qtyFocusRequester)
                                    .testTag("quick_item_qty_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = itemPrice,
                                onValueChange = { itemPrice = it },
                                label = { Text("السعر (${settings.currencySymbol})") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { addItem() }),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(priceFocusRequester)
                                    .testTag("quick_item_price_input")
                            )

                            Button(
                                onClick = { addItem() },
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("quick_add_item_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة")
                            }
                        }
                    }
                }
            }

            // Discount and Tax quick selector chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("خصم:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                listOf(0.0, 5.0, 10.0, 15.0, 20.0).forEach { p ->
                    FilterChip(
                        selected = discountPercent == p,
                        onClick = { discountPercent = p },
                        label = { Text("${p.toInt()}%") }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text("ضريبة:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                listOf(0.0, 5.0, 10.0, 15.0).forEach { p ->
                    FilterChip(
                        selected = taxPercent == p,
                        onClick = { taxPercent = p },
                        label = { Text("${p.toInt()}%") }
                    )
                }
            }

            // Items list in quick mode (reverse chronological)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(items.reversed()) { revIdx, item ->
                    val actualIdx = items.size - 1 - revIdx
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${actualIdx + 1}.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${item.qty} × ${FormatUtils.formatCurrency(item.price, settings.currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(
                                FormatUtils.formatCurrency(item.total, settings.currencySymbol),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { items.removeAt(actualIdx) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickCalculatorPad(
    expression: String,
    result: String,
    onKeyClick: (String) -> Unit
) {
    val rows = listOf(
        listOf("C", "←", "÷", "×"),
        listOf("7", "8", "9", "-"),
        listOf("4", "5", "6", "+"),
        listOf("1", "2", "3", "="),
        listOf("0", ".", "→", "=")
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expression.ifBlank { "0" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = result,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row.forEachIndexed { colIdx, key ->
                        if (key == "=" && colIdx > 0 && row[colIdx - 1] == "=") {
                            // skip duplicate
                        } else {
                            val isAction = key in listOf("C", "←")
                            val isOp = key in listOf("+", "-", "×", "÷", "=", "→")
                            Button(
                                onClick = { onKeyClick(key) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAction) MaterialTheme.colorScheme.errorContainer
                                    else if (isOp) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isAction) MaterialTheme.colorScheme.onErrorContainer
                                    else if (isOp) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    text = key,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun evaluateMathExpression(expr: String): String {
    if (expr.isBlank()) return "0"
    val clean = expr.replace("×", "*").replace("÷", "/")
    val numbers = mutableListOf<Double>()
    val ops = mutableListOf<Char>()
    var cur = ""

    for (i in clean.indices) {
        val c = clean[i]
        if (c in "+-*/" && i > 0) {
            numbers.add(cur.toDoubleOrNull() ?: 0.0)
            ops.add(c)
            cur = ""
        } else {
            cur += c
        }
    }
    if (cur.isNotEmpty()) {
        numbers.add(cur.toDoubleOrNull() ?: 0.0)
    }

    if (numbers.isEmpty()) return "0"

    var i = 0
    while (i < ops.size) {
        if (ops[i] == '*') {
            numbers[i] = numbers[i] * numbers[i + 1]
            numbers.removeAt(i + 1)
            ops.removeAt(i)
        } else if (ops[i] == '/') {
            val denom = numbers[i + 1]
            numbers[i] = if (denom != 0.0) numbers[i] / denom else 0.0
            numbers.removeAt(i + 1)
            ops.removeAt(i)
        } else {
            i++
        }
    }

    var result = numbers[0]
    for (j in ops.indices) {
        if (ops[j] == '+') result += numbers[j + 1]
        if (ops[j] == '-') result -= numbers[j + 1]
    }

    return if (result % 1.0 == 0.0) result.toInt().toString() else String.format("%.2f", result)
}
