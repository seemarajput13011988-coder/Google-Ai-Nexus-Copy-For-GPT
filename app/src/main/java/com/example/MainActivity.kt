package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.model.UserRole
import com.example.ui.screens.admin.AdminPortalScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.employee.EmployeePortalScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as KbNexusApp).repository

        setContent {
            val isDarkMode by repository.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUser by repository.currentUser.collectAsState()

                    if (currentUser == null) {
                        AuthScreen(
                            repository = repository,
                            onLoginSuccess = { user ->
                                // Logged in
                            }
                        )
                    } else {
                        val user = currentUser!!
                        if (user.role == UserRole.SUPER_ADMIN || user.role == UserRole.ADMIN) {
                            AdminPortalScreen(
                                repository = repository,
                                onLogout = { repository.logout() }
                            )
                        } else {
                            EmployeePortalScreen(
                                repository = repository,
                                employee = user,
                                onLogout = { repository.logout() }
                            )
                        }
                    }
                }
            }
        }
    }
}
