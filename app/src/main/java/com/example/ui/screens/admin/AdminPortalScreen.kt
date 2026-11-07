package com.example.ui.screens.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.data.repository.NexusRepository
import com.example.ui.components.StatusBadge
import com.example.ui.screens.media.MediaHubScreen
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    repository: NexusRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by repository.currentUser.collectAsState()
    val users by repository.users.collectAsState()
    val plans by repository.plans.collectAsState()

    var currentTab by remember { mutableIntStateOf(0) }
    var opsSubTab by remember { mutableIntStateOf(0) }

    // Dialog controllers
    var showShiftOverrideDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }

    val staffEmployees = remember(users) {
        users.filter { it.role == UserRole.EMPLOYEE && it.status == AccountStatus.APPROVED }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_portal_root"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_kb_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "K&B NET SERVICE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = CyanPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusBadge(
                                    text = if (currentUser?.role == UserRole.SUPER_ADMIN) "SUPER ADMIN" else "ADMIN",
                                    color = if (currentUser?.role == UserRole.SUPER_ADMIN) AmberWarning else CyanPrimary
                                )
                            }
                            Text(
                                text = "Nexus Enterprise Control Portal",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    val isDarkMode by repository.isDarkMode.collectAsState()
                    IconButton(
                        onClick = { repository.toggleDarkMode() },
                        modifier = Modifier.testTag("admin_theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme Mode",
                            tint = if (isDarkMode) GoldenAmber else RoyalPurple
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("admin_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Switch Profile / Sign Out",
                            tint = CoralRose
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("admin_bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Overview", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label = { Text("GPS/Shift", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                    label = { Text("Invoices", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.PermMedia, contentDescription = null) },
                    label = { Text("Media", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("CRM/Plans", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == 5,
                    onClick = { currentTab = 5 },
                    icon = { Icon(Icons.Default.Layers, contentDescription = null) },
                    label = { Text("Operations", fontSize = 10.sp) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> AdminDashboardTab(
                    repository = repository,
                    onNavigateToTab = { target ->
                        when (target) {
                            1 -> currentTab = 1
                            2 -> currentTab = 4 // CRM
                            3 -> { currentTab = 5; opsSubTab = 0 } // Tasks
                            4 -> { currentTab = 5; opsSubTab = 1 } // Finance
                            5 -> { currentTab = 5; opsSubTab = 2 } // Ops
                            6 -> currentTab = 2 // Invoices
                            7 -> currentTab = 3 // Media
                            else -> currentTab = target.coerceIn(0, 5)
                        }
                    },
                    onOpenShiftOverride = { showShiftOverrideDialog = true },
                    onOpenAddCustomer = { showAddCustomerDialog = true },
                    onOpenCreateTask = { showCreateTaskDialog = true },
                    onOpenAddTransaction = { showAddTransactionDialog = true }
                )
                1 -> AdminAttendanceGpsTab(
                    repository = repository,
                    onOpenShiftOverride = { showShiftOverrideDialog = true }
                )
                2 -> AdminInvoicingTab(
                    repository = repository
                )
                3 -> MediaHubScreen(
                    repository = repository
                )
                4 -> AdminCrmPlansTab(
                    repository = repository,
                    onOpenAddCustomer = { showAddCustomerDialog = true }
                )
                5 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = opsSubTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = ElectricBlue,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Tab(
                                selected = opsSubTab == 0,
                                onClick = { opsSubTab = 0 },
                                text = { Text("Tasks & Field", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                            )
                            Tab(
                                selected = opsSubTab == 1,
                                onClick = { opsSubTab = 1 },
                                text = { Text("Cashbook & Stock", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                            )
                            Tab(
                                selected = opsSubTab == 2,
                                onClick = { opsSubTab = 2 },
                                text = { Text("Complaints & Team", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (opsSubTab) {
                                0 -> AdminTasksTab(
                                    repository = repository,
                                    onOpenCreateTask = { showCreateTaskDialog = true }
                                )
                                1 -> AdminFinanceInventoryTab(
                                    repository = repository,
                                    onOpenAddTransaction = { showAddTransactionDialog = true }
                                )
                                2 -> AdminOperationsTab(
                                    repository = repository
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Shift Override Dialog
    if (showShiftOverrideDialog) {
        var selectedStaff by remember { mutableStateOf(staffEmployees.firstOrNull()) }
        var overrideReason by remember { mutableStateOf("") }
        var isStartAction by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showShiftOverrideDialog = false },
            title = { Text("Admin Shift Override") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Allows Super Admin to start/end employee shifts outside the normal 8:00 AM - 7:00 PM rule with recorded audit history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isStartAction,
                            onClick = { isStartAction = true },
                            label = { Text("Start Shift Override") }
                        )
                        FilterChip(
                            selected = !isStartAction,
                            onClick = { isStartAction = false },
                            label = { Text("End Shift Override") }
                        )
                    }

                    Text("Select Employee:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    staffEmployees.forEach { emp ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selectedStaff?.id == emp.id) CyanPrimary.copy(alpha = 0.1f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            RadioButton(
                                selected = selectedStaff?.id == emp.id,
                                onClick = { selectedStaff = emp }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "${emp.fullName} (${emp.designation})", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    OutlinedTextField(
                        value = overrideReason,
                        onValueChange = { overrideReason = it },
                        label = { Text("Override Audit Reason") },
                        placeholder = { Text("e.g. Night backbone restoration at POP") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emp = selectedStaff
                        if (emp != null) {
                            if (isStartAction) {
                                repository.adminStartShiftOverride(
                                    employee = emp,
                                    customStartMillis = System.currentTimeMillis(),
                                    reason = overrideReason.ifBlank { "Super Admin off-hours override start" }
                                )
                            } else {
                                val todayShift = repository.getTodayShiftForEmployee(emp.id)
                                if (todayShift != null) {
                                    repository.adminEndShiftOverride(
                                        recordId = todayShift.id,
                                        customEndMillis = System.currentTimeMillis(),
                                        reason = overrideReason.ifBlank { "Super Admin off-hours override end" }
                                    )
                                }
                            }
                        }
                        showShiftOverrideDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text(if (isStartAction) "Authorize Start Shift" else "Authorize End Shift")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShiftOverrideDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Add Customer Dialog
    if (showAddCustomerDialog) {
        var custName by remember { mutableStateOf("") }
        var custPhone by remember { mutableStateOf("") }
        var custAddress by remember { mutableStateOf("") }
        var custZone by remember { mutableStateOf("Sector 4") }
        var selectedPlan by remember { mutableStateOf(plans.firstOrNull()) }
        var connType by remember { mutableStateOf("FTTH Fiber") }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Register New ISP Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = custName,
                        onValueChange = { custName = it },
                        label = { Text("Customer Name / Business") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = custPhone,
                        onValueChange = { custPhone = it },
                        label = { Text("Phone Number (+91)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = custAddress,
                        onValueChange = { custAddress = it },
                        label = { Text("Installation Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = custZone,
                        onValueChange = { custZone = it },
                        label = { Text("Area / Sector Zone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Selected Plan:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${selectedPlan?.name} (₹${selectedPlan?.monthlyPriceInr?.toInt()}/mo)",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (custName.isNotBlank() && custPhone.isNotBlank()) {
                            val plan = selectedPlan ?: plans.first()
                            val nowIso = NexusRepository.getKolkataDateIso()
                            val cal = NexusRepository.getKolkataCalendar()
                            cal.add(java.util.Calendar.DAY_OF_YEAR, 30)
                            val expIso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(cal.time)

                            val newCust = IspCustomer(
                                id = "KB-CUST-" + (100 + (repository.customers.value.size + 1)),
                                fullName = custName.trim(),
                                phone = custPhone.trim(),
                                address = custAddress.trim(),
                                areaZone = custZone.trim(),
                                provider = plan.provider,
                                planId = plan.id,
                                planName = plan.name,
                                speedMbps = plan.speedMbps,
                                monthlyCharges = plan.monthlyPriceInr,
                                installationDate = nowIso,
                                expiryDate = expIso,
                                connectionType = connType,
                                status = CustomerStatus.ACTIVE
                            )
                            repository.addCustomer(newCust)
                            showAddCustomerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Save Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Create Task Dialog
    if (showCreateTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var taskDesc by remember { mutableStateOf("") }
        var selectedEmp by remember { mutableStateOf(staffEmployees.firstOrNull()) }
        var taskPriority by remember { mutableStateOf(TaskPriority.HIGH) }
        var taskLoc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateTaskDialog = false },
            title = { Text("Assign Work / Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Title") },
                        placeholder = { Text("e.g. Splicing drop fiber at Sector 3 Pole 12") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = taskDesc,
                        onValueChange = { taskDesc = it },
                        label = { Text("Work Details & Instructions") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = taskLoc,
                        onValueChange = { taskLoc = it },
                        label = { Text("Location Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Assign To:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    staffEmployees.forEach { emp ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = selectedEmp?.id == emp.id,
                                onClick = { selectedEmp = emp }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${emp.fullName} (${emp.designation})", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emp = selectedEmp ?: staffEmployees.first()
                        if (taskTitle.isNotBlank()) {
                            val newTask = TaskItem(
                                id = "tsk_" + System.currentTimeMillis().toString().takeLast(6),
                                title = taskTitle.trim(),
                                description = taskDesc.trim(),
                                assignedEmployeeId = emp.id,
                                assignedEmployeeName = emp.fullName,
                                createdBy = currentUser?.fullName ?: "Super Admin",
                                priority = taskPriority,
                                status = TaskStatus.PENDING,
                                deadlineMillis = System.currentTimeMillis() + (6 * 3600000),
                                locationAddress = taskLoc.trim()
                            )
                            repository.addTask(newTask)
                            showCreateTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Assign Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTaskDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Add Transaction Voucher Dialog (Tally Inspired)
    if (showAddTransactionDialog) {
        var txnType by remember { mutableStateOf(TransactionType.INCOME) }
        var txnCategory by remember { mutableStateOf("Customer Renewal") }
        var txnAmount by remember { mutableStateOf("1500") }
        var txnDesc by remember { mutableStateOf("") }
        var txnMode by remember { mutableStateOf(PaymentMode.UPI) }

        AlertDialog(
            onDismissRequest = { showAddTransactionDialog = false },
            title = { Text("Record Accounting Voucher") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = txnType == TransactionType.INCOME,
                            onClick = {
                                txnType = TransactionType.INCOME
                                txnCategory = "Customer Renewal"
                            },
                            label = { Text("Income (+)") }
                        )
                        FilterChip(
                            selected = txnType == TransactionType.EXPENSE,
                            onClick = {
                                txnType = TransactionType.EXPENSE
                                txnCategory = "Equipment Purchase"
                            },
                            label = { Text("Expense (-)") }
                        )
                    }

                    OutlinedTextField(
                        value = txnCategory,
                        onValueChange = { txnCategory = it },
                        label = { Text("Ledger Category") },
                        placeholder = { Text("e.g. Bandwidth / Equipment / Renewal") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = txnAmount,
                        onValueChange = { txnAmount = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = txnDesc,
                        onValueChange = { txnDesc = it },
                        label = { Text("Voucher Narration / Note") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment Mode:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PaymentMode.values().forEach { mode ->
                            FilterChip(
                                selected = txnMode == mode,
                                onClick = { txnMode = mode },
                                label = { Text(mode.name, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = txnAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val newTxn = AccountingTransaction(
                                id = "TXN-" + System.currentTimeMillis().toString().takeLast(8),
                                type = txnType,
                                category = txnCategory.trim(),
                                amount = amount,
                                paymentMode = txnMode,
                                dateMillis = System.currentTimeMillis(),
                                description = txnDesc.trim().ifBlank { "$txnCategory transaction" },
                                recordedBy = currentUser?.fullName ?: "Super Admin"
                            )
                            repository.addTransaction(newTxn)
                            showAddTransactionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Save to Cashbook")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTransactionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
