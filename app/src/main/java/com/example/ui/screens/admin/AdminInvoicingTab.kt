package com.example.ui.screens.admin

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.data.repository.NexusRepository
import com.example.ui.components.PaymentModeLogo
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.io.File
import java.io.FileOutputStream


private fun createTaxInvoicePdf(context: Context, invoice: IspInvoice): android.net.Uri {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    var pageNumber = 0
    var page = document.startPage(pageInfo)
    var canvas = page.canvas
    var y = 48f

    val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(30, 41, 59)
        textSize = 10f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    val bold = Paint(regular).apply { typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
    val title = Paint(bold).apply { textSize = 20f; color = android.graphics.Color.rgb(15, 23, 42) }
    val muted = Paint(regular).apply { color = android.graphics.Color.rgb(100, 116, 139); textSize = 9f }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(203, 213, 225)
        strokeWidth = 1f
    }
    val money = { value: Double -> "₹" + String.format(Locale.ENGLISH, "%,.2f", value) }

    fun newPage() {
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber + 1).create())
        canvas = page.canvas
        y = 48f
    }
    fun line() {
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f
    }
    fun write(value: String, paint: Paint = regular, x: Float = 40f, advance: Float = 16f) {
        canvas.drawText(value.take(105), x, y, paint)
        y += advance
    }
    fun tableHeader() {
        canvas.drawRect(40f, y - 12f, 555f, y + 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(226, 232, 240) })
        write("DESCRIPTION", bold, 46f, 20f)
        canvas.drawText("QTY", 350f, y - 4f, bold)
        canvas.drawText("RATE", 405f, y - 4f, bold)
        canvas.drawText("AMOUNT", 490f, y - 4f, bold)
        y += 8f
        line()
    }

    try {
        write("K&B NET SERVICE", title, advance = 24f)
        write("TAX INVOICE", bold, advance = 18f)
        write("Invoice No: ${invoice.invoiceNumber}", regular, advance = 15f)
        write("Invoice Date: ${SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(invoice.invoiceDateMillis))}", regular, advance = 15f)
        write("Due Date: ${SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(invoice.dueDateMillis))}", regular, advance = 18f)
        line()
        write("BILL TO", bold, advance = 16f)
        write(invoice.customerName, regular, advance = 14f)
        if (invoice.customerPhone.isNotBlank()) write("Phone: ${invoice.customerPhone}", regular, advance = 14f)
        if (invoice.customerAddress.isNotBlank()) {
            invoice.customerAddress.split("\n").forEach { write(it, regular, advance = 14f) }
        }
        if (invoice.billingPeriodStart.isNotBlank() || invoice.billingPeriodEnd.isNotBlank()) {
            write("Billing period: ${invoice.billingPeriodStart} to ${invoice.billingPeriodEnd}", regular, advance = 16f)
        }
        line()
        tableHeader()

        invoice.items.forEach { item ->
            if (y > 735f) {
                newPage()
                write("K&B NET SERVICE • ${invoice.invoiceNumber} (continued)", bold, advance = 24f)
                tableHeader()
            }
            write(item.description, regular, 46f, 16f)
            canvas.drawText(item.quantity.toString(), 350f, y - 16f, regular)
            canvas.drawText(money(item.unitRate), 405f, y - 16f, regular)
            canvas.drawText(money(item.totalAmount), 490f, y - 16f, regular)
            if (item.hsnSacCode.isNotBlank()) write("SAC/HSN: ${item.hsnSacCode}", muted, 46f, 14f)
        }
        line()
        write("Subtotal: ${money(invoice.subtotal)}", regular, 360f, 16f)
        if (invoice.discountAmount > 0.0) write("Discount: -${money(invoice.discountAmount)}", regular, 360f, 16f)
        if (invoice.cgstAmount > 0.0) write("CGST: ${money(invoice.cgstAmount)}", regular, 360f, 16f)
        if (invoice.sgstAmount > 0.0) write("SGST: ${money(invoice.sgstAmount)}", regular, 360f, 16f)
        if (invoice.igstAmount > 0.0) write("IGST: ${money(invoice.igstAmount)}", regular, 360f, 16f)
        write("TOTAL: ${money(invoice.totalAmount)}", title, 360f, 24f)
        write("Paid: ${money(invoice.paidAmount)}", regular, 360f, 16f)
        write("Balance Due: ${money(invoice.balanceDue)}", bold, 360f, 20f)
        write("Status: ${invoice.status.name.replace("_", " ")}", bold, 360f, 18f)
        y += 10f
        if (invoice.notes.isNotBlank()) {
            write("Notes", bold, advance = 16f)
            invoice.notes.split("\n").forEach { write(it, regular, advance = 14f) }
        }
        y += 14f
        write("Generated by K&B Nexus • ${invoice.createdBy}", muted, advance = 14f)

        document.finishPage(page)
        val directory = File(context.filesDir, "invoices").apply { if (!exists()) mkdirs() }
        val safeName = invoice.invoiceNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val outputFile = File(directory, "$safeName.pdf")
        FileOutputStream(outputFile).use { document.writeTo(it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile)
    } finally {
        document.close()
    }
}

@Composable
fun AdminInvoicingTab(
    repository: NexusRepository,
    modifier: Modifier = Modifier
) {
    val invoices by repository.invoices.collectAsState()
    val customers by repository.customers.collectAsState()
    val plans by repository.plans.collectAsState()
    val currentUser by repository.currentUser.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<InvoiceStatus?>(null) }

    // Dialog States
    var showCreateDialog by remember { mutableStateOf(false) }
    var invoiceToRecordPayment by remember { mutableStateOf<IspInvoice?>(null) }
    var invoiceToViewDetails by remember { mutableStateOf<IspInvoice?>(null) }

    // Calculation KPIs
    val totalBilled = invoices.sumOf { it.totalAmount }
    val totalCollected = invoices.sumOf { it.paidAmount }
    val totalOutstanding = invoices.sumOf { it.balanceDue }
    val paidCount = invoices.count { it.status == InvoiceStatus.PAID }
    val unpaidCount = invoices.count { it.status == InvoiceStatus.UNPAID }
    val partialCount = invoices.count { it.status == InvoiceStatus.PARTIALLY_PAID }

    val filteredInvoices = invoices.filter { inv ->
        val matchesSearch = inv.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                inv.customerName.contains(searchQuery, ignoreCase = true) ||
                inv.customerPhone.contains(searchQuery) ||
                inv.customerId.contains(searchQuery, ignoreCase = true)
        val matchesStatus = selectedStatusFilter == null || inv.status == selectedStatusFilter
        matchesSearch && matchesStatus
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("admin_invoicing_lazy_column"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Header & Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Billing & Invoicing Engine",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Professional GST Invoices, ISP Subscriptions & Payment Receipts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("create_invoice_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Invoice", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // 4 KPI Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Billed",
                        value = "₹" + String.format(Locale.ENGLISH, "%,.0f", totalBilled),
                        subtitle = "${invoices.size} Invoices Generated",
                        icon = Icons.Default.ReceiptLong,
                        iconTint = ElectricBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Collections Received",
                        value = "₹" + String.format(Locale.ENGLISH, "%,.0f", totalCollected),
                        subtitle = "$paidCount Paid in Full",
                        icon = Icons.Default.CheckCircle,
                        iconTint = MintEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Outstanding Due",
                        value = "₹" + String.format(Locale.ENGLISH, "%,.0f", totalOutstanding),
                        subtitle = "$unpaidCount Unpaid • $partialCount Partial",
                        icon = Icons.Default.PendingActions,
                        iconTint = CoralRose,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "GST Tax Collected",
                        value = "₹" + String.format(Locale.ENGLISH, "%,.0f", invoices.sumOf { it.cgstAmount + it.sgstAmount }),
                        subtitle = "18% CGST + SGST Telecom",
                        icon = Icons.Default.AccountBalance,
                        iconTint = GoldenAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Search Bar & Filter Chips
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by invoice #, customer name, mobile...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricBlue) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invoice_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedStatusFilter == null,
                            onClick = { selectedStatusFilter = null },
                            label = { Text("All (${invoices.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = selectedStatusFilter == InvoiceStatus.UNPAID,
                            onClick = { selectedStatusFilter = InvoiceStatus.UNPAID },
                            label = { Text("Unpaid ($unpaidCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = selectedStatusFilter == InvoiceStatus.PARTIALLY_PAID,
                            onClick = { selectedStatusFilter = InvoiceStatus.PARTIALLY_PAID },
                            label = { Text("Partial ($partialCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = selectedStatusFilter == InvoiceStatus.PAID,
                            onClick = { selectedStatusFilter = InvoiceStatus.PAID },
                            label = { Text("Paid ($paidCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }

        // Invoice List Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Invoices (${filteredInvoices.size})",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Sorted by Recent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (filteredInvoices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Invoices Match Current Filter", fontWeight = FontWeight.Bold)
                        Text("Create a new invoice or change search terms", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filteredInvoices, key = { it.id }) { inv ->
                val (statusColor, statusBg) = when (inv.status) {
                    InvoiceStatus.PAID -> Pair(MintEmerald, MintEmerald.copy(alpha = 0.12f))
                    InvoiceStatus.PARTIALLY_PAID -> Pair(GoldenAmber, GoldenAmber.copy(alpha = 0.12f))
                    InvoiceStatus.UNPAID -> Pair(CoralRose, CoralRose.copy(alpha = 0.12f))
                    InvoiceStatus.CANCELLED -> Pair(Color.Gray, Color.Gray.copy(alpha = 0.12f))
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .testTag("invoice_card_${inv.invoiceNumber}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Top Header: Invoice No + Status Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(statusBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = inv.invoiceNumber,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(inv.invoiceDateMillis)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = statusBg,
                                modifier = Modifier.border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            ) {
                                Text(
                                    text = inv.status.name.replace("_", " "),
                                    color = statusColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Customer Details & Items Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = inv.customerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "ID: ${inv.customerId} • ${inv.customerPhone}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = inv.customerAddress,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹" + String.format(Locale.ENGLISH, "%,.2f", inv.totalAmount),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (inv.status != InvoiceStatus.PAID) {
                                    Text(
                                        text = "Due: ₹" + String.format(Locale.ENGLISH, "%,.2f", inv.balanceDue),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = CoralRose
                                    )
                                } else {
                                    Text(
                                        text = "Paid in Full",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MintEmerald
                                    )
                                }
                            }
                        }

                        // Line items chips
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                inv.items.take(2).forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "• ${item.description}", fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                        Text(text = "₹${item.totalAmount}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (inv.items.size > 2) {
                                    Text(text = "+ ${inv.items.size - 2} more item(s)", fontSize = 10.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Action Buttons Bar
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { invoiceToViewDetails = inv },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Invoice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (inv.status != InvoiceStatus.PAID && inv.status != InvoiceStatus.CANCELLED) {
                                Button(
                                    onClick = { invoiceToRecordPayment = inv },
                                    colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pay Due", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // ==================== CREATE INVOICE DIALOG ====================
    if (showCreateDialog) {
        CreateInvoiceDialog(
            customers = customers,
            plans = plans,
            onDismiss = { showCreateDialog = false },
            onCreate = { custId, custName, custPhone, custAddr, items, discount, gstRate, periodStart, periodEnd, notes ->
                repository.createInvoice(
                    customerId = custId,
                    customerName = custName,
                    customerPhone = custPhone,
                    customerAddress = custAddr,
                    items = items,
                    discountAmount = discount,
                    gstRatePercent = gstRate,
                    billingPeriodStart = periodStart,
                    billingPeriodEnd = periodEnd,
                    notes = notes,
                    createdBy = currentUser?.fullName ?: "Super Admin"
                )
                showCreateDialog = false
            }
        )
    }

    // ==================== RECORD PAYMENT DIALOG ====================
    invoiceToRecordPayment?.let { inv ->
        RecordInvoicePaymentDialog(
            invoice = inv,
            onDismiss = { invoiceToRecordPayment = null },
            onRecord = { amount, mode, txnRef, note ->
                repository.recordInvoicePayment(
                    invoiceId = inv.id,
                    amount = amount,
                    paymentMode = mode,
                    transactionReference = txnRef,
                    note = note,
                    recordedBy = currentUser?.fullName ?: "Admin"
                )
                invoiceToRecordPayment = null
            }
        )
    }

    // ==================== TAX INVOICE PRINTABLE / VIEWER DIALOG ====================
    invoiceToViewDetails?.let { inv ->
        TaxInvoiceViewDialog(
            invoice = inv,
            onDismiss = { invoiceToViewDetails = null },
            onShare = {
                val shareBody = """
                    *K&B NET SERVICE - TAX INVOICE*
                    Invoice No: ${inv.invoiceNumber}
                    Customer: ${inv.customerName} (${inv.customerId})
                    Amount: ₹${String.format(Locale.ENGLISH, "%,.2f", inv.totalAmount)}
                    Status: ${inv.status.name}
                    Paid: ₹${String.format(Locale.ENGLISH, "%,.2f", inv.paidAmount)}
                    Balance Due: ₹${String.format(Locale.ENGLISH, "%,.2f", inv.balanceDue)}
                    
                    Pay online via UPI: kbnet@icici
                    Helpline: +91 98765 43210
                """.trimIndent()
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareBody)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Invoice via"))
            }
        )
    }
}

// ==============================================================================
// CREATE INVOICE DIALOG IMPLEMENTATION
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceDialog(
    customers: List<IspCustomer>,
    plans: List<IspPlan>,
    onDismiss: () -> Unit,
    onCreate: (
        customerId: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        items: List<InvoiceLineItem>,
        discount: Double,
        gstRate: Double,
        periodStart: String,
        periodEnd: String,
        notes: String
    ) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<IspCustomer?>(customers.firstOrNull()) }
    var customerName by remember { mutableStateOf(selectedCustomer?.fullName ?: "") }
    var customerId by remember { mutableStateOf(selectedCustomer?.id ?: "") }
    var customerPhone by remember { mutableStateOf(selectedCustomer?.phone ?: "") }
    var customerAddress by remember { mutableStateOf(selectedCustomer?.address ?: "") }

    var billingStart by remember { mutableStateOf("2026-11-01") }
    var billingEnd by remember { mutableStateOf("2026-11-30") }
    var gstRate by remember { mutableDoubleStateOf(18.0) }
    var discountInput by remember { mutableStateOf("0") }
    var invoiceNotes by remember { mutableStateOf("Thank you for choosing K&B NET SERVICE high-speed optical fiber.") }

    // Line Items State
    val initialPlan = plans.firstOrNull()
    var lineItems by remember {
        mutableStateOf(
            listOf(
                InvoiceLineItem(
                    id = "item_1",
                    description = initialPlan?.name ?: "100 Mbps Unlimited Broadband Plan (Monthly)",
                    hsnSacCode = "998422",
                    quantity = 1,
                    unitRate = initialPlan?.monthlyPriceInr ?: 799.0,
                    totalAmount = initialPlan?.monthlyPriceInr ?: 799.0
                )
            )
        )
    }

    var showCustomerMenu by remember { mutableStateOf(false) }

    val subtotal = lineItems.sumOf { it.totalAmount }
    val discount = discountInput.toDoubleOrNull() ?: 0.0
    val taxableAmount = (subtotal - discount).coerceAtLeast(0.0)
    val gstAmount = taxableAmount * (gstRate / 100.0)
    val grandTotal = taxableAmount + gstAmount

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, tint = ElectricBlue)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Create GST Tax Invoice",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Customer Selection
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Customer Information", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Select from existing customers
                                ExposedDropdownMenuBox(
                                    expanded = showCustomerMenu,
                                    onExpandedChange = { showCustomerMenu = !showCustomerMenu }
                                ) {
                                    OutlinedTextField(
                                        value = "${selectedCustomer?.fullName ?: "Select Customer"} (${selectedCustomer?.id ?: ""})",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Choose Registered Customer") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCustomerMenu) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = showCustomerMenu,
                                        onDismissRequest = { showCustomerMenu = false }
                                    ) {
                                        customers.forEach { c ->
                                            DropdownMenuItem(
                                                text = { Text("${c.fullName} • ${c.id} (${c.provider})") },
                                                onClick = {
                                                    selectedCustomer = c
                                                    customerName = c.fullName
                                                    customerId = c.id
                                                    customerPhone = c.phone
                                                    customerAddress = c.address
                                                    showCustomerMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customerPhone,
                                        onValueChange = { customerPhone = it },
                                        label = { Text("Phone") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = customerAddress,
                                        onValueChange = { customerAddress = it },
                                        label = { Text("Installation Address") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.5f)
                                    )
                                }
                            }
                        }
                    }

                    // Line Items Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Invoice Line Items", fontWeight = FontWeight.Black, fontSize = 14.sp)

                            // Quick Template Item Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                AssistChip(
                                    onClick = {
                                        lineItems = lineItems + InvoiceLineItem(
                                            id = "item_" + UUID.randomUUID().toString().take(6),
                                            description = "Syrotech Gigabit Dual Band Wi-Fi ONT Device",
                                            hsnSacCode = "851762",
                                            quantity = 1,
                                            unitRate = 1450.0,
                                            totalAmount = 1450.0
                                        )
                                    },
                                    label = { Text("+ ONT Router", fontSize = 10.sp) }
                                )
                                AssistChip(
                                    onClick = {
                                        lineItems = lineItems + InvoiceLineItem(
                                            id = "item_" + UUID.randomUUID().toString().take(6),
                                            description = "FTTH Fiber Drop Cable & Precision Splicing Setup",
                                            hsnSacCode = "998719",
                                            quantity = 1,
                                            unitRate = 1000.0,
                                            totalAmount = 1000.0
                                        )
                                    },
                                    label = { Text("+ Installation", fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    // Items Table / Cards
                    items(lineItems) { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.description, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("HSN/SAC: ${item.hsnSacCode} • Qty: ${item.quantity} × ₹${item.unitRate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("₹" + String.format(Locale.ENGLISH, "%,.2f", item.totalAmount), fontWeight = FontWeight.Black, fontSize = 14.sp)
                                if (lineItems.size > 1) {
                                    IconButton(
                                        onClick = { lineItems = lineItems.filter { it.id != item.id } }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CoralRose, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Calculation & Tax Summary
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ElectricBlue.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal", fontSize = 12.sp)
                                    Text("₹" + String.format(Locale.ENGLISH, "%,.2f", subtotal), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Discount (₹)", fontSize = 12.sp)
                                    OutlinedTextField(
                                        value = discountInput,
                                        onValueChange = { discountInput = it },
                                        singleLine = true,
                                        modifier = Modifier.width(100.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("GST Rate", fontSize = 12.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(0.0, 5.0, 12.0, 18.0).forEach { rate ->
                                            FilterChip(
                                                selected = gstRate == rate,
                                                onClick = { gstRate = rate },
                                                label = { Text("${rate.toInt()}%", fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("CGST (${gstRate / 2}%) + SGST (${gstRate / 2}%)", fontSize = 12.sp)
                                    Text("₹" + String.format(Locale.ENGLISH, "%,.2f", gstAmount), fontSize = 12.sp)
                                }
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Grand Total Payable", fontWeight = FontWeight.Black, fontSize = 15.sp, color = ElectricBlue)
                                    Text("₹" + String.format(Locale.ENGLISH, "%,.2f", grandTotal), fontWeight = FontWeight.Black, fontSize = 16.sp, color = ElectricBlue)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (customerName.isNotBlank() && lineItems.isNotEmpty()) {
                                onCreate(
                                    customerId,
                                    customerName,
                                    customerPhone,
                                    customerAddress,
                                    lineItems,
                                    discount,
                                    gstRate,
                                    billingStart,
                                    billingEnd,
                                    invoiceNotes
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Invoice", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==============================================================================
// RECORD PAYMENT DIALOG
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordInvoicePaymentDialog(
    invoice: IspInvoice,
    onDismiss: () -> Unit,
    onRecord: (amount: Double, mode: PaymentMode, txnRef: String, note: String) -> Unit
) {
    var amountInput by remember { mutableStateOf(invoice.balanceDue.toString()) }
    var selectedMode by remember { mutableStateOf(PaymentMode.UPI) }
    var txnReference by remember { mutableStateOf("") }
    var paymentNote by remember { mutableStateOf("Payment received against invoice ${invoice.invoiceNumber}") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MintEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = MintEmerald)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Record Payment Receipt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Invoice Summary Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Invoice: ${invoice.invoiceNumber} • ${invoice.customerName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Total Billed: ₹${String.format(Locale.ENGLISH, "%,.2f", invoice.totalAmount)} • Paid: ₹${String.format(Locale.ENGLISH, "%,.2f", invoice.paidAmount)}", fontSize = 11.sp)
                        Text(
                            text = "Remaining Balance Due: ₹${String.format(Locale.ENGLISH, "%,.2f", invoice.balanceDue)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = CoralRose
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                errorMessage?.let {
                    Text(text = it, color = CoralRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it; errorMessage = null },
                    label = { Text("Payment Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Payment Method:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                // Payment Mode Selector with brand logos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PaymentModeLogo(mode = mode, size = 16.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(mode.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = txnReference,
                    onValueChange = { txnReference = it },
                    label = { Text("Transaction Ref / UTR / Cheque No.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = paymentNote,
                    onValueChange = { paymentNote = it },
                    label = { Text("Payment Remarks") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val amount = amountInput.toDoubleOrNull() ?: 0.0
                            if (amount <= 0.0) {
                                errorMessage = "Enter a valid amount greater than 0."
                                return@Button
                            }
                            if (amount > invoice.balanceDue) {
                                errorMessage = "Amount cannot exceed balance due (₹${invoice.balanceDue})."
                                return@Button
                            }
                            onRecord(amount, selectedMode, txnReference, paymentNote)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record Payment", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==============================================================================
// TAX INVOICE PRINTABLE / VIEWER DIALOG (PDF STYLE)
// ==============================================================================
@Composable
fun TaxInvoiceViewDialog(
    invoice: IspInvoice,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .shadow(12.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Top Action Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Official Tax Invoice",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Row {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = ElectricBlue)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }

                Divider(color = Color(0xFFE2E8F0))

                // Printable Invoice Sheet Body
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Business Header & Logo
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "K&B NET SERVICE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = ElectricBlue,
                                    letterSpacing = 1.sp
                                )
                                Text("High Speed Broadband & FTTH Telecom Solutions", fontSize = 11.sp, color = Color(0xFF475569))
                                Text("Plot 42, Telecom Commercial Hub, Sector 6", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text("GSTIN: 07AAAAA0000A1Z5 • State: Delhi (07)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                Text("Email: billing@kbnet.com • Support: +91 98765 43210", fontSize = 10.sp, color = Color(0xFF64748B))
                            }

                            // PAID / UNPAID STAMP
                            val stampColor = if (invoice.status == InvoiceStatus.PAID) MintEmerald else CoralRose
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, stampColor),
                                color = stampColor.copy(alpha = 0.08f),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = invoice.status.name,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.5.sp,
                                    color = stampColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Invoice Metadata Box
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("INVOICE NUMBER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Text(invoice.invoiceNumber, fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("INVOICE DATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Text(SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(Date(invoice.invoiceDateMillis)), fontSize = 11.sp, color = Color(0xFF0F172A))
                                }
                                Column {
                                    Text("BILLING PERIOD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Text("${invoice.billingPeriodStart} to ${invoice.billingPeriodEnd}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("PAYMENT DUE DATE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Text(SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(Date(invoice.dueDateMillis)), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoralRose)
                                }
                            }
                        }
                    }

                    // Bill To Customer Section
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("BILLED TO (CUSTOMER DETAILS)", fontSize = 9.sp, fontWeight = FontWeight.Black, color = ElectricBlue)
                                Text(invoice.customerName, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("Customer ID: ${invoice.customerId} • Mobile: ${invoice.customerPhone}", fontSize = 11.sp, color = Color(0xFF334155))
                                Text("Address: ${invoice.customerAddress}", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                        }
                    }

                    // Itemized Table
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Item Description", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(2f), color = Color(0xFF1E293B))
                                Text("SAC", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.8f), color = Color(0xFF1E293B))
                                Text("Qty", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.5f), color = Color(0xFF1E293B))
                                Text("Rate (₹)", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = Color(0xFF1E293B))
                                Text("Total (₹)", fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = Color(0xFF1E293B))
                            }

                            invoice.items.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}. ${item.description}", fontSize = 11.sp, modifier = Modifier.weight(2f), color = Color(0xFF0F172A))
                                    Text(item.hsnSacCode, fontSize = 10.sp, modifier = Modifier.weight(0.8f), color = Color(0xFF475569))
                                    Text("${item.quantity}", fontSize = 11.sp, modifier = Modifier.weight(0.5f), color = Color(0xFF0F172A))
                                    Text(String.format(Locale.ENGLISH, "%,.2f", item.unitRate), fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = Color(0xFF0F172A))
                                    Text(String.format(Locale.ENGLISH, "%,.2f", item.totalAmount), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = Color(0xFF0F172A))
                                }
                                Divider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }

                    // Totals & GST Breakdown
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(modifier = Modifier.width(260.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal:", fontSize = 11.sp, color = Color(0xFF475569))
                                Text("₹" + String.format(Locale.ENGLISH, "%,.2f", invoice.subtotal), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                            }
                            if (invoice.discountAmount > 0) {
                                Row(modifier = Modifier.width(260.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Discount:", fontSize = 11.sp, color = MintEmerald)
                                    Text("-₹" + String.format(Locale.ENGLISH, "%,.2f", invoice.discountAmount), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MintEmerald)
                                }
                            }
                            Row(modifier = Modifier.width(260.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CGST (${invoice.gstRatePercent / 2}%):", fontSize = 11.sp, color = Color(0xFF475569))
                                Text("₹" + String.format(Locale.ENGLISH, "%,.2f", invoice.cgstAmount), fontSize = 11.sp, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.width(260.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("SGST (${invoice.gstRatePercent / 2}%):", fontSize = 11.sp, color = Color(0xFF475569))
                                Text("₹" + String.format(Locale.ENGLISH, "%,.2f", invoice.sgstAmount), fontSize = 11.sp, color = Color(0xFF0F172A))
                            }
                            Divider(modifier = Modifier.width(260.dp).padding(vertical = 4.dp))
                            Row(modifier = Modifier.width(260.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TOTAL AMOUNT:", fontWeight = FontWeight.Black, fontSize = 13.sp, color = ElectricBlue)
                                Text("₹" + String.format(Locale.ENGLISH, "%,.2f", invoice.totalAmount), fontWeight = FontWeight.Black, fontSize = 14.sp, color = ElectricBlue)
                            }
                            Row(modifier = Modifier.width(260.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Paid Amount:", fontSize = 11.sp, color = MintEmerald)
                                Text("₹" + String.format(Locale.ENGLISH, "%,.2f", invoice.paidAmount), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MintEmerald)
                            }
                            Row(modifier = Modifier.width(260.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("BALANCE DUE:", fontWeight = FontWeight.Black, fontSize = 12.sp, color = CoralRose)
                                Text("₹" + String.format(Locale.ENGLISH, "%,.2f", invoice.balanceDue), fontWeight = FontWeight.Black, fontSize = 13.sp, color = CoralRose)
                            }
                        }
                    }

                    // Payments Recorded History
                    if (invoice.payments.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("PAYMENT RECEIPTS LOG", fontSize = 9.sp, fontWeight = FontWeight.Black, color = MintEmerald)
                                    invoice.payments.forEach { p ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${SimpleDateFormat("dd-MM-yy", Locale.ENGLISH).format(Date(p.paymentDateMillis))} • ${p.paymentMode.name} (${p.transactionReference})",
                                                fontSize = 10.sp,
                                                color = Color(0xFF334155)
                                            )
                                            Text(text = "₹${p.amount}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MintEmerald)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bank Details & Signatory Footer
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("BANK PAYMENT DETAILS:", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF475569))
                                Text("A/c Name: K&B NET SERVICE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Bank: HDFC Bank Ltd • A/c: 50200088991122", fontSize = 10.sp, color = Color(0xFF334155))
                                Text("IFSC: HDFC0001234 • UPI ID: kbnet@icici", fontSize = 10.sp, color = Color(0xFF334155))
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("For K&B NET SERVICE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("Authorized Signatory", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                // Bottom Buttons: Share WhatsApp + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share via WhatsApp / Email", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
