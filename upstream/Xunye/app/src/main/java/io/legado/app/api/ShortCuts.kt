package io.legado.app.api

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import io.legado.app.R
import io.legado.app.ui.main.MainIntent
import io.legado.app.ui.main.MainRouteConst
import io.legado.app.ui.widget.components.icon.MaterialSymbol
import io.legado.app.ui.widget.components.icon.MaterialSymbolAxes
import io.legado.app.ui.widget.components.icon.Symbols

/** SPEC-164: launcher long-press shortcuts. */
object ShortCuts {

    private const val ID_BOOKMARKS = "bookmarks"
    private const val ID_NEW_TAB = "new_tab"
    private const val ID_SCAN = "scan_qr"
    private const val ID_BOOKSHELF = "bookshelf"

    fun buildShortCuts(context: Context) {
        ShortcutManagerCompat.setDynamicShortcuts(
            context,
            listOf(
                buildBookmarks(context),
                buildNewTab(context),
                buildScan(context),
                buildBookshelf(context),
            ),
        )
    }

    private fun buildBookmarks(context: Context): ShortcutInfoCompat {
        val intent = MainIntent.createLauncherIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainIntent.EXTRA_START_ROUTE, MainRouteConst.ROUTE_BROWSER_BOOKMARKS)
            action = Intent.ACTION_VIEW
        }
        return ShortcutInfoCompat.Builder(context, ID_BOOKMARKS)
            .setShortLabel("收藏/书签")
            .setLongLabel("收藏/书签")
            .setIcon(symbolIcon(context, Symbols.Bookmarks))
            .setIntent(intent)
            .build()
    }

    private fun buildNewTab(context: Context): ShortcutInfoCompat {
        val intent = MainIntent.createHomeIntent(context).apply {
            // SPEC-187: createLauncherIntent carries no action; ShortcutManager rejects
            // action-less intents and the whole setDynamicShortcuts batch fails silently.
            action = Intent.ACTION_VIEW
            putExtra(MainIntent.EXTRA_BROWSER_NEW_TAB, true)
        }
        return ShortcutInfoCompat.Builder(context, ID_NEW_TAB)
            .setShortLabel("新建标签页")
            .setLongLabel("新建标签页")
            .setIcon(symbolIcon(context, Symbols.Add))
            .setIntent(intent)
            .build()
    }

    private fun buildScan(context: Context): ShortcutInfoCompat {
        val intent = Intent().setClassName(
            context.packageName,
            "io.legado.app.ui.qrcode.QrCodeActivity",
        ).apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ShortcutInfoCompat.Builder(context, ID_SCAN)
            .setShortLabel("扫一扫")
            .setLongLabel("扫一扫")
            .setIcon(symbolIcon(context, Symbols.QrCodeScanner))
            .setIntent(intent)
            .build()
    }

    private fun buildBookshelf(context: Context): ShortcutInfoCompat {
        val intent = MainIntent.createHomeIntent(context).apply {
            // SPEC-187: action-less intent fails ShortcutManager validation (see buildNewTab).
            action = Intent.ACTION_VIEW
            putExtra(MainIntent.EXTRA_HOME_TAB, MainIntent.HOME_TAB_BOOKSHELF)
        }
        return ShortcutInfoCompat.Builder(context, ID_BOOKSHELF)
            .setShortLabel("阅读列表")
            .setLongLabel("阅读列表")
            .setIcon(symbolIcon(context, Symbols.Book))
            .setIntent(intent)
            .build()
    }

    /** SPEC-166: system night mode, not the in-app theme override. */
    internal fun systemNight(): Boolean {
        val mode = Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    internal fun shortcutPalette(night: Boolean): Pair<Int, Int> {
        val bg = if (night) 0xFF121212.toInt() else 0xFFF6F1EA.toInt()
        val fg = if (night) 0xFFF6F1EA.toInt() else 0xFF1C1B19.toInt()
        return bg to fg
    }

    /** 24dp corners on a 108dp launcher icon slot. */
    internal fun shortcutCornerRadius(sizePx: Int): Float = sizePx * (24f / 108f)

    /** Rounded 24dp fill + the same Material Symbol glyph as in the app. */
    private fun symbolIcon(context: Context, symbol: MaterialSymbol): IconCompat {
        val (bg, fg) = shortcutPalette(systemNight())
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = bg
        val radius = shortcutCornerRadius(size)
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, paint)
        val typeface = ResourcesCompat.getFont(context, R.font.material_symbols_outlined)
            ?: Typeface.DEFAULT
        paint.reset()
        paint.isAntiAlias = true
        paint.color = fg
        paint.typeface = typeface
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = size * 0.46f
        paint.style = Paint.Style.FILL
        // Same axes as SymbolIcon. Variation settings crashed the process when
        // this ran inside a ContentProvider (SPEC-165); here a failure falls
        // back to the default glyph instead of killing startup.
        val variation =
            "'wght' ${MaterialSymbolAxes.WEIGHT}, " +
                "'GRAD' ${MaterialSymbolAxes.GRADE}, " +
                "'opsz' ${MaterialSymbolAxes.OPTICAL_SIZE}, " +
                "'FILL' 0"
        runCatching { paint.fontVariationSettings = variation }
        runCatching { paint.fontFeatureSettings = "liga, clig" }
        val metrics = paint.fontMetrics
        val y = size / 2f - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(symbol.ligature, size / 2f, y, paint)
        return IconCompat.createWithBitmap(bitmap)
    }
}
