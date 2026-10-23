# Horizon – a tiny home screen for Android TV

A minimal, Apple TV–style launcher in about 27 KB. One row of apps along the bottom, a big clock
with a soft shadow, and slowly drifting landscape photos. After 12 seconds without a key press the
app row fades away and only the photo remains.

- Full-screen random landscape photos, cross-fading every minute with a slow zoom
- Large clock, full date and an optional lunar-calendar line (Vietnamese lunar calendar)
- Apps sorted with common streaming apps first, then alphabetical; a Settings tile at the end
- No accounts, no analytics, no ads. The only permission is `INTERNET` (to download photos)
- No third-party libraries, pure Java, minSdk 21 (Android 5.0+)

## Install

Full step-by-step guide (install from the TV itself or with adb, set as default home, troubleshooting,
how to go back):

- English: [docs/INSTALL.md](docs/INSTALL.md)
- Tiếng Việt: [docs/INSTALL.vi.md](docs/INSTALL.vi.md)

Quick version with adb:

```
adb connect <tv-ip>:5555
adb install -r Horizon-1.0.0.apk
adb shell cmd package set-home-activity app.horizontv.home/.MainActivity
```

If Home still opens the stock launcher, disable it (reversible); see the guide.

## Photos and credits

Photos are Wikimedia Commons *featured pictures of landscapes*. They are downloaded on demand
(about 1 MB each, up to 40 cached on the device) and are not included in the APK. Each photo has
its own free licence (mostly CC BY-SA); the photographer, licence and source are shown at the top
left of the screen while the photo is displayed. Photo list: `app/src/main/res/raw/photos.tsv`.

Wikimedia may rate-limit clients. If a download fails, Horizon shows a cached photo, or the
gradient background when nothing is cached yet. Before distributing widely, set your own project
URL in the `UA` string in `PhotoWall.java`, as Wikimedia's user-agent policy asks.

## Build

```
export ANDROID_HOME=/path/to/android-sdk
export HORIZON_KEYSTORE=/path/to/your.jks HORIZON_STORE_PASS=...
gradle assembleRelease
```

Requires JDK 17, Android Gradle Plugin 8.7.3, compileSdk 34. The signing key must stay the same
across releases, otherwise TVs cannot update in place.

## Known limits

- Built from a launcher that runs on one Android 8 TV (32-bit). This public build has not yet been
  tested on hardware, and other TVs and boxes are untested.
- The lunar-calendar line follows the Vietnamese lunar calendar and is always shown.
- Some TV firmwares block or override third-party home screens.
