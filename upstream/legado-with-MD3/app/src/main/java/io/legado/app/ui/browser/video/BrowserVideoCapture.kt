package io.legado.app.ui.browser.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import io.legado.app.browser.engine.BrowserDownloadPaths
import java.io.File
import java.io.FileOutputStream

/** SPEC-136: capture a frame from the player view into public Pictures. */
object BrowserVideoCapture {

    // SPEC-147 R9: 已知视频宽高比时按比例中心裁剪（黑边消除）；null 不裁。
    fun captureToPictures(
        root: View,
        titleHint: String,
        videoAspect: Pair<Int, Int>? = null,
        onDone: (Result<File>) -> Unit,
    ) {
        fun finish(result: Result<File>) {
            Handler(Looper.getMainLooper()).post { onDone(result) }
        }
        tryCapture(root) { bitmap ->
            val cropped = bitmap?.let { cropToVideoAspect(it, videoAspect) }
            if (cropped == null) {
                bitmap?.recycle()
                finish(Result.failure(IllegalStateException("无法截取画面")))
                return@tryCapture
            }
            runCatching {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    BrowserDownloadPaths.FOLDER_NAME,
                )
                if (!dir.exists()) dir.mkdirs()
                val name = "shot_${System.currentTimeMillis()}.jpg"
                val dest = BrowserDownloadPaths.uniqueFile(dir, name)
                FileOutputStream(dest).use { out ->
                    cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                cropped.recycle()
                dest
            }.also { finish(it) }
        }
    }

    /** SPEC-147 R9: 抓帧是整个视图（含黑边）；已知视频比例时中心裁出视频画面。 */
    private fun cropToVideoAspect(bitmap: Bitmap, videoAspect: Pair<Int, Int>?): Bitmap {
        if (videoAspect == null) return bitmap
        val (vw, vh) = videoAspect
        if (vw <= 0 || vh <= 0) return bitmap
        val targetRatio = vw.toDouble() / vh
        val bw = bitmap.width
        val bh = bitmap.height
        if (bw <= 0 || bh <= 0) return bitmap
        val bitmapRatio = bw.toDouble() / bh
        var cropW = bw
        var cropH = bh
        if (bitmapRatio > targetRatio) {
            // 视图比视频更宽（左右黑边）→ 裁宽。
            cropW = (bh * targetRatio).toInt().coerceIn(1, bw)
        } else if (bitmapRatio < targetRatio) {
            // 视图比视频更高（上下黑边）→ 裁高。
            cropH = (bw / targetRatio).toInt().coerceIn(1, bh)
        }
        if (cropW == bw && cropH == bh) return bitmap
        val x = (bw - cropW) / 2
        val y = (bh - cropH) / 2
        val cropped = Bitmap.createBitmap(bitmap, x, y, cropW, cropH)
        if (cropped != bitmap) bitmap.recycle()
        return cropped
    }

    private fun tryCapture(root: View, done: (Bitmap?) -> Unit) {
        val texture = findChild<TextureView>(root)
        if (texture != null) {
            done(texture.bitmap)
            return
        }
        val surface = findChild<SurfaceView>(root)
        if (surface != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val w = surface.width.coerceAtLeast(1)
            val h = surface.height.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            try {
                PixelCopy.request(surface, bitmap, { result ->
                    if (result == PixelCopy.SUCCESS) done(bitmap) else {
                        bitmap.recycle()
                        done(null)
                    }
                }, Handler(Looper.getMainLooper()))
            } catch (_: Throwable) {
                bitmap.recycle()
                done(drawFallback(root))
            }
            return
        }
        done(drawFallback(root))
    }

    private fun drawFallback(view: View): Bitmap? {
        if (view.width <= 0 || view.height <= 0) return null
        return runCatching {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        }.getOrNull()
    }

    private fun <T : View> findChild(root: View, clazz: Class<T>): T? {
        if (clazz.isInstance(root)) {
            @Suppress("UNCHECKED_CAST")
            return root as T
        }
        if (root !is ViewGroup) return null
        for (i in 0 until root.childCount) {
            findChild(root.getChildAt(i), clazz)?.let { return it }
        }
        return null
    }

    private inline fun <reified T : View> findChild(root: View): T? =
        findChild(root, T::class.java)
}
