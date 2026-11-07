package com.example.ui.screens.employee

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.*
import com.example.data.repository.NexusRepository
import com.example.service.KbLocationService
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.screens.media.MediaHubScreen
import com.example.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeePortalScreen(
    repository: NexusRepository,
    employee: User,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableIntStateOf(0) }

    val attendanceRecords by repository.attendanceRecords.collectAsState()
    val tasks by repository.tasks.collectAsState()
    val complaints by repository.complaints.collectAsState()
    val chats by repository.chatMessages.collectAsState()
    val requests by repository.requests.collectAsState()
    val salaries by repository.salaries.collectAsState()

    val todayShift = remember(attendanceRecords, employee.id) {
        repository.getTodayShiftForEmployee(employee.id)
    }

    val myTasks = remember(tasks, employee.id) {
        tasks.filter { it.assignedEmployeeId == employee.id }
    }

    val myComplaints = remember(complaints, employee.id) {
        complaints.filter { it.assignedTechnicianId == employee.id }
    }

    val myRequests = remember(requests, employee.id) {
        requests.filter { it.employeeId == employee.id }
    }

    val mySalarySlip = remember(salaries, employee.id) {
        salaries.firstOrNull { it.employeeId == employee.id }
    }

    var gpsError by remember { mutableStateOf<String?>(null) }
    var isStartingShift by remember { mutableStateOf(false) }
    var isEndingShift by remember { mutableStateOf(false) }
    val employeeLocations by repository.employeeLocations.collectAsState()
    val myLocation = remember(employeeLocations, employee.id) {
        employeeLocations.firstOrNull { it.employeeId == employee.id }
    }

    // A shift is created only after a real device GPS fix is received.
    val beginShiftWithGps: () -> Unit = {
        gpsError = null
        isStartingShift = true
        val locationClient = LocationServices.getFusedLocationProviderClient(context)
        locationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            isStartingShift = false
            if (location == null || location.accuracy > 100f) {
                gpsError = "A reliable GPS fix is not available yet. Turn on Location and try again."
            } else {
                val result = repository.startEmployeeShift(employee, location.latitude, location.longitude)
                result.onSuccess { shift ->
                    KbLocationService.start(context, employee.id, employee.fullName, shift.id)
                }.onFailure { error ->
                    gpsError = error.message ?: "Unable to start shift."
                }
            }
        }.addOnFailureListener { error ->
            isStartingShift = false
            gpsError = error.message ?: "Unable to read the device location."
        }
    }

    // Request location and notification permissions before starting the foreground service.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            beginShiftWithGps()
        } else {
            gpsError = "Location permission is required to start an attendance shift."
        }
    }

    LaunchedEffect(todayShift?.id, todayShift?.status) {
        if (todayShift?.status == ShiftStatus.ACTIVE_SHIFT) {
            KbLocationService.start(context, employee.id, employee.fullName, todayShift.id)
        } else {
            KbLocationService.stop(context)
        }
    }

    // Timer tick for live ongoing shift duration
    var currentDurationMillis by remember { mutableLongStateOf(0L) }
    LaunchedEffect(todayShift) {
        if (todayShift != null && todayShift.status == ShiftStatus.ACTIVE_SHIFT) {
            while (true) {
                currentDurationMillis = System.currentTimeMillis() - todayShift.shiftStartTimeMillis
                delay(1000L)
            }
        }
    }

    // Request Dialog State
    var showRequestDialog by remember { mutableStateOf(false) }
    var requestType by remember { mutableStateOf(RequestType.LEAVE_APPLICATION) }
    var requestTitle by remember { mutableStateOf("") }
    var requestDesc by remember { mutableStateOf("") }

    // Pulsing indicator for active live tracking
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("employee_portal_root"),
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
                                    text = employee.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusBadge(text = "FIELD STAFF", color = VioletAccent)
                            }
                            Text(
                                text = "${employee.designation} • K&B NET SERVICE",
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
                        modifier = Modifier.testTag("employee_theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme Mode",
                            tint = if (isDarkMode) GoldenAmber else RoyalPurple
                        )
                    }
                    IconButton(
                        onClick = {
                            KbLocationService.stop(context)
                            onLogout()
                        },
                        modifier = Modifier.testTag("employee_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
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
                modifier = Modifier.testTag("employee_bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    label = { Text("Shift", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                    label = { Text("Tasks (${myTasks.count { it.status != TaskStatus.COMPLETED }})", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    label = { Text("Tickets (${myComplaints.count { it.status != ComplaintStatus.RESOLVED && it.status != ComplaintStatus.CLOSED }})", fontSize = 10.sp) }
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
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                    label = { Text("Team", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == 5,
                    onClick = { currentTab = 5 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile", fontSize = 10.sp) }
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
                0 -> {
                    // TAB 0: Shift Punch & Live GPS Tracking
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .testTag("employee_shift_tab"),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))

                            // Shift Rules Guidance Card
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Official Shift Timing: 8:00 AM – 7:00 PM",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Asia/Kolkata • Start shift at the start of your field duties. Foreground GPS will track location during active shift.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Main Shift Action Card (CRITICAL REQUIREMENT)
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(3.dp, RoundedCornerShape(20.dp))
                                    .border(
                                        width = if (todayShift != null && todayShift.status == ShiftStatus.ACTIVE_SHIFT) 2.dp else 1.dp,
                                        color = if (todayShift != null && todayShift.status == ShiftStatus.ACTIVE_SHIFT) MintEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .testTag("employee_shift_punch_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (todayShift == null) {
                                        // NOT STARTED
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(ElectricBlue.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = ElectricBlue,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "Ready to Begin Duty?",
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = "Date: ${NexusRepository.getKolkataDateIso()} • K&B Field Ops",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(18.dp))

                                        Button(
                                            onClick = {
                                                val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                                val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                                if (fineGranted || coarseGranted) {
                                                    beginShiftWithGps()
                                                } else {
                                                    val permissions = mutableListOf(
                                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                                    )
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                                                    }
                                                    permissionLauncher.launch(permissions.toTypedArray())
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("start_shift_button"),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isStartingShift,
                                            colors = ButtonDefaults.buttonColors(containerColor = MintEmerald)
                                        ) {
                                            Icon(Icons.Default.PlayCircle, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isStartingShift) "Getting GPS Fix…" else "Start Daily Shift",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        gpsError?.let { message ->
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(message, color = CoralRose, style = MaterialTheme.typography.bodySmall)
                                        }
                                    } else if (todayShift.status == ShiftStatus.ACTIVE_SHIFT) {
                                        // SHIFT IN PROGRESS (ACTIVE)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(MintEmerald.copy(alpha = pulseAlpha))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "SHIFT ACTIVE & IN PROGRESS",
                                                fontWeight = FontWeight.Black,
                                                color = MintEmerald,
                                                fontSize = 13.sp,
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Live Running Duration Clock
                                        val totalSecs = currentDurationMillis / 1000
                                        val hours = totalSecs / 3600
                                        val mins = (totalSecs % 3600) / 60
                                        val secs = totalSecs % 60
                                        Text(
                                            text = String.format("%02d:%02d:%02d", hours, mins, secs),
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 38.sp
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Started at ${NexusRepository.formatKolkataTime(todayShift.shiftStartTimeMillis)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // GPS Tracking Indicator
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MintEmerald.copy(alpha = 0.12f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, MintEmerald.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = when (myLocation?.locationStatus) {
                                                            LocationStatus.LIVE -> "Live GPS Active"
                                                            LocationStatus.GPS_DISABLED -> "Device Location is OFF"
                                                            LocationStatus.PERMISSION_DENIED -> "Location Permission Required"
                                                            LocationStatus.LOCATION_UNAVAILABLE -> "Waiting for GPS Fix"
                                                            LocationStatus.OFFLINE, null -> "GPS Status Unavailable"
                                                        },
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 13.sp,
                                                        color = MintEmerald
                                                    )
                                                    Text(
                                                        text = when (myLocation?.locationStatus) {
                                                            LocationStatus.LIVE -> "Last fix: ${NexusRepository.formatKolkataTime(myLocation.timestampMillis)} • Accuracy ±${myLocation.accuracyMeters.toInt()} m"
                                                            LocationStatus.GPS_DISABLED -> "Turn on Location to resume shift tracking."
                                                            LocationStatus.PERMISSION_DENIED -> "Grant location permission to resume tracking."
                                                            LocationStatus.LOCATION_UNAVAILABLE -> "Waiting for the device to provide a location fix."
                                                            LocationStatus.OFFLINE, null -> "Location updates have not been received yet."
                                                        },
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        // PROMINENT END SHIFT BUTTON
                                        Button(
                                            onClick = {
                                                if (!isEndingShift) {
                                                    isEndingShift = true
                                                    val locationClient = LocationServices.getFusedLocationProviderClient(context)
                                                    locationClient.getCurrentLocation(
                                                        Priority.PRIORITY_HIGH_ACCURACY,
                                                        CancellationTokenSource().token
                                                    ).addOnSuccessListener { location ->
                                                        repository.endEmployeeShift(
                                                            todayShift.id,
                                                            location?.latitude ?: 0.0,
                                                            location?.longitude ?: 0.0
                                                        )
                                                        KbLocationService.stop(context)
                                                        isEndingShift = false
                                                    }.addOnFailureListener {
                                                        repository.endEmployeeShift(todayShift.id, 0.0, 0.0)
                                                        KbLocationService.stop(context)
                                                        isEndingShift = false
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("end_shift_button"),
                                            shape = RoundedCornerShape(12.dp),
                                            enabled = !isEndingShift,
                                            colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                                        ) {
                                            Icon(Icons.Default.StopCircle, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isEndingShift) "Ending Shift…" else "End Shift",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    } else {
                                        // COMPLETED OR AUTO-CLOSED FOR TODAY
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(VioletAccent.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = VioletAccent,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = if (todayShift.isAutoClosed) "Shift Auto-Closed at 7:00 PM" else "Shift Completed for Today",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Clock In: ${NexusRepository.formatKolkataTime(todayShift.shiftStartTimeMillis)} • " +
                                                    "Clock Out: ${NexusRepository.formatKolkataTime(todayShift.shiftEndTimeMillis ?: 0L)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Text(
                                            text = "Total Duration: ${todayShift.durationMinutes / 60}h ${todayShift.durationMinutes % 60}m",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanPrimary
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "✓ Great work! The next daily shift will become available on the next working day starting 8:00 AM.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Recent Attendance History for this Employee
                        item {
                            Text(
                                text = "My Recent Attendance History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val myHistory = attendanceRecords.filter { it.employeeId == employee.id }
                        items(myHistory) { record ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Date: ${record.dateIso}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "${NexusRepository.formatKolkataTime(record.shiftStartTimeMillis)} - " +
                                                    if (record.shiftEndTimeMillis != null) NexusRepository.formatKolkataTime(record.shiftEndTimeMillis) else "Active",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    StatusBadge(
                                        text = record.status.name.replace("_", " "),
                                        color = when (record.status) {
                                            ShiftStatus.ACTIVE_SHIFT -> EmeraldSuccess
                                            ShiftStatus.COMPLETED -> VioletAccent
                                            ShiftStatus.AUTO_CLOSED -> AmberWarning
                                            ShiftStatus.ADMIN_OVERRIDE -> CyanPrimary
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                1 -> {
                    // TAB 1: My Field Tasks
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .testTag("employee_tasks_tab"),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "My Assigned Field Tasks (${myTasks.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (myTasks.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No active tasks currently assigned to you. Enjoy your shift!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

                        items(myTasks, key = { it.id }) { task ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PriorityBadge(
                                            priorityName = task.priority.name,
                                            color = when (task.priority) {
                                                TaskPriority.CRITICAL -> RoseDanger
                                                TaskPriority.HIGH -> AmberWarning
                                                TaskPriority.MEDIUM -> CyanGlow
                                                TaskPriority.LOW -> EmeraldSuccess
                                            }
                                        )
                                        StatusBadge(
                                            text = task.status.name.replace("_", " "),
                                            color = when (task.status) {
                                                TaskStatus.COMPLETED -> EmeraldSuccess
                                                TaskStatus.IN_PROGRESS -> CyanPrimary
                                                TaskStatus.ACCEPTED -> VioletAccent
                                                TaskStatus.PENDING -> AmberWarning
                                                else -> Color.Gray
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(text = task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    if (task.locationAddress.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "📍 ${task.locationAddress}", style = MaterialTheme.typography.labelSmall, color = CyanPrimary)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action buttons depending on state
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (task.status == TaskStatus.PENDING) {
                                            Button(
                                                onClick = { repository.updateTaskStatus(task.id, TaskStatus.ACCEPTED) },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Accept Task")
                                            }
                                        } else if (task.status == TaskStatus.ACCEPTED) {
                                            Button(
                                                onClick = { repository.updateTaskStatus(task.id, TaskStatus.IN_PROGRESS) },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Start Work")
                                            }
                                        } else if (task.status == TaskStatus.IN_PROGRESS) {
                                            Button(
                                                onClick = {
                                                    repository.updateTaskStatus(task.id, TaskStatus.COMPLETED, "Completed by field technician")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Mark Work Completed")
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
                }

                2 -> {
                    // TAB 2: Assigned Service Tickets / Field Visits
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .testTag("employee_tickets_tab"),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Assigned Fiber & Customer Tickets (${myComplaints.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (myComplaints.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No customer complaints currently assigned to you.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

                        items(myComplaints, key = { it.id }) { tkt ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PriorityBadge(
                                            priorityName = tkt.priority.name,
                                            color = if (tkt.priority == ComplaintPriority.CRITICAL) RoseDanger else AmberWarning
                                        )
                                        StatusBadge(
                                            text = tkt.status.name,
                                            color = if (tkt.status == ComplaintStatus.RESOLVED) EmeraldSuccess else CyanPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = tkt.issueTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "Customer: ${tkt.customerName} • ${tkt.customerPhone}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = CyanPrimary)
                                    Text(text = "Address: ${tkt.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = tkt.description, style = MaterialTheme.typography.bodySmall)

                                    if (tkt.status != ComplaintStatus.RESOLVED && tkt.status != ComplaintStatus.CLOSED) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                repository.updateComplaintStatus(tkt.id, ComplaintStatus.RESOLVED, "Field technician tested optical power and restored internet.")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Resolve Ticket")
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                3 -> {
                    // TAB 3: Company Media Hub (Photos, Videos, Proofs)
                    MediaHubScreen(repository = repository)
                }

                4 -> {
                    // TAB 4: Team Chat & Announcements
                    var chatInput by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .testTag("employee_team_tab")
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Team Bulletin & Broadcasts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(chats.reversed(), key = { it.id }) { msg ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (msg.isBroadcast)
                                            CyanLight.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = msg.senderName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                if (msg.isBroadcast) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    StatusBadge(text = "📢 BROADCAST", color = CyanPrimary)
                                                }
                                            }
                                            Text(
                                                text = NexusRepository.formatKolkataTime(msg.timestampMillis),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = msg.content, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text("Send update to team...") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (chatInput.isNotBlank()) {
                                        repository.sendChatMessage(
                                            sender = employee,
                                            content = chatInput.trim(),
                                            channel = "TEAM"
                                        )
                                        chatInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                }

                5 -> {
                    // TAB 5: Profile, Requests & Salary Slip
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .testTag("employee_profile_tab"),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))

                            // Employee Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(CyanPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(employee.avatarEmoji, fontSize = 26.sp)
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = employee.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(text = "${employee.designation} • ${employee.department}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "Joined: ${employee.joiningDate} • ${employee.phone}", style = MaterialTheme.typography.labelSmall, color = CyanPrimary)
                                    }
                                }
                            }
                        }

                        // Submit Administrative Request Button
                        item {
                            Button(
                                onClick = { showRequestDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                            ) {
                                Icon(Icons.Default.PostAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Submit Request (Leave / Advance / Shift Correction)")
                            }
                        }

                        // My Requests List
                        if (myRequests.isNotEmpty()) {
                            item {
                                Text(
                                    text = "My Submitted Requests",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(myRequests) { req ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = req.requestTitle, fontWeight = FontWeight.Bold)
                                            StatusBadge(
                                                text = req.status.name.replace("_", " "),
                                                color = when (req.status) {
                                                    RequestStatus.APPROVED -> EmeraldSuccess
                                                    RequestStatus.PENDING_REVIEW -> AmberWarning
                                                    RequestStatus.REJECTED -> RoseDanger
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = req.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (req.adminDecisionNotes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "Admin Note: ${req.adminDecisionNotes}", style = MaterialTheme.typography.labelSmall, color = CyanPrimary)
                                        }
                                    }
                                }
                            }
                        }

                        // Salary Slip
                        if (mySalarySlip != null) {
                            val slip = mySalarySlip!!
                            item {
                                Text(
                                    text = "My Latest Salary Slip (${slip.monthString})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Base Monthly Salary", style = MaterialTheme.typography.bodyMedium)
                                            Text("₹${slip.baseSalary.toInt()}", fontWeight = FontWeight.Bold)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Verified Days Worked", style = MaterialTheme.typography.bodyMedium)
                                            Text("${slip.daysWorked} days", fontWeight = FontWeight.Bold)
                                        }
                                        if (slip.bonusAmount > 0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Performance Bonus", style = MaterialTheme.typography.bodyMedium, color = EmeraldSuccess)
                                                Text("+₹${slip.bonusAmount.toInt()}", fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Net Paid into Bank", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Text("₹${slip.netSalary.toInt()}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge, color = EmeraldSuccess)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    // Submit Request Dialog
    if (showRequestDialog) {
        AlertDialog(
            onDismissRequest = { showRequestDialog = false },
            title = { Text("Submit Administrative Request") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Request Type:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = requestType == RequestType.LEAVE_APPLICATION,
                            onClick = { requestType = RequestType.LEAVE_APPLICATION },
                            label = { Text("Leave", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = requestType == RequestType.ATTENDANCE_CORRECTION,
                            onClick = { requestType = RequestType.ATTENDANCE_CORRECTION },
                            label = { Text("Correction", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = requestType == RequestType.SALARY_ADVANCE,
                            onClick = { requestType = RequestType.SALARY_ADVANCE },
                            label = { Text("Advance", fontSize = 11.sp) }
                        )
                    }

                    OutlinedTextField(
                        value = requestTitle,
                        onValueChange = { requestTitle = it },
                        label = { Text("Subject / Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = requestDesc,
                        onValueChange = { requestDesc = it },
                        label = { Text("Details & Justification") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (requestTitle.isNotBlank()) {
                            repository.submitRequest(employee, requestType, requestTitle, requestDesc)
                            showRequestDialog = false
                            requestTitle = ""
                            requestDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("Submit to Admin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRequestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
