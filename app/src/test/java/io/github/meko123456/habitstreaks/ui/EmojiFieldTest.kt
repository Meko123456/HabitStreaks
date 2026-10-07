package io.github.meko123456.habitstreaks.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** What the habit editor's emoji field keeps of what was typed into it. */
class EmojiFieldTest {

    // Written as escapes so the joiners and variation selectors are visible.
    private val technologist = "🧑‍💻" // 🧑‍💻
    private val family = "👨‍👩‍👧‍👦" // 👨‍👩‍👧‍👦
    private val womanRunning = "🏃🏽‍♀️" // 🏃🏽‍♀️
    private val georgia = "🇬🇪" // 🇬🇪

    @Test
    fun `an emoji made of several code points is kept whole`() {
        assertEquals("five chars, which the field used to cut after four", 5, technologist.length)
        assertEquals(technologist, lastGrapheme(technologist))
        assertEquals(family, lastGrapheme(family))
        assertEquals(womanRunning, lastGrapheme(womanRunning))
        assertEquals(georgia, lastGrapheme(georgia))
    }

    @Test
    fun `a newly picked emoji replaces the one already there`() {
        assertEquals("📚", lastGrapheme("🔥📚"))
        assertEquals(technologist, lastGrapheme("🔥$technologist"))
    }

    @Test
    fun `spaces around it are dropped, and nothing stays nothing`() {
        assertEquals("🔥", lastGrapheme("🔥 "))
        assertEquals("", lastGrapheme(""))
        assertEquals("", lastGrapheme("   "))
    }
}
