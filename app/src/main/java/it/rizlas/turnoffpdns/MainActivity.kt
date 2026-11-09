package it.rizlas.turnoffpdns

import android.app.Activity
import android.app.UiModeManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var enabled: Switch
    private lateinit var homeNetworks: EditText
    private lateinit var awayHostname: EditText
    private lateinit var modeOff: RadioButton
    private lateinit var modeAuto: RadioButton
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enabled = findViewById(R.id.enabled)
        homeNetworks = findViewById(R.id.home_networks)
        awayHostname = findViewById(R.id.away_hostname)
        modeOff = findViewById(R.id.mode_off)
        modeAuto = findViewById(R.id.mode_auto)
        status = findViewById(R.id.status)

        val cfg = Config.load(this)
        enabled.isChecked = cfg.enabled
        homeNetworks.setText(cfg.homeNetworks)
        awayHostname.setText(cfg.awayHostname)
        if (cfg.homeMode == Config.MODE_AUTO) modeAuto.isChecked = true else modeOff.isChecked = true

        findViewById<Button>(R.id.save).setOnClickListener { save() }
        setupTheme()
    }

    /** Per-app theme exists since Android 12: older versions follow the system. */
    private fun setupTheme() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            findViewById<View>(R.id.theme_section).visibility = View.GONE
            return
        }
        val group = findViewById<RadioGroup>(R.id.theme)
        group.check(
            when (Config.theme(this)) {
                Config.THEME_LIGHT -> R.id.theme_light
                Config.THEME_DARK -> R.id.theme_dark
                else -> R.id.theme_system
            },
        )
        group.setOnCheckedChangeListener { _, id ->
            val (theme, mode) = when (id) {
                R.id.theme_light -> Config.THEME_LIGHT to UiModeManager.MODE_NIGHT_NO
                R.id.theme_dark -> Config.THEME_DARK to UiModeManager.MODE_NIGHT_YES
                else -> Config.THEME_SYSTEM to UiModeManager.MODE_NIGHT_AUTO
            }
            Config.setTheme(this, theme)
            getSystemService(UiModeManager::class.java).setApplicationNightMode(mode)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun save() {
        val (_, invalid) = Cidr.parseList(homeNetworks.text.toString())
        if (invalid.isNotEmpty()) {
            homeNetworks.error = getString(R.string.error_networks, invalid.joinToString(", "))
            return
        }
        val hostname = awayHostname.text.toString().trim()
        if (hostname.isEmpty()) {
            awayHostname.error = getString(R.string.error_hostname)
            return
        }
        val cfg = Config.Values(
            enabled = enabled.isChecked,
            homeNetworks = homeNetworks.text.toString().trim(),
            awayHostname = hostname,
            homeMode = if (modeAuto.isChecked) Config.MODE_AUTO else Config.MODE_OFF,
        )
        Config.save(this, cfg)
        if (cfg.enabled) {
            DnsService.start(this)
        } else {
            DnsService.stop(this)
        }
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
        status.postDelayed({ refreshStatus() }, 500)
    }

    private fun refreshStatus() {
        val cm = getSystemService(ConnectivityManager::class.java)
        val lp = cm.activeNetwork?.let { cm.getLinkProperties(it) }
        val dns = lp?.dnsServers?.joinToString(", ") { it.hostAddress ?: "?" } ?: "no network"
        val nets = Cidr.parseList(Config.load(this).homeNetworks).first
        val where = when {
            lp == null -> "-"
            DnsSwitcher.isHome(lp, nets) -> "home"
            else -> "away"
        }
        val mode = DnsSwitcher.currentMode(this) ?: "-"
        val spec = DnsSwitcher.currentSpecifier(this)
        val current = if (mode == "hostname") "$mode ($spec)" else mode
        val perm = if (DnsSwitcher.canWrite(this)) {
            getString(R.string.perm_ok)
        } else {
            getString(R.string.perm_missing, packageName)
        }
        status.text = getString(
            R.string.status,
            perm,
            current,
            dns,
            where,
            Config.lastAction(this),
        )
    }
}
