package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.NexusRepository
import com.example.ui.components.IspProviderLogo
import com.example.ui.components.PaymentModeLogo
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun AdminCrmPlansTab(
    repository: NexusRepository,
    onOpenAddCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customers by repository.customers.collectAsState()
    val plans by repository.plans.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedProviderFilter by remember { mutableStateOf("ALL") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var showPlanDialog by remember { mutableStateOf(false) }

    // Renew dialog state
    var renewingCustomer by remember { mutableStateOf<IspCustomer?>(null) }
    var renewalPaymentMode by remember { mutableStateOf(PaymentMode.UPI) }

    val providersList = remember(plans) {
        listOf("ALL") + plans.map { it.provider }.distinct()
    }

    val filteredCustomers = remember(customers, searchQuery, selectedProviderFilter, selectedStatusFilter) {
        customers.filter { cust ->
            val matchesSearch = cust.fullName.contains(searchQuery, ignoreCase = true) ||
                    cust.phone.contains(searchQuery) ||
                    cust.id.contains(searchQuery, ignoreCase = true) ||
                    cust.address.contains(searchQuery, ignoreCase = true)

            val matchesProvider = selectedProviderFilter == "ALL" || cust.provider == selectedProviderFilter
            val matchesStatus = selectedStatusFilter == "ALL" || cust.status.name == selectedStatusFilter

            matchesSearch && matchesProvider && matchesStatus
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("admin_crm_lazy_column"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Header & Add buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ISP Customers & Plans (CRM)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${customers.size} Total Subscribers • Live Plan & Billing Sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onOpenAddCustomer,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_customer_button")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Customer", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search customer name, phone, ID, or sector...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricBlue) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("crm_search_field"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
        }

        // Provider Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(providersList) { provider ->
                    FilterChip(
                        selected = selectedProviderFilter == provider,
                        onClick = { selectedProviderFilter = provider },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (provider != "ALL") {
                                    IspProviderLogo(provider = provider, size = 18.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(if (provider == "ALL") "All Providers" else provider, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }

        // Customer Cards List with Proper Logos
        items(filteredCustomers, key = { it.id }) { customer ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .testTag("customer_card_${customer.id}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Proper Official ISP Provider Logo
                            IspProviderLogo(provider = customer.provider, size = 44.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = customer.fullName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusBadge(
                                        text = customer.status.name,
                                        color = when (customer.status) {
                                            CustomerStatus.ACTIVE -> MintEmerald
                                            CustomerStatus.EXPIRED -> CoralRose
                                            CustomerStatus.SUSPENDED -> GoldenAmber
                                            CustomerStatus.PENDING_INSTALL -> VividCyan
                                            CustomerStatus.INACTIVE -> Color.Gray
                                        }
                                    )
                                }
                                Text(
                                    text = "${customer.id} • ${customer.provider} • ${customer.connectionType}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { renewingCustomer = customer },
                            colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("renew_btn_${customer.id}")
                        ) {
                            Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Renew", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Plan: ${customer.planName} (${customer.speedMbps} Mbps)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Address: ${customer.address}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Contact: ${customer.phone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹${customer.monthlyCharges.toInt()}/mo",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleLarge,
                                color = ElectricBlue
                            )
                            Text(
                                text = "Expiry: ${customer.expiryDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (customer.status == CustomerStatus.EXPIRED) CoralRose else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (customer.status == CustomerStatus.EXPIRED) FontWeight.Black else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "IP: ${customer.ipAddress}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text(text = "ONT MAC: ${customer.onuMacAddress}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // ISP Plan Packages Section with Brand Logos
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configured ISP Broadband Plans",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(onClick = { showPlanDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(plans) { plan ->
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
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IspProviderLogo(provider = plan.provider, size = 42.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = plan.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusBadge(text = "${plan.speedMbps} Mbps", color = VividCyan)
                            }
                            Text(
                                text = "${plan.provider} • ${plan.dataLimit} • Validity: ${plan.validityDays} days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (plan.iptvIncluded) {
                                Text(
                                    text = "📺 Includes IPTV with ${plan.channelCount}+ Live HD Channels",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MintEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹${plan.monthlyPriceInr.toInt()}",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge,
                            color = ElectricBlue
                        )
                        Text(
                            text = "+18% GST",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Subscription Renewal Dialog with Proper Payment Logos
    if (renewingCustomer != null) {
        val cust = renewingCustomer!!
        AlertDialog(
            onDismissRequest = { renewingCustomer = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IspProviderLogo(provider = cust.provider, size = 32.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Subscription Renewal")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Customer: ${cust.fullName} (${cust.id})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Plan: ${cust.planName} (${cust.speedMbps} Mbps)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Renewal Amount: ₹${(cust.monthlyCharges * 1.18).toInt()} (incl. 18% GST)",
                        fontWeight = FontWeight.Black,
                        color = MintEmerald,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Select Payment Mode:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(6.dp))

                    PaymentMode.values().forEach { mode ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (renewalPaymentMode == mode) ElectricBlue.copy(alpha = 0.1f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { renewalPaymentMode = mode }
                                .border(
                                    width = if (renewalPaymentMode == mode) 1.5.dp else 0.5.dp,
                                    color = if (renewalPaymentMode == mode) ElectricBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = renewalPaymentMode == mode,
                                    onClick = { renewalPaymentMode = mode }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                PaymentModeLogo(mode = mode, size = 28.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = when (mode) {
                                        PaymentMode.UPI -> "UPI (PhonePe, GPay, Paytm)"
                                        PaymentMode.CASH -> "Cash Collection"
                                        PaymentMode.BANK_TRANSFER -> "Bank Transfer (NEFT / IMPS)"
                                        PaymentMode.CHEQUE -> "Cheque Voucher"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ Automatically extends validity +30 days\n✓ Automatically creates Cashbook Income Voucher",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.recordCustomerRenewal(cust.id, renewalPaymentMode)
                        renewingCustomer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintEmerald)
                ) {
                    Text("Collect & Renew", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renewingCustomer = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Plan Dialog
    if (showPlanDialog) {
        var planName by remember { mutableStateOf("") }
        var provider by remember { mutableStateOf("RailWire / RailTel") }
        var speedText by remember { mutableStateOf("100") }
        var priceText by remember { mutableStateOf("699") }
        var iptvIncluded by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showPlanDialog = false },
            title = { Text("Add New ISP Plan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = planName,
                        onValueChange = { planName = it },
                        label = { Text("Plan Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        label = { Text("Provider (e.g. BSNL, RailWire, Air Fibre)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = speedText,
                            onValueChange = { speedText = it },
                            label = { Text("Speed (Mbps)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Price (₹/mo)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = iptvIncluded, onCheckedChange = { iptvIncluded = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Include Free IPTV & Channels", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (planName.isNotBlank()) {
                            val newPlan = IspPlan(
                                id = "plan_" + System.currentTimeMillis().toString().takeLast(6),
                                name = planName.trim(),
                                provider = provider.trim(),
                                speedMbps = speedText.toIntOrNull() ?: 100,
                                monthlyPriceInr = priceText.toDoubleOrNull() ?: 699.0,
                                iptvIncluded = iptvIncluded,
                                channelCount = if (iptvIncluded) 350 else 0
                            )
                            repository.addOrUpdatePlan(newPlan)
                            showPlanDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Save Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
