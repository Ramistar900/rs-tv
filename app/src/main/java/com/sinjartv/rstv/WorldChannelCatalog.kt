package com.sinjartv.rstv

import org.json.JSONObject
import java.net.URI

internal data class WorldChannelCandidate(
    val id: String,
    val name: String,
    val alternativeNames: List<String>,
    val countryCode: String,
    val countryName: String,
    val countryFlag: String,
    val languageLabels: List<String>,
    val languageSearchValues: List<String>,
    val categoryIds: List<String>,
    val categoryLabels: List<String>,
    val websiteUrl: String?,
    val candidateStreamCount: Int
)

internal object WorldChannelCatalogParser {
    private const val MAX_CATALOG_CHARS = 64 * 1024 * 1024
    private const val MAX_RECORDS = 50_000

    fun parseCountryCodes(json: String): List<String> {
        require(json.length <= MAX_CATALOG_CHARS) { "ملف الكتالوج أكبر من الحد المسموح" }
        val root = JSONObject(json)
        val records = root.optJSONArray("records")
            ?: throw IllegalArgumentException("ملف الكتالوج لا يحتوي records")
        require(records.length() <= MAX_RECORDS) { "عدد سجلات القنوات يتجاوز الحد المسموح" }

        val countryCodes = linkedSetOf<String>()
        for (index in 0 until records.length()) {
            val record = records.optJSONObject(index) ?: continue
            if (record.optString("status") != "candidate") continue
            val countryCode = record.optJSONObject("country")
                ?.optString("code")
                ?.trim()
                ?.uppercase()
                .orEmpty()
            if (countryCode.matches(Regex("[A-Z]{2}")) && countryCode != "ZZ") {
                countryCodes += countryCode
            }
        }
        return countryCodes.sorted()
    }

    fun parse(json: String): List<WorldChannelCandidate> {
        require(json.length <= MAX_CATALOG_CHARS) { "ملف الكتالوج أكبر من الحد المسموح" }
        val root = JSONObject(json)
        val records = root.optJSONArray("records")
            ?: throw IllegalArgumentException("ملف الكتالوج لا يحتوي records")
        require(records.length() <= MAX_RECORDS) { "عدد القنوات في الكتالوج يتجاوز الحد المسموح" }

        val result = ArrayList<WorldChannelCandidate>(records.length())
        val seenIds = HashSet<String>()
        for (index in 0 until records.length()) {
            val record = records.optJSONObject(index) ?: continue
            if (record.optString("status") != "candidate") continue
            if (record.optString("rights_review_status") != "not_reviewed") continue

            val id = record.optString("id").trim()
            val name = record.optString("name").trim()
            if (id.isBlank() || name.isBlank() || !seenIds.add(id)) continue

            val country = record.optJSONObject("country")
            val countryCode = country?.optString("code").orEmpty().ifBlank { "ZZ" }
            val countryName = country?.optString("name").orEmpty().ifBlank { "غير محدد" }
            val countryFlag = country?.optString("flag").orEmpty().ifBlank { "🌐" }

            val languages = record.optJSONArray("languages")
            val languageLabels = ArrayList<String>()
            val languageSearchValues = ArrayList<String>()
            if (languages != null) {
                for (languageIndex in 0 until languages.length()) {
                    val language = languages.optJSONObject(languageIndex) ?: continue
                    val code = language.optString("code").trim()
                    val label = language.optString("name").trim().ifBlank { code }
                    if (code.isNotBlank()) languageSearchValues += code
                    if (label.isNotBlank()) languageLabels += label
                }
            }

            val categoryIds = record.optJSONArray("categories").toStringList()
            val categoryLabels = record.optJSONArray("category_names")
                .toStringList()
                .ifEmpty { categoryIds }

            val alternatives = record.optJSONArray("alternative_names").toStringList()
            val streams = record.optJSONArray("streams")
            result += WorldChannelCandidate(
                id = id,
                name = name,
                alternativeNames = alternatives,
                countryCode = countryCode,
                countryName = countryName,
                countryFlag = countryFlag,
                languageLabels = languageLabels.distinct(),
                languageSearchValues = languageSearchValues.distinct(),
                categoryIds = categoryIds,
                categoryLabels = categoryLabels,
                websiteUrl = record.optString("website").takeIf(::isSafeHttpsUrl),
                candidateStreamCount = streams?.length() ?: 0
            )
        }
        return result
    }

    private fun org.json.JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val value = optString(index).trim()
                if (value.isNotBlank() && value != "null") add(value)
            }
        }.distinct()
    }

    private fun isSafeHttpsUrl(value: String): Boolean {
        if (value.isBlank()) return false
        return runCatching {
            val uri = URI(value)
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null
        }.getOrDefault(false)
    }
}
