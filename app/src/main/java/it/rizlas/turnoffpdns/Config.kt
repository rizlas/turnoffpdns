package it.rizlas.turnoffpdns

import android.content.Context

object Config {
    private const val FILE = "config"
    private const val K_ENABLED = "enabled"
    private const val K_HOME_NETWORKS = "home_networks"
    private const val K_AWAY_HOSTNAME = "away_hostname"
    private const val K_HOME_MODE = "home_mode"
    private const val K_LAST_ACTION = "last_action"
    private const val K_THEME = "theme"

    /** private_dns_mode values used at home. */
    const val MODE_OFF = "off"
    const val MODE_AUTO = "opportunistic"

    const val DEFAULT_HOSTNAME = "dns.adguard-dns.com"

    data class Values(
        val enabled: Boolean,
        /** Subnets/IPs of the home DNS servers, one per line. */
        val homeNetworks: String,
        /** DNS-over-TLS hostname used away from home. */
        val awayHostname: String,
        /** Private DNS mode at home: [MODE_OFF] or [MODE_AUTO]. */
        val homeMode: String,
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(ctx: Context): Values {
        val p = prefs(ctx)
        return Values(
            enabled = p.getBoolean(K_ENABLED, false),
            homeNetworks = p.getString(K_HOME_NETWORKS, "")!!,
            awayHostname = p.getString(K_AWAY_HOSTNAME, DEFAULT_HOSTNAME)!!,
            homeMode = p.getString(K_HOME_MODE, MODE_OFF)!!,
        )
    }

    fun save(ctx: Context, v: Values) {
        prefs(ctx).edit()
            .putBoolean(K_ENABLED, v.enabled)
            .putString(K_HOME_NETWORKS, v.homeNetworks)
            .putString(K_AWAY_HOSTNAME, v.awayHostname)
            .putString(K_HOME_MODE, v.homeMode)
            .apply()
    }

    /** App theme, applied with UiModeManager.setApplicationNightMode. */
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun theme(ctx: Context): String = prefs(ctx).getString(K_THEME, THEME_SYSTEM)!!

    fun setTheme(ctx: Context, theme: String) {
        prefs(ctx).edit().putString(K_THEME, theme).apply()
    }

    fun lastAction(ctx: Context): String = prefs(ctx).getString(K_LAST_ACTION, "-")!!

    fun setLastAction(ctx: Context, text: String) {
        prefs(ctx).edit().putString(K_LAST_ACTION, text).apply()
    }
}
