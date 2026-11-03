# RS TV — Sinjar TV Show

RS TV is an independent Android streaming application project for phones, tablets, Android TV, and TV boxes.

## Android foundation

The Android application is built with Kotlin, Jetpack Compose, and Media3. It can import a user-selected local M3U/M3U8 playlist, list its channels, search by channel/group, and play a selected HTTPS stream. This first playback phase does not bundle a public channel catalogue or certify the rights of imported streams.

Build locally with Gradle 8.9 and JDK 17:

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

GitHub Actions runs the playlist-parser unit tests and builds a debug APK on changes to the Android app, Gradle configuration, or its workflow.

## Project direction

- Adaptive touch and TV-remote interfaces.
- Authorized free-to-air and official internet streams only.
- M3U/M3U8 playlists and XMLTV programme guides where permitted.
- Internet channels organized by country, language, and category.
- A separate worldwide satellite directory grouped by satellite/orbital position.
- Optional Community VPN integration, subject to the security design below.

Read [Global Satellite Catalogue](docs/SATELLITE-CATALOG.md) for the satellite data model and vertical per-satellite channel-list behavior. Satellite frequencies are tuning metadata, not internet streams; live reception requires compatible DVB-S/S2/S2X tuner hardware.

## Community VPN security baseline

The selected direction is **Community VPN**. The first phase will let users deliberately select a community provider and import a provider-issued configuration on-device. RS TV will not bundle anonymous public VPN endpoints, collect private keys, or connect automatically.

Read [RS VPN Community Security Design](docs/RS-VPN-COMMUNITY-SECURITY.md) before implementing VPN functionality. This is an architecture specification, not a working VPN feature. No VPN servers or endpoints are currently configured.

A VPN does not decrypt paid channels or grant streaming rights. Only streams that are free and authorized by their rights holders may be included in the built-in channel catalogue.

## Current status

- Android project foundation: merged into `main`.
- M3U/M3U8 local playlist import, channel list/search, and Media3 playback: implemented for HTTPS stream URLs.
- Imported playlists remain user-provided input; public reachability does not establish redistribution rights.
- Internet channel-source ingestion: candidate importer and catalogue planning are present; reviewed catalogue integration is pending.
- Satellite catalogue: schema and product/data contract documented; no global frequency dataset or satellite playback integration is claimed yet.
- RS TV logo: source image remains at [rs-tv-logo.png](rs-tv-logo.png); app resource integration is pending.
- VPN: security design documented; no implementation or live service yet.
