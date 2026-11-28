import test from "node:test";
import assert from "node:assert/strict";
import worker, { calculatePlaylistPosition } from "../src/index.mjs";

const playlist = [
  { url: "https://example.org/a.mp4", duration: 10 },
  { url: "https://example.org/b.mp4", duration: 20 }
];
const startAtUtc = "2026-11-28T00:00:00.000Z";

test("uses one UTC timeline to select the current video and offset", () => {
  const state = calculatePlaylistPosition(playlist, startAtUtc, Date.parse(startAtUtc) + 15_000);
  assert.equal(state.status, "playing");
  assert.equal(state.playlistIndex, 1);
  assert.equal(state.offsetSeconds, 5);
});

test("loops playlist duration consistently", () => {
  const state = calculatePlaylistPosition(playlist, startAtUtc, Date.parse(startAtUtc) + 35_000);
  assert.equal(state.playlistIndex, 0);
  assert.equal(state.offsetSeconds, 5);
  assert.equal(state.cycle, 1);
});

test("does not invent a start time before schedule start", () => {
  const state = calculatePlaylistPosition(playlist, startAtUtc, Date.parse(startAtUtc) - 1000);
  assert.equal(state.status, "not_started");
  assert.equal(state.offsetSeconds, 0);
});

test("rejects invalid URLs and non-positive durations", () => {
  assert.throws(() => calculatePlaylistPosition(
    [{ url: "http://example.org/a.mp4", duration: 10 }], startAtUtc
  ), /HTTPS/);
  assert.throws(() => calculatePlaylistPosition(
    [{ url: "https://example.org/a.mp4", duration: 0 }], startAtUtc
  ), /positive/);
});

test("Worker discovers all channel numbers present in tv.json dynamically", async () => {
  const catalog = Object.fromEntries(
    Array.from({ length: 20 }, (_, index) => [String(index + 1), playlist])
  );
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response(JSON.stringify(catalog), {
    status: 200, headers: { "content-type": "application/json" }
  });
  try {
    const response = await worker.fetch(new Request("https://worker.test/api/channels"));
    const body = await response.json();
    assert.equal(body.count, 20);
    assert.equal(body.channels[8], "9");
    assert.equal(body.channels[19], "20");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("a newly added channel is served without a code change", async () => {
  const catalog = { "1": playlist, "9": playlist };
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response(JSON.stringify(catalog), {
    status: 200, headers: { "content-type": "application/json" }
  });
  try {
    const response = await worker.fetch(new Request("https://worker.test/api/channel/9"));
    const body = await response.json();
    assert.equal(response.status, 200);
    assert.equal(body.channel, "9");
    assert.equal(body.playlistCount, 2);
    assert.equal(body.sync.status, "schedule_not_configured");
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("returns 404 for a channel absent from the live catalog", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response(JSON.stringify({ "1": playlist }), { status: 200 });
  try {
    const response = await worker.fetch(new Request("https://worker.test/api/channel/9"));
    assert.equal(response.status, 404);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("does not expose arbitrary URL proxying", async () => {
  const response = await worker.fetch(new Request(
    "https://worker.test/?proxy=https://example.org/secret"
  ));
  assert.equal(response.status, 404);
});

test("health and UTC clock endpoints respond", async () => {
  const health = await worker.fetch(new Request("https://worker.test/health"));
  assert.equal((await health.json()).ok, true);
  const clock = await worker.fetch(new Request("https://worker.test/api/clock"));
  assert.ok((await clock.json()).serverTimeUtc);
});
