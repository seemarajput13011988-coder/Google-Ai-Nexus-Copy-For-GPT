package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.repository.NexusRepository

class KbNexusApp : Application() {

    companion object {
        const val TRACKING_CHANNEL_ID = "kb_nexus_tracking_channel"
        lateinit var instance: KbNexusApp
            private set
    }

    lateinit var repository: NexusRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = NexusRepository.getInstance(applicationContext)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "K&B Nexus Shift Tracking"
            val descriptionText = "Persistent notification indicating authorized shift GPS tracking is in progress"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(TRACKING_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
