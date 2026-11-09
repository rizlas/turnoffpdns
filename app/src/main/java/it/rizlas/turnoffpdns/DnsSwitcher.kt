package it.rizlas.turnoffpdns

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.provider.Settings
import android.util.Log
import java.text.DateFormat
import java.util.Date

object DnsSwitcher {
    private const val TAG = "TurnOffPDNS"
    private const val KEY_MODE = "private_dns_mode"
    private const val KEY_SPECIFIER = "private_dns_specifier"
    private const val MODE_HOSTNAME = "hostname"

    fun canWrite(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun currentMode(ctx: Context): String? = Settings.Global.getString(ctx.contentResolver, KEY_MODE)

    fun currentSpecifier(ctx: Context): String? =
        Settings.Global.getString(ctx.contentResolver, KEY_SPECIFIER)

    /** Home = at least one network DNS server falls in one of the configured subnets. */
    fun isHome(lp: LinkProperties, homeNetworks: List<Cidr>): Boolean =
        lp.dnsServers.any { dns -> homeNetworks.any { it.contains(dns) } }

    /** Evaluates the current default network (used at startup and after saving). */
    fun evaluateActive(ctx: Context) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val lp = cm.activeNetwork?.let { cm.getLinkProperties(it) } ?: return
        evaluate(ctx, lp)
    }

    @Synchronized
    fun evaluate(ctx: Context, lp: LinkProperties) {
        val cfg = Config.load(ctx)
        if (!cfg.enabled) return
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
