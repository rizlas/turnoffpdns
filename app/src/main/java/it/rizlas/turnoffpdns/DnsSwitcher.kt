package it.rizlas.turnoffpdns

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import android.util.Log
import java.text.DateFormat
import java.util.Date

object DnsSwitcher {
    private const val TAG = "TurnOffPDNS"
    private const val KEY_MODE = "private_dns_mode"
    private const val KEY_SPECIFIER = "private_dns_specifier"
    private const val MODE_HOSTNAME = "hostname"
    private const val MODE_OFF = "off"

    fun canWrite(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun currentMode(ctx: Context): String? = Settings.Global.getString(ctx.contentResolver, KEY_MODE)

    fun currentSpecifier(ctx: Context): String? =
        Settings.Global.getString(ctx.contentResolver, KEY_SPECIFIER)

    /** Home = at least one network DNS server falls in one of the configured subnets. */
    fun isHome(lp: LinkProperties, homeNetworks: List<Cidr>): Boolean =
        lp.dnsServers.any { dns -> homeNetworks.any { it.contains(dns) } }

    /**
     * True if any connected network is a VPN, even one this app is excluded from.
     * [ignore] skips a network that was just lost but may still be listed.
     */
    @Suppress("DEPRECATION")
    fun isVpnActive(ctx: Context, ignore: Network? = null): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        return cm.allNetworks.any {
            it != ignore && cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }
    }

    /** Evaluates the current default network (used at startup and after saving). */
    fun evaluateActive(ctx: Context, lostVpn: Network? = null) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val active = cm.activeNetwork?.takeIf { it != lostVpn }
        val lp = active?.let { cm.getLinkProperties(it) } ?: return
        evaluate(ctx, lp, lostVpn)
    }

    @Synchronized
    fun evaluate(ctx: Context, lp: LinkProperties, lostVpn: Network? = null) {
        val cfg = Config.load(ctx)
        if (!cfg.enabled) return
        if (isVpnActive(ctx, lostVpn)) {
            apply(ctx, MODE_OFF, null)
            return
        }
        val home = isHome(lp, Cidr.parseList(cfg.homeNetworks).first)
        if (home) apply(ctx, cfg.homeMode, null) else apply(ctx, MODE_HOSTNAME, cfg.awayHostname)
    }

    private fun apply(ctx: Context, mode: String, specifier: String?) {
        if (mode == MODE_HOSTNAME && specifier.isNullOrBlank()) return
        val sameMode = currentMode(ctx) == mode
        val sameSpec = specifier == null || currentSpecifier(ctx) == specifier
        if (sameMode && sameSpec) return

        val target = if (specifier != null) "$mode ($specifier)" else mode
        if (!canWrite(ctx)) {
            record(ctx, "permission missing, cannot set $target")
            return
        }
        val cr = ctx.contentResolver
        if (specifier != null) Settings.Global.putString(cr, KEY_SPECIFIER, specifier)
        Settings.Global.putString(cr, KEY_MODE, mode)
        record(ctx, "set $target")
    }

    private fun record(ctx: Context, msg: String) {
        Log.i(TAG, msg)
        val now = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date())
        Config.setLastAction(ctx, "$now: $msg")
    }
}
