package com.example.data.repository

import android.content.Context
import com.example.data.model.*
import com.example.data.storage.EncryptedStateStore
import com.example.security.PasswordHasher
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class NexusRepository private constructor(private val context: Context) {

    private val stateStore = EncryptedStateStore(context)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var persistenceEnabled = true

    private inline fun <reified T : Any> decodeList(key: String): List<T> {
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        return try {
            val stored = stateStore.getString(key)
            if (stored == null && stateStore.contains(key)) {
                persistenceEnabled = false
                emptyList()
            } else {
                moshi.adapter<List<T>>(type).fromJson(stored ?: "[]") ?: emptyList()
            }
        } catch (_: Exception) {
            persistenceEnabled = false
            emptyList()
        }
    }

    private inline fun <reified T : Any> persistList(key: String, flow: StateFlow<List<T>>) {
        if (!persistenceEnabled) return
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        val adapter = moshi.adapter<List<T>>(type)
        persistenceScope.launch {
            flow.collect { records -> stateStore.putString(key, adapter.toJson(records)) }
        }
    }

    private fun isAdministrator(): Boolean =
        _currentUser.value?.role == UserRole.SUPER_ADMIN || _currentUser.value?.role == UserRole.ADMIN

    private fun isSuperAdministrator(): Boolean = _currentUser.value?.role == UserRole.SUPER_ADMIN

    companion object {
        @Volatile
        private var INSTANCE: NexusRepository? = null

        fun getInstance(context: Context): NexusRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NexusRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun resetInstance(context: Context): NexusRepository {
            return NexusRepository(context.applicationContext).also { INSTANCE = it }
        }

        fun getKolkataCalendar(): Calendar {
            return Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        }

        fun getKolkataDateIso(timeMillis: Long = System.currentTimeMillis()): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            return sdf.format(Date(timeMillis))
        }

        fun formatKolkataTime(timeMillis: Long): String {
            val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            return sdf.format(Date(timeMillis))
        }

        fun formatKolkataDateTime(timeMillis: Long): String {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            return sdf.format(Date(timeMillis))
        }
    }

    // 0. Theme Mode (Bright Light Mode by default, toggleable in UI)
    private val _isDarkMode = MutableStateFlow<Boolean>(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // 1. Current Session
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // 2. Users
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // 3. Attendance
    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    // 4. GPS Locations
    private val _employeeLocations = MutableStateFlow<List<EmployeeLocation>>(emptyList())
    val employeeLocations: StateFlow<List<EmployeeLocation>> = _employeeLocations.asStateFlow()

    // 5. Tasks
    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    // 6. Customers (CRM)
    private val _customers = MutableStateFlow<List<IspCustomer>>(emptyList())
    val customers: StateFlow<List<IspCustomer>> = _customers.asStateFlow()

    // 7. ISP Plans
    private val _plans = MutableStateFlow<List<IspPlan>>(emptyList())
    val plans: StateFlow<List<IspPlan>> = _plans.asStateFlow()

    // 8. Accounting / Finance
    private val _transactions = MutableStateFlow<List<AccountingTransaction>>(emptyList())
    val transactions: StateFlow<List<AccountingTransaction>> = _transactions.asStateFlow()

    // 9. Salaries
    private val _salaries = MutableStateFlow<List<SalarySlip>>(emptyList())
    val salaries: StateFlow<List<SalarySlip>> = _salaries.asStateFlow()

    // 10. Inventory
    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    // 11. Support / Complaints
    private val _complaints = MutableStateFlow<List<ComplaintTicket>>(emptyList())
    val complaints: StateFlow<List<ComplaintTicket>> = _complaints.asStateFlow()

    // 12. Chat & Announcements
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // 13. Requests
    private val _requests = MutableStateFlow<List<EmployeeRequest>>(emptyList())
    val requests: StateFlow<List<EmployeeRequest>> = _requests.asStateFlow()

    // 14. Professional Invoices & Billing
    private val _invoices = MutableStateFlow<List<IspInvoice>>(emptyList())
    val invoices: StateFlow<List<IspInvoice>> = _invoices.asStateFlow()

    // 15. GPS Breadcrumbs & Location Route History
    private val _locationBreadcrumbs = MutableStateFlow<List<LocationBreadcrumb>>(emptyList())
    val locationBreadcrumbs: StateFlow<List<LocationBreadcrumb>> = _locationBreadcrumbs.asStateFlow()

    // 16. Media Library: Photos & Videos
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    private val _mediaAlbums = MutableStateFlow<List<MediaAlbum>>(emptyList())
    val mediaAlbums: StateFlow<List<MediaAlbum>> = _mediaAlbums.asStateFlow()

    init {
        // Start with empty real business records. No fictional staff, customers, invoices,
        // shifts, locations, chats, media or demo credentials are inserted.
        _users.value = decodeList("users")
        _attendanceRecords.value = decodeList("attendance")
        _employeeLocations.value = decodeList("employee_locations")
        _tasks.value = decodeList("tasks")
        _customers.value = decodeList("customers")
        _plans.value = decodeList("plans")
        _transactions.value = decodeList("transactions")
        _salaries.value = decodeList("salaries")
        _inventory.value = decodeList("inventory")
        _complaints.value = decodeList("complaints")
        _chatMessages.value = decodeList("chat_messages")
        _requests.value = decodeList("employee_requests")
        _invoices.value = decodeList("invoices")
        _locationBreadcrumbs.value = decodeList("location_breadcrumbs")
        _mediaItems.value = decodeList("media_items")
        _mediaAlbums.value = decodeList("media_albums")

        persistList("users", _users)
        persistList("attendance", _attendanceRecords)
        persistList("employee_locations", _employeeLocations)
        persistList("tasks", _tasks)
        persistList("customers", _customers)
        persistList("plans", _plans)
        persistList("transactions", _transactions)
        persistList("salaries", _salaries)
        persistList("inventory", _inventory)
        persistList("complaints", _complaints)
        persistList("chat_messages", _chatMessages)
        persistList("employee_requests", _requests)
        persistList("invoices", _invoices)
        persistList("location_breadcrumbs", _locationBreadcrumbs)
        persistList("media_items", _mediaItems)
        persistList("media_albums", _mediaAlbums)
        checkAndAutoCloseExpiredShifts()
    }

    // --- Authentication & User Operations ---

    fun authenticate(username: String, password: String, adminOnly: Boolean): Result<User> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your username and password."))
        }
        val user = _users.value.firstOrNull {
            it.username.equals(username.trim(), ignoreCase = true) &&
                PasswordHasher.verify(password, it.passwordSalt, it.passwordHash)
        } ?: return Result.failure(IllegalArgumentException("Invalid username or password."))

        if (user.status != AccountStatus.APPROVED) {
            val message = when (user.status) {
                AccountStatus.PENDING_APPROVAL -> "Your registration is waiting for Super Admin approval."
                AccountStatus.REJECTED -> "Your registration was not approved. Contact administration."
                AccountStatus.SUSPENDED -> "This account is suspended. Contact administration."
                AccountStatus.APPROVED -> "Account approved."
            }
            return Result.failure(IllegalStateException(message))
        }
        if (adminOnly && user.role == UserRole.EMPLOYEE) {
            return Result.failure(SecurityException("This is a staff account. Use Staff Login."))
        }
        _currentUser.value = user
        return Result.success(user)
    }

    fun logout() {
        _currentUser.value = null
    }

    fun createInitialSuperAdmin(fullName: String, username: String, password: String): Result<User> {
        if (_users.value.any { it.role == UserRole.SUPER_ADMIN }) {
            return Result.failure(SecurityException("The initial Super Admin account has already been created."))
        }
        if (fullName.isBlank() || username.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter the Super Admin name and username."))
        }
        if (password.length < 10 || password.none { it.isLetter() } || password.none { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Use at least 10 characters with letters and numbers."))
        }
        if (_users.value.any { it.username.equals(username.trim(), ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("That username is already registered."))
        }
        val digest = PasswordHasher.create(password)
        val user = User(
            id = "usr_" + UUID.randomUUID().toString(),
            fullName = fullName.trim(),
            username = username.trim(),
            passwordHash = digest.hash,
            passwordSalt = digest.salt,
            phone = "",
            email = "",
            address = "",
            designation = "Super Administrator",
            department = "Executive Management",
            joiningDate = getKolkataDateIso(),
            baseSalary = 0.0,
            role = UserRole.SUPER_ADMIN,
            status = AccountStatus.APPROVED
        )
        _users.value = listOf(user) + _users.value
        _currentUser.value = user
        return Result.success(user)
    }

    fun registerNewEmployee(
        fullName: String,
        username: String,
        password: String,
        phone: String,
        email: String,
        address: String,
        designation: String,
        department: String,
        reason: String
    ): Result<User> {
        val current = _users.value
        if (fullName.isBlank() || username.isBlank() || phone.isBlank()) {
            return Result.failure(IllegalArgumentException("Name, username and phone are required."))
        }
        if (password.length < 10 || password.none { it.isLetter() } || password.none { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Use at least 10 characters with letters and numbers."))
        }
        if (current.any { it.username.equals(username, ignoreCase = true) }) {
            return Result.failure(Exception("Username '$username' is already taken."))
        }
        if (current.any { it.phone == phone }) {
            return Result.failure(Exception("Phone number '$phone' is already registered or has a pending request."))
        }
        if (email.isNotBlank() && current.any { it.email.equals(email.trim(), ignoreCase = true) }) {
            return Result.failure(Exception("This email is already registered or has a pending request."))
        }

        val digest = PasswordHasher.create(password)
        val newUser = User(
            id = "usr_" + UUID.randomUUID().toString().take(8),
            fullName = fullName.trim(),
            username = username.trim(),
            passwordHash = digest.hash,
            passwordSalt = digest.salt,
            phone = phone.trim(),
            email = email.trim(),
            address = address.trim(),
            designation = designation.trim(),
            department = department.trim(),
            joiningDate = getKolkataDateIso(),
            baseSalary = 0.0,
            role = UserRole.EMPLOYEE,
            status = AccountStatus.PENDING_APPROVAL,
            registrationReason = reason.trim(),
            avatarEmoji = "👤"
        )
        _users.value = current + newUser
        return Result.success(newUser)
    }

    fun approveEmployeeRegistration(userId: String) {
        if (!isAdministrator()) return
        _users.value = _users.value.map {
            if (it.id == userId) it.copy(status = AccountStatus.APPROVED) else it
        }
    }

    fun rejectEmployeeRegistration(userId: String) {
        if (!isAdministrator()) return
        _users.value = _users.value.map {
            if (it.id == userId) it.copy(status = AccountStatus.REJECTED) else it
        }
    }

    fun updateEmployeeDetails(updatedUser: User) {
        val actor = _currentUser.value ?: return
        if (!isAdministrator()) return
        if (actor.role != UserRole.SUPER_ADMIN && updatedUser.role != UserRole.EMPLOYEE) return
        if (updatedUser.role == UserRole.SUPER_ADMIN && actor.role != UserRole.SUPER_ADMIN) return
        _users.value = _users.value.map { if (it.id == updatedUser.id) updatedUser else it }
        if (_currentUser.value?.id == updatedUser.id) {
            _currentUser.value = updatedUser
        }
    }

    fun deleteEmployee(userId: String) {
        if (!isSuperAdministrator()) return
        if (_users.value.firstOrNull { it.id == userId }?.role == UserRole.SUPER_ADMIN) return
        _users.value = _users.value.filter { it.id != userId }
    }

    // --- Shift & Attendance Management ---

    fun getTodayShiftForEmployee(employeeId: String): AttendanceRecord? {
        val todayIso = getKolkataDateIso()
        return _attendanceRecords.value.firstOrNull {
            it.employeeId == employeeId && it.dateIso == todayIso
        }
    }

    /**
     * Start Shift rule:
     * - Starting shift NEVER marks work as completed.
     * - Records startTime, leaves endTime null, status = ACTIVE_SHIFT.
     */
    fun startEmployeeShift(
        employee: User,
        lat: Double,
        lng: Double
    ): Result<AttendanceRecord> {
        val currentCalendar = getKolkataCalendar()
        val minuteOfDay = currentCalendar.get(Calendar.HOUR_OF_DAY) * 60 + currentCalendar.get(Calendar.MINUTE)
        if (minuteOfDay !in 480..1140) {
            return Result.failure(IllegalStateException("Staff shifts can start between 8:00 AM and 7:00 PM. Ask an administrator for an override."))
        }
        if (!lat.isFinite() || !lng.isFinite() || lat !in -90.0..90.0 || lng !in -180.0..180.0 || (lat == 0.0 && lng == 0.0)) {
            return Result.failure(IllegalArgumentException("A valid live GPS fix is required before starting a shift."))
        }
        if (_currentUser.value?.id != employee.id) {
            return Result.failure(SecurityException("Only the signed-in employee can start their own shift."))
        }
        val todayIso = getKolkataDateIso()
        val existing = _attendanceRecords.value.firstOrNull {
            it.employeeId == employee.id && it.dateIso == todayIso
        }

        if (existing != null && existing.status == ShiftStatus.ACTIVE_SHIFT) {
            return Result.failure(Exception("A shift is already active for today!"))
        }

        val newRecord = AttendanceRecord(
            id = "att_" + UUID.randomUUID().toString().take(8),
            employeeId = employee.id,
            employeeName = employee.fullName,
            dateIso = todayIso,
            shiftStartTimeMillis = System.currentTimeMillis(),
            shiftEndTimeMillis = null,
            status = ShiftStatus.ACTIVE_SHIFT,
            durationMinutes = 0,
            isAutoClosed = false,
            isManualClose = false,
            locationStartLat = lat,
            locationStartLng = lng
        )

        _attendanceRecords.value = listOf(newRecord) + _attendanceRecords.value.filter {
            !(it.employeeId == employee.id && it.dateIso == todayIso)
        }

        // Also update live location
        updateLiveLocation(
            EmployeeLocation(
                employeeId = employee.id,
                employeeName = employee.fullName,
                latitude = lat,
                longitude = lng,
                accuracyMeters = 5.0f,
                timestampMillis = System.currentTimeMillis(),
                isLive = true,
                shiftId = newRecord.id,
                areaDescription = "Shift Start Location (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
            )
        )

        return Result.success(newRecord)
    }

    /**
     * End Shift rule:
     * - Only clicking End Shift completes the active shift.
     * - Calculates accurate duration and records location.
     */
    fun endEmployeeShift(
        recordId: String,
        endLat: Double = 0.0,
        endLng: Double = 0.0
    ): Result<AttendanceRecord> {
        val record = _attendanceRecords.value.firstOrNull { it.id == recordId }
            ?: return Result.failure(Exception("Shift record not found."))
        val actor = _currentUser.value
        if (actor == null || (actor.id != record.employeeId && actor.role !in setOf(UserRole.ADMIN, UserRole.SUPER_ADMIN))) {
            return Result.failure(SecurityException("You cannot end another employee's shift."))
        }

        val now = System.currentTimeMillis()
        val durationMins = ((now - record.shiftStartTimeMillis) / 60000).coerceAtLeast(1)

        val updated = record.copy(
            shiftEndTimeMillis = now,
            status = ShiftStatus.COMPLETED,
            durationMinutes = durationMins,
            isManualClose = true,
            locationEndLat = endLat,
            locationEndLng = endLng
        )

        _attendanceRecords.value = _attendanceRecords.value.map {
            if (it.id == recordId) updated else it
        }

        // Mark location as offline / shift ended
        _employeeLocations.value = _employeeLocations.value.map {
            if (it.employeeId == record.employeeId) {
                it.copy(isLive = false, timestampMillis = now, shiftId = "")
            } else it
        }

        return Result.success(updated)
    }

    /**
     * Admin Shift Override:
     * - Admin can force start shift outside normal 8 AM - 7 PM hours
     */
    fun adminStartShiftOverride(
        employee: User,
        customStartMillis: Long,
        reason: String
    ): AttendanceRecord {
        check(isAdministrator()) { "Administrator access required." }
        require(reason.isNotBlank()) { "An override reason is required." }
        val dateIso = getKolkataDateIso(customStartMillis)
        val newRecord = AttendanceRecord(
            id = "att_ovr_" + UUID.randomUUID().toString().take(8),
            employeeId = employee.id,
            employeeName = employee.fullName,
            dateIso = dateIso,
            shiftStartTimeMillis = customStartMillis,
            shiftEndTimeMillis = null,
            status = ShiftStatus.ACTIVE_SHIFT,
            durationMinutes = 0,
            adminOverrideReason = reason.ifBlank { "Admin manual shift start override" },
            isAutoClosed = false,
            isManualClose = false
        )
        _attendanceRecords.value = listOf(newRecord) + _attendanceRecords.value.filter {
            !(it.employeeId == employee.id && it.dateIso == dateIso)
        }
        return newRecord
    }

    /**
     * Admin Shift Override: Force end an active shift
     */
    fun adminEndShiftOverride(
        recordId: String,
        customEndMillis: Long,
        reason: String
    ): Result<AttendanceRecord> {
        if (!isAdministrator()) return Result.failure(SecurityException("Administrator access required."))
        if (reason.isBlank()) return Result.failure(IllegalArgumentException("An override reason is required."))
        val record = _attendanceRecords.value.firstOrNull { it.id == recordId }
            ?: return Result.failure(Exception("Shift record not found."))

        val durationMins = ((customEndMillis - record.shiftStartTimeMillis) / 60000).coerceAtLeast(1)
        val updated = record.copy(
            shiftEndTimeMillis = customEndMillis,
            status = ShiftStatus.ADMIN_OVERRIDE,
            durationMinutes = durationMins,
            adminOverrideReason = reason.ifBlank { "Admin manual shift end override" }
        )

        _attendanceRecords.value = _attendanceRecords.value.map {
            if (it.id == recordId) updated else it
        }
        return Result.success(updated)
    }

    /**
     * Admin Attendance Correction
     */
    fun adminCorrectAttendance(
        recordId: String,
        newStartMillis: Long,
        newEndMillis: Long,
        reason: String
    ): Result<AttendanceRecord> {
        if (!isAdministrator()) return Result.failure(SecurityException("Administrator access required."))
        if (reason.isBlank() || newEndMillis <= newStartMillis) return Result.failure(IllegalArgumentException("Enter a valid time range and correction reason."))
        val record = _attendanceRecords.value.firstOrNull { it.id == recordId }
            ?: return Result.failure(Exception("Record not found."))

        val duration = ((newEndMillis - newStartMillis) / 60000).coerceAtLeast(1)
        val updated = record.copy(
            shiftStartTimeMillis = newStartMillis,
            shiftEndTimeMillis = newEndMillis,
            status = ShiftStatus.ADMIN_OVERRIDE,
            durationMinutes = duration,
            adminOverrideReason = reason
        )
        _attendanceRecords.value = _attendanceRecords.value.map {
            if (it.id == recordId) updated else it
        }
        return Result.success(updated)
    }

    /**
     * Automatic Shift Ending:
     * - If active past configured 7:00 PM (19:00), automatically closes with status AUTO_CLOSED.
     * - Does NOT prevent next day's shift.
     */
    fun checkAndAutoCloseExpiredShifts() {
        val now = System.currentTimeMillis()
        val todayIso = getKolkataDateIso(now)
        _attendanceRecords.value = _attendanceRecords.value.map { record ->
            if (record.status != ShiftStatus.ACTIVE_SHIFT) return@map record
            val shiftDay = getKolkataCalendar().apply { timeInMillis = record.shiftStartTimeMillis }
            shiftDay.set(Calendar.HOUR_OF_DAY, 19)
            shiftDay.set(Calendar.MINUTE, 0)
            shiftDay.set(Calendar.SECOND, 0)
            shiftDay.set(Calendar.MILLISECOND, 0)
            val regularClose = shiftDay.timeInMillis
            // An admin may start a shift after 7 PM; give that override an 8-hour window.
            // All shifts that start before 7 PM still close at 7 PM, including early overrides.
            val scheduledClose = if (record.shiftStartTimeMillis >= regularClose) {
                record.shiftStartTimeMillis + 8L * 60L * 60L * 1000L
            } else regularClose
            if (record.dateIso < todayIso || now >= scheduledClose) {
                val closeTime = maxOf(record.shiftStartTimeMillis, scheduledClose)
                val duration = ((closeTime - record.shiftStartTimeMillis) / 60000).coerceAtLeast(0)
                _employeeLocations.value = _employeeLocations.value.map { location ->
                    if (location.employeeId == record.employeeId) {
                        location.copy(
                            isLive = false,
                            locationStatus = LocationStatus.OFFLINE,
                            timestampMillis = closeTime,
                            shiftId = ""
                        )
                    } else location
                }
                record.copy(
                    shiftEndTimeMillis = closeTime,
                    status = ShiftStatus.AUTO_CLOSED,
                    durationMinutes = duration,
                    isAutoClosed = true
                )
            } else record
        }
    }

    fun isShiftActive(shiftId: String): Boolean =
        _attendanceRecords.value.any { it.id == shiftId && it.status == ShiftStatus.ACTIVE_SHIFT }

    // --- GPS Tracking ---

    fun updateLiveLocation(location: EmployeeLocation) {
        if (location.employeeId.isBlank() || !location.latitude.isFinite() || !location.longitude.isFinite()) return
        val current = _employeeLocations.value
        val existingIndex = current.indexOfFirst { it.employeeId == location.employeeId }
        _employeeLocations.value = if (existingIndex >= 0) {
            current.toMutableList().apply { set(existingIndex, location) }
        } else {
            current + location
        }
    }

    fun updateLocationStatus(employeeId: String, status: LocationStatus) {
        if (employeeId.isBlank()) return
        val existing = _employeeLocations.value.firstOrNull { it.employeeId == employeeId }
        if (existing == null) {
            val employee = _users.value.firstOrNull { it.id == employeeId }
            _employeeLocations.value = _employeeLocations.value + EmployeeLocation(
                employeeId = employeeId,
                employeeName = employee?.fullName ?: "Staff",
                latitude = 0.0,
                longitude = 0.0,
                isLive = false,
                locationStatus = status,
                batteryPercent = -1,
                shiftId = ""
            )
        } else {
            _employeeLocations.value = _employeeLocations.value.map {
                if (it.employeeId == employeeId) it.copy(
                    isLive = status == LocationStatus.LIVE,
                    locationStatus = status
                ) else it
            }
        }
    }

    fun addLocationBreadcrumb(location: EmployeeLocation) {
        if (!location.latitude.isFinite() || !location.longitude.isFinite()) return
        val point = LocationBreadcrumb(
            id = "loc_" + UUID.randomUUID().toString(),
            employeeId = location.employeeId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracyMeters,
            timestampMillis = location.timestampMillis,
            areaNote = location.areaDescription
        )
        _locationBreadcrumbs.value = (listOf(point) + _locationBreadcrumbs.value).take(10000)
    }

    // --- Task Management ---

    fun addTask(task: TaskItem) {
        if (!isAdministrator()) return
        _tasks.value = listOf(task) + _tasks.value
    }

    fun updateTaskStatus(taskId: String, newStatus: TaskStatus, completionNotes: String = "") {
        val actor = _currentUser.value ?: return
        val task = _tasks.value.firstOrNull { it.id == taskId } ?: return
        if (!isAdministrator() && task.assignedEmployeeId != actor.id) return
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) {
                it.copy(
                    status = newStatus,
                    completionNotes = if (completionNotes.isNotBlank()) completionNotes else it.completionNotes
                )
            } else it
        }
    }

    fun reassignTask(taskId: String, employee: User) {
        if (!isAdministrator() || employee.role != UserRole.EMPLOYEE || employee.status != AccountStatus.APPROVED) return
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) {
                it.copy(assignedEmployeeId = employee.id, assignedEmployeeName = employee.fullName)
            } else it
        }
    }

    // --- Customer Management (CRM) ---

    fun addCustomer(customer: IspCustomer) {
        if (!isAdministrator()) return
        _customers.value = listOf(customer) + _customers.value
    }

    fun updateCustomer(customer: IspCustomer) {
        if (!isAdministrator()) return
        _customers.value = _customers.value.map { if (it.id == customer.id) customer else it }
    }

    fun recordCustomerRenewal(customerId: String, paymentMode: PaymentMode) {
        if (!isAdministrator()) return
        val cust = _customers.value.firstOrNull { it.id == customerId } ?: return
        val cal = getKolkataCalendar()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        val newExpiry = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(cal.time)

        _customers.value = _customers.value.map {
            if (it.id == customerId) it.copy(expiryDate = newExpiry, status = CustomerStatus.ACTIVE) else it
        }

        // Record income transaction in accounting module
        val txn = AccountingTransaction(
            id = "TXN-" + System.currentTimeMillis().toString().takeLast(8),
            type = TransactionType.INCOME,
            category = "Customer Renewal",
            amount = cust.monthlyCharges * 1.18, // Incl GST
            paymentMode = paymentMode,
            dateMillis = System.currentTimeMillis(),
            description = "Subscription renewal for ${cust.fullName} (${cust.planName})",
            referenceId = customerId
        )
        addTransaction(txn)
    }

    // --- ISP Plans ---

    fun addOrUpdatePlan(plan: IspPlan) {
        if (!isAdministrator()) return
        val current = _plans.value
        val idx = current.indexOfFirst { it.id == plan.id }
        _plans.value = if (idx >= 0) {
            current.toMutableList().apply { set(idx, plan) }
        } else {
            current + plan
        }
    }

    fun deletePlan(planId: String) {
        if (!isAdministrator()) return
        _plans.value = _plans.value.filter { it.id != planId }
    }

    // --- Finance & Accounting (Tally Inspired) ---

    fun addTransaction(transaction: AccountingTransaction) {
        if (!isAdministrator()) return
        val actorName = _currentUser.value?.fullName ?: return
        _transactions.value = listOf(transaction.copy(recordedBy = actorName)) + _transactions.value
    }

    fun getTotalIncome(): Double {
        return _transactions.value.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    fun getTotalExpenses(): Double {
        return _transactions.value.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    fun getNetProfit(): Double {
        return getTotalIncome() - getTotalExpenses()
    }

    // --- Inventory Management ---

    fun addInventoryItem(item: InventoryItem) {
        if (!isAdministrator()) return
        _inventory.value = listOf(item) + _inventory.value
    }

    fun updateStock(itemId: String, changeAmount: Int) {
        if (!isAdministrator() || changeAmount == 0) return
        _inventory.value = _inventory.value.map {
            if (it.id == itemId) {
                val newAvailable = (it.availableStock + changeAmount).coerceAtLeast(0)
                it.copy(availableStock = newAvailable, lastUpdatedMillis = System.currentTimeMillis())
            } else it
        }
    }

    fun issueEquipment(itemId: String, quantity: Int, recipient: String) {
        if (!isAdministrator() || quantity <= 0 || recipient.isBlank()) return
        _inventory.value = _inventory.value.map {
            if (it.id == itemId) {
                val newAvail = (it.availableStock - quantity).coerceAtLeast(0)
                val newIssued = it.issuedStock + quantity
                it.copy(availableStock = newAvail, issuedStock = newIssued, lastUpdatedMillis = System.currentTimeMillis())
            } else it
        }
    }

    // --- Complaints & Support ---

    fun addComplaint(ticket: ComplaintTicket) {
        val actor = _currentUser.value ?: return
        if (!isAdministrator() && ticket.assignedTechnicianId != actor.id) return
        _complaints.value = listOf(ticket) + _complaints.value
    }

    fun updateComplaintStatus(ticketId: String, status: ComplaintStatus, resolution: String = "") {
        val actor = _currentUser.value ?: return
        val ticket = _complaints.value.firstOrNull { it.id == ticketId } ?: return
        if (!isAdministrator() && ticket.assignedTechnicianId != actor.id) return
        val now = System.currentTimeMillis()
        _complaints.value = _complaints.value.map {
            if (it.id == ticketId) {
                it.copy(
                    status = status,
                    resolvedAtMillis = if (status == ComplaintStatus.RESOLVED || status == ComplaintStatus.CLOSED) now else it.resolvedAtMillis,
                    resolutionNotes = if (resolution.isNotBlank()) resolution else it.resolutionNotes
                )
            } else it
        }
    }

    fun assignComplaintTechnician(ticketId: String, tech: User) {
        if (!isAdministrator() || tech.role != UserRole.EMPLOYEE || tech.status != AccountStatus.APPROVED) return
        _complaints.value = _complaints.value.map {
            if (it.id == ticketId) {
                it.copy(
                    assignedTechnicianId = tech.id,
                    assignedTechnicianName = tech.fullName,
                    status = ComplaintStatus.ASSIGNED
                )
            } else it
        }
    }

    // --- Chat & Announcements ---

    fun sendChatMessage(sender: User, content: String, channel: String = "TEAM", isBroadcast: Boolean = false) {
        if (_currentUser.value?.id != sender.id || content.isBlank()) return
        if ((isBroadcast || channel == "ANNOUNCEMENT") && !isAdministrator()) return
        val msg = ChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            senderId = sender.id,
            senderName = sender.fullName,
            senderRole = sender.role,
            channel = channel,
            content = content.trim(),
            timestampMillis = System.currentTimeMillis(),
            isBroadcast = isBroadcast
        )
        _chatMessages.value = _chatMessages.value + msg
    }

    // --- Employee Requests ---

    fun submitRequest(employee: User, type: RequestType, title: String, description: String) {
        if (_currentUser.value?.id != employee.id || title.isBlank() || description.isBlank()) return
        val req = EmployeeRequest(
            id = "req_" + UUID.randomUUID().toString().take(8),
            employeeId = employee.id,
            employeeName = employee.fullName,
            type = type,
            requestTitle = title.trim(),
            description = description.trim(),
            requestedDateMillis = System.currentTimeMillis(),
            status = RequestStatus.PENDING_REVIEW
        )
        _requests.value = listOf(req) + _requests.value
    }

    fun reviewRequest(requestId: String, isApproved: Boolean, adminNotes: String) {
        if (!isAdministrator()) return
        _requests.value = _requests.value.map {
            if (it.id == requestId) {
                it.copy(
                    status = if (isApproved) RequestStatus.APPROVED else RequestStatus.REJECTED,
                    adminDecisionNotes = adminNotes,
                    reviewedAtMillis = System.currentTimeMillis()
                )
            } else it
        }
    }

    // --- Invoicing & Billing Engine ---

    fun createInvoice(
        customerId: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        items: List<InvoiceLineItem>,
        discountAmount: Double,
        gstRatePercent: Double,
        billingPeriodStart: String,
        billingPeriodEnd: String,
        notes: String,
        createdBy: String = "Super Admin"
    ): Result<IspInvoice> {
        if (!isAdministrator()) return Result.failure(SecurityException("Administrator access required."))
        if (items.isEmpty() || items.any { it.quantity <= 0 || it.unitRate < 0.0 }) return Result.failure(IllegalArgumentException("Add valid invoice line items."))
        val subtotal = items.sumOf { it.totalAmount }
        val discountedSubtotal = (subtotal - discountAmount).coerceAtLeast(0.0)
        val gstTotal = discountedSubtotal * (gstRatePercent / 100.0)
        val cgst = gstTotal / 2.0
        val sgst = gstTotal / 2.0
        val totalAmount = discountedSubtotal + gstTotal

        val seq = _invoices.value.size + 1
        val invoiceYear = getKolkataCalendar().get(Calendar.YEAR)
        val invoiceNo = "INV-${invoiceYear}-${String.format("%04d", seq)}"

        val newInvoice = IspInvoice(
            id = "inv_" + UUID.randomUUID().toString().take(8),
            invoiceNumber = invoiceNo,
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            items = items,
            subtotal = subtotal,
            discountAmount = discountAmount,
            gstRatePercent = gstRatePercent,
            cgstAmount = cgst,
            sgstAmount = sgst,
            igstAmount = 0.0,
            totalAmount = totalAmount,
            paidAmount = 0.0,
            balanceDue = totalAmount,
            status = InvoiceStatus.UNPAID,
            invoiceDateMillis = System.currentTimeMillis(),
            dueDateMillis = System.currentTimeMillis() + (7L * 24 * 3600 * 1000),
            billingPeriodStart = billingPeriodStart,
            billingPeriodEnd = billingPeriodEnd,
            notes = notes,
            createdBy = _currentUser.value?.fullName ?: createdBy
        )

        _invoices.value = listOf(newInvoice) + _invoices.value
        return Result.success(newInvoice)
    }

    fun recordInvoicePayment(
        invoiceId: String,
        amount: Double,
        paymentMode: PaymentMode,
        transactionReference: String,
        note: String = "Payment received",
        recordedBy: String = "Admin"
    ): Result<IspInvoice> {
        if (!isAdministrator()) return Result.failure(SecurityException("Administrator access required."))
        if (amount <= 0.0) return Result.failure(IllegalArgumentException("Payment amount must be greater than zero."))
        val invoice = _invoices.value.firstOrNull { it.id == invoiceId }
            ?: return Result.failure(Exception("Invoice not found."))

        if (invoice.status == InvoiceStatus.PAID) {
            return Result.failure(Exception("Invoice is already fully paid."))
        }

        val newPaid = (invoice.paidAmount + amount).coerceAtMost(invoice.totalAmount)
        val newBalance = (invoice.totalAmount - newPaid).coerceAtLeast(0.0)
        val newStatus = if (newBalance <= 0.01) InvoiceStatus.PAID else InvoiceStatus.PARTIALLY_PAID

        val paymentRecord = InvoicePaymentRecord(
            id = "pay_" + UUID.randomUUID().toString().take(8),
            invoiceId = invoiceId,
            amount = amount,
            paymentMode = paymentMode,
            transactionReference = transactionReference.trim(),
            paymentDateMillis = System.currentTimeMillis(),
            note = note.trim()
        )

        val updated = invoice.copy(
            paidAmount = newPaid,
            balanceDue = newBalance,
            status = newStatus,
            payments = invoice.payments + paymentRecord
        )

        _invoices.value = _invoices.value.map { if (it.id == invoiceId) updated else it }

        // Also record in accounting ledger as income voucher
        addTransaction(
            AccountingTransaction(
                id = "TXN-" + UUID.randomUUID().toString().take(8),
                type = TransactionType.INCOME,
                category = "Customer Invoice Payment",
                amount = amount,
                paymentMode = paymentMode,
                description = "Payment for ${invoice.invoiceNumber} (${invoice.customerName}) - $note",
                referenceId = invoice.invoiceNumber,
                recordedBy = _currentUser.value?.fullName ?: recordedBy
            )
        )

        return Result.success(updated)
    }

    fun cancelInvoice(invoiceId: String) {
        if (!isAdministrator()) return
        _invoices.value = _invoices.value.map {
            if (it.id == invoiceId) it.copy(status = InvoiceStatus.CANCELLED) else it
        }
    }

    // --- Location Breadcrumbs & GPS Route History ---

    fun recordLocationBreadcrumb(
        employeeId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        note: String = ""
    ) {
        val actor = _currentUser.value ?: return
        if (actor.id != employeeId && !isAdministrator()) return
        if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return
        val bc = LocationBreadcrumb(
            id = "bc_" + UUID.randomUUID().toString().take(8),
            employeeId = employeeId,
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy,
            timestampMillis = System.currentTimeMillis(),
            areaNote = note
        )
        _locationBreadcrumbs.value = _locationBreadcrumbs.value + bc
    }

    fun getLocationBreadcrumbsForEmployee(employeeId: String): List<LocationBreadcrumb> {
        val actor = _currentUser.value ?: return emptyList()
        if (actor.id != employeeId && !isAdministrator()) return emptyList()
        return _locationBreadcrumbs.value.filter { it.employeeId == employeeId }
    }

    // --- Staff Management Admin Controls ---

    fun suspendEmployeeAccount(userId: String, suspend: Boolean) {
        if (!isAdministrator()) return
        val actor = _currentUser.value ?: return
        val target = _users.value.firstOrNull { it.id == userId } ?: return
        if (actor.role != UserRole.SUPER_ADMIN && target.role != UserRole.EMPLOYEE) return
        if (target.role == UserRole.SUPER_ADMIN || target.id == actor.id) return
        _users.value = _users.value.map {
            if (it.id == userId) {
                it.copy(status = if (suspend) AccountStatus.SUSPENDED else AccountStatus.APPROVED)
            } else it
        }
    }

    fun correctAttendanceRecord(
        recordId: String,
        newStatus: ShiftStatus,
        newStartMillis: Long,
        newEndMillis: Long?,
        reason: String,
        adminName: String
    ): Result<AttendanceRecord> {
        if (!isAdministrator()) return Result.failure(SecurityException("Administrator access required."))
        if (reason.isBlank()) return Result.failure(IllegalArgumentException("A correction reason is required."))
        val existing = _attendanceRecords.value.firstOrNull { it.id == recordId }
            ?: return Result.failure(Exception("Attendance record not found."))

        val duration = if (newEndMillis != null && newEndMillis > newStartMillis) {
            (newEndMillis - newStartMillis) / 60000
        } else {
            existing.durationMinutes
        }

        val updated = existing.copy(
            status = newStatus,
            shiftStartTimeMillis = newStartMillis,
            shiftEndTimeMillis = newEndMillis,
            durationMinutes = duration,
            adminOverrideReason = "Corrected by $adminName: $reason"
        )

        _attendanceRecords.value = _attendanceRecords.value.map {
            if (it.id == recordId) updated else it
        }
        return Result.success(updated)
    }

    // --- Photo & Video Media Management ---

    fun uploadMediaItem(
        title: String,
        type: MediaType,
        category: MediaCategory,
        description: String,
        tags: List<String>,
        uploader: User,
        customerId: String = "",
        customerName: String = "",
        relatedJobId: String = "",
        albumName: String = "General Uploads",
        durationSeconds: Int = 0,
        fileSizeBytes: Long = 0L,
        fileUri: String = ""
    ): MediaItem {
        require(_currentUser.value?.id == uploader.id) { "You can only upload media under your signed-in account." }
        val newItem = MediaItem(
            id = "med_" + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            type = type,
            category = category,
            fileUri = fileUri,
            resolution = "",
            fileSizeBytes = fileSizeBytes,
            durationSeconds = durationSeconds,
            tags = tags,
            description = description.trim(),
            uploaderId = uploader.id,
            uploaderName = uploader.fullName,
            uploaderRole = uploader.role,
            customerId = customerId,
            customerName = customerName,
            relatedJobId = relatedJobId,
            albumName = albumName,
            uploadDateMillis = System.currentTimeMillis(),
            approvalStatus = if (uploader.role == UserRole.EMPLOYEE) MediaApprovalStatus.PENDING_REVIEW else MediaApprovalStatus.APPROVED,
            visualAssetType = if (type == MediaType.VIDEO) "VIDEO_GENERAL" else "PHOTO_GENERAL"
        )
        _mediaItems.value = listOf(newItem) + _mediaItems.value
        return newItem
    }

    fun deleteMediaItem(mediaId: String) {
        val actor = _currentUser.value ?: return
        val item = _mediaItems.value.firstOrNull { it.id == mediaId } ?: return
        if (!isAdministrator() && item.uploaderId != actor.id) return
        _mediaItems.value = _mediaItems.value.filter { it.id != mediaId }
    }

    fun updateMediaApprovalStatus(mediaId: String, status: MediaApprovalStatus) {
        if (!isAdministrator()) return
        _mediaItems.value = _mediaItems.value.map {
            if (it.id == mediaId) it.copy(approvalStatus = status) else it
        }
    }

    fun toggleMediaFavorite(mediaId: String) {
        if (_currentUser.value == null) return
        _mediaItems.value = _mediaItems.value.map {
            if (it.id == mediaId) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    fun createMediaAlbum(name: String, description: String, category: MediaCategory, createdBy: String): MediaAlbum {
        check(isAdministrator()) { "Administrator access required." }
        val album = MediaAlbum(
            id = "alb_" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            description = description.trim(),
            category = category,
            createdBy = _currentUser.value?.fullName ?: createdBy,
            createdAtMillis = System.currentTimeMillis()
        )
        _mediaAlbums.value = _mediaAlbums.value + album
        return album
    }
}
