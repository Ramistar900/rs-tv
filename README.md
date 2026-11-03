# RS TV — Sinjar TV Show

RS TV is an independent Android streaming application project for phones, tablets, Android TV, and TV boxes.

## Android foundation

The initial Android application is built with Kotlin, Jetpack Compose, Android Gradle Plugin, and Media3 dependencies. The UI foundation is designed to support touch devices and Android TV/TV Box navigation. The initial screen is a placeholder; it does not yet load or play channel streams.

Build locally with Gradle 8.9 and JDK 17:

```bash
gradle --no-daemon :app:assembleDebug
```

GitHub Actions builds a debug APK on changes to the Android app, Gradle configuration, or its workflow.

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

- Android project foundation: created on `android-foundation`.
- Initial Compose home screen: created; channel playback is not wired yet.
- Media3 dependencies: declared; player integration is pending.
- Internet channel-source ingestion: candidate importer and catalogue planning are present; Android catalogue integration is pending.
- Satellite catalogue: schema and product/data contract documented; no global frequency dataset or satellite playback integration is claimed yet.
- RS TV logo: source image remains at [rs-tv-logo.png](rs-tv-logo.png); app resource integration is pending.
- VPN: security design documented; no implementation or live service yet.
