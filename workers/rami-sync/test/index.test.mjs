import test from "node:test";
import assert from "node:assert/strict";
import { calculatePlaybackState } from "../src/index.mjs";

const channel = {
  id: "rami-tv-1",
  startAtUtc: "2026-11-28T00:00:00.000Z",
  loop: true,
  videos: [
    { id: "one", title: "Video one", url: "https://example.org/one.mp4", durationMs: 10_000 },
    { id: "two", title: "Video two", url: "https://example.org/two.mp4", durationMs: 20_000 }
  ]
};

test("selects the first video at the exact channel start", () => {
  const state = calculatePlaybackState(channel, Date.parse(channel.startAtUtc));
  assert.equal(state.status, "playing");
  assert.equal(state.video.id, "one");
  assert.equal(state.videoPositionMs, 0);
});

test("selects the correct offset in the second video", () => {
  const state = calculatePlaybackState(channel, Date.parse(channel.startAtUtc) + 15_000);
  assert.equal(state.video.id, "two");
  assert.equal(state.videoIndex, 1);
  assert.equal(state.videoPositionMs, 5_000);
});

test("loops the complete playlist on one shared timeline", () => {
  const state = calculatePlaybackState(channel, Date.parse(channel.startAtUtc) + 35_000);
  assert.equal(state.cycle, 1);
  assert.equal(state.video.id, "one");
  assert.equal(state.videoPositionMs, 5_000);
});

test("reports a future start without advancing the playlist", () => {
  const state = calculatePlaybackState(channel, Date.parse(channel.startAtUtc) - 5_000);
  assert.equal(state.status, "not_started");
  assert.equal(state.videoPositionMs, 0);
});

test("reports ended for a non-looping channel", () => {
  const state = calculatePlaybackState(
    { ...channel, loop: false },
    Date.parse(channel.startAtUtc) + 30_000
  );
  assert.equal(state.status, "ended");
});

test("rejects invalid or non-HTTPS video URLs", () => {
  assert.throws(() => calculatePlaybackState({
    ...channel,
    videos: [{ id: "bad", url: "http://example.org/a.mp4", durationMs: 1000 }]
  }), /HTTPS/);
});

test("rejects missing durations", () => {
  assert.throws(() => calculatePlaybackState({
    ...channel,
    videos: [{ id: "bad", url: "https://example.org/a.mp4" }]
  }), /durationMs/);
});
