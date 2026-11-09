package com.sinjartv.rstv

import org.junit.Assert.assertEquals
import org.junit.Test

class WorldChannelCatalogTest {
    @Test
    fun extractsUniqueCountryCodesWithoutBuildingChannelObjects() {
        val json = """
            {
              "records": [
                {"status":"candidate","country":{"code":"SY"}},
                {"status":"candidate","country":{"code":"LB"}},
                {"status":"candidate","country":{"code":"SY"}},
                {"status":"candidate","country":{"code":"ZZ"}},
                {"status":"not_candidate","country":{"code":"IQ"}},
                {"status":"candidate","country":{"code":"IQ"}}
              ]
            }
        """.trimIndent()

        assertEquals(
            listOf("IQ", "LB", "SY"),
            WorldChannelCatalogParser.parseCountryCodes(json)
        )
    }
}
