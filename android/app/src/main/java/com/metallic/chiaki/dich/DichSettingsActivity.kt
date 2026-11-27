// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - màn hình cài đặt (mã dịch, game, giọng đọc)

package com.metallic.chiaki.dich

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class DichSettingsActivity: AppCompatActivity()
{
	private lateinit var prefs: DichPrefs
	private lateinit var root: LinearLayout

	private lateinit var gemini1: EditText
	private lateinit var gemini2: EditText
	private lateinit var gemini3: EditText
	private lateinit var groq: EditText
	private lateinit var geminiModel: EditText
	private lateinit var groqModel: EditText
	private lateinit var speakSwitch: SwitchMaterial
	private lateinit var muteSwitch: SwitchMaterial
	private lateinit var originalSwitch: SwitchMaterial
	private lateinit var rateSeek: SeekBar
	private lateinit var testResult: TextView

	private val dp get() = resources.displayMetrics.density

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		title = "Cài đặt dịch phụ đề"
		prefs = DichPrefs(this)

		val scroll = ScrollView(this)
		root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			val p = (16 * dp).toInt()
			setPadding(p, p, p, p * 3)
		}
		scroll.addView(root)
		setContentView(scroll)

		header("1. Mã dịch miễn phí")
		note("Gemini: tối đa 3 mã, hết lượt mã này tự chuyển mã khác. Groq dùng dự phòng khi cả 3 mã Gemini hết lượt.")
		button("🧭  Hướng dẫn lấy mã từng bước") { startActivity(Intent(this, KeyGuideActivity::class.java)) }
		val keys = prefs.geminiKeys
		gemini1 = field("Mã Gemini 1", keys.getOrElse(0) { "" }, secret = true)
		gemini2 = field("Mã Gemini 2", keys.getOrElse(1) { "" }, secret = true)
		gemini3 = field("Mã Gemini 3", keys.getOrElse(2) { "" }, secret = true)
		link("Lấy mã Gemini (Google AI Studio)", "https://aistudio.google.com/apikey")
		groq = field("Mã Groq", prefs.groqKey, secret = true)
		link("Lấy mã Groq", "https://console.groq.com/keys")
		geminiModel = field("Model Gemini", prefs.geminiModel)
		groqModel = field("Model Groq", prefs.groqModel)

		button("Lưu và dịch thử") { save(); testTranslate() }
		testResult = TextView(this).apply { setPadding(0, (8 * dp).toInt(), 0, 0) }
		root.addView(testResult)

		header("2. Game đang chơi")
		note("Tên nhân vật, giới tính, cách xưng hô, văn phong được lưu trong Sổ tay game.")
		button("📒  Mở Sổ tay game") { startActivity(Intent(this, GameBookActivity::class.java)) }

		header("3. Hiển thị & giọng đọc")
		speakSwitch = switch("Đọc to phụ đề tiếng Việt", prefs.speak)
		muteSwitch = switch("Tắt tiếng game trên điện thoại (TV đã có tiếng)", prefs.muteGame)
		originalSwitch = switch("Hiện cả câu tiếng Anh gốc", prefs.showOriginal)
		note("Tốc độ đọc")
		rateSeek = SeekBar(this).apply {
			max = 15 // 0.5 .. 2.0
			progress = ((prefs.speechRate - 0.5f) * 10).toInt().coerceIn(0, 15)
		}
		root.addView(rateSeek)
		button("Cài giọng đọc tiếng Việt cho máy") {
			try { startActivity(Intent("android.speech.tts.engine.INSTALL_TTS_DATA")) }
			catch(e: Exception) { Toast.makeText(this, "Vào Cài đặt > Chuyển văn bản thành giọng nói", Toast.LENGTH_LONG).show() }
		}

		header("4. TV")
		note("Hiện phụ đề đè lên hình PS5 trên Android TV / Google TV, giọng đọc ra loa TV.")
		button("📺  Kết nối TV") { startActivity(Intent(this, TvConnectActivity::class.java)) }

		header("5. Ghép đôi PS5")
		note("Ở màn hình chính bấm nút + → Đăng ký máy (Register), chọn PS5. Account ID có thể lấy bằng cách đăng nhập PSN (nên dùng tài khoản PSN thứ 2). Mã PIN lấy trên PS5: Cài đặt → Hệ thống → Chơi từ xa → Liên kết thiết bị.")
		button("Đăng nhập PSN lấy Account ID") {
			startActivity(Intent(this, PsnLoginActivity::class.java))
		}
		if(prefs.psnAccountId.isNotEmpty())
			note("Account ID đã lưu: ${prefs.psnAccountId}")

		header("Khác")
		button("📖  Hướng dẫn") { startActivity(Intent(this, GuideActivity::class.java)) }
		button("ℹ️  Giới thiệu & quyền riêng tư") { startActivity(Intent(this, AboutActivity::class.java)) }

		button("Lưu") { save(); finish() }
	}

	override fun onResume()
	{
		super.onResume()
		// có thể vừa nhập mã ở màn hình hướng dẫn
		val keys = prefs.geminiKeys
		gemini1.setText(keys.getOrElse(0) { "" })
		gemini2.setText(keys.getOrElse(1) { "" })
		gemini3.setText(keys.getOrElse(2) { "" })
		groq.setText(prefs.groqKey)
	}

	override fun onPause()
	{
		super.onPause()
		save()
	}

	private fun save()
	{
		prefs.geminiKeys = listOf(gemini1, gemini2, gemini3).map { it.text.toString().trim() }.filter { it.isNotEmpty() }
		prefs.groqKey = groq.text.toString()
		prefs.geminiModel = geminiModel.text.toString()
		prefs.groqModel = groqModel.text.toString()
		prefs.speak = speakSwitch.isChecked
		prefs.muteGame = muteSwitch.isChecked
		prefs.showOriginal = originalSwitch.isChecked
		prefs.speechRate = 0.5f + rateSeek.progress / 10f
	}

	private fun testTranslate()
	{
		if(!prefs.hasAnyKey)
		{
			testResult.text = "Chưa nhập mã nào."
			return
		}
		testResult.text = "Đang dịch thử…"
		val translator = Translator(prefs)
		lifecycleScope.launch {
			val t = translator.translate("We'll find a way. Together.")
			testResult.text = if(t != null) "✅ Dịch được: ${t.vietnamese}"
				else "❌ Lỗi: ${translator.lastError ?: "không rõ"} – kiểm tra lại mã hoặc tên model."
		}
	}

	// ---- dựng giao diện ----

	private fun header(text: String)
	{
		root.addView(TextView(this).apply {
			this.text = text
			textSize = 18f
			setTypeface(typeface, android.graphics.Typeface.BOLD)
			setPadding(0, (24 * dp).toInt(), 0, (4 * dp).toInt())
		})
	}

	private fun note(text: String)
	{
		root.addView(TextView(this).apply {
			this.text = text
			alpha = 0.75f
			setPadding(0, (4 * dp).toInt(), 0, (4 * dp).toInt())
		})
	}

	private fun field(hint: String, value: String, secret: Boolean = false, multiline: Boolean = false): EditText
	{
		val layout = TextInputLayout(this).apply {
			this.hint = hint
			if(secret) endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
		}
		val edit = TextInputEditText(layout.context).apply {
			setText(value)
			inputType = when
			{
				secret -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
				multiline -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
				else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
			}
			if(multiline)
			{
				minLines = 4
				gravity = Gravity.TOP or Gravity.START
			}
			else
				maxLines = 1
		}
		layout.addView(edit)
		root.addView(layout, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			.apply { topMargin = (8 * dp).toInt() })
		return edit
	}

	private fun switch(text: String, checked: Boolean): SwitchMaterial
	{
		val s = SwitchMaterial(this).apply {
			this.text = text
			isChecked = checked
		}
		root.addView(s)
		return s
	}

	private fun button(text: String, onClick: () -> Unit)
	{
		val b = MaterialButton(this).apply {
			this.text = text
			setOnClickListener { onClick() }
		}
		root.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			.apply { topMargin = (8 * dp).toInt() })
	}

	private fun link(text: String, url: String)
	{
		button("🔗 $text") {
			try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
			catch(e: Exception) { Toast.makeText(this, url, Toast.LENGTH_LONG).show() }
		}
	}
}
