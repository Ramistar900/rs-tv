package com.sinjartv.rstv

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets

internal data class RemotePlaylistSource(
    val id: String,
    val name: String,
    val description: String,
    val playlistUrl: String,
    val provider: String,
    val attribution: String? = null,
    val allowedHosts: Set<String> = setOf(URI(playlistUrl).host.lowercase())
)

internal data class OfficialLiveDestination(
    val id: String,
    val name: String,
    val description: String,
    val url: String
)

internal object RemotePlaylistSources {
    val playlists = listOf(
        RemotePlaylistSource(
            id = "doubo-world",
            name = "Doubo — القنوات العالمية",
            description = "فهرس يومي لقنوات يعلن ناشره أنها متاحة مجاناً للعامة. يفحص الروابط إلى أول مقطع فيديو ويستخدم نطاقات المحطات الرسمية أو CDN/منصات مجانية معتمدة ضمن سياسته.",
            playlistUrl = "https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/index.m3u",
            provider = "Doubo Public Channel Directory",
            attribution = "يجب إسناد Doubo Public Channel Directory وفق CC BY 4.0. ترخيص الفهرس لا يمنح حقوق محتوى القنوات."
        ),
        RemotePlaylistSource(
            id = "doubo-turkey",
            name = "Doubo — تركيا",
            description = "قائمة القنوات المصنفة لتركيا ضمن الدليل اليومي نفسه.",
            playlistUrl = "https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/countries/tr.m3u",
            provider = "Doubo Public Channel Directory",
            attribution = "إسناد CC BY 4.0: Doubo Public Channel Directory."
        ),
        RemotePlaylistSource(
            id = "doubo-arabic",
            name = "Doubo — القنوات العربية",
            description = "قائمة حسب اللغة العربية (رمز ISO 639-3: ara) من الدليل نفسه.",
            playlistUrl = "https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/languages/ara.m3u",
            provider = "Doubo Public Channel Directory",
            attribution = "إسناد CC BY 4.0: Doubo Public Channel Directory."
        ),
        RemotePlaylistSource(
            id = "iptv-org-world",
            name = "IPTV-org — العالم",
            description = "فهرس مجتمعي واسع لقنوات الإنترنت، مقسّم بحسب الدول واللغات والتصنيفات. الروابط مرشحة وليست كلها مضمونة أو مرخصة.",
            playlistUrl = "https://iptv-org.github.io/iptv/index.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-arab",
            name = "IPTV-org — المنطقة العربية",
            description = "قنوات مصنفة ضمن المنطقة العربية في المشروع المجتمعي.",
            playlistUrl = "https://iptv-org.github.io/iptv/regions/arab.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-turkey",
            name = "IPTV-org — تركيا",
            description = "قنوات مصنفة لتركيا في فهرس IPTV-org.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/tr.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "free-tv",
            name = "Free-TV — قائمة منتقاة",
            description = "قائمة مجتمعية تفضّل الجودة وقلة الروابط، وتقول إنها تقتصر على القنوات المجانية رسمياً. قد تكون بعض القنوات مقيدة جغرافياً.",
            playlistUrl = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8",
            provider = "Free-TV/IPTV"
        ),
        RemotePlaylistSource(
            id = "freecasthub",
            name = "FreeCastHub — قنوات عامة",
            description = "قائمة مجتمعية إضافية لفئات الأخبار والرياضة والتعليم والطقس. مصدر ثانوي؛ يلزم فحص كل قناة وحقوقها.",
            playlistUrl = "https://raw.githubusercontent.com/freecasthub/public-iptv/main/playlist.m3u",
            provider = "freecasthub/public-iptv"
        ),
        RemotePlaylistSource(
            id = "m3upt-portuguese",
            name = "M3UPT — البرتغال والقنوات البرتغالية",
            description = "قائمة برتغالية تتضمن قنوات تلفزيونية وإذاعية وروابط بث دولية. بعض الإدخالات قد تكون غير HTTPS أو مقيدة جغرافياً أو مشفرة؛ يستورد التطبيق فقط الروابط التي يقبلها محلل الأمان.",
            playlistUrl = "https://raw.githubusercontent.com/LITUATUI/M3UPT/main/M3U/M3UPT.m3u",
            provider = "LITUATUI/M3UPT",
            attribution = "قائمة مجتمعية لقنوات يصفها ناشرها بأنها رسمية ومجانية؛ افحص حقوق كل قناة وشروطها قبل اعتمادها."
        ),
        RemotePlaylistSource(
            id = "iptv-org-country-index",
            name = "IPTV-org — فهرس القنوات حسب الدولة",
            description = "القائمة العالمية نفسها مرتبة في مجموعات الدول لتسهيل التصفح؛ ليست إضافة مستقلة إلى عدد القنوات بعد إزالة التكرار.",
            playlistUrl = "https://iptv-org.github.io/iptv/index.country.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-language-index",
            name = "IPTV-org — فهرس القنوات حسب اللغة",
            description = "القائمة العالمية نفسها مرتبة حسب اللغة؛ تُدمج القنوات المتطابقة مع المصادر الأخرى.",
            playlistUrl = "https://iptv-org.github.io/iptv/index.language.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-category-index",
            name = "IPTV-org — فهرس القنوات حسب التصنيف",
            description = "القائمة العالمية نفسها مرتبة حسب التصنيف مثل الأخبار والرياضة والأطفال والموسيقى؛ لا تُحسب التكرارات كقنوات جديدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/index.category.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-region-index",
            name = "IPTV-org — فهرس القنوات حسب المنطقة",
            description = "القائمة العالمية نفسها مرتبة حسب المناطق الجغرافية؛ هذا تنظيم إضافي وليس مصدراً مستقلاً لزيادة العدد الفريد.",
            playlistUrl = "https://iptv-org.github.io/iptv/index.region.m3u",
            provider = "iptv-org/iptv"
        )
    )

    val officialDestinations = listOf(
        OfficialLiveDestination(
            id = "aljazeera-ar",
            name = "الجزيرة — البث الحي",
            description = "صفحة البث الرسمية، وقد تعرض أكثر من قناة حسب التوفر.",
            url = "https://www.aljazeera.net/video/live"
        ),
        OfficialLiveDestination(
            id = "aljazeera-en",
            name = "Al Jazeera English",
            description = "مشاهدة البث من صفحة القناة الرسمية.",
            url = "https://www.aljazeera.com/video/live/"
        ),
        OfficialLiveDestination(
            id = "cgtn",
            name = "CGTN — قنوات متعددة",
            description = "صفحة CGTN الرسمية لقنواتها الإنجليزية والعربية والإسبانية والفرنسية والوثائقية.",
            url = "https://www.cgtn.com/tv"
        ),
        OfficialLiveDestination(
            id = "euronews",
            name = "Euronews Live",
            description = "البث التلفزيوني المباشر عبر الموقع الرسمي؛ قد تختلف الإتاحة حسب المنطقة.",
            url = "https://www.euronews.com/live"
        ),
        OfficialLiveDestination(
            id = "nasa",
            name = "NASA+",
            description = "خدمة ناسا الرسمية المجانية لمحتوى مباشر وعند الطلب؛ يتطلب التشغيل استخدام خدمة ناسا الرسمية.",
            url = "https://plus.nasa.gov/"
        )
    )
}

internal object RemotePlaylistDownloader {
    private const val MAX_BYTES = 12 * 1024 * 1024
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val MAX_REDIRECTS = 4

    fun download(source: RemotePlaylistSource): String {
        val initial = requireAllowedUri(source, URI(source.playlistUrl))
        var current = initial.toURL()
        var redirects = 0

        while (true) {
            val connection = (current.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "RS-TV-Android/1.0")
                setRequestProperty("Accept", "audio/x-mpegurl,application/vnd.apple.mpegurl,text/plain,*/*")
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    require(redirects < MAX_REDIRECTS) { "تجاوز المصدر الحد المسموح لإعادة التوجيه" }
                    val location = connection.getHeaderField("Location")
                        ?: throw IllegalArgumentException("أعاد المصدر تحويلاً بلا عنوان")
                    val next = current.toURI().resolve(location)
                    requireAllowedUri(source, next)
                    current = next.toURL()
                    redirects++
                    continue
                }

                require(responseCode in 200..299) { "تعذر جلب القائمة (HTTP $responseCode)" }
                val declaredLength = connection.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
                require(declaredLength <= MAX_BYTES) { "حجم القائمة أكبر من الحد المسموح" }

                val bytes = ByteArrayOutputStream()
                connection.inputStream.use { input ->
                    val buffer = ByteArray(8_192)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_BYTES) { "حجم القائمة أكبر من الحد المسموح" }
                        bytes.write(buffer, 0, count)
                    }
                }

                val content = String(bytes.toByteArray(), StandardCharsets.UTF_8).removePrefix("\uFEFF")
                require(content.lineSequence().firstOrNull()?.trim()?.startsWith("#EXTM3U", ignoreCase = true) == true) {
                    "المصدر لم يُرجع قائمة M3U صالحة"
                }
                return content
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun requireAllowedUri(source: RemotePlaylistSource, uri: URI): URI {
        require(uri.scheme.equals("https", ignoreCase = true)) { "يُسمح باتصالات HTTPS فقط" }
        require(!uri.host.isNullOrBlank() && uri.userInfo == null) { "عنوان المصدر غير صالح" }
        require(uri.host.lowercase() in source.allowedHosts) { "رفض تحويل المصدر إلى نطاق غير معتمد" }
        return uri
    }
}
