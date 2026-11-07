package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.NexusRepository
import com.example.security.PasswordHasher
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun passwordHashIsSaltedAndVerifiable() {
        val password = "NexusSecure2026"
        val first = PasswordHasher.create(password)
        val second = PasswordHasher.create(password)

        assertNotEquals(password, first.hash)
        assertNotEquals(first.salt, second.salt)
        assertTrue(PasswordHasher.verify(password, first.salt, first.hash))
        assertFalse(PasswordHasher.verify("WrongPassword2026", first.salt, first.hash))
    }

    @Test
    fun appStartsWithoutDemoAccountsOrBusinessRecords() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteSharedPreferences("kb_nexus_secure_state")
        val repository = NexusRepository.resetInstance(context)

        assertTrue(repository.users.value.isEmpty())
        assertTrue(repository.customers.value.isEmpty())
        assertTrue(repository.attendanceRecords.value.isEmpty())
        assertTrue(repository.invoices.value.isEmpty())
        assertTrue(repository.employeeLocations.value.isEmpty())
        assertNull(repository.currentUser.value)
    }

    @Test
    fun onlyOneInitialSuperAdminCanBeCreatedAndAuthenticated() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteSharedPreferences("kb_nexus_secure_state")
        val repository = NexusRepository.resetInstance(context)

        val created = repository.createInitialSuperAdmin(
            fullName = "K&B Owner",
            username = "owner",
            password = "NexusSecure2026"
        )
        assertTrue(created.isSuccess)
        val account = created.getOrNull()!!
        assertNotEquals("NexusSecure2026", account.passwordHash)
        assertTrue(account.passwordSalt.isNotBlank())

        val duplicate = repository.createInitialSuperAdmin(
            fullName = "Second Owner",
            username = "owner2",
            password = "AnotherSecure2026"
        )
        assertTrue(duplicate.isFailure)

        repository.logout()
        assertTrue(repository.authenticate("owner", "NexusSecure2026", adminOnly = true).isSuccess)
        repository.logout()
        assertTrue(repository.authenticate("owner", "wrong-password", adminOnly = true).isFailure)
    }
}
