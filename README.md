# FURAN

**Android focus launcher for GrapheneOS.** Two modes — Dumb (locked, allowlist only) and Smart (full drawer). Unlock flow uses a self-hosted ntfy server with HMAC-signed approval messages.

Designed for Pixel devices running GrapheneOS. No Google Play Services dependency.

---

## Setup

### 1. Clone and bootstrap Gradle

```bash
cd furan-net
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

### 2. Font assets (required before building)

Download and place in `app/src/main/res/font/`:

| File | Source |
|------|--------|
| `orbitron_regular.ttf` | [fonts.google.com/specimen/Orbitron](https://fonts.google.com/specimen/Orbitron) |
| `orbitron_bold.ttf` | same |
| `orbitron_extrabold.ttf` | same |
| `jetbrainsmono_regular.ttf` | [jetbrains.com/lp/mono](https://www.jetbrains.com/lp/mono/) |
| `jetbrainsmono_medium.ttf` | same |

### 3. Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

On the device, go to **Settings → Apps → Default apps → Home app** and select **FURAN**.

---

## Device Owner (required for app suspension)

App suspension requires FURAN to be the device owner. Run this **once** via ADB after a **factory reset** or on a fresh GrapheneOS install (device owner must be set before any accounts are added):

```bash
adb shell dpm set-device-owner com.liquidfuran.furan/.receiver.AdminReceiver
```

This command is also shown with a copy button in **Settings → Device Admin**.

> **Note:** On GrapheneOS, if you already have a user profile, set device owner immediately after setup before adding a Google account or other accounts.

---

## ntfy Unlock Flow

### Server setup

You need a self-hosted ntfy instance. Two topics:
- **Request topic** (e.g. `furan-unlock`) — FURAN publishes here when user taps the sigil
- **Approval topic** (e.g. `furan-approved`) — wife's automation publishes HMAC-signed approval here

### Wife's approval message format

```
APPROVE:{unix_timestamp}:{hmac_sha256}
```

Where the HMAC is computed as:
```
HMAC-SHA256(key=shared_secret, data="APPROVE:{unix_timestamp}")
```

Example shell command (for ntfy automation or a Tasker/Automate profile):
```bash
SECRET="your_shared_secret"
TS=$(date +%s)
PAYLOAD="APPROVE:${TS}"
HMAC=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $2}')
curl -d "${PAYLOAD}:${HMAC}" https://ntfy.example.com/furan-approved
```

### Configure in app

Open FURAN → long-press `lf · tetrafuranose` footer → Settings → ntfy Configuration:
- Enter your ntfy server URL
- Enter request and approval topic names
- Generate or paste the shared secret
- Tap **Show QR** to display a QR code for the wife to scan

---

## Architecture

```
app/
├── receiver/
│   ├── BootReceiver.kt          # Re-engages dumb mode on reboot
│   └── AdminReceiver.kt         # Device Admin receiver
├── service/
│   └── NtfyListenerService.kt   # Foreground SSE listener for approval
├── ui/
│   ├── dumb/                    # Locked home screen
│   ├── smart/                   # Full launcher with search drawer
│   └── settings/                # Schedule, ntfy, allowlist, admin
├── launcher/
│   └── LauncherActivity.kt      # Root activity, animated mode switcher
├── admin/
│   └── DeviceAdminManager.kt    # setPackagesSuspended wrapper
├── data/
│   ├── AppRepository.kt         # PackageManager queries
│   ├── PrefsRepository.kt       # DataStore — mode, config, schedule, allowlist
│   └── NtfyRepository.kt        # ntfy HTTP publish + SSE listener
├── worker/
│   └── ScheduleWorker.kt        # WorkManager — scheduled dumb mode transitions
├── tile/
│   └── FuranQsTileService.kt    # Quick Settings tile
└── model/                       # AppInfo, FuranMode, SigilState, Schedule, NtfyConfig
```

---

## Vex Sigil States

| State | Visual |
|-------|--------|
| IDLE | Static, low-opacity glow |
| REQUESTING | Brief flash — sending to ntfy |
| WAITING | Slow continuous rotation |
| APPROVED | Pulse scale + full cyan glow |
| DENIED | Magenta tint + horizontal shake |

---

## Schedule

Weekly schedule is configured in Settings. Each day has an independent start/end time for dumb mode. WorkManager schedules one-time workers at the next transition boundary and re-queues itself after each firing.

Manual lock (sigil tap / QS tile) always works. Scheduled unlock does not override a manual lock — only wife's ntfy approval unlocks.

---

## DataStore Keys

| Key | Type | Description |
|-----|------|-------------|
| `furan_mode` | String | `DUMB` or `SMART` |
| `ntfy_server` | String | ntfy server base URL |
| `ntfy_request_topic` | String | topic for unlock requests |
| `ntfy_approval_topic` | String | topic FURAN listens on |
| `ntfy_secret` | String | HMAC-SHA256 shared secret |
| `schedule_enabled` | Boolean | whether schedule is active |
| `schedule_json` | String | serialized `WeekSchedule` |
| `allowlist_packages` | Set\<String\> | packages visible in dumb mode |
| `sigil_state` | String | transient sigil animation state |

---

## Permissions

| Permission | Why |
|------------|-----|
| `RECEIVE_BOOT_COMPLETED` | Restore mode after reboot |
| `INTERNET` | ntfy API |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | SSE listener service |
| `QUERY_ALL_PACKAGES` | List installed apps for drawer + allowlist |
| `POST_NOTIFICATIONS` | Foreground service notification |
| Device Owner (`dpm set-device-owner`) | `setPackagesSuspended` for app lockdown |

---

*lf · tetrafuranose*
# furan-net
