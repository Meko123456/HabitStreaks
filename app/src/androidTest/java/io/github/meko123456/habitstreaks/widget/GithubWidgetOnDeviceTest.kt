package io.github.meko123456.habitstreaks.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.util.Size
import android.util.SizeF
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import io.github.meko123456.habitstreaks.debug.DemoGithubWidget
import io.github.meko123456.habitstreaks.debug.DemoGithubWidgetReceiver
import java.io.FileInputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The GitHub widget pushed through the real widget service, at the widest it will ever be asked for.
 *
 * `WidgetHeatmapTest` and `WidgetParcelBudgetTest` settle on paper that an update fits the cap an
 * app targeting SDK 37 is held to: 1.5 × the display's pixels × 4 bytes of bitmaps and icons, and an
 * `IllegalArgumentException` for anything over. This asks the platform instead. It hosts the debug
 * build's demo widget — the shipping [GithubWidget], fed a made-up year — binds it at the width that
 * makes the largest heatmap `specFor` can draw, in two sizes as a launcher would, and inspects what
 * arrives in the host.
 *
 * CI runs it on an Android 17 emulator with the smallest panel the widget is likely to meet, which is
 * where the cap leaves the least room. That emulator is also the only Android 17 this project has:
 * the M1 it is developed on cannot accelerate an API 37 image, and without acceleration it never
 * boots.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31) // OPTION_APPWIDGET_SIZES, which is how the sizes are handed over
class GithubWidgetOnDeviceTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = AppWidgetManager.getInstance(context)
    private lateinit var host: RecordingHost
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    /** The heatmap at its largest: found by sweeping widths the way `WidgetHeatmapTest` does. */
    private val worst = generateSequence(0f) { it + 0.5f }
        .takeWhile { it <= 4000f }
        .map { contentWidth -> contentWidth to WidgetHeatmap.specFor(contentWidth) }
        .maxBy { (_, spec) -> spec.widthPx.toLong() * spec.heightPx }
        .let { (contentWidth, spec) -> Worst(contentWidth + GithubWidget.PADDING * 2, spec) }

    @Before
    fun bindTheDemoWidget() {
        // What a launcher is granted by the user; a test is granted it by the shell. The user is
        // given as a number: on the Android 16 image `--user current` fails with the usage text.
        val granted = shell("appwidget grantbind --package ${context.packageName} --user ${Process.myUid() / PER_USER_RANGE}")
        // Assigned before anything that can throw, so a setup that fails is reported as itself and
        // not as the teardown tripping over a host that was never made — which is all the first
        // Android 17 run that got this far could say.
        host = RecordingHost(context)
        startListeningOnceUnlocked()
        appWidgetId = host.allocateAppWidgetId()
        val provider = ComponentName(context, DemoGithubWidgetReceiver::class.java)
        assertTrue(
            "could not bind $provider; grantbind said \"${granted.trim()}\"",
            manager.bindAppWidgetIdIfAllowed(appWidgetId, provider, options(sizes = 2)),
        )
        instrumentation.runOnMainSync { host.createView(context, appWidgetId, manager.getAppWidgetInfo(appWidgetId)) }
        runBlocking { DemoGithubWidget().update(context, GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)) }
    }

    @After
    fun unbind() {
        if (!::host.isInitialized) return
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) host.deleteAppWidgetId(appWidgetId)
        host.stopListening()
    }

    @Test
    fun theLargestHeatmapTheWidgetDrawsIsAcceptedAndShown() {
        val shown = host.awaitShowing("the heatmap") { it.images.isNotEmpty() || it.isGlanceError() }

        assertEquals("the widget showed $shown", listOf(worst.size), shown.images)
        assertTrue(
            "two ${worst.bytes}-byte heatmaps against a cap of ${platformCap()}",
            2 * worst.bytes < platformCap(),
        )
    }

    /**
     * The formula the budget analysis rests on, measured rather than quoted: one row of pixels under
     * six bytes per display pixel goes through, one row over is refused — and refused by throwing in
     * the caller, which is this process. That is the crash the issue was about, and the reason the
     * next test matters.
     */
    @Test
    fun thePlatformRefusesAnUpdateOverSixBytesPerDisplayPixelAndOnlyThen() {
        val width = displaySize().x
        val rows = (platformCap() / (4L * width)).toInt()

        manager.updateAppWidget(appWidgetId, imageUpdate(Bitmap.createBitmap(width, rows - 1, Bitmap.Config.ARGB_8888)))
        assertThrows(IllegalArgumentException::class.java) {
            manager.updateAppWidget(appWidgetId, imageUpdate(Bitmap.createBitmap(width, rows + 1, Bitmap.Config.ARGB_8888)))
        }
    }

    /**
     * What the widget does when an update of its own is over the cap. Two sizes can never get there,
     * so this asks for as many as it takes; a RemoteViews carries at most [MAX_SIZES], which is
     * enough on a small panel and not on a large one — where the cap therefore cannot be reached.
     *
     * Glance catches the platform's exception around its own `updateAppWidget` call and shows its
     * error layout instead, so the widget fails to draw and the process lives. Reaching the
     * assertions at all is half the proof: a crash would have ended the run here.
     */
    @Test
    fun anUpdateOverTheCapFailsToDrawInsteadOfCrashingAndTheWidgetRecovers() {
        host.awaitShowing("the heatmap") { it.images == listOf(worst.size) }
        val sizesToOverflow = (platformCap() / worst.bytes + 1).toInt()
        assumeTrue(
            "$sizesToOverflow sizes needed to pass the cap on this display; a RemoteViews holds $MAX_SIZES",
            sizesToOverflow <= MAX_SIZES,
        )

        manager.updateAppWidgetOptions(appWidgetId, options(sizes = sizesToOverflow))
        val failed = host.awaitShowing("Glance's error layout") { it.isGlanceError() }
        assertEquals("no heatmap alongside the error", emptyList<Size>(), failed.images)

        manager.updateAppWidgetOptions(appWidgetId, options(sizes = 2))
        host.awaitShowing("the heatmap again") { it.images == listOf(worst.size) }
    }

    private fun options(sizes: Int) = Bundle().apply {
        val heights = List(sizes) { 150f + 10f * it }
        putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, ArrayList(heights.map { SizeF(worst.widgetWidthDp, it) }))
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, worst.widgetWidthDp.toInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, worst.widgetWidthDp.toInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heights.min().toInt())
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heights.max().toInt())
    }

    private fun imageUpdate(bitmap: Bitmap) =
        RemoteViews(context.packageName, android.R.layout.activity_list_item).apply {
            setImageViewBitmap(android.R.id.icon, bitmap)
        }

    /** The display the way the widget service reads it when it sets the cap: real size, default display. */
    private fun displaySize() = Point().also {
        @Suppress("DEPRECATION") // getRealSize is what AppWidgetServiceImpl calls, so it is what matches
        context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY).getRealSize(it)
    }

    private fun platformCap(): Long = displaySize().let { 6L * it.x * it.y }

    private fun Shown.isGlanceError() =
        context.getString(androidx.glance.appwidget.R.string.glance_error_layout_text_v2) in texts

    /**
     * Starts the host, waiting out a widget service that does not yet count the user as unlocked.
     *
     * On the Android 17 emulator the service refused the host with "User 0 must be unlocked for
     * widgets to be available" a minute after boot had completed, while this very process — which
     * is not direct-boot aware — was running. Only that refusal is retried, for up to a minute, and
     * a host still refused after it fails with the user's state as the shell reports it.
     */
    private fun startListeningOnceUnlocked(timeoutMillis: Long = 60_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (true) {
            try {
                host.startListening()
                return
            } catch (refused: IllegalStateException) {
                if ("must be unlocked" !in refused.message.orEmpty()) throw refused
                if (SystemClock.uptimeMillis() > deadline) {
                    throw AssertionError(
                        "widget host still refused after ${timeoutMillis / 1000}s; user state: " +
                            shell("am get-started-user-state ${Process.myUid() / PER_USER_RANGE}").trim(),
                        refused,
                    )
                }
                Thread.sleep(1_000)
            }
        }
    }

    /** Runs [command] as the shell and returns what it printed. */
    private fun shell(command: String): String =
        instrumentation.uiAutomation.executeShellCommand(command).use { output ->
            FileInputStream(output.fileDescriptor).use { it.readBytes().decodeToString() }
        }

    private data class Worst(val widgetWidthDp: Float, val spec: HeatmapSpec) {
        val size = Size(spec.widthPx, spec.heightPx)
        val bytes = 4L * spec.widthPx * spec.heightPx
    }

    /** What a host view is showing after an update: the size of each image, and every piece of text. */
    private data class Shown(val images: List<Size>, val texts: List<String>)

    /** A host that notes what each update left on screen, so a test can wait for the one it expects. */
    private class RecordingHost(context: Context) : AppWidgetHost(context, HOST_ID) {
        private val updates = LinkedBlockingQueue<Shown>()

        override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
            object : AppWidgetHostView(context) {
                override fun updateAppWidget(remoteViews: RemoteViews?) {
                    super.updateAppWidget(remoteViews)
                    if (remoteViews != null) updates.put(shown())
                }
            }

        fun awaitShowing(what: String, timeoutSeconds: Long = 60, matches: (Shown) -> Boolean): Shown {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
            var last: Shown? = null
            while (true) {
                val next = updates.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS)
                    ?: throw AssertionError("nothing showing $what within ${timeoutSeconds}s; last update showed $last")
                if (matches(next)) return next
                last = next
            }
        }

        private fun View.shown(): Shown {
            val images = mutableListOf<Size>()
            val texts = mutableListOf<String>()
            fun visit(view: View) {
                when (view) {
                    is ImageView -> (view.drawable as? BitmapDrawable)?.bitmap?.let { images += Size(it.width, it.height) }
                    is TextView -> texts += view.text.toString()
                }
                if (view is ViewGroup) repeat(view.childCount) { visit(view.getChildAt(it)) }
            }
            visit(this)
            return Shown(images, texts)
        }
    }

    private companion object {
        const val HOST_ID = 0x4854 // "HT"; any id not used by another host in this app
        const val MAX_SIZES = 16 // RemoteViews(Map<SizeF, RemoteViews>) refuses more
        const val PER_USER_RANGE = 100_000 // uid = user id × this + app id, as UserHandle has it
    }
}
