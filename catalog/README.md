# RS TV Channel Catalog

This directory defines the source registry and catalogue rules for the RS TV channel library.

## Source registry

See [sources.json](sources.json) for the initial source registry.

## Catalogue presentation

The channel browser must be organized by country and show each country's flag beside its name.

### Country selector

- Provide a first option named **الكل** (All).
- **الكل** displays all approved channels from every country together in one combined results list/grid; it must not group them into separate country pages or hide channels from other countries.
- Each country option shows its national flag and country name, for example 🇹🇷 تركيا and 🇮🇶 العراق.
- Selecting a country filters the same catalogue view to that country's approved channels.
- The selected country filter must be visually clear and reversible by selecting **الكل**.
- Country flags are presentation metadata derived from ISO 3166-1 alpha-2 country codes. Do not infer a channel's country from its language or channel name.
- For territories or countries without a reliable flag mapping, show the country/territory name and a neutral globe icon rather than inventing a flag.

### Combined list behavior

- In **الكل**, show channels from all countries in one continuous, scrollable catalogue.
- Display the channel's country flag and country name on each channel card/row so the origin remains visible even in the combined view.
- Use a deterministic sort: country display name, then channel display name; allow search and category/language filters without changing the country attribution.
- De-duplicate channels using a stable channel ID, not only the display name.
- Do not mix approved free channels with encrypted/paid channels. The encrypted-channel directory is a separate later phase and must have a separate filter/category and access status.

## Internet channels

- IPTV-org is a discovery source for public playlists grouped by country, language, and category.
- Official free streaming services such as Pluto TV, Rakuten TV, and Samsung TV Plus should initially be linked as official services. Their streams must not be extracted or re-hosted without permission.
- A publicly reachable M3U/M3U8 URL is not, by itself, evidence of permission to redistribute it.

## Satellite FTA channels

Use a dedicated satellite catalogue for free-to-air channel names and transponder data. Keep satellite tuning data separate from internet stream URLs. A satellite frequency is not a stream URL and requires compatible reception hardware.

## Channel record fields

Every channel record should include:
- Stable channel ID and display name.
- ISO 3166-1 alpha-2 country code (where applicable).
- Language(s), category/categories, and logo attribution.
- Delivery type: internet, satellite FTA, or official service link.
- Stream/tuning metadata appropriate to the delivery type.
- Source attribution and first-party rights evidence.
- Rights-review state and last metadata/stream-check timestamps.

## Admission states

Every candidate channel must have an explicit state:
- candidate: discovered, not yet reviewed.
- stream_checked: URL was tested, rights are still unverified.
- rights_reviewed: reliable first-party evidence supports inclusion.
- approved: passes both technical and rights review.
- rejected: unavailable, paid/encrypted without authorization, or rights are unclear.

Only approved channels may appear in the default RS TV catalogue.

## Reliability and maintenance

- Grade each internet stream using repeated tests; never promise zero buffering.
- Prefer official broadcaster/CDN sources over third-party relays.
- Show the last check time and hide stale or repeatedly failing streams.
- A successful technical test does not prove copyright permission.
- Recheck links periodically and remove broken or disputed entries.

## Encrypted channels — later phase

Keep encrypted/paid channels out of the free catalogue. A later directory may list channel identity, country, language, official provider, and legal subscription route. Playback requires valid provider authorization; do not include leaked playlists, decryption keys, or unauthorized streams.
