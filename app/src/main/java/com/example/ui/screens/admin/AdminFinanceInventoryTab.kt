package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.NexusRepository
import com.example.ui.components.EquipmentCategoryLogo
import com.example.ui.components.PaymentModeLogo
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun AdminFinanceInventoryTab(
    repository: NexusRepository,
    onOpenAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by repository.transactions.collectAsState()
    val salaries by repository.salaries.collectAsState()
    val inventory by repository.inventory.collectAsState()

    var sectionTab by remember { mutableIntStateOf(0) } // 0: Cashbook & Ledger, 1: Salaries, 2: Inventory
    var showAddStockDialog by remember { mutableStateOf<InventoryItem?>(null) }
    var stockChangeAmount by remember { mutableStateOf("5") }

    val totalIncome = repository.getTotalIncome()
    val totalExpenses = repository.getTotalExpenses()
    val netProfit = repository.getNetProfit()

    val lowStockItems = inventory.filter { it.availableStock <= it.minThresholdAlert }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("admin_finance_lazy_column"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Sub Tab Row
            TabRow(
                selectedTabIndex = sectionTab,
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .testTag("finance_section_tab_row"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ElectricBlue
            ) {
                Tab(
                    selected = sectionTab == 0,
                    onClick = { sectionTab = 0 },
                    text = { Text("Tally Cashbook", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = sectionTab == 1,
                    onClick = { sectionTab = 1 },
                    text = { Text("Payroll Salaries", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = sectionTab == 2,
                    onClick = { sectionTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Inventory", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            if (lowStockItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = CoralRose
                                ) {
                                    Text(
                                        text = "${lowStockItems.size}",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        when (sectionTab) {
            0 -> {
                // Cashbook & Ledger Overview
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Financial Ledger & Cashbook",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Tally-Compatible Multi-Ledger Recording",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onOpenAddTransaction,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Voucher", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Total Inflow",
                            value = "₹${String.format("%,.0f", totalIncome)}",
                            subtitle = "Renewals & Installs",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            iconTint = MintEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Total Outflow",
                            value = "₹${String.format("%,.0f", totalExpenses)}",
                            subtitle = "Uplink & Purchases",
                            icon = Icons.AutoMirrored.Filled.TrendingDown,
                            iconTint = CoralRose,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Net Profit",
                            value = "₹${String.format("%,.0f", netProfit)}",
                            subtitle = "P&L Balance",
                            icon = Icons.Default.AccountBalance,
                            iconTint = ElectricBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Transaction Vouchers with Proper Payment Logos
                items(transactions, key = { it.id }) { txn ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                PaymentModeLogo(mode = txn.paymentMode, size = 36.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(
                                            text = txn.type.name,
                                            color = if (txn.type == TransactionType.INCOME) MintEmerald else CoralRose
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = txn.category,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = txn.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${txn.id} • ${NexusRepository.formatKolkataDateTime(txn.dateMillis)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Text(
                                text = (if (txn.type == TransactionType.INCOME) "+₹" else "-₹") + String.format("%,.0f", txn.amount),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (txn.type == TransactionType.INCOME) MintEmerald else CoralRose
                            )
                        }
                    }
                }
            }

            1 -> {
                // Payroll Salaries
                item {
                    Text(
                        text = "Monthly Staff Salary Slips",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Calculated against verified attendance, working hours, advances and bonuses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(salaries, key = { it.id }) { sal ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = sal.employeeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "Pay Month: ${sal.monthString}", style = MaterialTheme.typography.labelSmall, color = ElectricBlue, fontWeight = FontWeight.Bold)
                                }
                                StatusBadge(text = sal.status, color = MintEmerald)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Base Salary: ₹${sal.baseSalary.toInt()}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Days Worked: ${sal.daysWorked} days (${sal.totalShiftHours} hrs)", style = MaterialTheme.typography.bodySmall)
                                    if (sal.advanceDeduction > 0) {
                                        Text(text = "Advance Deductions: -₹${sal.advanceDeduction.toInt()}", style = MaterialTheme.typography.labelSmall, color = CoralRose, fontWeight = FontWeight.Bold)
                                    }
                                    if (sal.bonusAmount > 0) {
                                        Text(text = "Performance Bonus: +₹${sal.bonusAmount.toInt()}", style = MaterialTheme.typography.labelSmall, color = MintEmerald, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Net Payout", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "₹${sal.netSalary.toInt()}",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MintEmerald
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Hardware Inventory with Equipment Logos
                if (lowStockItems.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CoralRose.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CoralRose.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = CoralRose)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alert: ${lowStockItems.size} items have fallen below minimum threshold! Reorder immediately.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CoralRose,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Warehouse & Field Stock Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                items(inventory, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Proper Equipment Category Logo
                                EquipmentCategoryLogo(category = item.category, size = 42.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (item.availableStock <= item.minThresholdAlert) {
                                            StatusBadge(text = "LOW STOCK", color = CoralRose)
                                        }
                                    }
                                    Text(
                                        text = "${item.id} • ${item.category.name.replace("_", " ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Total Stock: ${item.totalStock} ${item.unit} • Field Issued: ${item.issuedStock} ${item.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${item.availableStock} ${item.unit}",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (item.availableStock <= item.minThresholdAlert) CoralRose else MintEmerald
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { showAddStockDialog = item },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Add Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add Stock Dialog
    if (showAddStockDialog != null) {
        val targetItem = showAddStockDialog!!
        AlertDialog(
            onDismissRequest = { showAddStockDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EquipmentCategoryLogo(category = targetItem.category, size = 30.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Stock")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Item: ${targetItem.name}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Current Available: ${targetItem.availableStock} ${targetItem.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = stockChangeAmount,
                        onValueChange = { stockChangeAmount = it },
                        label = { Text("Quantity to Add (${targetItem.unit})") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = stockChangeAmount.toIntOrNull() ?: 0
                        if (qty > 0) {
                            repository.updateStock(targetItem.id, qty)
                        }
                        showAddStockDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Add to Inventory", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStockDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
