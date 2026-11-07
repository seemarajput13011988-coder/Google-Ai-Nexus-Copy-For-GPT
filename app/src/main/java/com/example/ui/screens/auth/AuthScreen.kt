package com.example.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AccountStatus
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.repository.NexusRepository
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    repository: NexusRepository,
    onLoginSuccess: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode by repository.isDarkMode.collectAsState()
    val users by repository.users.collectAsState()

    // 0: Admin Login, 1: Staff Login, 2: Staff Request
    var selectedTab by remember { mutableIntStateOf(0) }

    // Admin Login Fields
    var adminUsername by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var initialAdminName by remember { mutableStateOf("") }
    var adminPasswordVisible by remember { mutableStateOf(false) }
    var adminError by remember { mutableStateOf<String?>(null) }

    // Staff Login Fields
    var staffUsername by remember { mutableStateOf("") }
    var staffPassword by remember { mutableStateOf("") }
    var staffPasswordVisible by remember { mutableStateOf(false) }
    var staffError by remember { mutableStateOf<String?>(null) }

    // Staff Request Fields
    var regFullName by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regAddress by remember { mutableStateOf("") }
    var regDesignation by remember { mutableStateOf("Optical Fiber Splicer") }
    var regDepartment by remember { mutableStateOf("Field Technical Ops") }
    var regReason by remember { mutableStateOf("") }
    var regSuccessMsg by remember { mutableStateOf<String?>(null) }
    var regErrorMsg by remember { mutableStateOf<String?>(null) }

    // Status Lookup
    var lookupQuery by remember { mutableStateOf("") }
    var lookupResultUser by remember { mutableStateOf<User?>(null) }
    var lookupSearched by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("auth_screen_root"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Theme Mode Toggle on Top Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable { repository.toggleDarkMode() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme Mode",
                            tint = if (isDarkMode) GoldenAmber else RoyalPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDarkMode) "Light Mode" else "Dark Mode",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bright Vibrant Brand Header with Glowing Emblem
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ElectricBlue, VividCyan, RoyalPurple)
                        )
                    )
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_kb_logo),
                    contentDescription = "K&B Nexus Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(21.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = ElectricBlue.copy(alpha = 0.12f),
                modifier = Modifier.border(1.dp, ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(50))
            ) {
                Text(
                    text = "K&B NET SERVICE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                    color = ElectricBlue,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "MANAGEMENT NEXUS",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Enterprise Portal: GPS • Staff Attendance • Billing • ISP CRM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3-way Navigation Tabs: Admin Login, Staff Login, Staff Request
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .testTag("auth_tab_row"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ElectricBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        adminError = null
                        staffError = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Admin Login", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        adminError = null
                        staffError = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Staff Login", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        regErrorMsg = null
                        regSuccessMsg = null
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Staff Request", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            when (selectedTab) {
                // ==================== TAB 0: ADMIN LOGIN ====================
                0 -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(18.dp))
                            .border(1.5.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ElectricBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Admin Portal Login",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Super Admin & Administrative Staff Only",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            adminError?.let { err ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CoralRose.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, CoralRose.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CoralRose, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = err, color = CoralRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            OutlinedTextField(
                                value = adminUsername,
                                onValueChange = { adminUsername = it; adminError = null },
                                label = { Text("Admin Username") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_username_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = adminPassword,
                                onValueChange = { adminPassword = it; adminError = null },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricBlue) },
                                trailingIcon = {
                                    IconButton(onClick = { adminPasswordVisible = !adminPasswordVisible }) {
                                        Icon(
                                            imageVector = if (adminPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Password Visibility"
                                        )
                                    }
                                },
                                visualTransformation = if (adminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_password_input")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    repository.authenticate(adminUsername, adminPassword, adminOnly = true)
                                        .onSuccess { authenticatedUser ->
                                            adminError = null
                                            onLoginSuccess(authenticatedUser)
                                        }
                                        .onFailure { error ->
                                            adminError = error.message ?: "Unable to sign in."
                                        }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_login_submit_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In as Administrator", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (users.none { it.role == UserRole.SUPER_ADMIN }) {
                                Divider(modifier = Modifier.padding(vertical = 12.dp))
                                Text(
                                    text = "First-time Super Admin setup",
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBlue,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Create the single initial owner account. Admin creation is locked after this setup.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = initialAdminName,
                                    onValueChange = { initialAdminName = it; adminError = null },
                                    label = { Text("Super Admin full name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        repository.createInitialSuperAdmin(initialAdminName, adminUsername, adminPassword)
                                            .onSuccess { created ->
                                                adminError = null
                                                onLoginSuccess(created)
                                            }
                                            .onFailure { error ->
                                                adminError = error.message ?: "Unable to create the initial administrator."
                                            }
                                    },
                                    enabled = initialAdminName.isNotBlank() && adminUsername.isNotBlank() && adminPassword.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create Initial Super Admin", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ==================== TAB 1: STAFF LOGIN ====================
                1 -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(18.dp))
                            .border(1.5.dp, MintEmerald.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MintEmerald.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = MintEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Staff & Technician Login",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Field Splicers, Technicians & Operations Staff",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            staffError?.let { err ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CoralRose.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, CoralRose.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CoralRose, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = err, color = CoralRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            OutlinedTextField(
                                value = staffUsername,
                                onValueChange = { staffUsername = it; staffError = null },
                                label = { Text("Staff ID or Username") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MintEmerald) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("staff_username_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = staffPassword,
                                onValueChange = { staffPassword = it; staffError = null },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MintEmerald) },
                                trailingIcon = {
                                    IconButton(onClick = { staffPasswordVisible = !staffPasswordVisible }) {
                                        Icon(
                                            imageVector = if (staffPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Password Visibility"
                                        )
                                    }
                                },
                                visualTransformation = if (staffPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("staff_password_input")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    repository.authenticate(staffUsername, staffPassword, adminOnly = false)
                                        .onSuccess { authenticatedUser ->
                                            if (authenticatedUser.role == UserRole.EMPLOYEE) {
                                                staffError = null
                                                onLoginSuccess(authenticatedUser)
                                            } else {
                                                repository.logout()
                                                staffError = "Administrator accounts must use Admin Login."
                                            }
                                        }
                                        .onFailure { error ->
                                            staffError = error.message ?: "Unable to sign in."
                                        }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("staff_login_submit_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MintEmerald),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In to Staff Portal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                        }
                    }
                }

                // ==================== TAB 2: STAFF REQUEST (ONBOARDING) ====================
                2 -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(18.dp))
                            .border(1.5.dp, TelecomOrange.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TelecomOrange.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = TelecomOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Staff Registration Request",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Submit request for Admin review and approval",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            regSuccessMsg?.let { success ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MintEmerald.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MintEmerald.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = success, color = MintEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            regErrorMsg?.let { err ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CoralRose.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, CoralRose.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CoralRose, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = err, color = CoralRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            OutlinedTextField(
                                value = regFullName,
                                onValueChange = { regFullName = it; regErrorMsg = null },
                                label = { Text("Full Name *") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_fullname_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = regUsername,
                                    onValueChange = { regUsername = it; regErrorMsg = null },
                                    label = { Text("Desired Username *") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_username_input")
                                )
                                OutlinedTextField(
                                    value = regPassword,
                                    onValueChange = { regPassword = it; regErrorMsg = null },
                                    label = { Text("Password *") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_password_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = regPhone,
                                    onValueChange = { regPhone = it; regErrorMsg = null },
                                    label = { Text("Mobile (+91) *") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_phone_input")
                                )
                                OutlinedTextField(
                                    value = regEmail,
                                    onValueChange = { regEmail = it; regErrorMsg = null },
                                    label = { Text("Email (Optional)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_email_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regAddress,
                                onValueChange = { regAddress = it; regErrorMsg = null },
                                label = { Text("Residential Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = regDesignation,
                                    onValueChange = { regDesignation = it },
                                    label = { Text("Designation") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = regDepartment,
                                    onValueChange = { regDepartment = it },
                                    label = { Text("Department") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regReason,
                                onValueChange = { regReason = it },
                                label = { Text("Reason / Qualifications for Joining") },
                                minLines = 2,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (regFullName.isBlank() || regUsername.isBlank() || regPassword.isBlank() || regPhone.isBlank()) {
                                        regErrorMsg = "Please fill all mandatory fields (*)."
                                        return@Button
                                    }
                                    val result = repository.registerNewEmployee(
                                        fullName = regFullName,
                                        username = regUsername,
                                        password = regPassword,
                                        phone = regPhone,
                                        email = regEmail,
                                        address = regAddress,
                                        designation = regDesignation,
                                        department = regDepartment,
                                        reason = regReason
                                    )
                                    if (result.isSuccess) {
                                        regSuccessMsg = "Registration request submitted successfully! Your account status is PENDING_APPROVAL. An administrator will review your application."
                                        regFullName = ""
                                        regUsername = ""
                                        regPassword = ""
                                        regPhone = ""
                                        regEmail = ""
                                        regAddress = ""
                                        regReason = ""
                                    } else {
                                        regErrorMsg = result.exceptionOrNull()?.message ?: "Failed to submit registration request."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("reg_submit_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = TelecomOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submit Registration Request", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Track Application Status Section
                            Divider()
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Track Existing Request Status",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enter your username or phone number to check if your account was approved",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = lookupQuery,
                                    onValueChange = { lookupQuery = it },
                                    label = { Text("Username or Phone") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        lookupSearched = true
                                        lookupResultUser = users.firstOrNull {
                                            it.username.equals(lookupQuery.trim(), ignoreCase = true) ||
                                                    it.phone.contains(lookupQuery.trim())
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }

                            if (lookupSearched) {
                                Spacer(modifier = Modifier.height(10.dp))
                                val user = lookupResultUser
                                if (user != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = user.fullName,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                StatusBadge(
                                                    text = user.status.name,
                                                    color = if (user.status == AccountStatus.APPROVED) MintEmerald else if (user.status == AccountStatus.PENDING_APPROVAL) GoldenAmber else CoralRose
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Role: ${user.designation} (${user.department})",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Applied on: ${user.joiningDate}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (user.status == AccountStatus.APPROVED) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "✅ Account is approved! You can now log in using the Staff Login tab.",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MintEmerald
                                                )
                                            } else if (user.status == AccountStatus.PENDING_APPROVAL) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "⏳ Application is currently under review by Super Admin.",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GoldenAmber
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "No registration request found for '$lookupQuery'.",
                                        color = CoralRose,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "K&B NET SERVICE • Secured Enterprise Portal v3.2",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
