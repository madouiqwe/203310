package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.InvoiceItem
import com.example.model.InvoiceStatus
import com.example.model.PaymentMethod
import com.example.ui.InvoiceViewModel
import com.example.ui.components.EditItemDialog
import com.example.ui.components.PackageBadge
import com.example.ui.components.formatCurrency
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditScreen(
    viewModel: InvoiceViewModel,
    onNavigateBack: () -> Unit,
    onSaveSuccess: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val draftInvoice by viewModel.draftInvoice.collectAsStateWithLifecycle()
    val isDraftCompletedInitially = viewModel.isDraftCompletedInitially

    val draft = draftInvoice ?: run {
        onNavigateBack()
        return
    }

    var editingItem by remember { mutableStateOf<InvoiceItem?>(null) }
    var isAddingItem by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.cancelDraft()
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "تعديل فاتورة (${draft.invoiceNumber})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        if (isDraftCompletedInitially || draft.status == InvoiceStatus.COMPLETED) {
                            Text(
                                text = "فاتورة مكتملة - التعديل متاح لجميع البيانات والبنود",
                                fontSize = 11.sp,
                                color = Color(0xFF137333),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.cancelDraft()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("edit_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "إلغاء")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveDraft { savedId ->
                                onSaveSuccess(savedId)
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp).testTag("save_invoice_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ التعديلات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "الإجمالي النهائي:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(draft.grandTotal),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveDraft { savedId ->
                                onSaveSuccess(savedId)
                            }
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("bottom_save_invoice_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حفظ التعديلات",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notice banner when editing a completed invoice
            if (isDraftCompletedInitially || draft.status == InvoiceStatus.COMPLETED) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEFF6FF)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("completed_edit_notice_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تعديل فاتورة مكتملة متاح بالكامل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1E40AF)
                            )
                            Text(
                                text = "يمكنك تعديل أي بند أو تغيير الأسعار وتحديث المدفوع. ستتم إعادة الحساب وحفظ الفاتورة مباشرة.",
                                fontSize = 11.sp,
                                color = Color(0xFF1E3A8A),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Customer Information Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "بيانات العميل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedTextField(
                        value = draft.customerName,
                        onValueChange = {
                            viewModel.updateDraftCustomer(
                                name = it,
                                phone = draft.customerPhone,
                                email = draft.customerEmail
                            )
                        },
                        label = { Text("اسم العميل أو الشركة *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_customer_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft.customerPhone,
                            onValueChange = {
                                viewModel.updateDraftCustomer(
                                    name = draft.customerName,
                                    phone = it,
                                    email = draft.customerEmail
                                )
                            },
                            label = { Text("رقم الهاتف") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("edit_customer_phone_input")
                        )

                        OutlinedTextField(
                            value = draft.customerEmail,
                            onValueChange = {
                                viewModel.updateDraftCustomer(
                                    name = draft.customerName,
                                    phone = draft.customerPhone,
                                    email = it
                                )
                            },
                            label = { Text("البريد الإلكتروني") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("edit_customer_email_input")
                        )
                    }
                }
            }

            // Invoice Number Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "بيانات الفاتورة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedTextField(
                        value = draft.invoiceNumber,
                        onValueChange = { viewModel.updateDraftInvoiceNumber(it) },
                        label = { Text("رقم الفاتورة *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_invoice_number_input")
                    )
                }
            }

            // Line Items Section - CRITICAL (Add / Edit / Remove items)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "سلع وبنود الفاتورة (${draft.items.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "اضغط على القلم للتعديل أو أضف سلعة جديدة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { isAddingItem = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_item_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة سلعة", fontSize = 13.sp)
                        }
                    }

                    if (draft.items.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "لا توجد سلع مسجلة في هذه الفاتورة.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { isAddingItem = true },
                                    modifier = Modifier.testTag("empty_add_item_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة أول سلعة الآن")
                                }
                            }
                        }
                    } else {
                        draft.items.forEachIndexed { index, item ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("item_row_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            if (item.packageInfo.isNotBlank()) {
                                                PackageBadge(packageText = item.packageInfo)
                                            }
                                        }

                                        if (item.description.isNotBlank()) {
                                            Text(
                                                text = item.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Automatic calculation row with package stepper & total price
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Package / Quantity Stepper
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(
                                                            onClick = { viewModel.decrementDraftItemQuantity(item.id) },
                                                            enabled = if (item.unitsPerPackage > 1.0) item.packagesCount > 1.0 else item.quantity > 1.0,
                                                            modifier = Modifier.size(28.dp).testTag("decrease_qty_${index}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Remove,
                                                                contentDescription = "إنقاص",
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = if (item.unitsPerPackage > 1.0) {
                                                                "${if (item.packagesCount == item.packagesCount.toLong().toDouble()) item.packagesCount.toLong().toString() else item.packagesCount.toString()} حزم"
                                                            } else {
                                                                if (item.quantity == item.quantity.toLong().toDouble()) item.quantity.toLong().toString() else item.quantity.toString()
                                                            },
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            modifier = Modifier.padding(horizontal = 4.dp).testTag("item_qty_label_${index}")
                                                        )
                                                        IconButton(
                                                            onClick = { viewModel.incrementDraftItemQuantity(item.id) },
                                                            modifier = Modifier.size(28.dp).testTag("increase_qty_${index}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "زيادة",
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = "× ${formatCurrency(item.unitPrice)}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                Spacer(modifier = Modifier.weight(1f))

                                                // Real-time calculated total
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "= ${formatCurrency(item.total)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp).testTag("item_total_badge_${index}")
                                                    )
                                                }
                                            }

                                            if (item.unitsPerPackage > 1.0) {
                                                Text(
                                                    text = "${formatCurrency(item.unitPrice)} (سعر السلعة) × (${if (item.unitsPerPackage == item.unitsPerPackage.toLong().toDouble()) item.unitsPerPackage.toLong().toString() else item.unitsPerPackage.toString()} حبة) × ${if (item.packagesCount == item.packagesCount.toLong().toDouble()) item.packagesCount.toLong().toString() else item.packagesCount.toString()} حزمة = ${formatCurrency(item.total)}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "سعر الحزمة الواحدة: ${formatCurrency(item.packagePrice)}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { editingItem = item },
                                            modifier = Modifier.size(32.dp).testTag("edit_item_button_${index}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "تعديل",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeDraftItem(item.id) },
                                            modifier = Modifier.size(32.dp).testTag("delete_item_button_${index}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Discounts & Tax & Financial adjustments
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "الخصومات والضريبة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    // Discount row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = if (draft.discountAmount == 0.0) "" else draft.discountAmount.toString(),
                            onValueChange = {
                                val value = it.toDoubleOrNull() ?: 0.0
                                viewModel.updateDraftDiscount(value, draft.isDiscountPercentage)
                            },
                            label = { Text("قيمة الخصم") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("edit_discount_input")
                        )

                        FilterChip(
                            selected = draft.isDiscountPercentage,
                            onClick = {
                                viewModel.updateDraftDiscount(draft.discountAmount, !draft.isDiscountPercentage)
                            },
                            label = { Text(if (draft.isDiscountPercentage) "نسبة مئوية %" else "مبلغ ثابت ر.س") },
                            leadingIcon = {
                                Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            modifier = Modifier.testTag("discount_type_chip")
                        )
                    }

                    // VAT Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ضريبة القيمة المضافة (15% VAT)",
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "الضريبة المحسوبة: ${formatCurrency(draft.calculatedTax)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = draft.taxPercentage > 0,
                            onCheckedChange = { checked ->
                                viewModel.updateDraftTax(if (checked) 15.0 else 0.0)
                            },
                            modifier = Modifier.testTag("tax_switch")
                        )
                    }
                }
            }

            // Payment Status & Method Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "حالة وطريقة السداد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    // Status Chips
                    Text(text = "حالة الفاتورة:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InvoiceStatus.entries.forEach { status ->
                            FilterChip(
                                selected = draft.status == status,
                                onClick = { viewModel.updateDraftStatus(status) },
                                label = { Text(status.labelAr) },
                                leadingIcon = {
                                    if (status == InvoiceStatus.COMPLETED) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                },
                                modifier = Modifier.testTag("status_chip_${status.name}")
                            )
                        }
                    }

                    // Amount Paid (especially when partially paid)
                    if (draft.status == InvoiceStatus.PARTIALLY_PAID) {
                        OutlinedTextField(
                            value = if (draft.paidAmount == 0.0) "" else draft.paidAmount.toString(),
                            onValueChange = {
                                val amount = it.toDoubleOrNull() ?: 0.0
                                viewModel.updateDraftPaidAmount(amount)
                            },
                            label = { Text("المبلغ المدفوع (ر.س) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_paid_amount_input")
                        )

                        Text(
                            text = "المتبقي للسداد: ${formatCurrency(draft.remainingBalance)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB06000)
                        )
                    }

                    HorizontalDivider()

                    // Payment Method Chips
                    Text(text = "طريقة الدفع:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = draft.paymentMethod == method,
                                onClick = { viewModel.updateDraftPaymentMethod(method) },
                                label = { Text(method.labelAr) },
                                modifier = Modifier.testTag("method_chip_${method.name}")
                            )
                        }
                    }
                }
            }

            // Notes and Terms Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ملاحظات وشروط",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    OutlinedTextField(
                        value = draft.notes,
                        onValueChange = { viewModel.updateDraftNotes(it, draft.terms) },
                        label = { Text("ملاحظات إضافية") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("edit_notes_input")
                    )

                    OutlinedTextField(
                        value = draft.terms,
                        onValueChange = { viewModel.updateDraftNotes(draft.notes, it) },
                        label = { Text("الشروط والأحكام") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("edit_terms_input")
                    )
                }
            }
        }
    }

    // Dialog for editing an existing item
    editingItem?.let { itemToEdit ->
        EditItemDialog(
            initialItem = itemToEdit,
            onDismiss = { editingItem = null },
            onConfirm = { updatedItem ->
                viewModel.updateDraftItem(updatedItem)
                editingItem = null
            }
        )
    }

    // Dialog for adding a new item
    if (isAddingItem) {
        EditItemDialog(
            initialItem = null,
            onDismiss = { isAddingItem = false },
            onConfirm = { newItem ->
                viewModel.addDraftItem(newItem)
                isAddingItem = false
            }
        )
    }
}
