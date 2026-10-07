package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.InvoiceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: InvoiceViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var companyName by remember { mutableStateOf(settings.companyName) }
    var companyPhone by remember { mutableStateOf(settings.companyPhone) }
    var companyAddress by remember { mutableStateOf(settings.companyAddress) }
    var currencySymbol by remember { mutableStateOf(settings.currencySymbol) }
    var themeMode by remember { mutableStateOf(settings.themeMode) }

    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Company Info Section
            SettingsSectionHeader(title = "بيانات المتجر والشركة", icon = Icons.Default.Business)

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
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("اسم الشركة / المتجر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("company_name_setting")
                    )

                    OutlinedTextField(
                        value = companyPhone,
                        onValueChange = { companyPhone = it },
                        label = { Text("رقم الهاتف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("company_phone_setting")
                    )

                    OutlinedTextField(
                        value = companyAddress,
                        onValueChange = { companyAddress = it },
                        label = { Text("العنوان") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("company_address_setting")
                    )

                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = { currencySymbol = it },
                        label = { Text("رمز العملة") },
                        placeholder = { Text("د.ج أو ر.س أو $") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("currency_symbol_setting")
                    )

                    Button(
                        onClick = {
                            viewModel.updateSettings(
                                settings.copy(
                                    companyName = companyName,
                                    companyPhone = companyPhone,
                                    companyAddress = companyAddress,
                                    currencySymbol = currencySymbol,
                                    themeMode = themeMode
                                )
                            )
                            scope.launch { snackbarHostState.showSnackbar("تم حفظ الإعدادات بنجاح") }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_settings_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ بيانات المتجر")
                    }
                }
            }

            // Theme Mode
            SettingsSectionHeader(title = "المظهر", icon = Icons.Default.Brightness6)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = themeMode == "light",
                    onClick = {
                        themeMode = "light"
                        viewModel.updateSettings(settings.copy(themeMode = "light"))
                    },
                    label = { Text("فاتح") }
                )
                FilterChip(
                    selected = themeMode == "dark",
                    onClick = {
                        themeMode = "dark"
                        viewModel.updateSettings(settings.copy(themeMode = "dark"))
                    },
                    label = { Text("داكن") }
                )
                FilterChip(
                    selected = themeMode == "system",
                    onClick = {
                        themeMode = "system"
                        viewModel.updateSettings(settings.copy(themeMode = "system"))
                    },
                    label = { Text("النظام") }
                )
            }

            // Backup & Restore
            SettingsSectionHeader(title = "النسخ الاحتياطي", icon = Icons.Default.Backup)

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val json = viewModel.exportJson()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, json)
                            type = "application/json"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "تصدير نسخة احتياطية"))
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("export_backup_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تصدير نسخة احتياطية (JSON)")
            }

            OutlinedButton(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("import_backup_button")
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("استيراد نسخة احتياطية")
            }

            // Danger Zone
            SettingsSectionHeader(title = "منطقة الخطر", icon = Icons.Default.Warning)

            OutlinedButton(
                onClick = { showClearDataConfirm = true },
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth().testTag("clear_all_data_button")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مسح كل البيانات")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("استيراد نسخة احتياطية") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الصق محتوى النسخة الاحتياطية (JSON) هنا:")
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val count = viewModel.importJson(importJsonText)
                                snackbarHostState.showSnackbar("تم استيراد $count فاتورة بنجاح")
                                showImportDialog = false
                                importJsonText = ""
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("فشل الاستيراد: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("استيراد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("مسح كل البيانات") },
            text = { Text("سيتم حذف كل الفواتير نهائياً ولا يمكن استرجاعها. هل أنت متأكد؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData {
                            scope.launch { snackbarHostState.showSnackbar("تم مسح كافة البيانات") }
                        }
                        showClearDataConfirm = false
                    }
                ) {
                    Text("مسح الكل", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
