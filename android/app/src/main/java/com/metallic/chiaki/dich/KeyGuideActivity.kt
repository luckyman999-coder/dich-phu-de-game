// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - hướng dẫn lấy mã Gemini / Groq từng bước, tự nhận mã khi người dùng copy

package com.metallic.chiaki.dich

import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.metallic.chiaki.dich.HomeActivity.Companion.ACCENT
import com.metallic.chiaki.dich.HomeActivity.Companion.AMBER
import com.metallic.chiaki.dich.HomeActivity.Companion.BG
import com.metallic.chiaki.dich.HomeActivity.Companion.CARD
import com.metallic.chiaki.dich.HomeActivity.Companion.MUTED
import com.metallic.chiaki.dich.HomeActivity.Companion.STROKE
import com.metallic.chiaki.dich.HomeActivity.Companion.TEXT
import kotlinx.coroutines.launch

class KeyGuideActivity: AppCompatActivity()
{
	private enum class Provider(val title: String, val url: String, val prefix: String) {
		GEMINI("Gemini (Google)", "https://aistudio.google.com/apikey", "AIza"),
		GROQ("Groq", "https://console.groq.com/keys", "gsk_")
	}

	private lateinit var prefs: DichPrefs
	private lateinit var root: LinearLayout
	private var provider = Provider.GEMINI
	private var result: String? = null
	private var lastSeenClip: String? = null

	private val dp get() = resources.displayMetrics.density
	private fun px(v: Int) = (v * dp).toInt()

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		prefs = DichPrefs(this)
		window.statusBarColor = BG
		window.navigationBarColor = BG
		val scroll = ScrollView(this).apply { setBackgroundColor(BG) }
		root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(px(20), px(20), px(20), px(40))
		}
		scroll.addView(root)
		setContentView(scroll)
		render()
	}

	/** Android chỉ cho đọc clipboard khi app đang được focus */
	override fun onWindowFocusChanged(hasFocus: Boolean)
	{
		super.onWindowFocusChanged(hasFocus)
		if(!hasFocus) return
		val clip = (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
		val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()?.trim() ?: return
		if(text == lastSeenClip) return
		lastSeenClip = text
		val p = Provider.values().firstOrNull { text.startsWith(it.prefix) } ?: return
		if(text.length < 20 || text.contains(' ')) return
		provider = p
		saveKey(text)
	}

	private fun saveKey(raw: String)
	{
		val key = raw.trim()
		when
		{
			key.startsWith(Provider.GEMINI.prefix) ->
			{
				val keys = prefs.geminiKeys
				if(key in keys) { result = "Mã này đã có rồi."; render(); return }
				if(keys.size >= 3) { result = "Đã đủ 3 mã Gemini. Xoá bớt trong Cài đặt nếu muốn thay."; render(); return }
				prefs.geminiKeys = keys + key
			}
			key.startsWith(Provider.GROQ.prefix) -> prefs.groqKey = key
			else -> { result = "Mã không đúng dạng (Gemini bắt đầu bằng AIza…, Groq bắt đầu bằng gsk_…)."; render(); return }
		}
		result = "Đã nhận mã, đang dịch thử…"
		render()
		test()
	}

	private fun test()
	{
		val tr = Translator(prefs)
		lifecycleScope.launch {
			val t = tr.translate("Stay close to me. The bridge won't hold for long.")
			result = if(t != null) "✅ Mã dùng được! Dịch thử: \"${t.vietnamese}\""
				else "❌ Mã chưa dùng được: ${tr.lastError ?: "không rõ lỗi"}. Kiểm tra lại đã copy đủ mã chưa."
			render()
		}
	}

	private fun render()
	{
		root.removeAllViews()
		val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		header.addView(text("‹", 28f, TEXT).apply { setPadding(0, 0, px(14), 0); setOnClickListener { finish() } })
		header.addView(text("Lấy mã dịch miễn phí", 22f, TEXT, true))
		root.addView(header)
		root.addView(text("Khoảng 2 phút. Mã miễn phí, không cần thẻ thanh toán.", 14f, MUTED).apply { setPadding(0, px(4), 0, px(16)) })

		// tình trạng mã hiện có
		val status = "Gemini: ${prefs.geminiKeys.size}/3 mã   ·   Groq: ${if(prefs.groqKey.isNotEmpty()) "đã có" else "chưa có"}"
		root.addView(text(status, 14f, if(prefs.hasAnyKey) ACCENT else AMBER).apply { setPadding(0, 0, 0, px(12)) })

		// chọn nhà cung cấp
		val tabs = LinearLayout(this)
		Provider.values().forEach { p ->
			tabs.addView(text(p.title, 15f, if(p == provider) BG else TEXT).apply {
				gravity = Gravity.CENTER
				setPadding(px(16), px(10), px(16), px(10))
				background = GradientDrawable().apply { cornerRadius = px(20).toFloat(); setColor(if(p == provider) ACCENT else CARD); setStroke(px(1), STROKE) }
				setOnClickListener { provider = p; result = null; render() }
			}, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = px(8) })
		}
		root.addView(tabs)

		val steps = when(provider)
		{
			Provider.GEMINI -> listOf(
				"Mở trang Google AI Studio" to "Bấm nút bên dưới. Đăng nhập bằng tài khoản Google (Gmail) bất kỳ.",
				"Tạo mã" to "Bấm \"Create API key\" (Tạo khoá API). Lần đầu có thể phải đồng ý điều khoản.",
				"Copy mã" to "Bấm biểu tượng copy cạnh mã vừa tạo. Mã bắt đầu bằng AIza…",
				"Quay lại Dịch Game" to "App tự nhận mã trong bộ nhớ tạm và dịch thử ngay. Không cần dán.")
			Provider.GROQ -> listOf(
				"Mở trang Groq" to "Bấm nút bên dưới, đăng nhập bằng Google hoặc email.",
				"Tạo mã" to "Bấm \"Create API Key\", đặt tên bất kỳ (ví dụ: dichgame) rồi bấm Submit.",
				"Copy mã" to "Copy mã hiện ra (chỉ hiện 1 lần). Mã bắt đầu bằng gsk_…",
				"Quay lại Dịch Game" to "App tự nhận mã và dịch thử. Groq được dùng khi các mã Gemini hết lượt.")
		}
		steps.forEachIndexed { i, (title, desc) ->
			val row = LinearLayout(this).apply {
				setPadding(px(14), px(14), px(14), px(14))
				background = GradientDrawable().apply { cornerRadius = px(16).toFloat(); setColor(CARD); setStroke(px(1), STROKE) }
			}
			row.addView(text("${i + 1}", 15f, BG, true).apply {
				gravity = Gravity.CENTER
				background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(ACCENT) }
			}, LinearLayout.LayoutParams(px(28), px(28)).apply { marginEnd = px(12) })
			val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
			col.addView(text(title, 16f, TEXT, true))
			col.addView(text(desc, 14f, MUTED))
			if(i == 0)
				col.addView(button("Mở ${provider.title}") {
					try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(provider.url))) }
					catch(e: Exception) { Toast.makeText(this, provider.url, Toast.LENGTH_LONG).show() }
				}, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(10) })
			row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
			root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(12) })
		}

		// dán thủ công
		root.addView(text("Hoặc dán mã vào đây", 13f, MUTED).apply { setPadding(px(4), px(20), 0, px(6)) })
		val manual = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		val input = EditText(this).apply {
			hint = "${provider.prefix}…"
			setTextColor(TEXT)
			setHintTextColor(MUTED)
			isSingleLine = true
		}
		manual.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		manual.addView(button("Lưu") { saveKey(input.text.toString()) }, LinearLayout.LayoutParams(px(90), ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = px(8) })
		root.addView(manual)

		result?.let { root.addView(text(it, 15f, if(it.startsWith("❌")) AMBER else ACCENT).apply { setPadding(0, px(16), 0, 0) }) }

		if(provider == Provider.GEMINI)
			root.addView(text("Mẹo: mỗi mã nên tạo ở một dự án (project) hoặc tài khoản Google khác nhau để có thêm lượt dịch miễn phí.", 13f, MUTED).apply { setPadding(0, px(16), 0, 0) })
	}

	private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
		text = t
		textSize = size
		setTextColor(color)
		if(bold) setTypeface(typeface, Typeface.BOLD)
	}

	private fun button(label: String, onClick: () -> Unit) = TextView(this).apply {
		text = label
		textSize = 15f
		setTypeface(typeface, Typeface.BOLD)
		setTextColor(BG)
		gravity = Gravity.CENTER
		setPadding(px(12), px(11), px(12), px(11))
		background = GradientDrawable().apply { cornerRadius = px(12).toFloat(); setColor(ACCENT) }
		setOnClickListener { onClick() }
	}
}
