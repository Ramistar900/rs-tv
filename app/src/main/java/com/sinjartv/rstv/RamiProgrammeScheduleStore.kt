package com.sinjartv.rstv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Weekly schedule entry for one local RAMİ channel.
 * The selected video's real duration defines the end; there is deliberately no end-time field.
 */
data class RamiProgrammeSchedule(
    val id: String = UUID.randomUUID().toString(),
    val channelId: Int,
    val videoUri: String,
    val startMinute: Int,
    val weekdays: Set<Int>,
    val durationMs: Long,
    val enabled: Boolean = true
) {
    init {
        require(channelId > 0)
        require(videoUri.startsWith("content://") || videoUri.startsWith("file://"))
        require(startMinute in 0 until 24 * 60)
        require(weekdays.isNotEmpty() && weekdays.all { it in 1..7 })
        require(durationMs > 0L && durationMs < WEEK_MS)
    }

    companion object {
        const val WEEK_MINUTES = 7 * 24 * 60
        const val WEEK_MS = WEEK_MINUTES * 60_000L
    }
}

object RamiProgrammeScheduleStore {
    private const val PREFS = "rami_programme_schedules"
    private const val KEY = "entries"

    fun load(context: Context, channelId: Int): List<RamiProgrammeSchedule> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                runCatching {
                    val item = array.getJSONObject(index)
                    val days = item.getJSONArray("weekdays")
                    RamiProgrammeSchedule(
                        id = item.getString("id"),
                        channelId = item.getInt("channelId"),
                        videoUri = item.getString("videoUri"),
                        startMinute = item.getInt("startMinute"),
                        weekdays = (0 until days.length()).map { days.getInt(it) }.toSet(),
                        durationMs = item.getLong("durationMs"),
                        enabled = item.optBoolean("enabled", true)
                    )
                }.getOrNull()
            }.filter { it.channelId == channelId }.sortedBy { it.startMinute }
        }.getOrDefault(emptyList())
    }

    fun loadAll(context: Context): List<RamiProgrammeSchedule> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                runCatching {
                    val item = array.getJSONObject(index)
                    val days = item.getJSONArray("weekdays")
                    RamiProgrammeSchedule(
                        id = item.getString("id"),
                        channelId = item.getInt("channelId"),
                        videoUri = item.getString("videoUri"),
                        startMinute = item.getInt("startMinute"),
                        weekdays = (0 until days.length()).map { days.getInt(it) }.toSet(),
                        durationMs = item.getLong("durationMs"),
                        enabled = item.optBoolean("enabled", true)
                    )
                }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, entries: List<RamiProgrammeSchedule>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().apply {
                put("id", entry.id)
                put("channelId", entry.channelId)
                put("videoUri", entry.videoUri)
                put("startMinute", entry.startMinute)
                put("weekdays", JSONArray(entry.weekdays.sorted()))
                put("durationMs", entry.durationMs)
                put("enabled", entry.enabled)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, array.toString()).apply()
    }

    fun hasConflict(
        candidate: RamiProgrammeSchedule,
        existing: List<RamiProgrammeSchedule>,
        excludingId: String? = null
    ): Boolean {
        val candidateSegments = weeklySegments(candidate)
        return existing.asSequence()
            .filter { it.channelId == candidate.channelId && it.enabled && it.id != excludingId }
            .any { other ->
                weeklySegments(other).any { a ->
                    candidateSegments.any { b -> a.first < b.second && b.first < a.second }
                }
            }
    }

    private fun weeklySegments(entry: RamiProgrammeSchedule): List<Pair<Long, Long>> {
        val start = entry.startMinute.toLong()
        val duration = (entry.durationMs + 59_999L) / 60_000L
        return entry.weekdays.flatMap { day ->
            val absoluteStart = (day - 1L) * 24L * 60L + start
            val absoluteEnd = absoluteStart + duration
            if (absoluteEnd <= RamiProgrammeSchedule.WEEK_MINUTES) {
                listOf(absoluteStart to absoluteEnd)
            } else {
                listOf(
                    absoluteStart to RamiProgrammeSchedule.WEEK_MINUTES.toLong(),
                    0L to (absoluteEnd - RamiProgrammeSchedule.WEEK_MINUTES)
                )
            }
        }
    }
}
