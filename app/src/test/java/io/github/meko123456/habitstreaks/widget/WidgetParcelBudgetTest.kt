package io.github.meko123456.habitstreaks.widget

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * A tripwire on the one assumption `MAX_BITMAP_BYTES` rests on that no other test can see.
 *
 * `WidgetHeatmapTest` proves the heatmap bitmap stays under 800 KB at every width. What it cannot
 * prove is that the heatmap is the *only* image in the update, and that is the half the platform
 * actually measures: an app targeting SDK 37 may not hand a widget host a RemoteViews whose
 * bitmaps **and icons together** exceed `1.5 × displayWidth × displayHeight × 4`, and overshooting
 * is a fatal IllegalArgumentException rather than a dropped frame.
 *
 * Today the whole widget package contains exactly one `ImageProvider` call and no `Icon` or
 * `Bitmap` at all, so one heatmap per composed size is the entire image weight. That is written
 * down in `WidgetHeatmap.MAX_BITMAP_BYTES`'s KDoc as a fact about the code — and until now nothing
 * made it stay true. Adding a second image is a one-line change in a file whose author has no
 * reason to read a comment about parcel budgets.
 *
 * So this reads the sources. It is a blunt instrument and deliberately so: it does not try to
 * understand the code, it fails when the shape of the assumption changes and makes someone
 * re-derive the budget. If the second image is genuinely wanted, update the KDoc's arithmetic and
 * the expected count here together.
 */
class WidgetParcelBudgetTest {

    /**
     * The file's code, with comments dropped.
     *
     * Necessary rather than tidy: the first run of this test flagged `MAX_BITMAP_BYTES`'s own
     * KDoc, which names all three types in a sentence explaining that none of them is used.
     */
    private fun File.codeLines(): List<IndexedValue<String>> =
        readLines().withIndex().mapNotNull { (i, raw) ->
            val trimmed = raw.trimStart()
            if (trimmed.startsWith("*") || trimmed.startsWith("/*") || trimmed.startsWith("//")) {
                null
            } else {
                IndexedValue(i, raw.substringBefore("//"))
            }
        }

    private val widgetSources: List<File> by lazy {
        val dir = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "app/src/main/java/io/github/meko123456/habitstreaks/widget") }
            .firstOrNull { it.isDirectory }
        if (dir == null) {
            fail("Could not locate the widget sources from ${File("").absolutePath}")
        }
        dir!!.listFiles { f -> f.extension == "kt" }!!.sortedBy { it.name }
    }

    /** Guards the guard: if this stops finding files, every assertion below passes vacuously. */
    @Test
    fun `the sources this test reads are actually there`() {
        val names = widgetSources.map { it.name }
        assertTrue("found $names", names.containsAll(listOf("GithubWidget.kt", "HabitsWidget.kt")))
        assertTrue(widgetSources.all { it.readText().isNotBlank() })
    }

    @Test
    fun `the widget update still carries exactly one image`() {
        val calls = widgetSources.flatMap { file ->
            file.codeLines()
                .filter { (_, line) -> "ImageProvider(" in line }
                .map { (i, _) -> "${file.name}:${i + 1}" }
        }
        assertEquals(
            "Image count changed. Re-derive the parcel budget in WidgetHeatmap.MAX_BITMAP_BYTES's " +
                "KDoc before updating this number — at targetSdk 37 the overflow is a crash. Found: $calls",
            listOf("GithubWidget.kt:136"),
            calls,
        )
    }

    @Test
    fun `nothing in the widget package reaches for an Icon or a raw Bitmap`() {
        // Icon is the type SDK 37 newly counts toward the same budget; a raw Bitmap is how one
        // arrives without going through ImageProvider. HeatmapBitmap is the library call that
        // produces the one image above, and is named here rather than matched loosely.
        val offenders = widgetSources.flatMap { file ->
            file.codeLines().mapNotNull { (i, line) ->
                val hit = Regex("""\b(Icon|Bitmap)\b""").find(line)?.value
                hit?.takeUnless { "HeatmapBitmap" in line }?.let { "${file.name}:${i + 1}: ${line.trim()}" }
            }
        }
        assertEquals("Unbudgeted image type in the widget parcel: $offenders", emptyList<String>(), offenders)
    }
}
