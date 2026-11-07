package com.example.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.KbNexusApp
import com.example.MainActivity
import com.example.R
import com.example.data.model.EmployeeLocation
import com.example.data.model.LocationStatus
import com.example.data.repository.NexusRepository
import com.example.data.storage.EncryptedStateStore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.Priority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

/**
 * Shift-scoped location foreground service.
 *
 * The service stores its active shift identity in encrypted local storage so Android can
 * recreate it after a process restart (START_STICKY). It never reports a live state until
 * a real location fix is received.
 */
class KbLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var stateStore: EncryptedStateStore
    private var locationCallback: LocationCallback? = null
    private var employeeId: String = ""
    private var employeeName: String = ""
    private var shiftId: String = ""
    private var lastBreadcrumbMillis: Long = 0L
    private var lastBreadcrumbLocation: Location? = null
    private var receiverRegistered = false
    private val autoCloseHandler = Handler(Looper.getMainLooper())
    private val autoCloseRunnable = object : Runnable {
        override fun run() {
            if (employeeId.isBlank() || shiftId.isBlank()) return
            val calendar = NexusRepository.getKolkataCalendar()
            val afterRegularEnd = calendar.get(Calendar.HOUR_OF_DAY) >= 19
            if (afterRegularEnd) {
                val repository = KbNexusApp.instance.repository
                repository.checkAndAutoCloseExpiredShifts()
                if (!repository.isShiftActive(shiftId)) {
                    stop(this@KbLocationService)
                    return
                }
            }
            autoCloseHandler.postDelayed(this, 60_000L)
        }
    }

    private val providersReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshLocationState()
        }
    }

    companion object {
        const val ACTION_START_TRACKING = "com.example.service.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.service.STOP_TRACKING"
        const val EXTRA_EMPLOYEE_ID = "extra_employee_id"
        const val EXTRA_EMPLOYEE_NAME = "extra_employee_name"
        const val EXTRA_SHIFT_ID = "extra_shift_id"
        const val NOTIFICATION_ID = 9021

        private const val KEY_EMPLOYEE_ID = "active_gps_employee_id"
        private const val KEY_EMPLOYEE_NAME = "active_gps_employee_name"
        private const val KEY_SHIFT_ID = "active_gps_shift_id"

        fun start(context: Context, employeeId: String, employeeName: String, shiftId: String) {
            val store = EncryptedStateStore(context.applicationContext)
            store.putString(KEY_EMPLOYEE_ID, employeeId)
            store.putString(KEY_EMPLOYEE_NAME, employeeName)
            store.putString(KEY_SHIFT_ID, shiftId)

            val intent = Intent(context, KbLocationService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_EMPLOYEE_ID, employeeId)
                putExtra(EXTRA_EMPLOYEE_NAME, employeeName)
                putExtra(EXTRA_SHIFT_ID, shiftId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val store = EncryptedStateStore(context.applicationContext)
            val activeEmployeeId = store.getString(KEY_EMPLOYEE_ID).orEmpty()
            if (activeEmployeeId.isNotBlank()) {
                runCatching {
                    (context.applicationContext as KbNexusApp).repository
                        .updateLocationStatus(activeEmployeeId, LocationStatus.OFFLINE)
                }
            }
            store.putString(KEY_EMPLOYEE_ID, "")
            store.putString(KEY_EMPLOYEE_NAME, "")
            store.putString(KEY_SHIFT_ID, "")
            context.stopService(Intent(context, KbLocationService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        stateStore = EncryptedStateStore(applicationContext)
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        ContextCompat.registerReceiver(this, providersReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_TRACKING -> {
                clearActiveSession()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_TRACKING -> {
                employeeId = intent.getStringExtra(EXTRA_EMPLOYEE_ID).orEmpty()
                employeeName = intent.getStringExtra(EXTRA_EMPLOYEE_NAME).orEmpty()
                shiftId = intent.getStringExtra(EXTRA_SHIFT_ID).orEmpty()
                saveActiveSession()
            }
            null -> restoreActiveSession()
            else -> restoreActiveSession()
        }

        if (employeeId.isBlank() || shiftId.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Checking GPS permission and device location…"))
        refreshLocationState()
        autoCloseHandler.removeCallbacks(autoCloseRunnable)
        autoCloseHandler.postDelayed(autoCloseRunnable, 60_000L)
        return START_STICKY
    }

    private fun restoreActiveSession() {
        employeeId = stateStore.getString(KEY_EMPLOYEE_ID).orEmpty()
        employeeName = stateStore.getString(KEY_EMPLOYEE_NAME).orEmpty()
        shiftId = stateStore.getString(KEY_SHIFT_ID).orEmpty()
    }

    private fun saveActiveSession() {
        stateStore.putString(KEY_EMPLOYEE_ID, employeeId)
        stateStore.putString(KEY_EMPLOYEE_NAME, employeeName)
        stateStore.putString(KEY_SHIFT_ID, shiftId)
    }

    private fun clearActiveSession() {
        stopLocationUpdates()
        if (employeeId.isNotBlank()) {
            KbNexusApp.instance.repository.updateLocationStatus(employeeId, LocationStatus.OFFLINE)
        }
        stateStore.putString(KEY_EMPLOYEE_ID, "")
        stateStore.putString(KEY_EMPLOYEE_NAME, "")
        stateStore.putString(KEY_SHIFT_ID, "")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun isDeviceLocationEnabled(): Boolean {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    private fun refreshLocationState() {
        if (employeeId.isBlank()) return
        if (!hasLocationPermission()) {
            stopLocationUpdates()
            publishStatus(LocationStatus.PERMISSION_DENIED, "Location permission is not granted")
            return
        }
        if (!isDeviceLocationEnabled()) {
            stopLocationUpdates()
            publishStatus(LocationStatus.GPS_DISABLED, "Turn on Location on this device")
            return
        }
        startLocationUpdates()
    }

    private fun publishStatus(status: LocationStatus, message: String) {
        KbNexusApp.instance.repository.updateLocationStatus(employeeId, status)
        updateNotification(message)
    }

    private fun buildNotification(message: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, KbNexusApp.TRACKING_CHANNEL_ID)
            .setContentTitle("K&B Nexus • Shift location")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermission() || !isDeviceLocationEnabled()) return
        stopLocationUpdates()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .setMinUpdateDistanceMeters(10f)
            .setWaitForAccurateLocation(true)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    publishStatus(LocationStatus.LOCATION_UNAVAILABLE, "Waiting for a usable GPS fix")
                }
            }

            override fun onLocationResult(result: LocationResult) {
                val fix: Location = result.lastLocation ?: return
                if (employeeId.isBlank() || shiftId.isBlank()) return
                val battery = (getSystemService(BATTERY_SERVICE) as BatteryManager)
                    .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val now = System.currentTimeMillis()
                val record = EmployeeLocation(
                    employeeId = employeeId,
                    employeeName = employeeName,
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    accuracyMeters = fix.accuracy,
                    timestampMillis = now,
                    isLive = true,
                    locationStatus = LocationStatus.LIVE,
                    batteryPercent = battery.takeIf { it in 0..100 } ?: -1,
                    shiftId = shiftId,
                    areaDescription = ""
                )
                KbNexusApp.instance.repository.updateLiveLocation(record)
                val previousBreadcrumb = lastBreadcrumbLocation
                if (previousBreadcrumb == null || now - lastBreadcrumbMillis >= 30_000L || previousBreadcrumb.distanceTo(fix) >= 25f) {
                    KbNexusApp.instance.repository.addLocationBreadcrumb(record)
                    lastBreadcrumbLocation = Location(fix)
                    lastBreadcrumbMillis = now
                }
                val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(now))
                updateNotification("Live GPS fix received at $time • ±${fix.accuracy.toInt()} m")
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            ).addOnFailureListener {
                publishStatus(LocationStatus.LOCATION_UNAVAILABLE, "Unable to receive GPS updates")
            }
        } catch (_: SecurityException) {
            publishStatus(LocationStatus.PERMISSION_DENIED, "Location permission was revoked")
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Do not stop the foreground service when the launcher task is swiped away.
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        autoCloseHandler.removeCallbacks(autoCloseRunnable)
        stopLocationUpdates()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(providersReceiver) }
            receiverRegistered = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
