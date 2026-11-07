package com.example.ui.screens.admin

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.NexusRepository
import com.example.ui.components.GpsRadarMap
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun AdminAttendanceGpsTab(
    repository: NexusRepository,
    onOpenShiftOverride: () -> Unit,
    modifier: Modifier = Modifier
) {
    val users by repository.users.collectAsState()
    val attendance by repository.attendanceRecords.collectAsState()
    val locations by repository.employeeLocations.collectAsState()
    val breadcrumbs by repository.locationBreadcrumbs.collectAsState()

    var selectedMapEmployeeId by remember { mutableStateOf<String?>(null) }
    var employeeForRouteHistory by remember { mutableStateOf<User?>(null) }
    var correctionTargetRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var correctionReason by remember { mutableStateOf("") }
    var selectedFilterDate by remember { mutableStateOf<String?>(null) }

    val staffEmployees = remember(users) {
        users.filter { it.role == UserRole.EMPLOYEE && it.status == AccountStatus.APPROVED }
    }

    val filteredRecords = remember(attendance, selectedFilterDate) {
        if (selectedFilterDate == null) attendance else attendance.filter { it.dateIso == selectedFilterDate }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("admin_attendance_lazy_column"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Action Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Attendance & GPS Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Asia/Kolkata Timezone • Live Tracking & Overrides",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onOpenShiftOverride,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("admin_override_button")
                ) {
                    Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shift Override", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Live Fleet Radar
        item {
            GpsRadarMap(
                locations = locations,
                selectedEmployeeId = selectedMapEmployeeId,
                onSelectEmployee = { selectedMapEmployeeId = it.employeeId }
            )
        }

        // Today's Staff Duty Roster
        item {
            Text(
                text = "Today's Workforce Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))

            staffEmployees.forEach { emp ->
                val todayShift = repository.getTodayShiftForEmployee(emp.id)
                val loc = locations.firstOrNull { it.employeeId == emp.id }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .testTag("staff_roster_${emp.id}")
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
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(ElectricBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emp.avatarEmoji, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = emp.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text(text = emp.designation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (todayShift != null) {
                                when (todayShift.status) {
                                    ShiftStatus.ACTIVE_SHIFT -> StatusBadge(text = "ON DUTY (ACTIVE)", color = MintEmerald)
                                    ShiftStatus.COMPLETED -> StatusBadge(text = "SHIFT ENDED", color = RoyalPurple)
                                    ShiftStatus.AUTO_CLOSED -> StatusBadge(text = "AUTO-CLOSED", color = GoldenAmber)
                                    ShiftStatus.ADMIN_OVERRIDE -> StatusBadge(text = "ADMIN OVERRIDE", color = ElectricBlue)
                                }
                            } else {
                                StatusBadge(text = "NOT STARTED", color = CoralRose)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (todayShift != null) {
                                    Text(
                                        text = "In: ${NexusRepository.formatKolkataTime(todayShift.shiftStartTimeMillis)}" +
                                                if (todayShift.shiftEndTimeMillis != null) " • Out: ${NexusRepository.formatKolkataTime(todayShift.shiftEndTimeMillis!!)}" else " • Ongoing",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (todayShift.adminOverrideReason.isNotBlank()) {
                                        Text(
                                            text = "Note: ${todayShift.adminOverrideReason}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldenAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "No shift clocked yet today (Normal shift window: 8 AM - 7 PM)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (loc != null) {
                                    Text(
                                        text = "GPS: ${if (loc.isLive) "Live (${loc.areaDescription})" else "Last known (${loc.areaDescription})"} • Battery ${loc.batteryPercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (loc.isLive) MintEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (loc.isLive) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { employeeForRouteHistory = emp },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(14.dp), tint = ElectricBlue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Route", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                                }

                                if (todayShift != null && todayShift.status == ShiftStatus.ACTIVE_SHIFT) {
                                    Button(
                                        onClick = {
                                            repository.adminEndShiftOverride(
                                                recordId = todayShift.id,
                                                customEndMillis = System.currentTimeMillis(),
                                                reason = "Super Admin manual end override"
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CoralRose),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("End", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (todayShift == null) {
                                    Button(
                                        onClick = {
                                            repository.adminStartShiftOverride(
                                                employee = emp,
                                                customStartMillis = System.currentTimeMillis(),
                                                reason = "Super Admin manual start override"
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Start", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Attendance History & Audit Trail
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Records & Audit Trail",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (selectedFilterDate != null) {
                    TextButton(onClick = { selectedFilterDate = null }) {
                        Text("Show All Dates", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            filteredRecords.forEach { record ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .shadow(2.dp, RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = record.employeeName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Date: ${record.dateIso}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusBadge(
                                    text = record.status.name.replace("_", " "),
                                    color = when (record.status) {
                                        ShiftStatus.ACTIVE_SHIFT -> MintEmerald
                                        ShiftStatus.COMPLETED -> RoyalPurple
                                        ShiftStatus.AUTO_CLOSED -> GoldenAmber
                                        ShiftStatus.ADMIN_OVERRIDE -> ElectricBlue
                                    }
                                )

                                IconButton(
                                    onClick = {
                                        correctionTargetRecord = record
                                        correctionReason = record.adminOverrideReason
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Correct Record",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Clock In: ${NexusRepository.formatKolkataTime(record.shiftStartTimeMillis)}" +
                                    if (record.shiftEndTimeMillis != null) " • Clock Out: ${NexusRepository.formatKolkataTime(record.shiftEndTimeMillis)}" else " • Ongoing" +
                                            " • Duration: ${record.durationMinutes / 60}h ${record.durationMinutes % 60}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (record.adminOverrideReason.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GoldenAmber.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, GoldenAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = "Correction / Override Note: ${record.adminOverrideReason}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldenAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(6.dp)
                                )
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

    // Attendance Correction Dialog
    if (correctionTargetRecord != null) {
        val target = correctionTargetRecord!!
        AlertDialog(
            onDismissRequest = { correctionTargetRecord = null },
            title = { Text("Correct Attendance Record") },
            text = {
                Column {
                    Text(
                        text = "Employee: ${target.employeeName} (${target.dateIso})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Original Shift: ${NexusRepository.formatKolkataTime(target.shiftStartTimeMillis)} - ${if (target.shiftEndTimeMillis != null) NexusRepository.formatKolkataTime(target.shiftEndTimeMillis) else "Active"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = correctionReason,
                        onValueChange = { correctionReason = it },
                        label = { Text("Mandatory Correction Reason") },
                        placeholder = { Text("e.g. Field power outage / server clock adjustment") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val endMillis = target.shiftEndTimeMillis ?: (target.shiftStartTimeMillis + 8 * 3600000)
                        repository.adminCorrectAttendance(
                            recordId = target.id,
                            newStartMillis = target.shiftStartTimeMillis,
                            newEndMillis = endMillis,
                            reason = correctionReason.ifBlank { "Super Admin audit correction" }
                        )
                        correctionTargetRecord = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Save Correction", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { correctionTargetRecord = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // GPS Breadcrumb Route History Dialog
    employeeForRouteHistory?.let { emp ->
        val empBreadcrumbs = breadcrumbs.filter { it.employeeId == emp.id }
        val loc = locations.firstOrNull { it.employeeId == emp.id }

        AlertDialog(
            onDismissRequest = { employeeForRouteHistory = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Route, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(emp.fullName, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("GPS Tracking & Route History", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    // Status Badge Banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (loc?.isLive == true) MintEmerald.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (loc?.isLive == true) MintEmerald.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (loc?.isLive == true) "● LIVE (Tracking Active)" else "○ LAST KNOWN LOCATION",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = if (loc?.isLive == true) MintEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Accuracy: ±${loc?.accuracyMeters ?: 5.0f}m • Battery: ${loc?.batteryPercent ?: 80}%",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = loc?.areaDescription ?: "Area N/A",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Today's Recorded Movement Breadcrumbs:", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (empBreadcrumbs.isEmpty()) {
                        Text(
                            text = "No route breadcrumbs recorded for this staff member yet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(empBreadcrumbs) { bc ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(ElectricBlue)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = bc.areaNote.ifBlank { "GPS Ping Checkpoint" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "Lat: ${String.format(Locale.ENGLISH, "%.4f", bc.latitude)}, Lng: ${String.format(Locale.ENGLISH, "%.4f", bc.longitude)} (±${bc.accuracyMeters}m)",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = NexusRepository.formatKolkataTime(bc.timestampMillis),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { employeeForRouteHistory = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Close")
                }
            }
        )
    }
}
