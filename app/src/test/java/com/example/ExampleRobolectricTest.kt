package com.example

import androidx.test.core.app.ApplicationProvider
import android.content.Context
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
    fun verifyAppNameResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("K&B Nexus", context.getString(R.string.app_name))
    }

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
}
