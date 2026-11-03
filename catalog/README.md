# RS TV Channel Catalog

This directory defines the source registry and catalogue rules for the RS TV channel library.

## Global channel data import

See [import-plan.json](import-plan.json) for the structured API endpoints, field mapping, country-flag behavior, and admission rules. The initial metadata source is the IPTV-org API, which publishes separate datasets for channels, streams, countries, languages, categories, and logos. Its documentation describes country codes, channel IDs, broadcast languages, and stream records. The API is a discovery source, not a guarantee of rights or stream uptime.

See [sources.json](sources.json) for all source records and review status.

## Catalogue presentation

The channel browser must be organized by country and show each country's flag beside its name.

### Country selector

- Provide a first option named **الكل** (All).
- **الكل** displays all approved channels from every country together in one combined continuous list/grid.
- Each country option shows its national flag and country name, for example 🇹🇷 تركيا and 🇮🇶 العراق.
- Selecting a country filters the same catalogue view to that country's approved channels.
- In **الكل**, show each channel's country flag and country name on its card/row.
- Use exact ISO country codes and the country metadata endpoint for flags. Never infer a flag from language or channel name.
- Unknown country mapping: show a neutral globe icon and the known country label.
- Search, category, and language filters must preserve country attribution.
- De-duplicate by stable channel ID.

## Internet channels

- Use the structured channels and streams datasets to discover candidates.
- Public M3U playlists can be used as an additional discovery/validation source.
- Official free streaming services should initially be linked as official services. Do not extract or re-host their streams without permission.
- A publicly reachable M3U/M3U8 URL is not evidence of permission to redistribute it.

## Satellite FTA channels

Maintain a separate satellite catalogue for free-to-air channel names and transponder data. Keep satellite tuning data separate from internet stream URLs. A satellite frequency is not an internet stream and requires compatible reception hardware.

## Admission states

- `candidate`: imported from a discovery source; not reviewed.
- `stream_checked`: URL was tested; rights remain unverified.
- `rights_reviewed`: reliable first-party evidence supports inclusion.
- `approved`: passes both technical and rights review.
- `rejected`: unavailable, paid/encrypted without authorization, or rights are unclear.

Only `approved` channels appear in the default RS TV catalogue. Imported API records must never be marked approved automatically.

## Reliability

- Grade each internet stream using repeated playback checks at different times and, where feasible, network locations.
- Prefer official broadcaster/CDN sources over third-party relays.
- Show last checked time and reliability grade.
- Hide stale or repeatedly failing streams from the default catalogue.
- Never promise zero buffering; upstream networks and viewer connections are outside RS TV's control.
- Health checks must be rate-limited and must not record or re-host content.

## Encrypted channels — later phase

Keep encrypted/paid channels out of the free catalogue. A later directory may list channel identity, country, language, official provider, and legal subscription route. Playback requires valid provider authorization; do not include leaked playlists, decryption keys, or unauthorized streams.
