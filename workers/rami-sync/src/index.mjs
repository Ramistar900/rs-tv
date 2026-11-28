const ARCHIVE_MP4_URL =
  "https://ia600904.us.archive.org/5/items/abdulrhma-mosad/%D8%AA%D9%84%D8%A7%D9%88%D8%A9%20%D8%A8%D8%B5%D9%88%D8%AA%20%23%D8%B9%D8%A8%D8%AF%D8%A7%D9%84%D8%B1%D8%AD%D9%85%D9%86_%D9%85%D8%B3%D8%B9%D8%AF%20%20%D8%AA%D9%84%D8%A7%D9%88%D8%A7%D8%AA%20%D9%87%D8%A7%D8%AF%D8%A6%D8%A9%20%D9%84%D9%86%D9%88%D9%85%20%D8%B9%D9%85%D9%8A%D9%82%20%D9%88%D8%B1%D8%A7%D8%AD%D8%A9%20%D9%86%D9%81%D8%B3%D9%8A%D8%A9%20_%20%23abdulrhma_mosad.mp4";

const MAX_SIMULATION_BYTES = 32 * 1024;
const MAX_VIDEOS = 200;

function json(data, status = 200) {
  return new Response(JSON.stringify(data, null, 2), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "GET, POST, OPTIONS",
      "access-control-allow-headers": "content-type"
    }
  });
}

function header(response, name) {
  return response.headers.get(name);
}

/**
 * Calculate the linear-TV position from one absolute UTC start time.
 * Every client given the same schedule and clock time gets the same item/offset.
 */
export function calculatePlaybackState(channel, nowMs = Date.now()) {
  if (!channel || typeof channel !== "object") {
    throw new TypeError("channel must be an object");
  }

  const startMs = Date.parse(channel.startAtUtc);
  if (!Number.isFinite(startMs)) {
    throw new TypeError("startAtUtc must be a valid ISO-8601 timestamp");
  }

  if (!Array.isArray(channel.videos) ||
      channel.videos.length < 1 ||
      channel.videos.length > MAX_VIDEOS) {
    throw new TypeError(`videos must contain 1-${MAX_VIDEOS} entries`);
  }

  const videos = channel.videos.map((video, index) => {
    const durationMs = Number(video?.durationMs);
    if (!video || typeof video.id !== "string" || !video.id.trim() ||
        !Number.isSafeInteger(durationMs) || durationMs <= 0) {
      throw new TypeError(`videos[${index}] needs an id and positive durationMs`);
    }
    let url;
    try {
      url = new URL(video.url);
    } catch {
      throw new TypeError(`videos[${index}] needs a valid HTTPS URL`);
    }
    if (url.protocol !== "https:") {
      throw new TypeError(`videos[${index}] URL must use HTTPS`);
    }
    return {
      id: video.id,
      title: typeof video.title === "string" ? video.title : "",
      url: url.toString(),
      durationMs
    };
  });

  const totalDurationMs = videos.reduce((sum, video) => sum + video.durationMs, 0);
  if (!Number.isSafeInteger(totalDurationMs) || totalDurationMs <= 0) {
    throw new TypeError("playlist duration is invalid");
  }

  const elapsedMs = nowMs - startMs;
  if (elapsedMs < 0) {
    return {
      status: "not_started",
      serverTimeMs: nowMs,
      startsAtMs: startMs,
      videoIndex: 0,
      video: videos[0],
      videoPositionMs: 0,
      playlistPositionMs: 0,
      totalDurationMs
    };
  }

  const looping = channel.loop !== false;
  if (!looping && elapsedMs >= totalDurationMs) {
    return {
      status: "ended",
      serverTimeMs: nowMs,
      startsAtMs: startMs,
      videoIndex: videos.length - 1,
      video: videos[videos.length - 1],
      videoPositionMs: videos[videos.length - 1].durationMs,
      playlistPositionMs: totalDurationMs,
      totalDurationMs
    };
  }

  const playlistPositionMs = looping
    ? elapsedMs % totalDurationMs
    : elapsedMs;

  let cursorMs = 0;
  for (let index = 0; index < videos.length; index += 1) {
    const video = videos[index];
    const endMs = cursorMs + video.durationMs;
    if (playlistPositionMs < endMs || index === videos.length - 1) {
      return {
        status: "playing",
        serverTimeMs: nowMs,
        startsAtMs: startMs,
        cycle: looping ? Math.floor(elapsedMs / totalDurationMs) : 0,
        videoIndex: index,
        video,
        videoPositionMs: playlistPositionMs - cursorMs,
        playlistPositionMs,
        totalDurationMs
      };
    }
    cursorMs = endMs;
  }

  throw new Error("Unable to resolve playlist position");
}

async function probeArchiveRange() {
  const target = new URL(ARCHIVE_MP4_URL);
  if (target.hostname !== "ia600904.us.archive.org" ||
      !target.pathname.startsWith("/5/items/abdulrhma-mosad/")) {
    throw new Error("Archive probe target is not allowlisted");
  }

  let headResult;
  try {
    const head = await fetch(target, { method: "HEAD", redirect: "follow" });
    headResult = {
      status: head.status,
      contentType: header(head, "content-type"),
      contentLength: header(head, "content-length"),
      acceptRanges: header(head, "accept-ranges"),
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
    rangeResult = {
      status: ranged.status,
      contentType: header(ranged, "content-type"),
      contentLength: header(ranged, "content-length"),
      acceptRanges: header(ranged, "accept-ranges"),
      contentRange: header(ranged, "content-range"),
      finalUrl: ranged.url,
      supported: ranged.status === 206 &&
        /^bytes 0-1023\//i.test(header(ranged, "content-range") || "")
    };
    // Never download the media body during this diagnostic.
    await ranged.body?.cancel();
  } catch (error) {
    rangeResult = { error: String(error), supported: false };
  }

  return {
    target: ARCHIVE_MP4_URL,
    testedAtUtc: new Date().toISOString(),
    head: headResult,
    range: rangeResult,
    note: "The Range response body is cancelled; this endpoint does not proxy or store the video."
  };
}

export default {
  async fetch(request) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "access-control-allow-origin": "*",
          "access-control-allow-methods": "GET, POST, OPTIONS",
          "access-control-allow-headers": "content-type"
        }
      });
    }

    if (request.method === "GET" && url.pathname === "/health") {
      return json({ ok: true, service: "rami-sync-prototype", version: 1 });
    }

    if (request.method === "GET" && url.pathname === "/clock") {
      const now = Date.now();
      return json({ serverTimeMs: now, serverTimeUtc: new Date(now).toISOString() });
    }

    if (request.method === "GET" && url.pathname === "/probe/archive-range") {
      return json(await probeArchiveRange());
    }

    if (request.method === "POST" && url.pathname === "/simulate") {
      const declaredLength = Number(request.headers.get("content-length") || 0);
      if (declaredLength > MAX_SIMULATION_BYTES) {
        return json({ error: "simulation payload too large" }, 413);
      }
      const raw = await request.text();
      if (raw.length > MAX_SIMULATION_BYTES) {
        return json({ error: "simulation payload too large" }, 413);
      }
      try {
        const payload = JSON.parse(raw);
        const nowMs = payload.nowMs == null ? Date.now() : Number(payload.nowMs);
        if (!Number.isFinite(nowMs)) throw new TypeError("nowMs must be a number");
        return json(calculatePlaybackState(payload.channel, nowMs));
      } catch (error) {
        return json({ error: error instanceof Error ? error.message : String(error) }, 400);
      }
    }

    return json({ error: "not found" }, 404);
  }
};
