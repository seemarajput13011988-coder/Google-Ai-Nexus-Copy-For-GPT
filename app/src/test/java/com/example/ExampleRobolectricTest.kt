package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import com.example.data.repository.NexusRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var repository: NexusRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        repository = NexusRepository.resetInstance(context)
    }

    @Test
    fun verifyAppNameResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("K&B Nexus", appName)
    }

    @Test
    fun verifyStartShiftDoesNotCompleteImmediately() {
        // Staff Anita does not have a shift today in initial seed
        val anita = repository.users.value.first { it.id == "usr_emp_anita" }
        val result = repository.startEmployeeShift(anita, 28.6139, 77.2090)
        assertTrue("Shift start should succeed", result.isSuccess)

        val activeShift = result.getOrNull()
        assertNotNull(activeShift)
        assertEquals(ShiftStatus.ACTIVE_SHIFT, activeShift?.status)
        assertNull("shiftEndTimeMillis must be null upon starting", activeShift?.shiftEndTimeMillis)
        assertFalse("Shift must NOT be marked auto closed", activeShift?.isAutoClosed ?: true)
    }

    @Test
    fun verifyEndShiftCompletesShiftAccurately() {
        val anita = repository.users.value.first { it.id == "usr_emp_anita" }
        val startResult = repository.startEmployeeShift(anita, 28.6139, 77.2090)
        val shiftId = startResult.getOrNull()!!.id

        val endResult = repository.endEmployeeShift(shiftId, 28.6150, 77.2100)
        assertTrue(endResult.isSuccess)

        val completed = endResult.getOrNull()
        assertNotNull(completed)
        assertEquals(ShiftStatus.COMPLETED, completed?.status)
        assertNotNull("Shift end time must be recorded", completed?.shiftEndTimeMillis)
        assertTrue("Manual close flag must be true", completed?.isManualClose ?: false)
    }

    @Test
    fun verifyAdminShiftOverrideAllowsCustomHours() {
        val anita = repository.users.value.first { it.id == "usr_emp_anita" }
        val customMorningTime = System.currentTimeMillis() - 3600000 // 1 hour ago
        val record = repository.adminStartShiftOverride(
            employee = anita,
            customStartMillis = customMorningTime,
            reason = "Night maintenance shift"
        )
        assertNotNull(record)
        assertEquals(ShiftStatus.ACTIVE_SHIFT, record.status)
        assertEquals("Night maintenance shift", record.adminOverrideReason)
    }

    @Test
    fun verifyCustomerRenewalExtendsValidityAndCreatesCashbookIncome() {
        val initialIncome = repository.getTotalIncome()
        val customer = repository.customers.value.first { it.id == "KB-CUST-104" } // Expired customer
        assertEquals(CustomerStatus.EXPIRED, customer.status)

        repository.recordCustomerRenewal(customer.id, PaymentMode.UPI)

        val updatedCustomer = repository.customers.value.first { it.id == "KB-CUST-104" }
        assertEquals("Customer status should be ACTIVE after renewal", CustomerStatus.ACTIVE, updatedCustomer.status)

        val newIncome = repository.getTotalIncome()
        assertTrue("Total income should increase by renewal amount", newIncome > initialIncome)
    }

    @Test
    fun verifyEmployeeRegistrationAndSuperAdminApproval() {
        val regResult = repository.registerNewEmployee(
            fullName = "Deepak Joshi",
            username = "deepak.j",
            password = "password123",
            phone = "+91 99887 76655",
            email = "deepak@kbnet.com",
            address = "Sector 12",
            designation = "FTTH Tech",
            department = "Field Technical Ops",
            reason = "Fiber team expansion"
        )
        assertTrue(regResult.isSuccess)
        val newUserId = regResult.getOrNull()!!.id

        val pending = repository.users.value.first { it.id == newUserId }
        assertEquals(AccountStatus.PENDING_APPROVAL, pending.status)

        repository.approveEmployeeRegistration(newUserId)
        val approved = repository.users.value.first { it.id == newUserId }
        assertEquals(AccountStatus.APPROVED, approved.status)
    }
}
