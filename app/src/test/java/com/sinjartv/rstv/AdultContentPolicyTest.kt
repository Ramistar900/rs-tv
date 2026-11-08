package com.sinjartv.rstv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdultContentPolicyTest {

    @Test
    fun classifiesExplicitChannelsSeparately() {
        val channel = channel(name = "Late Night", group = "XXX")

        assertEquals(AdultContentType.EXPLICIT, AdultContentPolicy.classify(channel))
        assertTrue(AdultContentPolicy.isAdult(channel))
    }

    @Test
    fun classifiesExplicitArabicGroup() {
        val channel = channel(name = "قناة خاصة", group = "قنوات للكبار فقط")

        assertEquals(AdultContentType.EXPLICIT, AdultContentPolicy.classify(channel))
    }

    @Test
    fun recognizesExplicitAgeRatings() {
        val tvMa = channel(name = "Drama", rating = "TV-MA")
        val nc17 = channel(name = "Film", rating = "NC-17")
        val eighteenPlus = channel(name = "Late", group = "18+")

        assertEquals(AdultContentType.AGE_RATED, AdultContentPolicy.classify(tvMa))
        assertEquals(AdultContentType.AGE_RATED, AdultContentPolicy.classify(nc17))
        assertEquals(AdultContentType.AGE_RATED, AdultContentPolicy.classify(eighteenPlus))
    }

    @Test
    fun doesNotTreatOrdinaryMovieOrSeriesGroupsAsAdult() {
        val movies = channel(name = "Cinema", group = "Movies")
        val series = channel(name = "Drama", group = "Series")
        val tv14 = channel(name = "Family", rating = "TV-14")

        assertFalse(AdultContentPolicy.isAdult(movies))
        assertFalse(AdultContentPolicy.isAdult(series))
        assertFalse(AdultContentPolicy.isAdult(tv14))
    }

    private fun channel(
        name: String,
        group: String? = null,
        rating: String? = null
    ) = RSTVChannel(
        name = name,
        tvgId = null,
        group = group,
        logoUrl = null,
        contentRating = rating,
        streamUrl = "https://example.test/live.m3u8"
    )
}
