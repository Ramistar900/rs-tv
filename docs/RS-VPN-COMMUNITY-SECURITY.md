# RS VPN — Community Mode Security Design

Status: architecture specification only. No VPN servers, community endpoints, credentials, or tunnels are configured by this document.

## 1. Product decision

RS TV will offer an optional **Community VPN** mode. It will not operate an RS-owned VPN server in this phase. A user may connect only to a community VPN provider they deliberately select and trust, using a configuration or integration that the provider is authorized to distribute.

A VPN changes the network route and visible IP address. It does not decrypt paid channels, grant content rights, or guarantee access to geo-restricted services. RS TV must not advertise it as a way to bypass subscriptions or licensing.

## 2. Trust model

Community VPN operators are third parties and must be treated as untrusted network intermediaries. They may observe connection metadata and, depending on the traffic and TLS protections, may attempt to inspect or manipulate traffic.

Required user-facing warnings:
- The VPN operator can see the user's source IP and connection metadata.
- HTTPS protects application content in transit, but does not hide all metadata.
- Do not use unknown community endpoints for banking, identity, or other sensitive activity.
- Free service availability, speed, logging practices, and jurisdiction are not guaranteed.
- The user must review the provider's privacy policy and terms before connecting.

No provider is marked "verified" merely because a configuration imports or a handshake succeeds.

## 3. Safe initial scope

Phase 1 is **bring-your-own-community-provider**:
- No bundled list of anonymous public VPN endpoints.
- No automatic connection to an endpoint selected by RS TV.
- No scraping, harvesting, or redistribution of community VPN credentials/configurations.
- The user imports a provider-issued WireGuard configuration or uses a provider's documented supported integration.
- Show provider name, endpoint host, country (if reliably supplied), configuration source, and last user confirmation.
- Require explicit consent before Android's VPN permission prompt and before each first connection to a provider.
- Provide a clear disconnect action and connection status.

Do not promise that arbitrary WireGuard configurations are compatible until the selected Android integration has been tested.

## 4. Android implementation boundary

When implementation begins:
- Use Android `VpnService` with a maintained, reviewed WireGuard-compatible implementation; do not implement cryptography or tunnelling primitives ourselves.
- Keep tunnel keys and imported configuration on-device, protected by Android Keystore where technically supported.
- Never send private keys, preshared keys, full configuration files, or traffic contents to RS TV servers, analytics, crash reports, or GitHub.
- Do not log secrets, DNS query contents, visited URLs, or packet data.
- Use a foreground service and a persistent system-visible VPN notification while connected.
- Respect Android's always-on VPN and lockdown settings; do not silently change them.
- Handle revocation, process death, network changes, airplane mode, captive portals, and reconnect failures safely.
- Provide per-app or full-device routing only when clearly explained; default to full-device tunnel only after explicit user choice.
- DNS handling must be explicit. Do not silently substitute an operator-controlled resolver without disclosure.
- Do not claim leak protection until IPv4, IPv6, and DNS leak tests pass on supported Android versions.

## 5. Backend and privacy

The first phase should require **no RS VPN backend**. If a later directory of community providers is introduced:
- Store public provider metadata only (name, documentation URL, supported protocol, jurisdiction, privacy-policy URL, last review date).
- Do not store user tunnel configs, private keys, passwords, or browsing history.
- Fetch directory updates over HTTPS from a controlled, signed release or API.
- Validate and review metadata; treat all provider-supplied strings and URLs as untrusted input.
- Do not remotely push a new endpoint into an active tunnel or connect users without consent.
- Provide a way to remove a provider and erase its local configuration.

## 6. Provider admission checklist

A provider may be listed only after a human review confirms:
1. A first-party website and public documentation exist.
2. The provider explicitly permits public/community use and redistribution of its connection profile, if profiles are distributed.
3. Privacy policy, logging statement, jurisdiction, and acceptable-use terms are available.
4. The operator and endpoint ownership are reasonably attributable.
5. The configuration uses supported modern cryptography and does not contain unexpected routes or DNS settings.
6. No embedded credentials, private keys, or shared secrets are committed to this repository.
7. The listing has a review date and a removal path.

A provider that fails any item is not listed. Community availability is not proof of safety.

## 7. Abuse and content boundaries

RS TV must not use VPN functionality to obtain paid-channel decryption keys, evade DRM, redistribute copyrighted streams without authorization, or defeat a broadcaster's access controls. Channel availability remains subject to the broadcaster's rights, provider terms, and local law.

## 8. Verification plan before release

- Static review of Android service lifecycle and permission handling.
- Verify no secrets are present in source, logs, analytics, or crash reports.
- Test connect/disconnect, permission denial, revoked permission, reboot, network handoff, and app force-stop.
- Test IPv4, IPv6, and DNS routing/leaks on each supported Android API level and device class.
- Verify notification visibility and that disconnect actually tears down the tunnel.
- Verify provider removal erases local config and keys.
- Verify no automatic connection occurs after install, update, or provider import.
- Publish a clear privacy notice and provider-risk warning.

## 9. Explicit non-goals for this phase

- No free unlimited VPN claim.
- No RS-operated server network.
- No anonymous public endpoint bundle.
- No central collection of user VPN credentials.
- No guarantee that a VPN will make a streaming service available in another country.
- No production release until the implementation and leak tests are complete.

## 10. Decision record

Selected mode: **Community**.

Implementation decision: start with a user-selected, provider-issued configuration and on-device tunnel handling. Do not embed unknown free endpoints. The current repository is an empty Android project shell, so this document is a design baseline, not a completed or tested VPN feature.
