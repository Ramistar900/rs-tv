const TV_JSON_URL =
  "https://gist.githubusercontent.com/Ramistar900/061b27eb6e7edb76a637021ae66e600e/raw/tv.json";
const ARCHIVE_MP4_URL =
  "https://ia600904.us.archive.org/5/items/abdulrhma-mosad/%D8%AA%D9%84%D8%A7%D9%88%D8%A9%20%D8%A8%D8%B5%D9%88%D8%AA%20%23%D8%B9%D8%A8%D8%AF%D8%A7%D9%84%D8%B1%D8%AD%D9%85%D9%86_%D9%85%D8%B3%D8%B9%D8%AF%20%20%D8%AA%D9%84%D8%A7%D9%88%D8%A7%D8%AA%20%D9%87%D8%A7%D8%AF%D8%A6%D8%A9%20%D9%84%D9%86%D9%88%D9%85%20%D8%B9%D9%85%D9%8A%D9%82%20%D9%88%D8%B1%D8%A7%D8%AD%D8%A9%20%D9%86%D9%81%D8%B3%D9%8A%D8%A9%20_%20%23abdulrhma_mosad.mp4";
const MAX_SIMULATION_BYTES = 32 * 1024;
const MAX_VIDEOS = 200;
const MAX_CHANNEL_NUMBER = 999999;
const CATALOG_CACHE_SECONDS = 20;

function json(data, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "GET, POST, OPTIONS",
      "access-control-allow-headers": "content-type",
      ...extraHeaders
    }
  });
}

function validateChannelNumber(value) {
  if (!/^\\d+$/.test(value || "")) return null;
  const number = Number(value);
  return Number.isSafeInteger(number) && number >= 1 && number <= MAX_CHANNEL_NUMBER
    ? String(number)
    : null;
}

function validatePlaylist(playlist) {
  if (!Array.isArray(playlist) || playlist.length < 1 || playlist.length > MAX_VIDEOS) {
    throw new TypeError(`playlist must contain 1-${MAX_VIDEOS} videos`);
  }
  return playlist.map((entry, index) => {
    if (!entry || typeof entry !== "object") {
      throw new TypeError(`playlist[${index}] must be an object`);
    }
    let parsed;
    try {
      parsed = new URL(entry.url);
    } catch {
      throw new TypeError(`playlist[${index}].url must be a valid HTTPS URL`);
    }
    if (parsed.protocol !== "https:") {
      throw new TypeError(`playlist[${index}].url must use HTTPS`);
    }
    const durationSeconds = Number(entry.duration);
    if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) {
      throw new TypeError(`playlist[${index}].duration must be a positive number of seconds`);
    }
    return {
      url: parsed.toString(),
      durationSeconds,
      title: typeof entry.title === "string" ? entry.title : null
    };
  });
}

export function calculatePlaylistPosition(playlist, startAtUtc, nowMs = Date.now()) {
  const startMs = Date.parse(startAtUtc);
  if (!Number.isFinite(startMs)) throw new TypeError("startAtUtc must be a valid UTC timestamp");
  const entries = validatePlaylist(playlist);
  const totalSeconds = entries.reduce((sum, item) => sum + item.durationSeconds, 0);
  const elapsedSeconds = (nowMs - startMs) / 1000;
  if (elapsedSeconds < 0) {
    return { status: "not_started", playlistIndex: 0, offsetSeconds: 0, totalDurationSeconds: totalSeconds };
  }
  const position = elapsedSeconds % totalSeconds;
  let cursor = 0;
  for (let index = 0; index < entries.length; index += 1) {
    const end = cursor + entries[index].durationSeconds;
    if (position < end || index === entries.length - 1) {
      return {
        status: "playing",
        playlistIndex: index,
        offsetSeconds: Number((position - cursor).toFixed(3)),
        cycle: Math.floor(elapsedSeconds / totalSeconds),
        totalDurationSeconds: totalSeconds
      };
    }
    cursor = end;
  }
  throw new Error("Unable to calculate playlist position");
}

async function loadCatalog() {
  const response = await fetch(TV_JSON_URL, {
    headers: { "accept": "application/json" },
    cf: { cacheTtl: CATALOG_CACHE_SECONDS, cacheEverything: true }
  });
  if (!response.ok) throw new Error(`catalog_fetch_failed_${response.status}`);
  const catalog = await response.json();
  if (!catalog || typeof catalog !== "object" || Array.isArray(catalog)) {
    throw new TypeError("tv.json must be an object keyed by channel number");
  }
  return catalog;
}

async function probeArchiveRange() {
  const target = new URL(ARCHIVE_MP4_URL);
  let headResult;
  try {
    const head = await fetch(target, { method: "HEAD", redirect: "follow" });
    headResult = {
      status: head.status,
      contentType: head.headers.get("content-type"),
      contentLength: head.headers.get("content-length"),
      acceptRanges: head.headers.get("accept-ranges"),
      finalUrl: head.url
    };
    await head.body?.cancel();
  } catch (error) {
    headResult = { error: String(error) };
  }
  let rangeResult;
  try {
    const ranged = await fetch(target, {
      method: "GET",
      headers: { Range: "bytes=0-1023" },
      redirect: "follow"
    });
    const contentRange = ranged.headers.get("content-range");
    rangeResult = {
      status: ranged.status,
      contentType: ranged.headers.get("content-type"),
      contentLength: ranged.headers.get("content-length"),
      acceptRanges: ranged.headers.get("accept-ranges"),
      contentRange,
      finalUrl: ranged.url,
      supported: ranged.status === 206 && /^bytes 0-1023\\//i.test(contentRange || "")
    };
    await ranged.body?.cancel();
  } catch (error) {
    rangeResult = { error: String(error), supported: false };
  }
  return {
    target: ARCHIVE_MP4_URL,
    testedAtUtc: new Date().toISOString(),
    head: headResult,
    range: rangeResult,
    note: "The response body is cancelled; this endpoint does not proxy or store video."
  };
}

export default {
  async fetch(request) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: {
        "access-control-allow-origin": "*",
        "access-control-allow-methods": "GET, POST, OPTIONS",
        "access-control-allow-headers": "content-type"
      }});
    }

    if (request.method === "GET" && url.pathname === "/health") {
      return json({ ok: true, service: "rami-tv-catalog-sync", version: 2 });
    }
    if (request.method === "GET" && (url.pathname === "/clock" || url.pathname === "/api/clock")) {
      const now = Date.now();
      return json({ serverTimeMs: now, serverTimeUtc: new Date(now).toISOString() });
    }
    if (request.method === "GET" && url.pathname === "/probe/archive-range") {
      return json(await probeArchiveRange());
    }

    if (request.method === "GET" && url.pathname === "/api/channels") {
      try {
        const catalog = await loadCatalog();
        const channels = Object.keys(catalog)
          .map(validateChannelNumber)
          .filter(Boolean)
          .sort((a, b) => Number(a) - Number(b));
        return json({ channels, count: channels.length, source: "tv.json" });
      } catch (error) {
        return json({ error: error.message || String(error) }, 502);
      }
    }

    const match = url.pathname.match(/^\\/api\\/channel\\/(\\d+)\\/?$/);
    if (request.method === "GET" && match) {
      const channelNumber = validateChannelNumber(match[1]);
      if (!channelNumber) return json({ error: "invalid_channel_number" }, 400);
      try {
        const catalog = await loadCatalog();
        if (!Object.prototype.hasOwnProperty.call(catalog, channelNumber)) {
          return json({ error: "channel_not_found", channel: channelNumber }, 404);
        }
        const playlist = validatePlaylist(catalog[channelNumber]);
        const result = {
          channel: channelNumber,
          status: "available",
          playlist,
          playlistCount: playlist.length,
          source: "tv.json",
          fetchedAtUtc: new Date().toISOString(),
          sync: { status: "schedule_not_configured" }
        };

        // tv.json may optionally define a global startAtUtc or a per-channel startAtUtc.
        // Existing catalog files without either remain readable; no start time is fabricated.
        const scheduleStart = catalog[channelNumber]?.startAtUtc || catalog.startAtUtc;
        if (scheduleStart) {
          const position = calculatePlaylistPosition(playlist, scheduleStart);
          result.sync = {
            ...position,
            status: position.status,
            scheduleStartUtc: new Date(Date.parse(scheduleStart)).toISOString(),
            currentVideoUrl: playlist[position.playlistIndex]?.url || null,
            videoDurationSeconds: playlist[position.playlistIndex]?.durationSeconds || null,
            serverTimeUtc: new Date().toISOString()
          };
        }
        return json(result, 200, { "cache-control": "public, max-age=5, s-maxage=20" });
      } catch (error) {
        return json({ error: error.message || String(error), channel: channelNumber }, 502);
      }
    }

    if (request.method === "POST" && url.pathname === "/simulate") {
      const declaredLength = Number(request.headers.get("content-length") || 0);
      if (declaredLength > MAX_SIMULATION_BYTES) return json({ error: "payload_too_large" }, 413);
      const raw = await request.text();
      if (raw.length > MAX_SIMULATION_BYTES) return json({ error: "payload_too_large" }, 413);
      try {
        const payload = JSON.parse(raw);
        return json(calculatePlaylistPosition(payload.playlist, payload.startAtUtc, Number(payload.nowMs ?? Date.now())));
      } catch (error) {
        return json({ error: error.message || String(error) }, 400);
      }
    }

    return json({ error: "not_found" }, 404);
  }
};
