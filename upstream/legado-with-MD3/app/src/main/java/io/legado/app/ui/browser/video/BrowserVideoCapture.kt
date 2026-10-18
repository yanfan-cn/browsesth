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

    fun captureToPictures(root: View, titleHint: String, onDone: (Result<File>) -> Unit) {
        fun finish(result: Result<File>) {
            Handler(Looper.getMainLooper()).post { onDone(result) }
        }
        tryCapture(root) { bitmap ->
            if (bitmap == null) {
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
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                bitmap.recycle()
                dest
            }.also { finish(it) }
        }
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
