# RS TV — Sinjar TV Show

RS TV is an Android app for phones, tablets, Android TV and TV boxes. Live television is discovered and imported through IPTV M3U/M3U8 playlists. The app does not tune satellite frequencies, operate a dish or require satellite hardware; a satellite broadcaster is available only when it publishes an online stream.

## Current app structure

- Adaptive Arabic-first interface for phones and Android TV.
- Home, Live Broadcast, IPTV Sources, Favorites, Movies Library, Programme Guide and Settings.
- The standalone «العالمية», YouTube discovery and old private/PIN tabs are removed from navigation.
- IPTV Sources is the only channel-discovery entry point. It presents country and regional M3U packages; the source page downloads a playlist but does not start playback.
- Downloaded source packages are saved in a separate imported-channel library. The user's local playlist, favorites and viewing history remain separate.
- Imported channels are watched from Live Broadcast. The source downloader does not replace the local playlist.
- Re-downloading a source refreshes its saved package. Duplicate stream URLs are removed when packages are merged, using the canonical URL and HTTP-header profile.
- XMLTV programme-guide import, secure XML parsing and channel matching through M3U tvg-id and XMLTV channel IDs.
- Adult-classified entries are excluded from ordinary channel browsing. There is no separate private/PIN content tab.

## IPTV country and region packages

The source directory is grouped by region and country. Arabic countries are separate packages, including Syria, Lebanon, Iraq, Palestine, Jordan, Egypt, Saudi Arabia, the Gulf countries and North Africa. Other groups include Europe, North America (United States and Canada), Latin America and South America, East Asia, other Asian countries, Africa and Oceania.

The app includes:
- 128 separate Doubo country M3U package entries, with the UK country path mapped to the publisher's UK playlist.
- IPTV-org global, regional, country, language and category M3U packages.
- Separate movie, series and sports category playlists, plus further topic categories.
- Other supplementary playlist sources, each labelled with its provider and source-specific notes.

Country, category and global playlists overlap. They are alternate packages, not additive channel counts. The imported library removes matching canonical stream URLs when sources are combined.

## Catalogue size and the 50,000 target

The bundled IPTV-org candidate snapshot generated on 2026-11-08 contained 31,758 upstream channel records, 18,637 stream records and 8,622 channel candidates with HTTPS streams across 180 represented countries. Every record remained marked candidate, not_reviewed and unrated.

The Doubo directory's 2026-11-08 published snapshot reported 2,982 listed channels across 128 countries and regions. Its country lists overlap its worldwide and category playlists, and it also overlaps other IPTV indexes. These published counts must not be added together as unique working RS TV channels.

The Android candidate index parser supports up to 50,000 records. That is a processing limit, not a claim that the library currently contains 50,000 unique, working or licensed channels. The app shows the number of available packages and the number of unique channels actually imported. The 50,000 target is not yet achieved.

## Rights, safety and reliability

An HTTPS URL, an M3U listing, or a successful HTTP response does not prove permission to redistribute or watch a channel. Public feeds may be geo-restricted, may stop working, or may have rights limited by territory, programme or event.

Doubo describes a publisher-side filter for feeds from broadcaster sites, broadcast-grade CDNs and official free platforms; its CC BY 4.0 licence applies to directory/link-selection data, not to the underlying programmes. IPTV-org and other community playlists are discovery sources and remain unreviewed unless separate channel-level checks pass. RS TV must not add pirated, paid-without-entitlement, DRM-bypassed, malicious, or unclear-rights streams, and it must not claim a source is licensed solely because it is public or technically reachable.

The playlist downloader accepts HTTPS stream URLs, applies source host/redirect and size checks, and keeps downloaded packages separate from the local playlist. Technical validation is not a legal approval or a guarantee of device playback.

## Catalogue and source documentation

- [IPTV package catalogue and source limitations](catalog/README.md)
- [IPTV source registry](catalog/sources.json)
- [M3U country-package import plan](catalog/import-plan.json)

The daily playlist quality audit now includes the 128 Doubo country M3U endpoints in its source registry. Pull-request runs execute audit unit tests; live endpoint/channel checks are skipped in pull-request context and run on the scheduled/default-branch workflow. An audit pass does not establish broadcast rights or real-device playback.

## Build and test

Requirements: JDK 17, Android SDK Platform 35, and Gradle 8.9.

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

GitHub Actions runs parser/source unit tests and assembles a debug APK for pull-request validation. A successful workflow build is necessary but is not a substitute for installing and testing the APK on a real phone, Android TV or TV box. Do not merge the current package changes until the user has tested the build.

## Remaining validation

- Complete the current PR's Android unit tests and APK build.
- Install the new APK and test country-package downloads, country names, duplicate removal, live playback, D-pad navigation and the existing local playlist on real devices.
- Run the scheduled source endpoint and channel-health checks after the changes are merged, then inspect the resulting unique-stream totals and failures.
- Add sources to the approved playable library only after source, rights, geographic and technical checks pass.

No physical-device playback test or 50,000-channel achievement is claimed as completed.
