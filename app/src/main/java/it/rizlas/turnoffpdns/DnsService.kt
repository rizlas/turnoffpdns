package it.rizlas.turnoffpdns

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.os.Build
import android.os.IBinder

/** Foreground service that follows the default network and updates Private DNS. */
class DnsService : Service() {
    private lateinit var cm: ConnectivityManager

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            cm.getLinkProperties(network)?.let { DnsSwitcher.evaluate(this@DnsService, it) }
        }

        override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) {
            DnsSwitcher.evaluate(this@DnsService, lp)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        cm = getSystemService(ConnectivityManager::class.java)
        cm.registerDefaultNetworkCallback(callback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Config.load(this).enabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        DnsSwitcher.evaluateActive(this)
        return START_STICKY
    }

    override fun onDestroy() {
        cm.unregisterNetworkCallback(callback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_MIN),
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dns)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "service"
        private const val NOTIFICATION_ID = 1

        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, DnsService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, DnsService::class.java))
        }
    }
}
