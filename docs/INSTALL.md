# Installing Horizon on your Android TV

Vietnamese version: [INSTALL.vi.md](INSTALL.vi.md)

Horizon is a home screen (launcher). Installing it takes two steps:

1. **Install the app** (5 minutes, no computer needed on most TVs).
2. **Make it the default home screen**, so the Home button opens Horizon (needs a computer once).

Do step 1 first and check that Horizon works before you do step 2.

Works on: Android TV 5.0 or newer with a remote and the usual app list. Not for Fire TV, Apple TV,
Samsung Tizen, LG webOS or Roku. Tested on one Android 8 TV only, so please report what you find.

---

## Step 1 – Install the app

Download `Horizon-1.0.0.apk` from the project's **Releases** page first.

### Option A – Downloader app (easiest, TV with Google Play)

1. On the TV, open Google Play and install **Downloader** (by AFTVnews).
2. Open Downloader and type the direct link to the APK (copy it from the Releases page), then press **Go**.
3. When asked, allow Downloader to **install unknown apps**
   (Settings → Apps → Security & restrictions → Unknown sources → Downloader → On).
4. Press **Install**, then **Open**.

### Option B – USB stick

1. Copy `Horizon-1.0.0.apk` to a USB stick (FAT32 or exFAT).
2. Plug it into the TV and open a file manager app on the TV (for example *File Commander* or *X-plore*).
3. Open the APK and press **Install**. Allow unknown apps for the file manager if asked.

### Option C – adb from a computer (also needed for step 2)

1. On the TV: **Settings → Device Preferences → About**, press **Build** seven times to enable
   Developer options. Then **Settings → Device Preferences → Developer options**, turn on
   **USB debugging** (on some TVs also **Network debugging** / **ADB debugging**).
2. Find the TV's IP address: **Settings → Network → your network**.
3. Install adb on your computer:
   - macOS: `brew install --cask android-platform-tools`
   - Windows: download "SDK Platform-Tools" from developer.android.com and unzip it
   - Linux: `sudo apt install adb`
4. Connect and install (computer and TV on the same Wi‑Fi or network):

```
adb connect <TV-IP>:5555
adb install -r Horizon-1.0.0.apk
```

   The first time, the TV shows "Allow USB debugging?". Choose **Always allow**.
   If it says `Success`, the app is installed.

   On Android 11+ TVs with **Wireless debugging**: use `adb pair <ip>:<pairing-port>` with the code
   shown on the TV, then `adb connect <ip>:<port>`.

### Check that it works

Open **Horizon** from the apps list. You should see:

- the clock, date and lunar-calendar line at the top right, with a soft shadow,
- a row of apps along the bottom (left/right to move, OK to open),
- after 10–20 seconds a landscape photo, with the photographer's name at the top left.

Photos need internet. After 12 seconds without a key press the app row fades away so only the photo
remains; press any key to bring it back.

---

## Step 2 – Make Horizon the default home screen

Android TV keeps its own launcher on the Home button. There are three ways, from easiest to most
reliable. After each one, press **Home** on the remote to test.

### Way 1 – Let the TV ask you (no computer)

Some TVs ask "Select a Home app" when more than one launcher is installed. If yours does, press
Home, choose **Horizon**, and tap **Always**. If nothing is asked, go to Way 2.

### Way 2 – Set it with adb

```
adb shell cmd package set-home-activity app.horizontv.home/.MainActivity
```

Press Home. If Horizon opens, you are done. On many TVs the manufacturer's launcher still wins
and Home opens the stock screen. In that case do Way 3.

### Way 3 – Turn off the stock launcher (reliable, reversible)

1. List launchers and note the package names:

```
adb shell pm list packages | grep -iE "launcher|home"
```

   Common stock launchers on Android TV 8 are `com.google.android.tvlauncher` and
   `com.google.android.leanbacklauncher`. Other brands have their own names. **Never disable
   `app.horizontv.home`.** Pick only the stock launcher(s) that are not Horizon.

2. Disable them (replace the name with the one on your TV):

```
adb shell pm disable-user --user 0 com.google.android.tvlauncher
adb shell pm disable-user --user 0 com.google.android.leanbacklauncher
```

3. Set Horizon as home and restart:

```
adb shell cmd package set-home-activity app.horizontv.home/.MainActivity
adb reboot
```

Safety: install Horizon and see it work (step 1) **before** disabling anything. If the screen ever
shows no launcher, connect with adb and run the "Go back" commands below.

---

## Go back to the original launcher

```
adb shell pm enable com.google.android.tvlauncher
adb shell pm enable com.google.android.leanbacklauncher
adb uninstall app.horizontv.home
adb reboot
```

Use the package names you disabled. To keep Horizon but stop it being the default, only run the
`pm enable` lines.

---

## Troubleshooting

| Problem | What to try |
|---|---|
| `adb: command not found` | Install adb (step 1, Option C, point 3). |
| `failed to connect` / `connection refused` | Turn on USB/Network debugging, restart the TV, check the IP address, make sure both are on the same network. |
| `failed to authenticate` | Look at the TV screen and accept "Allow USB debugging?". |
| `INSTALL_FAILED_...` when installing | Uninstall any older copy first: `adb uninstall app.horizontv.home`, then install again. |
| Home still opens the old launcher | Use Way 3 and reboot the TV. |
| No photos, only a dark gradient | Check the TV has internet. Wait one minute. Photos come from Wikimedia, which can slow down downloads; Horizon retries and keeps a cache. |
| Clock shows the wrong time | Settings → Date & time → Automatic. |
| App row has disappeared | It fades after 12 seconds on purpose. Press any key. |
| App missing from the row | Horizon lists apps that declare a TV or normal launcher icon. Sideloaded apps without one do not appear. |

## Privacy

Horizon asks for one permission, internet, only to download photos from Wikimedia Commons. It has no
accounts, ads or analytics.
