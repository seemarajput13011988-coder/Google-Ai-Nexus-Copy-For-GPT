package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.KbNexusApp
import com.example.MainActivity
import com.example.R
import com.example.data.model.EmployeeLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class KbLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var employeeId: String = ""
    private var employeeName: String = ""
    private var shiftId: String = ""

    companion object {
        const val ACTION_START_TRACKING = "com.example.service.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.service.STOP_TRACKING"
        const val EXTRA_EMPLOYEE_ID = "extra_employee_id"
        const val EXTRA_EMPLOYEE_NAME = "extra_employee_name"
        const val EXTRA_SHIFT_ID = "extra_shift_id"
        const val NOTIFICATION_ID = 9021

        fun start(context: Context, employeeId: String, employeeName: String, shiftId: String) {
            val intent = Intent(context, KbLocationService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_EMPLOYEE_ID, employeeId)
                putExtra(EXTRA_EMPLOYEE_NAME, employeeName)
                putExtra(EXTRA_SHIFT_ID, shiftId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, KbLocationService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                employeeId = intent.getStringExtra(EXTRA_EMPLOYEE_ID) ?: ""
                employeeName = intent.getStringExtra(EXTRA_EMPLOYEE_NAME) ?: "Staff Member"
                shiftId = intent.getStringExtra(EXTRA_SHIFT_ID) ?: ""
                val notification = buildTrackingNotification(employeeName)
                startForeground(NOTIFICATION_ID, notification)
                startLocationUpdates()
            }
            ACTION_STOP_TRACKING -> {
                stopLocationUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun buildTrackingNotification(name: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, KbNexusApp.TRACKING_CHANNEL_ID)
            .setContentTitle("K&B Nexus • Live Shift GPS Active")
            .setContentText("Tracking authorized work location for $name")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        stopLocationUpdates()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000L)
            .setMinUpdateDistanceMeters(10f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location: Location? = result.lastLocation
                if (location != null && employeeId.isNotBlank()) {
                    val record = EmployeeLocation(
                        employeeId = employeeId,
                        employeeName = employeeName,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = location.accuracy,
                        timestampMillis = System.currentTimeMillis(),
                        isLive = true,
                        batteryPercent = 88,
                        shiftId = shiftId,
                        areaDescription = "Sector 3 Field Node (${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)})"
                    )
                    KbNexusApp.instance.repository.updateLiveLocation(record)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            // Handled gracefully if permission revoked
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    override fun onDestroy() {
        stopLocationUpdates()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
