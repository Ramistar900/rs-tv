# RS TV — Sinjar TV Show

RS TV is an independent Android television app for phones, tablets, Android TV, and TV boxes. It is built with Kotlin, Jetpack Compose, and AndroidX Media3.

## Implemented Android features

- Adaptive navigation: bottom navigation on compact screens and a navigation rail on wide screens/TV layouts.
- Arabic-first interface with a black, red, and silver visual identity.
- Local M3U/M3U8 import, channel name/group search, and HTTPS stream playback.
- Local persistence for the imported playlist, favorites, and the recent channel history.
- XMLTV programme-guide import, secure XML parsing, and channel matching through M3U `tvg-id` and XMLTV `channel` IDs.
- Separate satellite-directory screen; it does not mix satellite tuning parameters with internet stream URLs.
- Android TV launcher entry and a dedicated TV banner; custom vector launcher icon.
- Settings to re-import local data and clear the saved playlist, favorites, or viewing history.
- Debug APK upload as a GitHub Actions artifact after tests and build succeed.

## Build and test

Requirements: JDK 17, Android SDK Platform 35, and Gradle 8.9.

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

GitHub Actions runs the M3U and XMLTV parser unit tests and assembles a debug APK for changes to Android sources, Gradle configuration, or the workflow. Successful runs publish the APK as the `rs-tv-debug-apk` workflow artifact for 14 days.

## Playlist and guide behavior

- Only HTTPS stream URLs with a valid host are accepted by the current M3U parser. HTTP, file, and other non-HTTPS stream entries are skipped.
- The imported playlist is copied into the app's private local storage; it is not uploaded to a server.
- Favorites and recent viewing history stay on the device.
- XMLTV parsing rejects DTD declarations and external entities, limits input size and the number of parsed programme entries, and ignores malformed programme rows.
- Programme matching uses the playlist channel's `tvg-id` and the XMLTV programme's `channel` value.
- Playlist/logo metadata is untrusted input. The app does not yet download arbitrary logo URLs.

## Satellite catalogue

The satellite screen is a separate reference area and currently shows only the starter regional sections. **A verified global satellite-frequency dataset has not yet been imported**, so it does not display invented frequencies or claim complete coverage. Read [Global Satellite Catalogue](docs/SATELLITE-CATALOG.md) for the data schema and quality rules.

Satellite frequencies are tuning metadata, not internet streams. Actual reception requires compatible DVB-S/S2/S2X tuner hardware and separate device integration. An ordinary phone or TV box cannot tune a dish through software alone.

## Channel catalogue and content rights

The candidate source importer and review workflow are in the repository, but an approved bundled channel catalogue is not yet integrated into the app. Imported playlists remain user-supplied input. Public reachability or free-to-air reception does not establish redistribution rights. Do not bypass DRM, conditional access, paid subscriptions, or provider entitlement.

## Community VPN

The chosen direction is an optional, user-controlled Community VPN with provider-issued configuration imported on-device. This remains a security design only; there are no configured VPN servers or working VPN controls. Read [RS VPN Community Security Design](docs/RS-VPN-COMMUNITY-SECURITY.md). A VPN does not grant rights to streams or decrypt paid channels.

## Remaining validation

- Test playback against a lawful HTTPS stream on real Android phone, tablet, Android TV, and TV-box hardware.
- Verify D-pad focus behavior and screen scaling on multiple TV launchers and remote controls.
- Integrate the original `rs-tv-logo.png` artwork into Android resources; the current launcher icon/banner are vector placeholders in the RS TV color palette.
- Import source-attributed satellite records only after independent technical verification.
- Integrate approved country/language/category channel data after rights and reliability review.
- Complete EPG edge-case checks against real provider XMLTV files and device timezone behavior.
- Add signed release configuration after the debug APK has been device-tested.

No physical device playback, satellite tuning, VPN connection, or release signing is claimed as completed.
