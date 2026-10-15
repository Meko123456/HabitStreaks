package io.github.meko123456.habitstreaks.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ContributionCountTest {

    @Test
    fun `one contribution is a contribution`() {
        // The GitHub card, the GitHub dialog and the widget's spoken description all said
        // "1 contributions".
        assertEquals("1 contribution", contributionCount(1))
        assertEquals("0 contributions", contributionCount(0))
        assertEquals("2 contributions", contributionCount(2))
        assertEquals("1873 contributions", contributionCount(1873))
    }
}
