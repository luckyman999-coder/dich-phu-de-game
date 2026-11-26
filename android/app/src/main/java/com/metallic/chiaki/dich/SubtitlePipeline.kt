// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Phụ Đề Game - chụp khung hình → OCR vùng phụ đề → dịch → hiện + đọc

package com.metallic.chiaki.dich

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

/** Nguồn khung hình: màn hình PS5 (SurfaceView) hoặc cảnh mẫu trong chế độ Xem thử */
fun interface FrameSource
{
	suspend fun capture(): Bitmap?
}

/** Chụp hình game đang phát trên SurfaceView bằng PixelCopy */
class SurfaceFrameSource(private val surfaceView: SurfaceView): FrameSource
{
	private var bitmap: Bitmap? = null

	override suspend fun capture(): Bitmap?
	{
		val holder = surfaceView.holder
		if(holder.surface == null || !holder.surface.isValid) return null
		val sw = surfaceView.width
		val sh = surfaceView.height
		if(sw <= 0 || sh <= 0) return null
		val scale = min(1f, 1920f / sw)
		val w = (sw * scale).toInt()
		val h = (sh * scale).toInt()
		val bmp = bitmap?.takeIf { it.width == w && it.height == h }
			?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bitmap = it }
		val ok = suspendCancellableCoroutine<Boolean> { cont ->
			try
			{
				PixelCopy.request(surfaceView, bmp, { result ->
					if(cont.isActive) cont.resume(result == PixelCopy.SUCCESS)
				}, copyHandler)
			}
			catch(e: IllegalArgumentException)
			{
				if(cont.isActive) cont.resume(false)
			}
		}
		return if(ok) bmp else null
	}

	fun release() = copyThread.quitSafely()
}

class SubtitlePipeline(
	private val frameSource: FrameSource,
	private val prefs: DichPrefs,
	private val translator: Translator,
	private val speaker: Speaker,
	private val onShow: (Translation) -> Unit,
	private val onClear: () -> Unit,
	private val onStatus: (String?) -> Unit,
	/** Bản dịch có sẵn (chế độ Xem thử dùng khi chưa có mã dịch) */
	private val fallback: ((String) -> Translation?)? = null)
{
	companion object
	{
		private const val TAG = "DichPipeline"
		private const val INTERVAL_MS = 300L
		private const val STABLE_FRAMES = 2      // chữ phải giống nhau 2 lần chụp liền mới dịch
		private const val EMPTY_FRAMES_TO_CLEAR = 3
		private const val VI_CHARS = "ăâđêôơưạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ"
	}

	private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

	private var loopJob: Job? = null
	private var translateJob: Job? = null
	private var scope: CoroutineScope? = null

	private var pendingText = ""
	private var pendingCount = 0
	private var shownText = ""
	private var emptyCount = 0

	fun start(scope: CoroutineScope)
	{
		if(loopJob?.isActive == true) return
		this.scope = scope
		loopJob = scope.launch(Dispatchers.Default) {
			while(isActive)
			{
				val t0 = System.currentTimeMillis()
				try { tick() }
				catch(e: CancellationException) { throw e }
				catch(e: Exception) { Log.w(TAG, "tick lỗi", e) }
				val dt = System.currentTimeMillis() - t0
				delay(max(50L, INTERVAL_MS - dt))
			}
		}
	}

	fun stop()
	{
		loopJob?.cancel()
		translateJob?.cancel()
		loopJob = null
		resetState()
		speaker.stop()
	}

	fun release()
	{
		stop()
		recognizer.close()
		(frameSource as? SurfaceFrameSource)?.release()
	}

	/** Đổi vùng / đổi game thì bắt đầu lại */
	fun resetState()
	{
		pendingText = ""
		pendingCount = 0
		shownText = ""
		emptyCount = 0
	}

	private suspend fun tick()
	{
		if(!prefs.enabled) return
		val frame = frameSource.capture() ?: return
		val crop = cropRegion(frame) ?: return
		val text = ocr(crop)
		if(crop !== frame) crop.recycle()
		handleText(text)
	}

	private fun cropRegion(frame: Bitmap): Bitmap?
	{
		val r = prefs.region
		val rect = Rect(
			(r.left * frame.width).toInt().coerceIn(0, frame.width - 1),
			(r.top * frame.height).toInt().coerceIn(0, frame.height - 1),
			(r.right * frame.width).toInt().coerceIn(1, frame.width),
			(r.bottom * frame.height).toInt().coerceIn(1, frame.height))
		if(rect.width() < 8 || rect.height() < 8) return null
		var crop = Bitmap.createBitmap(frame, rect.left, rect.top, rect.width(), rect.height())
		// Phụ đề nhỏ thì phóng to cho OCR đọc chuẩn hơn
		if(crop.height < 90)
		{
			val f = 90f / crop.height
			val up = Bitmap.createScaledBitmap(crop, (crop.width * f).toInt(), 90, true)
			crop.recycle()
			crop = up
		}
		return crop
	}

	private suspend fun ocr(bmp: Bitmap): String = suspendCancellableCoroutine { cont ->
		recognizer.process(InputImage.fromBitmap(bmp, 0))
			.addOnSuccessListener { result ->
				val lines = result.textBlocks.flatMap { it.lines }
					.sortedBy { it.boundingBox?.top ?: 0 }
					.map { it.text.trim() }
					.filter { it.isNotEmpty() }
				if(cont.isActive) cont.resume(lines.joinToString("\n"))
			}
			.addOnFailureListener { if(cont.isActive) cont.resume("") }
	}

	private fun handleText(raw: String)
	{
		val text = clean(raw)
		if(text.isEmpty())
		{
			emptyCount++
			pendingCount = 0
			pendingText = ""
			if(emptyCount == EMPTY_FRAMES_TO_CLEAR && shownText.isNotEmpty())
			{
				shownText = ""
				translateJob?.cancel()
				scope?.launch(Dispatchers.Main) { onClear() }
			}
			return
		}
		emptyCount = 0

		if(similar(text, pendingText)) pendingCount++
		else { pendingText = text; pendingCount = 1 }

		if(pendingCount != STABLE_FRAMES) return
		if(similar(text, shownText)) return
		shownText = text
		process(text)
	}

	private fun process(text: String)
	{
		val scope = scope ?: return
		translateJob?.cancel()
		translateJob = scope.launch {
			// Game đã có phụ đề tiếng Việt → chỉ đọc, không tốn lượt dịch
			val t = if(isVietnamese(text))
				Translation(text, text, "", Gender.UNKNOWN)
			else
			{
				if(!prefs.hasAnyKey && fallback != null)
					fallback.invoke(text)
				else if(!prefs.hasAnyKey)
				{
					withContext(Dispatchers.Main) { onStatus("Chưa có mã dịch – vào Cài đặt dịch để thêm mã Gemini/Groq") }
					return@launch
				}
				else
					translator.translate(text) ?: fallback?.invoke(text)
			}
			if(!isActive) return@launch
			withContext(Dispatchers.Main) {
				if(t == null)
					onStatus(translator.lastError ?: "Dịch thất bại")
				else
				{
					onStatus(null)
					onShow(t)
					speaker.speak(t)
				}
			}
		}
	}

	private fun clean(raw: String): String
	{
		val lines = raw.lines()
			.map { it.replace(Regex("\\s+"), " ").trim() }
			.filter { line -> line.count { it.isLetter() } >= 2 }
		val text = lines.joinToString("\n")
		return if(text.count { it.isLetter() } < 3) "" else text
	}

	private fun key(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

	/** Chấp nhận OCR nhảy vài ký tự giữa các lần chụp */
	private fun similar(a: String, b: String): Boolean
	{
		val ka = key(a)
		val kb = key(b)
		if(ka == kb) return true
		if(ka.isEmpty() || kb.isEmpty()) return false
		val longer = max(ka.length, kb.length)
		if(kotlin.math.abs(ka.length - kb.length) > longer / 8 + 1) return false
		return 1f - levenshtein(ka, kb).toFloat() / longer >= 0.9f
	}

	private fun levenshtein(a: String, b: String): Int
	{
		var prev = IntArray(b.length + 1) { it }
		var cur = IntArray(b.length + 1)
		for(i in 1..a.length)
		{
			cur[0] = i
			for(j in 1..b.length)
			{
				val cost = if(a[i - 1] == b[j - 1]) 0 else 1
				cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
			}
			val tmp = prev; prev = cur; cur = tmp
		}
		return prev[b.length]
	}

	private fun isVietnamese(text: String): Boolean
	{
		val letters = text.count { it.isLetter() }
		if(letters == 0) return false
		val vi = text.lowercase().count { VI_CHARS.indexOf(it) >= 0 }
		return vi.toFloat() / letters > 0.06f
	}
}
