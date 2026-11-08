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

internal enum class OfficialDestinationCategory(val title: String) {
    SPORTS("قنوات ومنصات الرياضة"),
    MOVIES("الأفلام والمسلسلات"),
    GENERAL("قنوات البث الرسمية")
}

internal data class OfficialLiveDestination(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val category: OfficialDestinationCategory = OfficialDestinationCategory.GENERAL
)

internal object RemotePlaylistSources {
    val worldLibraryBundleIds = listOf(
        "doubo-world",
        "doubo-arabic",
        "doubo-turkey",
        "iptv-org-world",
        "iptv-org-arab",
        "iptv-org-turkey",
        "iptv-org-europe",
        "iptv-org-western-europe",
        "iptv-org-east-asia",
        "iptv-org-north-america",
        "iptv-org-south-america",
        "iptv-org-latin-america",
        "iptv-org-us",
        "iptv-org-ca",
        "iptv-org-cn",
        "iptv-org-jp",
        "iptv-org-kr",
        "iptv-org-ru",
        "iptv-org-br",
        "iptv-org-ar",
        "iptv-org-cl",
        "iptv-org-co",
        "iptv-org-mx",
        "iptv-org-pe",
        "iptv-org-uk",
        "iptv-org-de",
        "iptv-org-fr",
        "iptv-org-it",
        "iptv-org-es",
        "iptv-org-pt",
        "iptv-org-pl",
        "iptv-org-nl",
        "iptv-org-se",
        "iptv-org-no"
    )

    val playlists = listOf(
        RemotePlaylistSource(
            id = "doubo-world",
            name = "Doubo — القنوات العالمية",
            description = "فهرس يومي لقنوات يعلن ناشره أنها متاحة مجاناً للعامة. يفحص الروابط إلى أول مقطع فيديو ويستخدم نطاقات المحطات الرسمية أو CDN/منصات مجانية معتمدة ضمن سياسته.",
            playlistUrl = "https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/index.m3u",
            provider = "Doubo Public Channel Directory",
            attribution = "يجب إسناد Doubo Public Channel Directory وفق CC BY 4.0. ترخيص الفهرس لا يمنح حقوق محتوى القنوات.",
            allowedHosts = setOf("cdn.jsdelivr.net", "fastly.jsdelivr.net", "gcore.jsdelivr.net", "testingcf.jsdelivr.net")
        ),
        RemotePlaylistSource(
            id = "doubo-turkey",
            name = "Doubo — تركيا",
            description = "قائمة القنوات المصنفة لتركيا ضمن الدليل اليومي نفسه.",
            playlistUrl = "https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/countries/tr.m3u",
            provider = "Doubo Public Channel Directory",
            attribution = "إسناد CC BY 4.0: Doubo Public Channel Directory.",
            allowedHosts = setOf("cdn.jsdelivr.net", "fastly.jsdelivr.net", "gcore.jsdelivr.net", "testingcf.jsdelivr.net")
        ),
        RemotePlaylistSource(
            id = "doubo-arabic",
            name = "Doubo — القنوات العربية",
            description = "قائمة حسب اللغة العربية (رمز ISO 639-3: ara) من الدليل نفسه.",
            playlistUrl = "https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/languages/ara.m3u",
            provider = "Doubo Public Channel Directory",
            attribution = "إسناد CC BY 4.0: Doubo Public Channel Directory.",
            allowedHosts = setOf("cdn.jsdelivr.net", "fastly.jsdelivr.net", "gcore.jsdelivr.net", "testingcf.jsdelivr.net")
        ),
        RemotePlaylistSource(
            id = "doubo-sports",
            name = "Doubo — الرياضة العالمية",
            description = "قائمة رياضية متخصصة يعلن الدليل أنها تضم نحو 100 قناة مجانية متاحة للعامة؛ أزلناها من العدّ المزدوج عند دمجها مع المصادر العالمية، وتظل كل قناة بحاجة إلى فحص مستقل.",
            playlistUrl = "https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/categories/sports.m3u",
            provider = "Doubo Public Channel Directory",
            attribution = "إسناد CC BY 4.0: Doubo Public Channel Directory. ترخيص اختيار الروابط لا يمنح حقوق بث البرامج نفسها.",
            allowedHosts = setOf("cdn.jsdelivr.net", "fastly.jsdelivr.net", "gcore.jsdelivr.net", "testingcf.jsdelivr.net")
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
            id = "iptv-org-sports",
            name = "IPTV-org — القنوات الرياضية",
            description = "قائمة مستقلة للقنوات المصنفة رياضية حول العالم. هي قائمة اكتشاف مجتمعية؛ الروابط قد تتوقف أو تُقيّد جغرافياً ولا تُعد معتمدة للحقوق تلقائياً.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/sports.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-movies",
            name = "IPTV-org — قنوات الأفلام",
            description = "قنوات تلفزيونية خطية تعرض أفلاماً وفق جدول بث؛ ليست مكتبة أفلام حسب الطلب.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/movies.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-series",
            name = "IPTV-org — قنوات المسلسلات",
            description = "قنوات تلفزيونية خطية تعرض المسلسلات والبرامج وفق جدول بث؛ وليست خدمة مشاهدة حسب الطلب.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/series.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-animation",
            name = "IPTV-org — الرسوم المتحركة",
            description = "قائمة قنوات الرسوم المتحركة العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/animation.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-auto",
            name = "IPTV-org — السيارات",
            description = "قائمة قنوات السيارات العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/auto.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-business",
            name = "IPTV-org — الأعمال",
            description = "قائمة قنوات الأعمال العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/business.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-classic",
            name = "IPTV-org — الكلاسيكيات",
            description = "قائمة قنوات الكلاسيكيات العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/classic.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-comedy",
            name = "IPTV-org — الكوميديا",
            description = "قائمة قنوات الكوميديا العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/comedy.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-cooking",
            name = "IPTV-org — الطبخ",
            description = "قائمة قنوات الطبخ العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/cooking.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-culture",
            name = "IPTV-org — الثقافة",
            description = "قائمة قنوات الثقافة العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/culture.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-documentary",
            name = "IPTV-org — الوثائقيات",
            description = "قائمة قنوات الوثائقيات العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/documentary.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-education",
            name = "IPTV-org — التعليم",
            description = "قائمة قنوات التعليم العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/education.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-entertainment",
            name = "IPTV-org — الترفيه",
            description = "قائمة قنوات الترفيه العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/entertainment.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-family",
            name = "IPTV-org — العائلة",
            description = "قائمة قنوات العائلة العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/family.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-general",
            name = "IPTV-org — المنوعات",
            description = "قائمة قنوات المنوعات العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/general.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-kids",
            name = "IPTV-org — الأطفال",
            description = "قائمة قنوات الأطفال العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/kids.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-legislative",
            name = "IPTV-org — البرلمان والسياسة",
            description = "قائمة قنوات البرلمان والسياسة العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/legislative.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-lifestyle",
            name = "IPTV-org — أسلوب الحياة",
            description = "قائمة قنوات أسلوب الحياة العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/lifestyle.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-music",
            name = "IPTV-org — الموسيقى",
            description = "قائمة قنوات الموسيقى العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/music.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-news",
            name = "IPTV-org — الأخبار",
            description = "قائمة قنوات الأخبار العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/news.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-outdoor",
            name = "IPTV-org — الأنشطة الخارجية",
            description = "قائمة قنوات الأنشطة الخارجية العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/outdoor.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-public",
            name = "IPTV-org — الخدمة العامة",
            description = "قائمة قنوات الخدمة العامة العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/public.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-relax",
            name = "IPTV-org — الاسترخاء",
            description = "قائمة قنوات الاسترخاء العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/relax.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-religious",
            name = "IPTV-org — الدينية",
            description = "قائمة قنوات الدينية العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/religious.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-science",
            name = "IPTV-org — العلوم",
            description = "قائمة قنوات العلوم العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/science.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-travel",
            name = "IPTV-org — السفر",
            description = "قائمة قنوات السفر العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/travel.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-weather",
            name = "IPTV-org — الطقس",
            description = "قائمة قنوات الطقس العالمية من فهرس IPTV-org. قد تتداخل مع الفهرس العالمي والتصنيفات الأخرى؛ تزال الروابط المتطابقة عند الدمج ولا تعني القائمة وحدها أن كل قناة تعمل أو أن حقوقها مؤكدة.",
            playlistUrl = "https://iptv-org.github.io/iptv/categories/weather.m3u",
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
            id = "romaxa-world-auto-checked",
            name = "World IPTV — فهرس عالمي بفحص دوري",
            description = "قائمة تجميعية عالمية يعلن ناشرها فحص روابطها وتحديثها كل عدة ساعات. قد تتضمن روابط مكررة أو مقيدة جغرافياً؛ تُزال التكرارات في مكتبة RS TV، ولا يُعد فحص الناشر إثباتاً لحقوق البث.",
            playlistUrl = "https://raw.githubusercontent.com/Romaxa55/world_ip_tv/master/output/index.m3u",
            provider = "Romaxa55/world_ip_tv",
            attribution = "مصدر تجميعي مجتمعي؛ تُحفظ نسبة المصدر، وتبقى حقوق كل قناة بحاجة إلى مراجعة مستقلة."
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
        ),
        RemotePlaylistSource(
            id = "iptv-org-europe",
            name = "IPTV-org — أوروبا",
            description = "قنوات الدول الأوروبية ضمن فهرس IPTV-org. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/regions/eur.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-western-europe",
            name = "IPTV-org — أوروبا الغربية",
            description = "قنوات أوروبا الغربية. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/regions/wer.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-east-asia",
            name = "IPTV-org — شرق آسيا",
            description = "قنوات شرق آسيا، مع تغطية الصين واليابان وكوريا بحسب بيانات الفهرس. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/regions/eas.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-north-america",
            name = "IPTV-org — أمريكا الشمالية",
            description = "قنوات أمريكا الشمالية، بما فيها الولايات المتحدة وكندا بحسب بيانات الفهرس. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/regions/noram.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-south-america",
            name = "IPTV-org — أمريكا الجنوبية",
            description = "قنوات دول أمريكا الجنوبية. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/regions/southam.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-latin-america",
            name = "IPTV-org — أمريكا اللاتينية",
            description = "قنوات أمريكا اللاتينية والكاريبي ضمن الفهرس. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/regions/latam.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-us",
            name = "IPTV-org — الولايات المتحدة الأمريكية",
            description = "قنوات مصنفة للولايات المتحدة. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/us.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-ca",
            name = "IPTV-org — كندا",
            description = "قنوات مصنفة لكندا. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/ca.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-cn",
            name = "IPTV-org — الصين",
            description = "قنوات مصنفة للصين. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/cn.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-jp",
            name = "IPTV-org — اليابان",
            description = "قنوات مصنفة لليابان. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/jp.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-kr",
            name = "IPTV-org — كوريا الجنوبية",
            description = "قنوات مصنفة لكوريا الجنوبية. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/kr.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-ru",
            name = "IPTV-org — روسيا",
            description = "قنوات مصنفة لروسيا. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/ru.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-br",
            name = "IPTV-org — البرازيل",
            description = "قنوات مصنفة للبرازيل. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/br.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-ar",
            name = "IPTV-org — الأرجنتين",
            description = "قنوات مصنفة للأرجنتين. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/ar.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-cl",
            name = "IPTV-org — تشيلي",
            description = "قنوات مصنفة لتشيلي. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/cl.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-co",
            name = "IPTV-org — كولومبيا",
            description = "قنوات مصنفة لكولومبيا. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/co.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-mx",
            name = "IPTV-org — المكسيك",
            description = "قنوات مصنفة للمكسيك. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/mx.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-pe",
            name = "IPTV-org — بيرو",
            description = "قنوات مصنفة لبيرو. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/pe.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-uk",
            name = "IPTV-org — المملكة المتحدة",
            description = "قنوات مصنفة للمملكة المتحدة. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/uk.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-de",
            name = "IPTV-org — ألمانيا",
            description = "قنوات مصنفة لألمانيا. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/de.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-fr",
            name = "IPTV-org — فرنسا",
            description = "قنوات مصنفة لفرنسا. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/fr.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-it",
            name = "IPTV-org — إيطاليا",
            description = "قنوات مصنفة لإيطاليا. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/it.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-es",
            name = "IPTV-org — إسبانيا",
            description = "قنوات مصنفة لإسبانيا. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/es.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-pt",
            name = "IPTV-org — البرتغال",
            description = "قنوات مصنفة للبرتغال. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/pt.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-pl",
            name = "IPTV-org — بولندا",
            description = "قنوات مصنفة لبولندا. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/pl.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-nl",
            name = "IPTV-org — هولندا",
            description = "قنوات مصنفة لهولندا. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/nl.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-se",
            name = "IPTV-org — السويد",
            description = "قنوات مصنفة للسويد. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/se.m3u",
            provider = "iptv-org/iptv"
        ),
        RemotePlaylistSource(
            id = "iptv-org-no",
            name = "IPTV-org — النرويج",
            description = "قنوات مصنفة للنرويج. القنوات تُنزّل كحزمة فقط وتظهر لاحقاً في صفحة البث المباشر؛ توفر الرابط لا يثبت الحقوق أو استمرار التشغيل.",
            playlistUrl = "https://iptv-org.github.io/iptv/countries/no.m3u",
            provider = "iptv-org/iptv"
        )
    )

    val officialDestinations = listOf(
        OfficialLiveDestination(
            id = "fifa-plus",
            name = "FIFA+ — كرة القدم العالمية",
            description = "مباريات مباشرة مختارة من اتحادات وطنية وأرشيف ومحتوى أصلي مجاني؛ جدول المباريات وحقوق المشاهدة يختلفان حسب الدولة والبطولة.",
            url = "https://www.plus.fifa.com/en",
            category = OfficialDestinationCategory.SPORTS
        ),
        OfficialLiveDestination(
            id = "eurovision-sport",
            name = "Eurovision Sport — رياضات العالم",
            description = "منصة مجانية للأحداث الرياضية المباشرة والإعادات والملخصات، مثل ألعاب القوى والسباحة والدراجات والرياضات القتالية؛ بعض الأحداث محدودة إقليمياً.",
            url = "https://eurovisionsport.com/en",
            category = OfficialDestinationCategory.SPORTS
        ),
        OfficialLiveDestination(
            id = "red-bull-tv",
            name = "Red Bull TV — رياضات وأفلام",
            description = "بث مجاني لأحداث مختارة في رياضة المحركات والدراجات والتزلج وركوب الأمواج، إضافة إلى أفلام ووثائقيات رياضية؛ التغطية تتغير حسب الحدث والمنطقة.",
            url = "https://www.redbull.com/int-en/tv",
            category = OfficialDestinationCategory.SPORTS
        ),
        OfficialLiveDestination(
            id = "trt-spor",
            name = "TRT Spor — تركيا",
            description = "صفحة البث الرسمية للقناة التركية؛ بعض المباريات والمقاطع قد تكون مقيدة بحقوق المنطقة أو لا تعمل خارج نطاق الترخيص.",
            url = "https://www.trtspor.com.tr/canli-yayin-izle/trt-spor",
            category = OfficialDestinationCategory.SPORTS
        ),
        OfficialLiveDestination(
            id = "fubo-sports-network",
            name = "Fubo Sports Network",
            description = "شبكة رياضية مجانية مدعومة بالإعلانات عبر منصات توزيع مختارة، مع تغطية إقليمية تختلف حسب البلد والمنصة؛ ليست باقة جميع الدوريات المدفوعة.",
            url = "https://www.fubosportsnetwork.com/",
            category = OfficialDestinationCategory.SPORTS
        ),
        OfficialLiveDestination(
            id = "rakuten-tv-free",
            name = "Rakuten TV — Free وLive TV",
            description = "أفلام ومسلسلات وقنوات مباشرة مجانية مدعومة بالإعلانات، وتشمل بعض القنوات الرياضية؛ الكتالوج وطريقة الدخول يختلفان حسب البلد والجهاز.",
            url = "https://www.rakuten.tv/",
            category = OfficialDestinationCategory.MOVIES
        ),
        OfficialLiveDestination(
            id = "pluto-tv",
            name = "Pluto TV — أفلام وقنوات مجانية",
            description = "مئات القنوات وعناوين أفلام مجانية مدعومة بالإعلانات؛ عدد القنوات والمحتوى والتوفر الجغرافي يتغير حسب البلد.",
            url = "https://pluto.tv/welcome/on-demand",
            category = OfficialDestinationCategory.MOVIES
        ),
        OfficialLiveDestination(
            id = "tubi",
            name = "Tubi — أفلام وقنوات مجانية",
            description = "مكتبة أفلام وقنوات مجانية قانونية في الدول المدعومة مثل الولايات المتحدة وكندا والمكسيك والمملكة المتحدة وأستراليا؛ غير مدرج ضمن التغطية الرسمية لتركيا.",
            url = "https://tubitv.com/",
            category = OfficialDestinationCategory.MOVIES
        ),
        OfficialLiveDestination(
            id = "internet-archive-movies",
            name = "Internet Archive — أفلام كلاسيكية",
            description = "أرشيف أفلام ومواد مرئية؛ يجب التأكد من حالة الحقوق لكل عنوان ونسخة على حدة قبل اعتباره متاحاً للمشاهدة أو إعادة الاستخدام.",
            url = "https://archive.org/details/movies",
            category = OfficialDestinationCategory.MOVIES
        ),
        OfficialLiveDestination(
            id = "plex-live-tv",
            name = "Plex — أفلام وقنوات مباشرة",
            description = "خدمة مجانية مدعومة بالإعلانات للأفلام والقنوات، لكن Plex يذكر أن Live TV غير متاح حالياً في تركيا؛ أدرجناه كخيار للدول المدعومة فقط.",
            url = "https://www.plex.tv/watch-free/",
            category = OfficialDestinationCategory.MOVIES
        ),
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
