# RS TV Channel Catalog

This directory defines the source registry and catalogue rules for the RS TV channel library.

## Global channel data import

See [import-plan.json](import-plan.json) for the structured API endpoints, field mapping, country-flag behavior, and admission rules. The initial metadata source is the IPTV-org API, which publishes separate datasets for channels, streams, countries, languages, categories, and logos. The API is a discovery source, not a guarantee of rights or stream uptime.

See [sources.json](sources.json) for all source records and review status.

## Internet-channel presentation

The internet channel browser is organized by country and shows each country's flag beside its name.

- Provide a first option named **الكل** (All).
- **الكل** displays all approved channels from every country in one combined continuous list/grid.
- Each country option shows its national flag and country name, for example 🇹🇷 تركيا and 🇮🇶 العراق.
- Selecting a country filters the same catalogue view to that country's approved channels.
- In **الكل**, show each channel's country flag and country name on its card/row.
- Use exact ISO country codes and country metadata for flags. Never infer a flag from language or channel name.
- Unknown country mapping: show a neutral globe icon and the known country label.
- Search, category, and language filters must preserve country attribution.
- De-duplicate by stable channel ID.

## Internet channels

- Use the structured channels and streams datasets to discover candidates.
- Public M3U playlists can be used as an additional discovery/validation source.
- Official free streaming services should initially be linked as official services. Do not extract or re-host their streams without permission.
- A publicly reachable M3U/M3U8 URL is not evidence of permission to redistribute it.

## Worldwide satellite directory

The satellite catalogue is a separate worldwide directory grouped vertically by satellite/orbital position (for example Nilesat, Arabsat, Suhail Sat, Yahsat, and other verified positions). Each channel row shows its name and verified country flag. The **كل الأقمار** view combines the groups without losing satellite attribution.

Read [Global Satellite Catalogue](../docs/SATELLITE-CATALOG.md) and validate records against [satellite-catalog.schema.json](satellite-catalog.schema.json). The model stores satellite positions, channel identities, and transmissions as separate entities so one channel can be carried by multiple satellites without duplicating its identity.

Satellite records include frequency (MHz), polarization, symbol rate (ksps), FEC, DVB standard, encryption status, source, and last verification time. Satellite tuning data is never an internet stream URL. Live satellite reception requires compatible DVB-S/S2/S2X tuner hardware; listing the data alone does not enable reception on a phone or TV.

FTA is a reception/encryption property, not permission to rebroadcast. Keep encrypted/paid services separate and never bypass conditional access, DRM, or provider entitlements. Do not invent frequencies or mark records verified without reliable source evidence.

## Admission states

- `candidate`: imported from a discovery source; not reviewed.
- `stream_checked`: internet URL was tested; rights remain unverified.
- `rights_reviewed`: reliable evidence supports the intended catalogue use.
- `approved`: passes the applicable technical and rights review.
- `rejected`: unavailable, paid/encrypted without authorization, or rights are unclear.

Only `approved` channels appear in the default playable catalogue. Imported API records must never be marked approved automatically.

## Reliability

- Grade each internet stream using repeated playback checks at different times and, where feasible, network locations.
- Prefer official broadcaster/CDN sources over third-party relays.
- Show last checked time and reliability grade.
- Hide stale or repeatedly failing streams from the default catalogue.
- Never promise zero buffering; upstream networks and viewer connections are outside RS TV's control.
- Health checks must be rate-limited and must not record or re-host content.

## Encrypted channels — later phase

Keep encrypted/paid channels out of the free catalogue. A later directory may list channel identity, country, language, official provider, and legal subscription route. Playback requires valid provider authorization; do not include leaked playlists, decryption keys, or unauthorized streams.

## Automated internet candidate import

The scheduled importer is implemented at `tools/import_channel_candidates.py`.
It downloads channels, streams, countries, languages, categories, logos, feeds,
and the upstream blocklist, joins records by stable channel ID, and writes
`catalog/generated/candidates.json`.

Run locally with Python 3:

```sh
python -m unittest discover -s tools -p 'test_*.py' -v
python tools/import_channel_candidates.py
```

GitHub Actions runs the same tests and refreshes the candidate file daily
(`.github/workflows/refresh-channel-candidates.yml`) or on manual dispatch.
The generated file is a review queue only: every record is marked
`candidate`, rights status `not_reviewed`, and reliability `unrated`.
It is not a playable/approved playlist. Adult, closed/replaced, blocklisted,
country-less, and no-stream records are excluded from this candidate output.

## Playback quality: 4K / FHD / HD / SD

The internet player should expose Auto plus the available stream variants in these
classes: 4K UHD (2160p), Full HD (1080p), HD (720p), and SD (480p/576p or
lower). Do not invent a quality variant when the provider does not supply it.
Source labels imported by the candidate importer are hints, not verified
resolution. The player must inspect the selected HLS/DASH variant or decoded
video dimensions and only then display a verified quality badge. Auto is the
default and should adapt to bandwidth/device capability. Never upscale an SD
or HD source and label it as native 4K. If the source has only one rendition,
only that rendition is selectable.
