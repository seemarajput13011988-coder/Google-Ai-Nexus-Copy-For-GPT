package com.example.data.repository

import android.content.Context
import com.example.data.model.*
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
        seedInitialEnterpriseData()
        checkAndAutoCloseExpiredShifts()
    }

    private fun seedInitialEnterpriseData() {
        val seededUsers = listOf(
            User(
                id = "usr_superadmin",
                fullName = "Sunil Rajput",
                username = "admin",
                password = "password123",
                phone = "+91 98765 43210",
                email = "sunil@kbnet.com",
                address = "Head Office, Sector 6, Central Hub",
                designation = "Super Admin & Director",
                department = "Executive Management",
                joiningDate = "2020-01-15",
                baseSalary = 120000.0,
                role = UserRole.SUPER_ADMIN,
                status = AccountStatus.APPROVED,
                avatarEmoji = "👑"
            ),
            User(
                id = "usr_admin2",
                fullName = "Pooja Sharma",
                username = "pooja.admin",
                password = "password123",
                phone = "+91 98234 56789",
                email = "pooja@kbnet.com",
                address = "Operations Floor, Suite 202",
                designation = "Operations & CRM Manager",
                department = "Administration",
                joiningDate = "2021-03-01",
                baseSalary = 65000.0,
                role = UserRole.ADMIN,
                status = AccountStatus.APPROVED,
                avatarEmoji = "👩‍💼"
            ),
            User(
                id = "usr_emp_vikram",
                fullName = "Vikram Singh",
                username = "vikram",
                password = "password123",
                phone = "+91 99112 33445",
                email = "vikram.field@kbnet.com",
                address = "Quarter 45, Railway Colony",
                designation = "Senior Optical Fiber Splicer",
                department = "Field Technical Ops",
                joiningDate = "2022-06-10",
                baseSalary = 34000.0,
                role = UserRole.EMPLOYEE,
                status = AccountStatus.APPROVED,
                avatarEmoji = "👨‍🔧"
            ),
            User(
                id = "usr_emp_rahul",
                fullName = "Rahul Verma",
                username = "rahul",
                password = "password123",
                phone = "+91 97123 44556",
                email = "rahul.net@kbnet.com",
                address = "B-12, Sector 4 Market",
                designation = "FTTH Installation Engineer",
                department = "Field Technical Ops",
                joiningDate = "2023-02-15",
                baseSalary = 30000.0,
                role = UserRole.EMPLOYEE,
                status = AccountStatus.APPROVED,
                avatarEmoji = "👷"
            ),
            User(
                id = "usr_emp_anita",
                fullName = "Anita Das",
                username = "anita",
                password = "password123",
                phone = "+91 98665 77889",
                email = "anita.crm@kbnet.com",
                address = "Flat 304, Green Heights",
                designation = "Helpdesk & Billing Officer",
                department = "Customer Support",
                joiningDate = "2023-08-01",
                baseSalary = 28000.0,
                role = UserRole.EMPLOYEE,
                status = AccountStatus.APPROVED,
                avatarEmoji = "👩‍💻"
            ),
            User(
                id = "usr_pending_amit",
                fullName = "Amit Rawat",
                username = "amit.r",
                password = "password123",
                phone = "+91 95443 22110",
                email = "amit.rawat@gmail.com",
                address = "Village Road, Near Old Substation",
                designation = "Junior Field Technician",
                department = "Field Technical Ops",
                joiningDate = "2026-11-01",
                baseSalary = 22000.0,
                role = UserRole.EMPLOYEE,
                status = AccountStatus.PENDING_APPROVAL,
                registrationReason = "Applied via website for Sector 8 field maintenance",
                avatarEmoji = "🧑‍🔧"
            )
        )
        _users.value = seededUsers
        // Default login as Super Admin for instantaneous evaluation
        _currentUser.value = seededUsers.first()

        val seededPlans = listOf(
            IspPlan("plan_rail_50", "RailWire 50 Mbps Unlimited", "RailWire / RailTel", 50, "Unlimited", 499.0, 18.0, 30, true, 350, "Symmetrical speed, Free OTT & Live TV"),
            IspPlan("plan_bsnl_100", "BSNL Bharat Fibre 100M", "BSNL", 100, "3300 GB FUP", 799.0, 18.0, 30, true, 400, "Low latency fiber broadband with Disney+ Hotstar"),
            IspPlan("plan_air_200", "Air Fibre Turbo 200M", "Air Fibre", 200, "Unlimited", 999.0, 18.0, 30, true, 450, "Wireless & FTTH hybrid gigabit connection"),
            IspPlan("plan_one_300", "One Broadband Pro 300M", "One Broadband", 300, "Unlimited", 1499.0, 18.0, 30, false, 0, "Commercial dedicated bandwidth with 1 Free Static IP"),
            IspPlan("plan_tikona_500", "Tikona Enterprise 500M", "Tikona", 500, "Unlimited", 2999.0, 18.0, 30, false, 0, "1:1 Leased line with 99.9% uptime SLA")
        )
        _plans.value = seededPlans

        val seededCustomers = listOf(
            IspCustomer("KB-CUST-101", "Rameshwar Dayal", "+91 98111 22334", "Plot 14, Sector 3 Main Market", "Sector 3", "RailWire / RailTel", "plan_rail_50", "RailWire 50 Mbps Unlimited", 50, 499.0, "2024-01-10", "2026-11-20", "FTTH Fiber", "192.168.10.14", "F4:8E:38:12:AA:01", "usr_emp_vikram", CustomerStatus.ACTIVE),
            IspCustomer("KB-CUST-102", "Dr. Alok Srivastava", "+91 98222 33445", "House 88, Civil Lines North", "Civil Lines", "BSNL", "plan_bsnl_100", "BSNL Bharat Fibre 100M", 100, 799.0, "2024-03-15", "2026-11-28", "FTTH Fiber", "192.168.10.88", "F4:8E:38:12:AA:02", "usr_emp_rahul", CustomerStatus.ACTIVE),
            IspCustomer("KB-CUST-103", "Kisan Rice Mill", "+91 98333 44556", "Industrial Area Gate 2", "Industrial Area", "Tikona", "plan_tikona_500", "Tikona Enterprise 500M", 500, 2999.0, "2023-11-01", "2026-11-12", "FTTH Fiber", "103.45.22.12", "D4:6E:0E:99:FF:01", "usr_emp_vikram", CustomerStatus.ACTIVE),
            IspCustomer("KB-CUST-104", "Priyanka Mehra", "+91 98444 55667", "Apt 204, Royal Palms, Sector 4", "Sector 4", "Air Fibre", "plan_air_200", "Air Fibre Turbo 200M", 200, 999.0, "2024-05-20", "2026-11-04", "Wireless AirFibre", "192.168.12.204", "BC:A8:A6:44:22:11", "usr_emp_rahul", CustomerStatus.EXPIRED),
            IspCustomer("KB-CUST-105", "Apex Coaching Institute", "+91 98555 66778", "Commercial Complex 2nd Floor", "City Center", "One Broadband", "plan_one_300", "One Broadband Pro 300M", 300, 1499.0, "2024-07-01", "2026-12-01", "FTTH Fiber", "103.45.22.50", "BC:A8:A6:55:33:22", "usr_emp_vikram", CustomerStatus.ACTIVE),
            IspCustomer("KB-CUST-106", "Sandeep Rawat", "+91 98666 77889", "Gali 3, Subhash Nagar", "Subhash Nagar", "RailWire / RailTel", "plan_rail_50", "RailWire 50 Mbps Unlimited", 50, 499.0, "2024-08-14", "2026-11-15", "FTTH Fiber", "192.168.10.45", "F4:8E:38:99:11:22", "usr_emp_rahul", CustomerStatus.ACTIVE),
            IspCustomer("KB-CUST-107", "Modern Diagnostic Lab", "+91 98777 88990", "Hospital Road Near Bus Stand", "Civil Lines", "BSNL", "plan_bsnl_100", "BSNL Bharat Fibre 100M", 100, 799.0, "2024-02-18", "2026-11-06", "FTTH Fiber", "192.168.10.92", "D4:6E:0E:11:44:77", "usr_emp_vikram", CustomerStatus.EXPIRED),
            IspCustomer("KB-CUST-108", "Harsh Vardhan", "+91 98888 99001", "Flat 501, Silver Residency", "Sector 8", "Air Fibre", "plan_air_200", "Air Fibre Turbo 200M", 200, 999.0, "2026-11-06", "2026-12-06", "FTTH Fiber", "192.168.14.10", "Pending Equipment", "usr_emp_rahul", CustomerStatus.PENDING_INSTALL)
        )
        _customers.value = seededCustomers

        val now = System.currentTimeMillis()
        val todayIso = getKolkataDateIso(now)
        val cal = getKolkataCalendar()
        cal.set(Calendar.HOUR_OF_DAY, 8)
        cal.set(Calendar.MINUTE, 30)
        val morningStart = cal.timeInMillis

        // Vikram has an ongoing shift today
        val seededAttendance = listOf(
            AttendanceRecord(
                id = "att_vikram_today",
                employeeId = "usr_emp_vikram",
                employeeName = "Vikram Singh",
                dateIso = todayIso,
                shiftStartTimeMillis = morningStart,
                shiftEndTimeMillis = null,
                status = ShiftStatus.ACTIVE_SHIFT,
                durationMinutes = ((now - morningStart) / 60000).coerceAtLeast(15),
                isAutoClosed = false,
                isManualClose = false,
                locationStartLat = 28.6210,
                locationStartLng = 77.2140
            ),
            AttendanceRecord(
                id = "att_rahul_today",
                employeeId = "usr_emp_rahul",
                employeeName = "Rahul Verma",
                dateIso = todayIso,
                shiftStartTimeMillis = morningStart + 900000,
                shiftEndTimeMillis = null,
                status = ShiftStatus.ACTIVE_SHIFT,
                durationMinutes = ((now - (morningStart + 900000)) / 60000).coerceAtLeast(10),
                isAutoClosed = false,
                isManualClose = false,
                locationStartLat = 28.6180,
                locationStartLng = 77.2280
            ),
            AttendanceRecord(
                id = "att_anita_yesterday",
                employeeId = "usr_emp_anita",
                employeeName = "Anita Das",
                dateIso = "2026-11-06",
                shiftStartTimeMillis = morningStart - 86400000,
                shiftEndTimeMillis = morningStart - 86400000 + 28800000,
                status = ShiftStatus.COMPLETED,
                durationMinutes = 480,
                isAutoClosed = false,
                isManualClose = true,
                locationStartLat = 28.6139,
                locationStartLng = 77.2090,
                locationEndLat = 28.6139,
                locationEndLng = 77.2090
            )
        )
        _attendanceRecords.value = seededAttendance

        val seededLocations = listOf(
            EmployeeLocation(
                employeeId = "usr_emp_vikram",
                employeeName = "Vikram Singh",
                latitude = 28.6255,
                longitude = 77.2185,
                accuracyMeters = 4.2f,
                timestampMillis = now - 35000, // 35s ago (LIVE)
                isLive = true,
                batteryPercent = 92,
                shiftId = "att_vikram_today",
                areaDescription = "Sector 3 Fiber Junction Box #4"
            ),
            EmployeeLocation(
                employeeId = "usr_emp_rahul",
                employeeName = "Rahul Verma",
                latitude = 28.6140,
                longitude = 77.2310,
                accuracyMeters = 6.8f,
                timestampMillis = now - 70000, // 70s ago (LIVE)
                isLive = true,
                batteryPercent = 78,
                shiftId = "att_rahul_today",
                areaDescription = "Sector 4 Main Road (Near Tower 2)"
            ),
            EmployeeLocation(
                employeeId = "usr_emp_anita",
                employeeName = "Anita Das",
                latitude = 28.6139,
                longitude = 77.2090,
                accuracyMeters = 12.0f,
                timestampMillis = now - 86400000, // Yesterday (LAST KNOWN)
                isLive = false,
                batteryPercent = 65,
                shiftId = "",
                areaDescription = "Head Office Desk #3"
            )
        )
        _employeeLocations.value = seededLocations

        val seededTasks = listOf(
            TaskItem(
                id = "tsk_101",
                title = "Emergency Optical Fiber Splicing",
                description = "24-core backbone severed near Sector 4 Flyover by road widening JCB. High loss on 8 nodes.",
                assignedEmployeeId = "usr_emp_vikram",
                assignedEmployeeName = "Vikram Singh",
                createdBy = "Sunil Rajput",
                priority = TaskPriority.CRITICAL,
                status = TaskStatus.IN_PROGRESS,
                deadlineMillis = now + 7200000, // in 2 hours
                locationAddress = "Sector 4 Flyover Pillar 12",
                completionNotes = "Spliced 12 cores so far, OTDR testing underway"
            ),
            TaskItem(
                id = "tsk_102",
                title = "New FTTH Connection Installation",
                description = "Customer Harsh Vardhan (KB-CUST-108). Route drop cable from Pole 4 to 5th floor flat. Configure Wi-Fi ONT.",
                assignedEmployeeId = "usr_emp_rahul",
                assignedEmployeeName = "Rahul Verma",
                createdBy = "Pooja Sharma",
                priority = TaskPriority.HIGH,
                status = TaskStatus.ACCEPTED,
                deadlineMillis = now + 14400000,
                customerId = "KB-CUST-108",
                customerName = "Harsh Vardhan",
                locationAddress = "Flat 501, Silver Residency, Sector 8"
            ),
            TaskItem(
                id = "tsk_103",
                title = "Router Upgrade & Optical Power Reading",
                description = "Customer Kisan Rice Mill (KB-CUST-103) requested gigabit port test and power attenuation check.",
                assignedEmployeeId = "usr_emp_vikram",
                assignedEmployeeName = "Vikram Singh",
                createdBy = "Sunil Rajput",
                priority = TaskPriority.MEDIUM,
                status = TaskStatus.COMPLETED,
                deadlineMillis = now - 3600000,
                customerId = "KB-CUST-103",
                customerName = "Kisan Rice Mill",
                locationAddress = "Industrial Area Gate 2",
                completionNotes = "Optical power -18.2 dBm. Symmetrical 500 Mbps verified. Signed job sheet uploaded."
            )
        )
        _tasks.value = seededTasks

        val seededTransactions = listOf(
            AccountingTransaction("TXN-202611-001", TransactionType.INCOME, "Customer Renewal", 3538.0, PaymentMode.UPI, now - 86400000 * 3, "Kisan Rice Mill 500M Monthly Renewal + GST", "KB-CUST-103"),
            AccountingTransaction("TXN-202611-002", TransactionType.INCOME, "Customer Renewal", 942.0, PaymentMode.CASH, now - 86400000 * 2, "Dr. Alok Srivastava BSNL 100M Renewal", "KB-CUST-102"),
            AccountingTransaction("TXN-202611-003", TransactionType.INCOME, "New Installation", 2500.0, PaymentMode.UPI, now - 86400000, "Installation & ONT charges Harsh Vardhan", "KB-CUST-108"),
            AccountingTransaction("TXN-202611-004", TransactionType.EXPENSE, "Bandwidth Uplink", 45000.0, PaymentMode.BANK_TRANSFER, now - 86400000 * 4, "RailTel 10 Gbps POP Bandwidth Monthly Invoice"),
            AccountingTransaction("TXN-202611-005", TransactionType.EXPENSE, "Equipment Purchase", 18500.0, PaymentMode.BANK_TRANSFER, now - 86400000 * 2, "10x Syrotech Dual Band Gigabit ONTs from Delhi Wholesale"),
            AccountingTransaction("TXN-202611-006", TransactionType.EXPENSE, "Fuel & Conveyance", 1800.0, PaymentMode.CASH, now - 14400000, "Field Technician Bike Petrol Allowance (Week 1)")
        )
        _transactions.value = seededTransactions

        val seededSalaries = listOf(
            SalarySlip("SAL-202610-01", "usr_emp_vikram", "Vikram Singh", "October 2026", 34000.0, 26, 208.0, 1000.0, 2500.0, 35500.0, "PAID", now - 86400000 * 7),
            SalarySlip("SAL-202610-02", "usr_emp_rahul", "Rahul Verma", "October 2026", 30000.0, 25, 200.0, 0.0, 1500.0, 31500.0, "PAID", now - 86400000 * 7),
            SalarySlip("SAL-202610-03", "usr_emp_anita", "Anita Das", "October 2026", 28000.0, 26, 208.0, 0.0, 1000.0, 29000.0, "PAID", now - 86400000 * 7)
        )
        _salaries.value = seededSalaries

        val seededInventory = listOf(
            InventoryItem("INV-001", "Syrotech GPON 1GE+1FE+WiFi Dual Band ONT", InventoryCategory.ONU_ONT_DEVICE, 40, 18, 22, "Pcs", 8, 1450.0),
            InventoryItem("INV-002", "TP-Link Archer C6 AC1200 Gigabit Router", InventoryCategory.ROUTER_WIFI6, 25, 12, 13, "Pcs", 5, 2150.0),
            InventoryItem("INV-003", "Sterlite 2-Core FTTH Drop Fiber Cable Reel", InventoryCategory.FIBER_OPTIC_CABLE, 4000, 1600, 2400, "Meters", 500, 5.5),
            InventoryItem("INV-004", "SC/UPC Fast Field Mechanical Connectors", InventoryCategory.PATCH_CORD_CONNECTOR, 500, 140, 360, "Pcs", 60, 18.0),
            InventoryItem("INV-005", "DVP-740 Optical Core Alignment Splicer", InventoryCategory.SPLICING_TOOL_EQUIPMENT, 3, 1, 2, "Sets", 1, 65000.0),
            InventoryItem("INV-006", "Tri-Wavelength Optical Power Meter & VFL", InventoryCategory.SPLICING_TOOL_EQUIPMENT, 6, 2, 4, "Sets", 2, 2800.0)
        )
        _inventory.value = seededInventory

        val seededComplaints = listOf(
            ComplaintTicket(
                id = "TKT-801",
                customerId = "KB-CUST-104",
                customerName = "Priyanka Mehra",
                customerPhone = "+91 98444 55667",
                address = "Apt 204, Royal Palms, Sector 4",
                issueTitle = "ONT Red Light LOS Blinking",
                description = "Internet stopped suddenly at 9 AM. LOS red light is continuously blinking on router.",
                priority = ComplaintPriority.CRITICAL,
                status = ComplaintStatus.IN_PROGRESS,
                assignedTechnicianId = "usr_emp_vikram",
                assignedTechnicianName = "Vikram Singh",
                createdAtMillis = now - 7200000,
                resolutionNotes = "Tracing drop fiber from building splitter box."
            ),
            ComplaintTicket(
                id = "TKT-802",
                customerId = "KB-CUST-101",
                customerName = "Rameshwar Dayal",
                customerPhone = "+91 98111 22334",
                address = "Plot 14, Sector 3 Main Market",
                issueTitle = "Frequent Packet Drop During Billing Hours",
                description = "Shop POS payment terminal disconnecting intermittently.",
                priority = ComplaintPriority.HIGH,
                status = ComplaintStatus.ASSIGNED,
                assignedTechnicianId = "usr_emp_rahul",
                assignedTechnicianName = "Rahul Verma",
                createdAtMillis = now - 18000000
            )
        )
        _complaints.value = seededComplaints

        val seededChats = listOf(
            ChatMessage("msg_1", "usr_superadmin", "Sunil Rajput (Super Admin)", UserRole.SUPER_ADMIN, "ANNOUNCEMENT", "📢 Important: BSNL POP maintenance scheduled tonight from 2:00 AM to 4:00 AM. Inquire if any enterprise line reports issues.", now - 14400000, true),
            ChatMessage("msg_2", "usr_admin2", "Pooja Sharma", UserRole.ADMIN, "TEAM", "Team, please verify all pending customer renewal receipts before 6 PM today.", now - 7200000),
            ChatMessage("msg_3", "usr_emp_vikram", "Vikram Singh", UserRole.EMPLOYEE, "TEAM", "Sector 4 cut core 1-12 spliced. Loss is -17.5 dBm on all live nodes.", now - 3600000)
        )
        _chatMessages.value = seededChats

        val seededRequests = listOf(
            EmployeeRequest(
                id = "req_001",
                employeeId = "usr_emp_rahul",
                employeeName = "Rahul Verma",
                type = RequestType.LEAVE_APPLICATION,
                requestTitle = "Festival Leave on 15th Nov",
                description = "Requesting 2 days leave for family function in hometown.",
                requestedDateMillis = now - 86400000,
                status = RequestStatus.APPROVED,
                adminDecisionNotes = "Approved by Super Admin. Vikram will cover Sector 4.",
                reviewedAtMillis = now - 43200000
            ),
            EmployeeRequest(
                id = "req_002",
                employeeId = "usr_emp_vikram",
                employeeName = "Vikram Singh",
                type = RequestType.EQUIPMENT_REQUEST,
                requestTitle = "New Cleaver Blade for DVP Splicer",
                description = "Current cleaver blade has exceeded 12,000 cuts and causing high splice loss.",
                requestedDateMillis = now - 18000000,
                status = RequestStatus.PENDING_REVIEW
            )
        )
        _requests.value = seededRequests

        // Seeded Location Breadcrumbs (Route History)
        val seededBreadcrumbs = listOf(
            LocationBreadcrumb("bc_1", "usr_emp_vikram", 28.6139, 77.2090, 4.0f, now - 7200000, "Depot Check-in"),
            LocationBreadcrumb("bc_2", "usr_emp_vikram", 28.6180, 77.2120, 5.2f, now - 5400000, "Sector 4 Main Road En Route"),
            LocationBreadcrumb("bc_3", "usr_emp_vikram", 28.6210, 77.2140, 3.8f, now - 3600000, "Sector 4 Flyover Cut Point"),
            LocationBreadcrumb("bc_4", "usr_emp_vikram", 28.6240, 77.2165, 4.5f, now - 1800000, "Testing Spliced Cores 1-12"),
            LocationBreadcrumb("bc_5", "usr_emp_vikram", 28.6255, 77.2185, 4.2f, now - 35000, "Sector 3 Fiber Junction Box #4 (Current)"),

            LocationBreadcrumb("bc_r1", "usr_emp_rahul", 28.6110, 77.2250, 6.0f, now - 5400000, "Sector 8 Depot Start"),
            LocationBreadcrumb("bc_r2", "usr_emp_rahul", 28.6125, 77.2280, 5.5f, now - 3600000, "Silver Residency Gate Entry"),
            LocationBreadcrumb("bc_r3", "usr_emp_rahul", 28.6140, 77.2310, 6.8f, now - 70000, "Flat 501 ONT Configuration (Current)")
        )
        _locationBreadcrumbs.value = seededBreadcrumbs

        // Seeded Professional Invoices with Multi-Item & GST
        val seededInvoices = listOf(
            IspInvoice(
                id = "inv_001",
                invoiceNumber = "INV-2026-0001",
                customerId = "KB-CUST-103",
                customerName = "Kisan Rice Mill",
                customerPhone = "+91 98333 44556",
                customerAddress = "Industrial Area Gate 2",
                items = listOf(
                    InvoiceLineItem("item_1", "Tikona Enterprise 500 Mbps Monthly Subscription", "998422", 1, 2999.0, 2999.0)
                ),
                subtotal = 2999.0,
                discountAmount = 0.0,
                gstRatePercent = 18.0,
                cgstAmount = 269.91,
                sgstAmount = 269.91,
                igstAmount = 0.0,
                totalAmount = 3538.82,
                paidAmount = 3538.82,
                balanceDue = 0.0,
                status = InvoiceStatus.PAID,
                invoiceDateMillis = now - (86400000L * 3),
                dueDateMillis = now + (86400000L * 7),
                billingPeriodStart = "2026-11-01",
                billingPeriodEnd = "2026-11-30",
                payments = listOf(
                    InvoicePaymentRecord("pay_1", "inv_001", 3538.82, PaymentMode.UPI, "UPI/202611/449102", now - (86400000L * 3), "Full payment via Google Pay")
                )
            ),
            IspInvoice(
                id = "inv_002",
                invoiceNumber = "INV-2026-0002",
                customerId = "KB-CUST-102",
                customerName = "Dr. Alok Srivastava",
                customerPhone = "+91 98222 33445",
                customerAddress = "House 88, Civil Lines North",
                items = listOf(
                    InvoiceLineItem("item_2", "BSNL Bharat Fibre 100 Mbps Monthly Renewal", "998422", 1, 799.0, 799.0)
                ),
                subtotal = 799.0,
                discountAmount = 0.0,
                gstRatePercent = 18.0,
                cgstAmount = 71.91,
                sgstAmount = 71.91,
                igstAmount = 0.0,
                totalAmount = 942.82,
                paidAmount = 942.82,
                balanceDue = 0.0,
                status = InvoiceStatus.PAID,
                invoiceDateMillis = now - (86400000L * 2),
                dueDateMillis = now + (86400000L * 5),
                billingPeriodStart = "2026-11-01",
                billingPeriodEnd = "2026-11-30",
                payments = listOf(
                    InvoicePaymentRecord("pay_2", "inv_002", 942.82, PaymentMode.CASH, "REC-CASH-8812", now - (86400000L * 2), "Cash collected at clinic")
                )
            ),
            IspInvoice(
                id = "inv_003",
                invoiceNumber = "INV-2026-0003",
                customerId = "KB-CUST-108",
                customerName = "Harsh Vardhan",
                customerPhone = "+91 98888 99001",
                customerAddress = "Flat 501, Silver Residency, Sector 8",
                items = listOf(
                    InvoiceLineItem("item_3a", "Air Fibre Turbo 200M (Month 1 Subscription)", "998422", 1, 999.0, 999.0),
                    InvoiceLineItem("item_3b", "Syrotech GPON Dual Band Wi-Fi ONT Device", "851762", 1, 1450.0, 1450.0),
                    InvoiceLineItem("item_3c", "FTTH Drop Cable & Precision Installation Setup", "998719", 1, 1000.0, 1000.0)
                ),
                subtotal = 3449.0,
                discountAmount = 0.0,
                gstRatePercent = 18.0,
                cgstAmount = 310.41,
                sgstAmount = 310.41,
                igstAmount = 0.0,
                totalAmount = 4069.82,
                paidAmount = 2500.00,
                balanceDue = 1569.82,
                status = InvoiceStatus.PARTIALLY_PAID,
                invoiceDateMillis = now - 86400000L,
                dueDateMillis = now + (86400000L * 4),
                billingPeriodStart = "2026-11-06",
                billingPeriodEnd = "2026-12-06",
                notes = "Installation partial advance ₹2,500 collected by technician Rahul Verma. Balance due upon speed sign-off.",
                payments = listOf(
                    InvoicePaymentRecord("pay_3", "inv_003", 2500.00, PaymentMode.UPI, "UPI/202611/981244", now - 86400000L, "Installation advance via PhonePe")
                )
            ),
            IspInvoice(
                id = "inv_004",
                invoiceNumber = "INV-2026-0004",
                customerId = "KB-CUST-104",
                customerName = "Priyanka Mehra",
                customerPhone = "+91 98444 55667",
                customerAddress = "Apt 204, Royal Palms, Sector 4",
                items = listOf(
                    InvoiceLineItem("item_4", "Air Fibre Turbo 200 Mbps Monthly Plan Renewal", "998422", 1, 999.0, 999.0)
                ),
                subtotal = 999.0,
                discountAmount = 0.0,
                gstRatePercent = 18.0,
                cgstAmount = 89.91,
                sgstAmount = 89.91,
                igstAmount = 0.0,
                totalAmount = 1178.82,
                paidAmount = 0.0,
                balanceDue = 1178.82,
                status = InvoiceStatus.UNPAID,
                invoiceDateMillis = now - (86400000L * 4),
                dueDateMillis = now + (86400000L * 2),
                billingPeriodStart = "2026-11-04",
                billingPeriodEnd = "2026-12-04"
            ),
            IspInvoice(
                id = "inv_005",
                invoiceNumber = "INV-2026-0005",
                customerId = "KB-CUST-105",
                customerName = "Apex Coaching Institute",
                customerPhone = "+91 98555 66778",
                customerAddress = "Commercial Complex 2nd Floor, City Center",
                items = listOf(
                    InvoiceLineItem("item_5a", "One Broadband Pro 300 Mbps (Quarterly 3 Months)", "998422", 3, 1499.0, 4497.0),
                    InvoiceLineItem("item_5b", "Dedicated Public Static IP (Quarterly)", "998422", 1, 600.0, 600.0)
                ),
                subtotal = 5097.0,
                discountAmount = 500.0, // Commercial discount
                gstRatePercent = 18.0,
                cgstAmount = 413.73,
                sgstAmount = 413.73,
                igstAmount = 0.0,
                totalAmount = 5424.46,
                paidAmount = 5424.46,
                balanceDue = 0.0,
                status = InvoiceStatus.PAID,
                invoiceDateMillis = now - (86400000L * 5),
                dueDateMillis = now + (86400000L * 10),
                billingPeriodStart = "2026-11-01",
                billingPeriodEnd = "2027-01-31",
                payments = listOf(
                    InvoicePaymentRecord("pay_5", "inv_005", 5424.46, PaymentMode.BANK_TRANSFER, "NEFT/HDFC/8839210", now - (86400000L * 5), "HDFC Corporate Net Banking")
                )
            )
        )
        _invoices.value = seededInvoices

        // Seeded Photo & Video Media Library
        val seededAlbums = listOf(
            MediaAlbum("alb_1", "Customer Installations 2026", "Customer premises FTTH fiber drops and optical ONT equipment setups", MediaCategory.CUSTOMER_INSTALLATION, 3),
            MediaAlbum("alb_2", "Fiber Backbone & Joint Closures", "Backbone splicing, OTDR test traces, and chamber box repairs", MediaCategory.FIBER_SPLICING_WORK, 2),
            MediaAlbum("alb_3", "Router & Tech Guides", "Staff video tutorials for router configuration and VLAN tagging", MediaCategory.TRAINING_TUTORIAL, 2)
        )
        _mediaAlbums.value = seededAlbums

        val seededMedia = listOf(
            MediaItem(
                id = "med_001",
                title = "Harsh Vardhan ONT Optical Power Reading (-18.4 dBm)",
                type = MediaType.PHOTO,
                category = MediaCategory.CUSTOMER_INSTALLATION,
                resolution = "1920x1080",
                fileSizeBytes = 3_450_000L,
                tags = listOf("FTTH", "Optical Power", "Sector 8", "Silver Residency"),
                description = "Signal power calibrated with Tri-Wavelength Optical Power Meter. Loss within optimal range for 200M Air Fibre.",
                uploaderId = "usr_emp_rahul",
                uploaderName = "Rahul Verma",
                uploaderRole = UserRole.EMPLOYEE,
                customerId = "KB-CUST-108",
                customerName = "Harsh Vardhan",
                relatedJobId = "tsk_102",
                albumName = "Customer Installations 2026",
                uploadDateMillis = now - 7200000,
                approvalStatus = MediaApprovalStatus.APPROVED,
                visualAssetType = "ONT_POWER"
            ),
            MediaItem(
                id = "med_002",
                title = "Sector 4 Flyover 24-Core Backbone Splice Tray",
                type = MediaType.PHOTO,
                category = MediaCategory.FIBER_SPLICING_WORK,
                resolution = "2400x1600",
                fileSizeBytes = 4_800_000L,
                tags = listOf("Fiber Cut", "Emergency", "24 Core", "DVP Splicer"),
                description = "Cores 1 to 12 spliced inside inline waterproof joint dome closure. Protection heat shrink sleeves applied.",
                uploaderId = "usr_emp_vikram",
                uploaderName = "Vikram Singh",
                uploaderRole = UserRole.EMPLOYEE,
                relatedJobId = "tsk_101",
                albumName = "Fiber Backbone & Joint Closures",
                uploadDateMillis = now - 5400000,
                approvalStatus = MediaApprovalStatus.APPROVED,
                visualAssetType = "FIBER_TRAY"
            ),
            MediaItem(
                id = "med_003",
                title = "Syrotech GPON Dual Band ONT Device Label Verification",
                type = MediaType.PHOTO,
                category = MediaCategory.ROUTER_ONT_SETUP,
                resolution = "1920x1080",
                fileSizeBytes = 2_100_000L,
                tags = listOf("Hardware", "MAC Address", "Inventory", "Syrotech"),
                description = "Barcode, Serial Number, and MAC Address (BC:A8:A6:44:22:11) verified before dispatch.",
                uploaderId = "usr_emp_rahul",
                uploaderName = "Rahul Verma",
                uploaderRole = UserRole.EMPLOYEE,
                customerId = "KB-CUST-108",
                customerName = "Harsh Vardhan",
                albumName = "Customer Installations 2026",
                uploadDateMillis = now - 86400000,
                approvalStatus = MediaApprovalStatus.APPROVED,
                visualAssetType = "ROUTER_LABEL"
            ),
            MediaItem(
                id = "med_004",
                title = "NOC Central Server Rack & 42U Patch Panel Wiring",
                type = MediaType.PHOTO,
                category = MediaCategory.NETWORK_EQUIPMENT,
                resolution = "3840x2160",
                fileSizeBytes = 6_200_000L,
                tags = listOf("NOC Hub", "Rack", "Switch", "Infrastructure"),
                description = "RailTel & BSNL 10G uplink SFP+ transceivers and Cat6A patch cord cabling at Central Hub.",
                uploaderId = "usr_superadmin",
                uploaderName = "Sunil Rajput",
                uploaderRole = UserRole.SUPER_ADMIN,
                albumName = "Fiber Backbone & Joint Closures",
                uploadDateMillis = now - (86400000L * 3),
                approvalStatus = MediaApprovalStatus.APPROVED,
                visualAssetType = "NOC_RACK"
            ),
            MediaItem(
                id = "med_005",
                title = "Signed Job Sheet & Speed Sign-Off (Kisan Rice Mill)",
                type = MediaType.PHOTO,
                category = MediaCategory.BUSINESS_DOCUMENT,
                resolution = "1600x1200",
                fileSizeBytes = 1_850_000L,
                tags = listOf("Signed Sheet", "SLA", "Tikona", "500M"),
                description = "Customer signature on 500 Mbps symmetrical verification and installation handover slip.",
                uploaderId = "usr_emp_vikram",
                uploaderName = "Vikram Singh",
                uploaderRole = UserRole.EMPLOYEE,
                customerId = "KB-CUST-103",
                customerName = "Kisan Rice Mill",
                relatedJobId = "tsk_103",
                uploadDateMillis = now - (86400000L * 4),
                approvalStatus = MediaApprovalStatus.APPROVED,
                visualAssetType = "JOB_SHEET"
            ),
            MediaItem(
                id = "med_006",
                title = "DVP-740 Optical Core Alignment & Arc Splicing Tutorial",
                type = MediaType.VIDEO,
                category = MediaCategory.TRAINING_TUTORIAL,
                resolution = "1920x1080 (60fps)",
                fileSizeBytes = 48_500_000L, // 48.5 MB
                durationSeconds = 135, // 2m 15s
                tags = listOf("Training", "Splicing", "Video Demo", "Field Guide"),
                description = "Step-by-step demonstration on stripping fiber buffer, precision cleaving, V-groove cleaning, and electrode arc welding.",
                uploaderId = "usr_emp_vikram",
                uploaderName = "Vikram Singh",
                uploaderRole = UserRole.EMPLOYEE,
                albumName = "Router & Tech Guides",
                uploadDateMillis = now - (86400000L * 2),
                approvalStatus = MediaApprovalStatus.APPROVED,
                visualAssetType = "VIDEO_SPLICING"
            ),
            MediaItem(
                id = "med_007",
                title = "TP-Link Archer C6 Dual Band PPPoE & VLAN 100 Setup Guide",
                type = MediaType.VIDEO,
                category = MediaCategory.ROUTER_ONT_SETUP,
                resolution = "1920x1080",
                fileSizeBytes = 94_000_000L, // 94 MB
                durationSeconds = 270, // 4m 30s
                tags = listOf("Configuration", "PPPoE", "VLAN", "Wi-Fi"),
                description = "Complete setup guide for technicians on configuring PPPoE username/password, binding VLAN 100 on WAN port, and 5GHz SSID security.",
                uploaderId = "usr_superadmin",
                uploaderName = "Sunil Rajput",
                uploaderRole = UserRole.SUPER_ADMIN,
                albumName = "Router & Tech Guides",
                uploadDateMillis = now - (86400000L * 5),
                approvalStatus = MediaApprovalStatus.APPROVED,
                visualAssetType = "VIDEO_ROUTER"
            ),
            MediaItem(
                id = "med_008",
                title = "Silver Residency Outdoor Drop Cable Routing & Tension Clamping",
                type = MediaType.VIDEO,
                category = MediaCategory.CUSTOMER_INSTALLATION,
                resolution = "1920x1080",
                fileSizeBytes = 36_200_000L, // 36.2 MB
                durationSeconds = 105, // 1m 45s
                tags = listOf("Drop Cable", "Tension Clamp", "Pole Routing", "Video"),
                description = "Technique for anchoring 2-core FRP messenger wire to pole hook without bending radius violation.",
                uploaderId = "usr_emp_rahul",
                uploaderName = "Rahul Verma",
                uploaderRole = UserRole.EMPLOYEE,
                customerId = "KB-CUST-108",
                customerName = "Harsh Vardhan",
                albumName = "Customer Installations 2026",
                uploadDateMillis = now - 18000000,
                approvalStatus = MediaApprovalStatus.APPROVED,
                visualAssetType = "VIDEO_POLE"
            )
        )
        _mediaItems.value = seededMedia
    }

    // --- Authentication & User Operations ---

    fun login(user: User) {
        _currentUser.value = user
    }

    fun logout() {
        _currentUser.value = null
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
        if (current.any { it.username.equals(username, ignoreCase = true) }) {
            return Result.failure(Exception("Username '$username' is already taken."))
        }
        if (current.any { it.phone == phone }) {
            return Result.failure(Exception("Phone number '$phone' is already registered."))
        }

        val newUser = User(
            id = "usr_" + UUID.randomUUID().toString().take(8),
            fullName = fullName.trim(),
            username = username.trim(),
            password = password.trim(),
            phone = phone.trim(),
            email = email.trim(),
            address = address.trim(),
            designation = designation.trim(),
            department = department.trim(),
            joiningDate = getKolkataDateIso(),
            baseSalary = 24000.0,
            role = UserRole.EMPLOYEE,
            status = AccountStatus.PENDING_APPROVAL,
            registrationReason = reason.trim(),
            avatarEmoji = "👤"
        )
        _users.value = current + newUser
        return Result.success(newUser)
    }

    fun approveEmployeeRegistration(userId: String) {
        _users.value = _users.value.map {
            if (it.id == userId) it.copy(status = AccountStatus.APPROVED) else it
        }
    }

    fun rejectEmployeeRegistration(userId: String) {
        _users.value = _users.value.map {
            if (it.id == userId) it.copy(status = AccountStatus.REJECTED) else it
        }
    }

    fun updateEmployeeDetails(updatedUser: User) {
        _users.value = _users.value.map { if (it.id == updatedUser.id) updatedUser else it }
        if (_currentUser.value?.id == updatedUser.id) {
            _currentUser.value = updatedUser
        }
    }

    fun deleteEmployee(userId: String) {
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
        lat: Double = 28.6139,
        lng: Double = 77.2090
    ): Result<AttendanceRecord> {
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
        endLat: Double = 28.6139,
        endLng: Double = 77.2090
    ): Result<AttendanceRecord> {
        val record = _attendanceRecords.value.firstOrNull { it.id == recordId }
            ?: return Result.failure(Exception("Shift record not found."))

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
        val cal = getKolkataCalendar()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val todayIso = getKolkataDateIso()

        _attendanceRecords.value = _attendanceRecords.value.map { record ->
            if (record.status == ShiftStatus.ACTIVE_SHIFT) {
                // If record is from previous days, auto close it at 7:00 PM of that date
                if (record.dateIso != todayIso) {
                    val closeTime = record.shiftStartTimeMillis + (8 * 3600000)
                    record.copy(
                        shiftEndTimeMillis = closeTime,
                        status = ShiftStatus.AUTO_CLOSED,
                        durationMinutes = 480,
                        isAutoClosed = true
                    )
                } else if (currentHour >= 19) {
                    // Auto close at 7:00 PM today
                    val closeCal = getKolkataCalendar().apply {
                        set(Calendar.HOUR_OF_DAY, 19)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    val closeTime = closeCal.timeInMillis
                    val duration = ((closeTime - record.shiftStartTimeMillis) / 60000).coerceAtLeast(1)
                    record.copy(
                        shiftEndTimeMillis = closeTime,
                        status = ShiftStatus.AUTO_CLOSED,
                        durationMinutes = duration,
                        isAutoClosed = true
                    )
                } else {
                    record
                }
            } else {
                record
            }
        }
    }

    // --- GPS Tracking ---

    fun updateLiveLocation(location: EmployeeLocation) {
        val current = _employeeLocations.value
        val existingIndex = current.indexOfFirst { it.employeeId == location.employeeId }
        _employeeLocations.value = if (existingIndex >= 0) {
            current.toMutableList().apply { set(existingIndex, location) }
        } else {
            current + location
        }
    }

    // --- Task Management ---

    fun addTask(task: TaskItem) {
        _tasks.value = listOf(task) + _tasks.value
    }

    fun updateTaskStatus(taskId: String, newStatus: TaskStatus, completionNotes: String = "") {
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
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) {
                it.copy(assignedEmployeeId = employee.id, assignedEmployeeName = employee.fullName)
            } else it
        }
    }

    // --- Customer Management (CRM) ---

    fun addCustomer(customer: IspCustomer) {
        _customers.value = listOf(customer) + _customers.value
    }

    fun updateCustomer(customer: IspCustomer) {
        _customers.value = _customers.value.map { if (it.id == customer.id) customer else it }
    }

    fun recordCustomerRenewal(customerId: String, paymentMode: PaymentMode) {
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
        val current = _plans.value
        val idx = current.indexOfFirst { it.id == plan.id }
        _plans.value = if (idx >= 0) {
            current.toMutableList().apply { set(idx, plan) }
        } else {
            current + plan
        }
    }

    fun deletePlan(planId: String) {
        _plans.value = _plans.value.filter { it.id != planId }
    }

    // --- Finance & Accounting (Tally Inspired) ---

    fun addTransaction(transaction: AccountingTransaction) {
        _transactions.value = listOf(transaction) + _transactions.value
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
        _inventory.value = listOf(item) + _inventory.value
    }

    fun updateStock(itemId: String, changeAmount: Int) {
        _inventory.value = _inventory.value.map {
            if (it.id == itemId) {
                val newAvailable = (it.availableStock + changeAmount).coerceAtLeast(0)
                it.copy(availableStock = newAvailable, lastUpdatedMillis = System.currentTimeMillis())
            } else it
        }
    }

    fun issueEquipment(itemId: String, quantity: Int, recipient: String) {
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
        _complaints.value = listOf(ticket) + _complaints.value
    }

    fun updateComplaintStatus(ticketId: String, status: ComplaintStatus, resolution: String = "") {
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
        val subtotal = items.sumOf { it.totalAmount }
        val discountedSubtotal = (subtotal - discountAmount).coerceAtLeast(0.0)
        val gstTotal = discountedSubtotal * (gstRatePercent / 100.0)
        val cgst = gstTotal / 2.0
        val sgst = gstTotal / 2.0
        val totalAmount = discountedSubtotal + gstTotal

        val seq = _invoices.value.size + 1
        val invoiceNo = "INV-2026-${String.format("%04d", seq)}"

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
            createdBy = createdBy
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
        val invoice = _invoices.value.firstOrNull { it.id == invoiceId }
            ?: return Result.failure(Exception("Invoice not found."))

        if (invoice.status == InvoiceStatus.PAID) {
            return Result.failure(Exception("Invoice is already fully paid."))
        }
        if (amount <= 0.0) {
            return Result.failure(Exception("Payment amount must be greater than zero."))
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
                recordedBy = recordedBy
            )
        )

        return Result.success(updated)
    }

    fun cancelInvoice(invoiceId: String) {
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
        return _locationBreadcrumbs.value.filter { it.employeeId == employeeId }
    }

    // --- Staff Management Admin Controls ---

    fun suspendEmployeeAccount(userId: String, suspend: Boolean) {
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
        fileSizeBytes: Long = 2_500_000L,
        fileUri: String = ""
    ): MediaItem {
        val newItem = MediaItem(
            id = "med_" + UUID.randomUUID().toString().take(8),
            title = title.trim(),
            type = type,
            category = category,
            fileUri = fileUri,
            resolution = if (type == MediaType.VIDEO) "1920x1080 (30fps)" else "1920x1080",
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
        _mediaItems.value = _mediaItems.value.filter { it.id != mediaId }
    }

    fun updateMediaApprovalStatus(mediaId: String, status: MediaApprovalStatus) {
        _mediaItems.value = _mediaItems.value.map {
            if (it.id == mediaId) it.copy(approvalStatus = status) else it
        }
    }

    fun toggleMediaFavorite(mediaId: String) {
        _mediaItems.value = _mediaItems.value.map {
            if (it.id == mediaId) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    fun createMediaAlbum(name: String, description: String, category: MediaCategory, createdBy: String): MediaAlbum {
        val album = MediaAlbum(
            id = "alb_" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            description = description.trim(),
            category = category,
            createdBy = createdBy,
            createdAtMillis = System.currentTimeMillis()
        )
        _mediaAlbums.value = _mediaAlbums.value + album
        return album
    }
}
