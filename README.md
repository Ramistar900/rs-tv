# RS TV — Sinjar TV Show

RS TV is an independent Android streaming application project for phones, tablets, Android TV, and TV boxes.

## Project direction

- Adaptive touch and TV-remote interfaces.
- Authorized free-to-air and official internet streams.
- M3U/M3U8 playlists and XMLTV programme guides where permitted.
- Channel catalogues organized by country, language, and category.
- Optional Community VPN integration, subject to the security design below.

## Community VPN security baseline

The selected direction is **Community VPN**. The first phase will let users deliberately select a community provider and import a provider-issued configuration on-device. RS TV will not bundle anonymous public VPN endpoints, collect private keys, or connect automatically.

Read [RS VPN Community Security Design](docs/RS-VPN-COMMUNITY-SECURITY.md) before implementing VPN functionality. This is an architecture specification, not a working VPN feature. No VPN servers or endpoints are currently configured.

A VPN does not decrypt paid channels or grant streaming rights. Only streams that are free and authorized by their rights holders may be included in the built-in channel catalogue.

## Current status

- Repository foundation: initialized.
- Android application source/build configuration: not yet created.
- Channel-source ingestion: not yet implemented.
- VPN: security design documented; no implementation or live service yet.
