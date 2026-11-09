# TurnOffPDNS

Android app that switches Private DNS based on the current network: at home it turns it
off (or sets it to automatic) so the home DNS is used, elsewhere it sets a DNS-over-TLS
hostname (`dns.adguard-dns.com` by default).

"Home" means at least one DNS server of the active network falls in one of the subnets
configured in the app, one per line (e.g. `192.168.1.0/24`). No location permission is
needed.

## Build and install

```sh
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant it.rizlas.turnoffpdns android.permission.WRITE_SECURE_SETTINGS
```

On Xiaomi/HyperOS, `pm grant` requires "USB debugging (Security settings)" enabled in
Developer options, followed by a reboot. It can be turned off once the permission is
granted. Also enable autostart and remove battery restrictions for the app.
