package io.github.meko123456.habitstreaks.ui

import java.text.DecimalFormatSymbols
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ContributionCountTest {

    @Test
    fun `one contribution is a contribution`() {
        // The GitHub card, the GitHub dialog and the widget's spoken description all said
        // "1 contributions".
        assertEquals("1 contribution", contributionCount(1, Locale.US))
        assertEquals("0 contributions", contributionCount(0, Locale.US))
        assertEquals("2 contributions", contributionCount(2, Locale.US))
    }

    @Test
    fun `a year's total is grouped as the language groups digits`() {
        // It read "1873 contributions in the last year".
        assertEquals("1,873 contributions", contributionCount(1873, Locale.US))
        val space = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ka")).groupingSeparator
        assertEquals("1${space}873 contributions", contributionCount(1873, Locale.forLanguageTag("ka")))
    }
}
