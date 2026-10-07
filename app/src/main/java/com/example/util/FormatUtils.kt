package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.CompanySettings
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceItem
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)

    fun formatCurrency(amount: Double, symbol: String = "د.ج"): String {
        return try {
            "${decimalFormat.format(amount)} $symbol"
        } catch (_: Exception) {
            String.format(Locale.US, "%.2f %s", amount, symbol)
        }
    }

    fun formatDate(date: Date = Date()): String {
        return try {
            dateFormat.format(date)
        } catch (_: Exception) {
            "${date.year + 1900}/${date.month + 1}/${date.date}"
        }
    }

    fun generateInvoiceNumber(): String {
        val ts = System.currentTimeMillis().toString()
        return if (ts.length > 5) ts.substring(ts.length - 6) else ts
    }

    fun shareInvoiceAsText(context: Context, invoice: InvoiceEntity, settings: CompanySettings) {
        val items = invoice.getItems()
        val sb = StringBuilder()
        sb.append("🧾 *فاتورة رقم:* ${invoice.invoiceNumber}\n")
        sb.append("🏢 *المتجر:* ${settings.companyName}\n")
        if (settings.companyPhone.isNotBlank()) sb.append("📞 *هاتف:* ${settings.companyPhone}\n")
        sb.append("👤 *العميل:* ${invoice.customer}\n")
        sb.append("📅 *التاريخ:* ${invoice.date}\n")
        sb.append("----------------------------\n")
        sb.append("📦 *الأصناف:*\n")
        items.forEachIndexed { i, it ->
            sb.append("${i + 1}. ${it.name}\n")
            sb.append("   ${it.qty} × ${formatCurrency(it.price, settings.currencySymbol)} = ${formatCurrency(it.total, settings.currencySymbol)}\n")
        }
        sb.append("----------------------------\n")
        sb.append("المجموع الفرعي: ${formatCurrency(invoice.subtotal, settings.currencySymbol)}\n")
        if (invoice.discount > 0) {
            sb.append("الخصم: -${formatCurrency(invoice.discount, settings.currencySymbol)}\n")
        }
        if (invoice.taxRate > 0) {
            sb.append("الضريبة (${invoice.taxRate}%): +${formatCurrency(invoice.taxAmount, settings.currencySymbol)}\n")
        }
        sb.append("💰 *الإجمالي النهائي: ${formatCurrency(invoice.total, settings.currencySymbol)}*\n")
        sb.append("الحالة: ${if (invoice.isPaid) "مدفوعة ✅" else "غير مدفوعة ⏳"}\n")
        if (invoice.notes.isNotBlank()) {
            sb.append("ملاحظات: ${invoice.notes}\n")
        }
        sb.append("----------------------------\n")
        sb.append("شكراً لتعاملكم معنا 🙏\n")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "مشاركة الفاتورة"))
    }

    fun printInvoice(context: Context, invoice: InvoiceEntity, settings: CompanySettings) {
        val items = invoice.getItems()
        val html = buildInvoiceHtml(invoice, items, settings)

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("فاتورة_${invoice.invoiceNumber}")
                printManager?.print(
                    "فاتورة_${invoice.invoiceNumber}",
                    printAdapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun buildInvoiceHtml(
        invoice: InvoiceEntity,
        items: List<InvoiceItem>,
        settings: CompanySettings
    ): String {
        val rowsHtml = StringBuilder()
        for (item in items) {
            rowsHtml.append(
                """
                <tr>
                    <td style="padding: 8px; border: 1px solid #ddd; text-align: right;">${item.name}</td>
                    <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${item.qty}</td>
                    <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${formatCurrency(item.price, settings.currencySymbol)}</td>
                    <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${formatCurrency(item.total, settings.currencySymbol)}</td>
                </tr>
                """.trimIndent()
            )
        }

        return """
        <!DOCTYPE html>
        <html dir="rtl" lang="ar">
        <head>
            <meta charset="utf-8">
            <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 20px; color: #333; }
                .header { display: flex; justify-content: space-between; border-bottom: 2px solid #006a60; padding-bottom: 12px; margin-bottom: 16px; }
                .title { font-size: 26px; font-weight: bold; color: #006a60; }
                .company-info { text-align: left; }
                .details-box { background-color: #f8f9fa; border-radius: 8px; padding: 12px; margin-bottom: 20px; }
                table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                th { background-color: #006a60; color: white; padding: 10px; border: 1px solid #006a60; }
                .totals { margin-top: 16px; text-align: left; }
                .grand-total { font-size: 20px; font-weight: bold; color: #006a60; }
                .status-badge { display: inline-block; padding: 4px 10px; border-radius: 4px; font-weight: bold; font-size: 12px; }
                .paid { background-color: #d4edda; color: #155724; }
                .unpaid { background-color: #fff3cd; color: #856404; }
                .footer { text-align: center; margin-top: 40px; color: #777; font-size: 13px; }
            </style>
        </head>
        <body>
            <div class="header">
                <div>
                    <div class="title">فاتورة مبيعات</div>
                    <div>رقم الفاتورة: <strong>${invoice.invoiceNumber}</strong></div>
                    <div>التاريخ: ${invoice.date}</div>
                </div>
                <div class="company-info">
                    <div style="font-size: 18px; font-weight: bold;">${settings.companyName}</div>
                    <div>هاتف: ${settings.companyPhone}</div>
                    <div>العنوان: ${settings.companyAddress}</div>
                </div>
            </div>

            <div class="details-box">
                <div>العميل: <strong>${invoice.customer}</strong></div>
                <div style="margin-top: 6px;">
                    حالة الدفع: 
                    <span class="status-badge ${if (invoice.isPaid) "paid" else "unpaid"}">
                        ${if (invoice.isPaid) "مدفوعة" else "غير مدفوعة"}
                    </span>
                </div>
            </div>

            <table>
                <thead>
                    <tr>
                        <th>الصنف</th>
                        <th>الكمية</th>
                        <th>السعر</th>
                        <th>الإجمالي</th>
                    </tr>
                </thead>
                <tbody>
                    $rowsHtml
                </tbody>
            </table>

            <div class="totals">
                <div>المجموع الفرعي: <strong>${formatCurrency(invoice.subtotal, settings.currencySymbol)}</strong></div>
                ${if (invoice.discount > 0) "<div>الخصم: -<strong>${formatCurrency(invoice.discount, settings.currencySymbol)}</strong></div>" else ""}
                ${if (invoice.taxRate > 0) "<div>الضريبة (${invoice.taxRate}%): +<strong>${formatCurrency(invoice.taxAmount, settings.currencySymbol)}</strong></div>" else ""}
                <hr style="border-top: 1px solid #ddd; margin: 8px 0; width: 250px; margin-inline-start: auto;">
                <div class="grand-total">الإجمالي: ${formatCurrency(invoice.total, settings.currencySymbol)}</div>
            </div>

            ${if (invoice.notes.isNotBlank()) "<div style='margin-top: 20px; padding: 10px; background: #eee; border-radius: 6px;'>ملاحظات: ${invoice.notes}</div>" else ""}

            <div class="footer">
                شكراً لتعاملكم معنا 🙏
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
