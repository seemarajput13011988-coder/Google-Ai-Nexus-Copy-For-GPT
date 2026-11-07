package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.example.ui.components.GpsRadarMap
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun AdminDashboardTab(
    repository: NexusRepository,
    onNavigateToTab: (Int) -> Unit,
    onOpenShiftOverride: () -> Unit,
    onOpenAddCustomer: () -> Unit,
    onOpenCreateTask: () -> Unit,
    onOpenAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val users by repository.users.collectAsState()
    val attendance by repository.attendanceRecords.collectAsState()
    val locations by repository.employeeLocations.collectAsState()
    val tasks by repository.tasks.collectAsState()
    val customers by repository.customers.collectAsState()
    val complaints by repository.complaints.collectAsState()
    val transactions by repository.transactions.collectAsState()

    val pendingRegistrations = users.filter { it.status == AccountStatus.PENDING_APPROVAL }
    val activeShifts = attendance.filter { it.status == ShiftStatus.ACTIVE_SHIFT }
    val liveTrackingCount = locations.count { it.isLive }
    val totalRevenue = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val pendingTasksCount = tasks.count { it.status != TaskStatus.COMPLETED }
    val expiredCustomersCount = customers.count { it.status == CustomerStatus.EXPIRED }
    val criticalComplaintsCount = complaints.count { it.status != ComplaintStatus.CLOSED && it.priority == ComplaintPriority.CRITICAL }

    var selectedMapEmployeeId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("admin_dashboard_lazy_column"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Pending Registrations Alert Banner
            if (pendingRegistrations.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldenAmber.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .border(1.5.dp, GoldenAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .testTag("pending_registrations_banner")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(GoldenAmber),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PendingActions, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Pending Registrations (${pendingRegistrations.size})",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            StatusBadge(text = "ACTION REQUIRED", color = GoldenAmber)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        pendingRegistrations.forEach { user ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "${user.designation} • ${user.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (user.registrationReason.isNotBlank()) {
                                            Text(text = "\"${user.registrationReason}\"", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { repository.approveEmployeeRegistration(user.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { repository.rejectEmployeeRegistration(user.id) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralRose),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Text(
                text = "Administrative Quick Actions",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Button(
                        onClick = onOpenShiftOverride,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.testTag("quick_shift_override_btn")
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shift Override", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    Button(
                        onClick = onOpenAddCustomer,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VividCyan),
                        modifier = Modifier.testTag("quick_add_customer_btn")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Customer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    Button(
                        onClick = onOpenCreateTask,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                        modifier = Modifier.testTag("quick_create_task_btn")
                    ) {
                        Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Assign Task", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    Button(
                        onClick = onOpenAddTransaction,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                        modifier = Modifier.testTag("quick_add_txn_btn")
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Voucher", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Operational Metrics Grid with Vibrant Cards
        item {
            Text(
                text = "Business Intelligence & KPIs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Active Shifts",
                        value = "${activeShifts.size}",
                        subtitle = "Staff on duty today",
                        icon = Icons.Default.Work,
                        iconTint = MintEmerald,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(1) }
                    )
                    StatCard(
                        title = "Live GPS Fleet",
                        value = "$liveTrackingCount",
                        subtitle = "Active beacons online",
                        icon = Icons.Default.GpsFixed,
                        iconTint = VividCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(1) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "ISP Customers",
                        value = "${customers.size}",
                        subtitle = "$expiredCustomersCount expired / renewal due",
                        icon = Icons.Default.Router,
                        iconTint = RoyalPurple,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(2) }
                    )
                    StatCard(
                        title = "Pending Tasks",
                        value = "$pendingTasksCount",
                        subtitle = "$criticalComplaintsCount urgent complaints",
                        icon = Icons.Default.Pending,
                        iconTint = GoldenAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(3) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Collections",
                        value = "₹${String.format("%,.0f", totalRevenue)}",
                        subtitle = "Income vouchers",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconTint = MintEmerald,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(4) }
                    )
                    StatCard(
                        title = "Net Profit (Tally)",
                        value = "₹${String.format("%,.0f", totalRevenue - totalExpenses)}",
                        subtitle = "Expenses: ₹${String.format("%,.0f", totalExpenses)}",
                        icon = Icons.Default.AccountBalance,
                        iconTint = ElectricBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(4) }
                    )
                }
            }
        }

        // Live GPS Fleet Radar
        item {
            GpsRadarMap(
                locations = locations,
                selectedEmployeeId = selectedMapEmployeeId,
                onSelectEmployee = { selectedMapEmployeeId = it.employeeId }
            )
        }

        // Currently On-Shift Technicians
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Staff Currently On Shift",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { onNavigateToTab(1) }) {
                    Text("View Attendance Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (activeShifts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    Text(
                        text = "No staff members are currently on shift. Admin can start shifts using 'Shift Override'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                activeShifts.forEach { shift ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
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
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = shift.employeeName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusBadge(text = "ON DUTY", color = MintEmerald)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Started at ${NexusRepository.formatKolkataTime(shift.shiftStartTimeMillis)} • Asia/Kolkata",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    repository.adminEndShiftOverride(
                                        recordId = shift.id,
                                        customEndMillis = System.currentTimeMillis(),
                                        reason = "Super Admin manual end override"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralRose),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("End Shift", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
