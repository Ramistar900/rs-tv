# RS TV Global Satellite Catalogue

## Purpose and scope

RS TV maintains a worldwide **satellite reference directory** grouped by orbital satellite position. The visible order is vertical: satellite heading (for example, Nilesat, Arabsat, Suhail Sat, Yahsat, and other verified operators/positions), followed by channel rows. Each row shows the channel name and its country flag when a verified ISO 3166-1 alpha-2 country code exists.

This document defines the data model and UI contract. It does not claim that a complete worldwide frequency database has already been imported.

## Three distinct delivery concepts

1. **Satellite directory:** satellite name, orbital position, transponder frequency, polarization, symbol rate, FEC, standard, modulation, encryption state, source, and verification date. These are tuning/reference facts, not playable URLs.
2. **Internet streams:** M3U/M3U8 or official stream URLs, handled by the separate internet-channel catalogue and its rights/reliability checks.
3. **Live satellite reception:** requires compatible DVB-S/S2/S2X tuner hardware and device integration. An ordinary phone, tablet, or Android TV device cannot tune a satellite dish by software alone.

Never put a satellite frequency in a stream URL field or imply that listing a frequency enables playback.

## Data relationships

The JSON Schema is [satellite-catalog.schema.json](../catalog/satellite-catalog.schema.json).

- `satellites` stores each satellite/orbital position once.
- `channels` stores each channel identity once, including its country code, languages, categories, and optional logo.
- `transmissions` links a channel to a satellite and a specific transponder. A channel can have multiple transmissions across multiple satellites; do not duplicate its channel identity.
- Each satellite and transmission carries source evidence and a UTC verification timestamp.
- The channel flag is derived only from the verified channel country code. It is not inferred from language, satellite footprint, or channel name. Unknown country uses a neutral globe and an unknown-country label.

## Presentation contract

- The first satellite view is **كل الأقمار** (All satellites), followed by verified satellite positions in a stable, human-readable order.
- Under each satellite, render channels in a vertical list. Show channel name and country flag beside it; show the satellite name/position in the group heading.
- Selecting a satellite filters the same list to its transmissions.
- Selecting a channel opens its detail view with satellite, orbital position, frequency (MHz), polarization, symbol rate (ksps), FEC, standard, encryption state, quality hint, source, and last verification time.
- A channel transmitted by more than one satellite appears under each applicable satellite but remains one channel entity.
- Search supports channel name, satellite, country, language, and category.
- Filters must not merge satellite reception records with internet stream records.

## Review and access states

- `candidate`: imported/discovered but not verified.
- `technical_checked`: tuning parameters have been checked against a reliable technical source.
- `rights_reviewed`: channel identity and the relevant use/display rights have been reviewed; this alone does not authorize rebroadcast.
- `approved`: permitted for the specific catalogue purpose and technically verified.
- `rejected`: incorrect, stale, inaccessible, or not suitable for inclusion.

FTA/encryption is a technical reception property, not a copyright licence. An FTA signal must not automatically be re-streamed, recorded, rebroadcast, or redistributed. Keep encrypted/paid services separate; never bypass encryption, DRM, conditional access, or provider entitlement.

## Data quality rules

- Do not invent satellite names, orbital positions, frequencies, symbol rates, FEC, channel-country mappings, or quality.
- Prefer satellite operator, broadcaster, or other primary technical sources. Secondary directories may be used as discovery references and must be independently checked before approval.
- Store source URL and last verification time for each satellite/transmission.
- Treat external metadata and URLs as untrusted input; validate and sanitize them.
- Do not label a channel 4K/FHD/HD from a directory hint alone. Verify actual video dimensions when a playable stream or tuner integration exists.
- Keep the worldwide directory extensible; do not claim exhaustive coverage until source coverage and freshness have been measured.

## Planned implementation order

1. Validate this schema and agree the catalogue relationships.
2. Build a satellite browser UI with a vertical group per satellite and channel rows with flags.
3. Add reviewed, source-attributed satellite data in batches, beginning with well-documented regional satellites.
4. Integrate tuner support only for compatible hardware, separately from Media3 internet playback.
5. Add tests for duplicate channel identities, broken satellite/channel references, missing country codes, stale records, and separation of satellite versus internet delivery.
