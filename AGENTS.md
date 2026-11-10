# AGENTS.md

Guidance for coding agents working on this repository.

## Project

Single-purpose Android app: switches Android Private DNS between a "home" mode (off or
opportunistic) and a DNS-over-TLS hostname when away. Home is detected by matching the
active network DNS servers against user-configured subnets. Keep the scope this narrow:
do not add features that were not asked for.

## Layout

- `app/src/main/java/it/rizlas/turnoffpdns/`
  - `DnsService.kt`: foreground service, follows the default network via
    `registerDefaultNetworkCallback`
  - `DnsSwitcher.kt`: home detection and writes to `Settings.Global`
    (`private_dns_mode`, `private_dns_specifier`), only when values change
  - `Cidr.kt`: IPv4/IPv6 subnet parsing and matching
  - `Config.kt`: `SharedPreferences` wrapper
  - `MainActivity.kt`: settings screen, status, theme selector
  - `BootReceiver.kt`: restarts the service on boot and app update
- `app/src/main/res/`: XML layout, strings, light (`values`) and dark (`values-night`)
  colors and themes

## Constraints

- No third-party or AndroidX dependencies: plain Android framework views and Kotlin
  stdlib only.
- `minSdk` 29, `targetSdk` 35, `compileSdk` 36.
- No location permission: detection must not rely on the Wi-Fi SSID.
- Writing secure settings needs `WRITE_SECURE_SETTINGS`, granted via adb. Never assume
  it is present: check `DnsSwitcher.canWrite`.
- Everything in English: code, comments, UI strings, logs, docs, commit messages.
- Docs lines at most 88 characters.

## Build

Gradle 8.14 does not run on Java 25, so use a JDK 21:

```sh
export JAVA_HOME=/path/to/jdk-21
./gradlew assembleDebug
```

`local.properties` (ignored by git) must point `sdk.dir` to the Android SDK.

## Device testing

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant it.rizlas.turnoffpdns android.permission.WRITE_SECURE_SETTINGS
adb shell settings get global private_dns_mode
adb shell settings get global private_dns_specifier
adb logcat -s TurnOffPDNS
```

The permission survives `install -r` but is lost on uninstall. On Xiaomi/HyperOS the
grant requires "USB debugging (Security settings)" enabled and a reboot, and adb
installs must be confirmed on the phone.

## Git

- Conventional Commits (`feat:`, `fix:`, ...), unscoped.
- Do not commit without explicit approval from the maintainer.
