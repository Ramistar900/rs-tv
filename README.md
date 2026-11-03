# RS TV — Sinjar TV Show

RS TV is an independent **internet-only** Android viewing app for phones, tablets, Android TV, and TV boxes. It is built with Kotlin, Jetpack Compose, and AndroidX Media3. Satellite reception, DVB tuners, dishes, and satellite-frequency directories are outside the product.

## Implemented Android features

- Adaptive navigation: bottom navigation on compact screens and a navigation rail on wide screens/TV layouts.
- Arabic-first interface with a black, red, and silver visual identity.
- Local M3U/M3U8 import, channel name/group search, and HTTPS stream playback.
- Local persistence for the imported playlist, favorites, and recent channel history.
- XMLTV programme-guide import, secure XML parsing, and channel matching through M3U `tvg-id` and XMLTV `channel` IDs.
- Android TV launcher entry and a dedicated TV banner; custom vector launcher icon.
- Settings to re-import local data and clear the saved playlist, favorites, or viewing history.
- Debug APK upload as a GitHub Actions artifact after tests and build succeed.

## Worldwide content discovery

- Automated worldwide internet-channel candidate import from the IPTV-org structured catalogue, with country, language, category, logo, feed, and blocklist metadata.
- The global candidate file is refreshed daily and remains a review queue. It is not automatically treated as free, authorized, stable, or ready to play.
- YouTube live discovery is planned through the official YouTube Data API and requires a protected `YOUTUBE_API_KEY`; results use official YouTube playback routes.
- Official broadcaster websites and internet services are added as verified external/embedded sources, according to each provider's rules.
- International movie and series metadata can be discovered through TMDB after configuring `TMDB_API_KEY` and attribution. Metadata is separate from streaming rights and playback.
- Public collection media such as Internet Archive items requires an item-by-item rights review.

See [Global Internet Catalogue](catalog/README.md), [source registry](catalog/sources.json), and [import plan](catalog/import-plan.json).

## Build and test

Requirements: JDK 17, Android SDK Platform 35, and Gradle 8.9.

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

GitHub Actions runs the M3U and XMLTV parser unit tests and assembles a debug APK for changes to Android sources, Gradle configuration, or the workflow. Successful runs publish the APK as the `rs-tv-debug-apk` workflow artifact for 14 days.

## Playlist and guide behavior

- Only HTTPS stream URLs with a valid host are accepted by the current M3U parser. HTTP, file, and other non-HTTPS stream entries are skipped.
- Imported playlists are copied into the app's private local storage; they are not uploaded to a server.
- Favorites and recent viewing history stay on the device.
- XMLTV parsing rejects DTD declarations and external entities, limits input size and the number of parsed programme entries, and ignores malformed programme rows.
- Programme matching uses the playlist channel's `tvg-id` and the XMLTV programme's `channel` value.
- Playlist/logo metadata is untrusted input. The app does not yet download arbitrary logo URLs.

## Channel and media rights

Imported public URLs are discovery candidates only. Public reachability or free-to-air reception does not establish redistribution rights. Do not bypass DRM, conditional access, paid subscriptions, or provider entitlement. Open YouTube and restricted broadcaster streams through their official playback routes.

## Community VPN

The chosen direction is an optional, user-controlled Community VPN with provider-issued configuration imported on-device. This remains a security design only; there are no configured VPN servers or working VPN controls. Read [RS VPN Community Security Design](docs/RS-VPN-COMMUNITY-SECURITY.md). A VPN does not grant rights to streams or decrypt paid channels.

## Remaining validation

- Connect the approved global channel catalogue to the Android app after rights review and verify its update/cache behavior.
- Configure and test official YouTube live discovery using a protected API key and YouTube's official playback route.
- Configure TMDB metadata access and provider attribution after terms/licensing review.
- Test playback against a lawful HTTPS stream on real Android phone, tablet, Android TV, and TV-box hardware.
- Verify D-pad focus behavior and screen scaling on multiple TV launchers and remote controls.
- Integrate the original `rs-tv-logo.png` artwork into Android resources; the current launcher icon/banner are vector placeholders in the RS TV color palette.
- Complete EPG edge-case checks against real provider XMLTV files and device timezone behavior.
- Add signed release configuration after the debug APK has been device-tested.

No physical device playback, provider API integration, VPN connection, or release signing is claimed as completed.
