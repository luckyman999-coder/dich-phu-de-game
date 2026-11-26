// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Phụ Đề Game - chế độ Xem thử: cảnh mẫu có phụ đề tiếng Anh, chạy thật OCR → dịch → đọc
// Không cần máy chơi game, không cần mã dịch (có bản dịch dự phòng cho cảnh mẫu)

package com.metallic.chiaki.dich

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DemoScript
{
	data class Line(val speaker: String, val en: String, val vi: String, val gender: Gender)

	val lines = listOf(
		Line("AERIN", "Stay close to me. The bridge won't hold for long.", "Theo sát tôi. Cây cầu không trụ được lâu đâu.", Gender.FEMALE),
		Line("KAEL", "I can see the tower from here. We're almost there.", "Từ đây tôi thấy được ngọn tháp rồi. Sắp tới nơi rồi.", Gender.MALE),
		Line("AERIN", "Don't look down. Just keep walking.", "Đừng nhìn xuống. Cứ đi tiếp đi.", Gender.FEMALE),
		Line("KAEL", "Something is following us in the dark.", "Có thứ gì đó đang bám theo chúng ta trong bóng tối.", Gender.MALE),
		Line("AERIN", "Then we run. Now!", "Vậy thì chạy. Ngay!", Gender.FEMALE),
		Line("KAEL", "We'll find a way. Together.", "Chúng ta sẽ tìm ra cách. Cùng nhau.", Gender.MALE)
	)

	private fun key(s: String) = s.lowercase().filter { it.isLetter() }

	/** Bản dịch dự phòng: so chữ OCR với kịch bản mẫu */
	fun fallback(ocr: String): Translation?
	{
		val k = key(ocr)
		val line = lines.firstOrNull { k.contains(key(it.en).take(20)) } ?: return null
		return Translation(ocr, line.vi, line.speaker.lowercase().replaceFirstChar { it.uppercase() }, line.gender)
	}
}

/** Cảnh game mẫu vẽ bằng code (bầu trời đêm, núi, 2 nhân vật) + phụ đề tiếng Anh đổi mỗi 4 giây */
class DemoSceneView(context: Context): View(context)
{
	var lineIndex = 0
		set(v) { field = v; invalidate() }

	private val sky = Paint()
	private val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(200, 255, 255, 255) }
	private val hill1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(46, 38, 84) }
	private val hill2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 24, 58) }
	private val figure = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(12, 10, 24) }
	private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 196, 120) }
	private val nameP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
	private val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }
	private val starPts = List(40) { PointF(((it * 73) % 100) / 100f, ((it * 37) % 45) / 100f) }

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)
	{
		val w = MeasureSpec.getSize(widthMeasureSpec)
		setMeasuredDimension(w, w * 9 / 16)
	}

	override fun onDraw(c: Canvas)
	{
		val w = width.toFloat(); val h = height.toFloat()
		sky.shader = LinearGradient(0f, 0f, 0f, h, Color.rgb(24, 20, 60), Color.rgb(70, 50, 110), Shader.TileMode.CLAMP)
		c.drawRect(0f, 0f, w, h, sky)
		starPts.forEach { c.drawCircle(it.x * w, it.y * h, w * 0.0025f, star) }
		c.drawCircle(w * 0.52f, h * 0.34f, w * 0.012f, glow)
		c.drawPath(Path().apply { moveTo(0f, h * 0.62f); quadTo(w * 0.3f, h * 0.45f, w * 0.6f, h * 0.6f); quadTo(w * 0.85f, h * 0.5f, w, h * 0.58f); lineTo(w, h); lineTo(0f, h); close() }, hill1)
		c.drawPath(Path().apply { moveTo(0f, h * 0.72f); quadTo(w * 0.5f, h * 0.62f, w, h * 0.74f); lineTo(w, h); lineTo(0f, h); close() }, hill2)
		// 2 nhân vật
		for((x, s) in listOf(0.47f to 1f, 0.55f to 0.85f))
		{
			val fh = h * 0.16f * s
			c.drawCircle(w * x, h * 0.6f - fh, fh * 0.16f, figure)
			c.drawRoundRect(RectF(w * x - fh * 0.14f, h * 0.6f - fh * 0.85f, w * x + fh * 0.14f, h * 0.6f), fh * 0.08f, fh * 0.08f, figure)
		}
		// phụ đề
		val line = DemoScript.lines[lineIndex % DemoScript.lines.size]
		nameP.textSize = h * 0.045f
		subP.textSize = h * 0.058f
		c.drawText(line.speaker, w / 2, h * 0.84f, nameP)
		c.drawText(line.en, w / 2, h * 0.92f, subP)
	}

	/** Vẽ cảnh ra bitmap để OCR (giống chụp hình PS5) */
	fun render(): Bitmap?
	{
		if(width == 0 || height == 0) return null
		val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
		draw(Canvas(bmp))
		return bmp
	}
}

class DemoActivity: AppCompatActivity()
{
	private lateinit var prefs: DichPrefs
	private lateinit var scene: DemoSceneView
	private lateinit var region: RegionSelectView
	private lateinit var viText: TextView
	private lateinit var enText: TextView
	private lateinit var speaker: Speaker
	private lateinit var pipeline: SubtitlePipeline
	private val tick = object: Runnable {
		override fun run()
		{
			scene.lineIndex = scene.lineIndex + 1
			scene.postDelayed(this, 4500)
		}
	}

	private val dp get() = resources.displayMetrics.density
	private fun px(v: Int) = (v * dp).toInt()

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		prefs = DichPrefs(this)
		window.statusBarColor = Color.BLACK

		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setBackgroundColor(Color.BLACK)
		}
		root.addView(TextView(this).apply {
			text = "XEM THỬ · cảnh mẫu do ứng dụng tạo, không cần máy chơi game"
			setTextColor(Color.argb(200, 255, 255, 255))
			textSize = 13f
			setPadding(px(16), px(16), px(16), px(10))
		})

		val frame = FrameLayout(this)
		scene = DemoSceneView(this)
		frame.addView(scene, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
		region = RegionSelectView(this).apply {
			region = RectF(0.06f, 0.78f, 0.94f, 0.97f)
			editing = false
		}
		frame.addView(region, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
		root.addView(frame)

		root.addView(TextView(this).apply {
			text = "Thử kéo khung"
			setTextColor(Color.rgb(159, 225, 203))
			textSize = 14f
			setPadding(px(16), px(10), px(16), 0)
			setOnClickListener {
				region.editing = !region.editing
				text = if(region.editing) "Xong" else "Thử kéo khung"
			}
		})

		val panel = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(px(20), px(28), px(20), px(20))
		}
		panel.addView(TextView(this).apply {
			text = "🇻🇳 Bản dịch tiếng Việt"
			setTextColor(Color.WHITE)
			textSize = 13f
			setPadding(px(10), px(4), px(10), px(4))
			background = GradientDrawable().apply { cornerRadius = px(12).toFloat(); setColor(Color.rgb(234, 88, 12)) }
		}, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.END })
		viText = TextView(this).apply {
			text = "Đang đọc phụ đề…"
			setTextColor(Color.WHITE)
			textSize = 24f
			setTypeface(typeface, Typeface.BOLD)
			gravity = Gravity.CENTER
			setPadding(0, px(16), 0, px(8))
		}
		panel.addView(viText)
		enText = TextView(this).apply {
			setTextColor(Color.argb(150, 255, 255, 255))
			textSize = 14f
			gravity = Gravity.CENTER
		}
		panel.addView(enText)
		root.addView(panel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

		root.addView(TextView(this).apply {
			text = "BẮT ĐẦU CHƠI THẬT"
			setTextColor(Color.WHITE)
			textSize = 17f
			setTypeface(typeface, Typeface.BOLD)
			gravity = Gravity.CENTER
			setPadding(0, px(14), 0, px(14))
			background = GradientDrawable().apply { cornerRadius = px(28).toFloat(); setColor(Color.rgb(108, 99, 255)) }
			setOnClickListener { finish() }
		}, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
			setMargins(px(20), 0, px(20), px(24))
		})
		setContentView(root)

		speaker = Speaker(this, prefs)
		// Pipeline dùng vùng khung riêng của chế độ xem thử
		val regionView = region
		val demoPrefs = object: DemoRegionPrefs(this@DemoActivity) { override val demoRegion: RectF get() = regionView.region }
		pipeline = SubtitlePipeline(
			frameSource = object: FrameSource {
				override suspend fun capture(): Bitmap? = withContext(Dispatchers.Main) { scene.render() }
			},
			prefs = demoPrefs,
			translator = Translator(prefs),
			speaker = speaker,
			onShow = { t ->
				viText.text = if(t.speaker.isNotEmpty()) "${t.speaker}: ${t.vietnamese}" else t.vietnamese
				enText.text = t.original.replace('\n', ' ')
			},
			onClear = { },
			onStatus = { msg -> if(msg != null) enText.text = msg },
			fallback = DemoScript::fallback)
	}

	override fun onResume()
	{
		super.onResume()
		scene.postDelayed(tick, 4500)
		pipeline.resetState()
		pipeline.start(lifecycleScope)
	}

	override fun onPause()
	{
		super.onPause()
		scene.removeCallbacks(tick)
		pipeline.stop()
	}

	override fun onDestroy()
	{
		super.onDestroy()
		pipeline.release()
		speaker.shutdown()
	}
}

/** Cấu hình cho chế độ xem thử: dùng mã dịch thật nếu có, nhưng vùng khung là của cảnh mẫu */
abstract class DemoRegionPrefs(context: Context): DichPrefs(context)
{
	abstract val demoRegion: RectF
	override var region: RectF
		get() = demoRegion
		set(_) {}
	override var enabled: Boolean
		get() = true
		set(_) {}
}
