package com.example.data.model

enum class UserRole {
    SUPER_ADMIN,
    ADMIN,
    EMPLOYEE
}

enum class AccountStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    SUSPENDED
}

data class User(
    val id: String,
    val fullName: String,
    val username: String,
    val password: String,
    val phone: String,
    val email: String,
    val address: String,
    val designation: String,
    val department: String,
    val joiningDate: String,
    val baseSalary: Double,
    val role: UserRole,
    val status: AccountStatus,
    val avatarEmoji: String = "👤",
    val registrationReason: String = "",
    val lastActiveMillis: Long = System.currentTimeMillis()
)

enum class ShiftStatus {
    ACTIVE_SHIFT,
    COMPLETED,
    AUTO_CLOSED,
    ADMIN_OVERRIDE
}

data class AttendanceRecord(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val dateIso: String, // yyyy-MM-dd
    val shiftStartTimeMillis: Long,
    val shiftEndTimeMillis: Long? = null,
    val status: ShiftStatus = ShiftStatus.ACTIVE_SHIFT,
    val durationMinutes: Long = 0,
    val isAutoClosed: Boolean = false,
    val isManualClose: Boolean = false,
    val adminOverrideReason: String = "",
    val locationStartLat: Double = 0.0,
    val locationStartLng: Double = 0.0,
    val locationEndLat: Double = 0.0,
    val locationEndLng: Double = 0.0
)

enum class LocationStatus {
    LIVE,
    OFFLINE,
    GPS_DISABLED,
    PERMISSION_DENIED,
    LOCATION_UNAVAILABLE
}

data class LocationBreadcrumb(
    val id: String,
    val employeeId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 5.0f,
    val timestampMillis: Long = System.currentTimeMillis(),
    val areaNote: String = ""
)

data class EmployeeLocation(
    val employeeId: String,
    val employeeName: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 5.0f,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isLive: Boolean = true,
    val locationStatus: LocationStatus = if (isLive) LocationStatus.LIVE else LocationStatus.OFFLINE,
    val batteryPercent: Int = 85,
    val shiftId: String = "",
    val areaDescription: String = "Field Node Sector 4"
)

enum class TaskPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

enum class TaskStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    REJECTED,
    ON_HOLD
}

data class TaskItem(
    val id: String,
    val title: String,
    val description: String,
    val assignedEmployeeId: String,
    val assignedEmployeeName: String,
    val createdBy: String = "Super Admin",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val deadlineMillis: Long,
    val customerId: String = "",
    val customerName: String = "",
    val locationAddress: String = "",
    val completionNotes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

enum class CustomerStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    EXPIRED,
    PENDING_INSTALL
}

data class IspCustomer(
    val id: String,
    val fullName: String,
    val phone: String,
    val address: String,
    val areaZone: String,
    val provider: String, // RailWire, BSNL Bharat Fibre, Tikona, Air Fibre, One Broadband
    val planId: String,
    val planName: String,
    val speedMbps: Int,
    val monthlyCharges: Double,
    val installationDate: String,
    val expiryDate: String,
    val connectionType: String = "FTTH Fiber",
    val ipAddress: String = "192.168.1.100",
    val onuMacAddress: String = "D4:6E:0E:12:34:56",
    val assignedEmployeeId: String = "",
    val status: CustomerStatus = CustomerStatus.ACTIVE
)

data class IspPlan(
    val id: String,
    val name: String,
    val provider: String,
    val speedMbps: Int,
    val dataLimit: String = "Unlimited High Speed",
    val monthlyPriceInr: Double,
    val gstPercent: Double = 18.0,
    val validityDays: Int = 30,
    val iptvIncluded: Boolean = true,
    val channelCount: Int = 350,
    val description: String = "High speed symmetrical fiber broadband"
)

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class PaymentMode {
    CASH,
    UPI,
    BANK_TRANSFER,
    CHEQUE
}

data class AccountingTransaction(
    val id: String,
    val type: TransactionType,
    val category: String,
    val amount: Double,
    val paymentMode: PaymentMode,
    val dateMillis: Long = System.currentTimeMillis(),
    val description: String,
    val referenceId: String = "",
    val recordedBy: String = "Admin"
)

data class SalarySlip(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val monthString: String,
    val baseSalary: Double,
    val daysWorked: Int,
    val totalShiftHours: Double,
    val advanceDeduction: Double = 0.0,
    val bonusAmount: Double = 0.0,
    val netSalary: Double,
    val status: String = "PAID",
    val paymentDateMillis: Long = System.currentTimeMillis()
)

enum class InvoiceStatus {
    PAID,
    UNPAID,
    PARTIALLY_PAID,
    CANCELLED
}

data class InvoiceLineItem(
    val id: String,
    val description: String,
    val hsnSacCode: String = "998422", // Telecom Internet Services SAC
    val quantity: Int = 1,
    val unitRate: Double,
    val totalAmount: Double = quantity * unitRate
)

data class InvoicePaymentRecord(
    val id: String,
    val invoiceId: String,
    val amount: Double,
    val paymentMode: PaymentMode,
    val transactionReference: String = "",
    val paymentDateMillis: Long = System.currentTimeMillis(),
    val note: String = "Payment received"
)

data class IspInvoice(
    val id: String,
    val invoiceNumber: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val items: List<InvoiceLineItem>,
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val gstRatePercent: Double = 18.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val balanceDue: Double,
    val status: InvoiceStatus = InvoiceStatus.UNPAID,
    val invoiceDateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long = System.currentTimeMillis() + (7L * 24 * 3600 * 1000),
    val billingPeriodStart: String = "",
    val billingPeriodEnd: String = "",
    val notes: String = "Thank you for choosing K&B NET SERVICE.",
    val payments: List<InvoicePaymentRecord> = emptyList(),
    val createdBy: String = "Super Admin"
)

enum class InventoryCategory {
    ROUTER_WIFI6,
    ONU_ONT_DEVICE,
    FIBER_OPTIC_CABLE,
    PATCH_CORD_CONNECTOR,
    SPLICING_TOOL_EQUIPMENT
}

data class InventoryItem(
    val id: String,
    val name: String,
    val category: InventoryCategory,
    val totalStock: Int,
    val availableStock: Int,
    val issuedStock: Int = 0,
    val unit: String = "Pcs",
    val minThresholdAlert: Int = 5,
    val unitCostInr: Double = 0.0,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

enum class ComplaintPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

enum class ComplaintStatus {
    NEW,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}

data class ComplaintTicket(
    val id: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val address: String,
    val issueTitle: String,
    val description: String,
    val priority: ComplaintPriority = ComplaintPriority.MEDIUM,
    val status: ComplaintStatus = ComplaintStatus.NEW,
    val assignedTechnicianId: String = "",
    val assignedTechnicianName: String = "Unassigned",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val resolvedAtMillis: Long? = null,
    val resolutionNotes: String = ""
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderRole: UserRole,
    val channel: String = "ANNOUNCEMENT", // ANNOUNCEMENT or TEAM or Direct ID
    val content: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isBroadcast: Boolean = false
)

enum class RequestType {
    LEAVE_APPLICATION,
    ATTENDANCE_CORRECTION,
    SALARY_ADVANCE,
    EQUIPMENT_REQUEST
}

enum class RequestStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED
}

data class EmployeeRequest(
    val id: String,
    val employeeId: String,
    val employeeName: String,
    val type: RequestType,
    val requestTitle: String,
    val description: String,
    val requestedDateMillis: Long = System.currentTimeMillis(),
    val status: RequestStatus = RequestStatus.PENDING_REVIEW,
    val adminDecisionNotes: String = "",
    val reviewedAtMillis: Long? = null
)

enum class MediaType {
    PHOTO,
    VIDEO
}

enum class MediaCategory {
    CUSTOMER_INSTALLATION,
    ROUTER_ONT_SETUP,
    NETWORK_EQUIPMENT,
    FIBER_SPLICING_WORK,
    STAFF_WORK,
    OFFICE_PREMISES,
    MAINTENANCE_REPAIR,
    BUSINESS_DOCUMENT,
    TRAINING_TUTORIAL,
    PROMOTIONAL_MARKETING
}

enum class MediaApprovalStatus {
    APPROVED,
    PENDING_REVIEW,
    REJECTED
}

data class MediaItem(
    val id: String,
    val title: String,
    val type: MediaType,
    val category: MediaCategory,
    val fileUri: String = "",
    val resolution: String = "1920x1080",
    val fileSizeBytes: Long = 2_500_000L,
    val durationSeconds: Int = 0, // > 0 for VIDEO
    val tags: List<String> = emptyList(),
    val description: String = "",
    val uploaderId: String,
    val uploaderName: String,
    val uploaderRole: UserRole = UserRole.EMPLOYEE,
    val customerId: String = "",
    val customerName: String = "",
    val relatedJobId: String = "",
    val albumName: String = "General Uploads",
    val uploadDateMillis: Long = System.currentTimeMillis(),
    val approvalStatus: MediaApprovalStatus = MediaApprovalStatus.APPROVED,
    val isFavorite: Boolean = false,
    val visualAssetType: String = "PHOTO" // For realistic vector/icon render in preview
)

data class MediaAlbum(
    val id: String,
    val name: String,
    val description: String,
    val category: MediaCategory = MediaCategory.CUSTOMER_INSTALLATION,
    val itemCount: Int = 0,
    val createdBy: String = "Admin",
    val createdAtMillis: Long = System.currentTimeMillis()
)
