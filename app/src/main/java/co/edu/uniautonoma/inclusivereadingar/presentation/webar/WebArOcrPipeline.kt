package co.edu.uniautonoma.inclusivereadingar.presentation.webar

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.Window
import android.webkit.WebView
import co.edu.uniautonoma.inclusivereadingar.BuildConfig
import co.edu.uniautonoma.inclusivereadingar.domain.ocr.OcrWordCandidate
import co.edu.uniautonoma.inclusivereadingar.domain.ocr.OcrWordNormalizer
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

/**
 * Copies only the central camera region shown by the WebView and performs bundled, on-device OCR.
 * No bitmap is persisted or sent through the network; each frame is recycled immediately.
 */
class WebArOcrPipeline(
    private val window: Window,
    private val callbackHandler: Handler = Handler(Looper.getMainLooper()),
    private val recognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )
) : Closeable {

    suspend fun scan(webView: WebView): List<OcrWordCandidate> {
        val frame = capture(webView)
        if (frame == null) {
            logScan(captureSucceeded = false, stats = null, candidates = emptyList())
            return emptyList()
        }
        val stats = frame.sampledLuminanceStats()
        val task = try {
            recognizer.process(InputImage.fromBitmap(frame.bitmap, 0))
        } catch (error: Exception) {
            frame.bitmap.recycle()
            logScan(captureSucceeded = true, stats = stats, candidates = emptyList())
            throw error
        }
        // ML Kit tasks cannot be cancelled. Keep the source alive until native processing ends.
        task.addOnCompleteListener {
            if (!frame.bitmap.isRecycled) frame.bitmap.recycle()
        }
        return try {
            val result = task.awaitResult()
            val candidates = result.textBlocks
                .asSequence()
                .flatMap { it.lines.asSequence() }
                .flatMap { it.elements.asSequence() }
                .mapNotNull { element -> frame.toCandidate(element) }
                .toList()
            logScan(captureSucceeded = true, stats = stats, candidates = candidates)
            candidates
        } catch (error: Exception) {
            logScan(captureSucceeded = true, stats = stats, candidates = emptyList())
            throw error
        }
    }

    override fun close() = recognizer.close()

    private suspend fun capture(webView: WebView): CapturedFrame? =
        withContext(Dispatchers.Main.immediate) {
            if (!webView.isAttachedToWindow || !webView.isShown ||
                webView.width <= 0 || webView.height <= 0 || window.peekDecorView() == null
            ) {
                return@withContext null
            }

            val location = IntArray(2)
            webView.getLocationInWindow(location)
            val viewRect = Rect(
                location[0],
                location[1],
                location[0] + webView.width,
                location[1] + webView.height
            )
            val sourceRect = viewRect.centralRecognitionRegion()
            val decor = window.decorView
            if (!sourceRect.intersect(0, 0, decor.width, decor.height) || sourceRect.isEmpty) {
                return@withContext null
            }

            val scale = minOf(
                1f,
                MAX_BITMAP_EDGE.toFloat() / maxOf(sourceRect.width(), sourceRect.height())
            )
            val bitmap = Bitmap.createBitmap(
                (sourceRect.width() * scale).roundToInt().coerceAtLeast(1),
                (sourceRect.height() * scale).roundToInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )

            val copied = suspendCancellableCoroutine { continuation ->
                // Also covers cancellation after PixelCopy resumes but before dispatch consumes it.
                continuation.invokeOnCancellation {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
                try {
                    PixelCopy.request(
                        window,
                        sourceRect,
                        bitmap,
                        { result ->
                            if (!continuation.isActive) {
                                bitmap.recycle()
                            } else {
                                continuation.resume(result == PixelCopy.SUCCESS)
                            }
                        },
                        callbackHandler
                    )
                } catch (_: IllegalArgumentException) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    } else if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }

            if (!copied) {
                if (!bitmap.isRecycled) bitmap.recycle()
                return@withContext null
            }
            CapturedFrame(
                bitmap = bitmap,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height,
                sourceRect = sourceRect,
                viewRect = viewRect
            )
        }

    private suspend fun Task<Text>.awaitResult(): Text =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            addOnFailureListener { error ->
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }

    private data class CapturedFrame(
        val bitmap: Bitmap,
        val bitmapWidth: Int,
        val bitmapHeight: Int,
        val sourceRect: Rect,
        val viewRect: Rect
    ) {
        fun sampledLuminanceStats(): LuminanceStats {
            var count = 0
            var sum = 0.0
            var sumOfSquares = 0.0
            val stepX = (bitmapWidth / SAMPLE_GRID_SIZE).coerceAtLeast(1)
            val stepY = (bitmapHeight / SAMPLE_GRID_SIZE).coerceAtLeast(1)

            var y = stepY / 2
            while (y < bitmapHeight) {
                var x = stepX / 2
                while (x < bitmapWidth) {
                    val pixel = bitmap.getPixel(x, y)
                    val red = (pixel shr 16) and 0xFF
                    val green = (pixel shr 8) and 0xFF
                    val blue = pixel and 0xFF
                    val luminance = RED_LUMINANCE * red +
                        GREEN_LUMINANCE * green +
                        BLUE_LUMINANCE * blue
                    sum += luminance
                    sumOfSquares += luminance * luminance
                    count += 1
                    x += stepX
                }
                y += stepY
            }

            if (count == 0) return LuminanceStats(0.0, 0.0, 0)
            val mean = sum / count
            val variance = (sumOfSquares / count - mean * mean).coerceAtLeast(0.0)
            return LuminanceStats(mean, variance, count)
        }

        fun toCandidate(element: Text.Element): OcrWordCandidate? {
            val word = OcrWordNormalizer.normalize(element.text) ?: return null
            val bounds = element.boundingBox ?: return null
            if (bounds.isEmpty || bitmapWidth == 0 || bitmapHeight == 0) return null

            val sourceCenterX = sourceRect.left +
                (bounds.exactCenterX() / bitmapWidth.toFloat()) * sourceRect.width()
            val sourceCenterY = sourceRect.top +
                (bounds.exactCenterY() / bitmapHeight.toFloat()) * sourceRect.height()
            val centerX = (sourceCenterX - viewRect.left) / viewRect.width().toFloat()
            val centerY = (sourceCenterY - viewRect.top) / viewRect.height().toFloat()

            val relativeWidth =
                (bounds.width().toFloat() / bitmapWidth) * sourceRect.width() / viewRect.width()
            val relativeHeight =
                (bounds.height().toFloat() / bitmapHeight) * sourceRect.height() / viewRect.height()

            return OcrWordCandidate(
                word = word,
                confidence = element.confidence,
                centerX = centerX.coerceIn(0f, 1f),
                centerY = centerY.coerceIn(0f, 1f),
                relativeArea = (relativeWidth * relativeHeight).coerceIn(0f, 1f)
            )
        }
    }

    private data class LuminanceStats(
        val mean: Double,
        val variance: Double,
        val sampleCount: Int
    )

    private fun logScan(
        captureSucceeded: Boolean,
        stats: LuminanceStats?,
        candidates: List<OcrWordCandidate>
    ) {
        if (!BuildConfig.DEBUG) return
        val candidateMetrics = candidates.joinToString(prefix = "[", postfix = "]") {
            String.format(
                Locale.ROOT,
                "%.3f/%.3f/%.3f/%.4f",
                it.confidence,
                it.centerX,
                it.centerY,
                it.relativeArea
            )
        }
        Log.d(
            LOG_TAG,
            String.format(
                Locale.ROOT,
                "capture=%d candidates=%d luminanceMean=%.2f luminanceVariance=%.2f samples=%d metrics=%s",
                if (captureSucceeded) 1 else 0,
                candidates.size,
                stats?.mean ?: -1.0,
                stats?.variance ?: -1.0,
                stats?.sampleCount ?: 0,
                candidateMetrics
            )
        )
    }

    private fun Rect.centralRecognitionRegion(): Rect {
        val horizontalInset = (width() * HORIZONTAL_INSET_RATIO).roundToInt()
        val topInset = (height() * TOP_INSET_RATIO).roundToInt()
        val bottomInset = (height() * BOTTOM_INSET_RATIO).roundToInt()
        return Rect(
            left + horizontalInset,
            top + topInset,
            right - horizontalInset,
            bottom - bottomInset
        )
    }

    companion object {
        /** Delay between complete OCR passes; recognition never overlaps. */
        const val SCAN_INTERVAL_MS = 850L
        const val CAMERA_WARM_UP_MS = 1_000L
        private const val MAX_BITMAP_EDGE = 960
        private const val HORIZONTAL_INSET_RATIO = 0.06f
        private const val TOP_INSET_RATIO = 0.16f
        private const val BOTTOM_INSET_RATIO = 0.24f
        private const val SAMPLE_GRID_SIZE = 16
        private const val RED_LUMINANCE = 0.2126
        private const val GREEN_LUMINANCE = 0.7152
        private const val BLUE_LUMINANCE = 0.0722
        private const val LOG_TAG = "WebArOcr"
    }
}
