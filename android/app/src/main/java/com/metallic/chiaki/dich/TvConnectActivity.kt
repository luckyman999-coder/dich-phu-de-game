// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - kết nối app Dịch Game TV: tìm TV trong mạng, nhập mã ghép đôi

package com.metallic.chiaki.dich

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.metallic.chiaki.dich.HomeActivity.Companion.ACCENT
import com.metallic.chiaki.dich.HomeActivity.Companion.AMBER
import com.metallic.chiaki.dich.HomeActivity.Companion.BG
import com.metallic.chiaki.dich.HomeActivity.Companion.CARD
import com.metallic.chiaki.dich.HomeActivity.Companion.MUTED
import com.metallic.chiaki.dich.HomeActivity.Companion.STROKE
import com.metallic.chiaki.dich.HomeActivity.Companion.TEXT
import kotlinx.coroutines.launch

class TvConnectActivity: AppCompatActivity()
{
	companion object
	{
		const val TV_APK_URL = "https://github.com/luckyman999-coder/dich-phu-de-game/releases/download/tv-moi-nhat/Dich-Game-TV.apk"
	}

	private lateinit var prefs: DichPrefs
	private lateinit var root: LinearLayout
	private var finder: TvFinder? = null
	private val found = LinkedHashMap<String, FoundTv>()
	private var message: String? = null

	private val dp get() = resources.displayMetrics.density
	private fun px(v: Int) = (v * dp).toInt()

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		prefs = DichPrefs(this)
		window.statusBarColor = BG
		window.navigationBarColor = BG
		val scroll = ScrollView(this).apply { setBackgroundColor(BG) }
		root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(px(20), px(20), px(20), px(40)) }
		scroll.addView(root)
		setContentView(scroll)
		render()
	}

	override fun onResume()
	{
		super.onResume()
		finder = TvFinder(this) { tv -> runOnUiThread { found[tv.host] = tv; render() } }.also { it.start() }
	}

	override fun onPause()
	{
		super.onPause()
		finder?.stop()
		finder = null
	}

	private fun render()
	{
		root.removeAllViews()
		val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		header.addView(text("‹", 28f, TEXT).apply { setPadding(0, 0, px(14), 0); setOnClickListener { finish() } })
		header.addView(text("Kết nối TV", 22f, TEXT, true))
		root.addView(header)
		root.addView(text("Phụ đề tiếng Việt hiện đè lên hình PS5 trên TV, giọng đọc phát ra loa TV. Hỗ trợ Android TV / Google TV.", 14f, MUTED).apply { setPadding(0, px(4), 0, 0) })

		// TV đã ghép đôi
		root.addView(section("TV CỦA BẠN"))
		val paired = card().apply { setPadding(px(16), px(14), px(16), px(14)) }
		if(prefs.tvToken.isEmpty())
			paired.addView(text("Chưa ghép đôi TV nào", 15f, MUTED))
		else
		{
			paired.addView(text(prefs.tvName.ifEmpty { prefs.tvHost }, 16f, TEXT, true))
			paired.addView(text("${prefs.tvHost}:${prefs.tvPort}", 13f, MUTED))
			val sw = SwitchMaterial(this).apply {
				text = "Đọc thoại bằng loa TV (điện thoại im)"
				setTextColor(TEXT)
				isChecked = prefs.speakOnTv
				setOnCheckedChangeListener { _, c -> prefs.speakOnTv = c }
			}
			paired.addView(sw)
			paired.addView(text("Huỷ ghép đôi", 14f, AMBER).apply {
				setPadding(0, px(8), 0, 0)
				setOnClickListener { prefs.tvToken = ""; prefs.tvHost = ""; prefs.tvName = ""; render() }
			})
		}
		root.addView(paired)

		// TV tìm thấy
		root.addView(section("TV TRONG MẠNG WI-FI NHÀ"))
		val list = card()
		if(found.isEmpty())
			list.addView(text("Đang tìm… Mở app Dịch Game TV trên TV và để TV, điện thoại cùng Wi-Fi.", 14f, MUTED).apply { setPadding(px(16), px(16), px(16), px(16)) })
		found.values.forEachIndexed { i, tv ->
			val row = LinearLayout(this).apply {
				gravity = Gravity.CENTER_VERTICAL
				setPadding(px(16), px(14), px(16), px(14))
				setOnClickListener { askCode(tv) }
			}
			val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
			col.addView(text("📺  " + tv.name, 16f, TEXT))
			col.addView(text(tv.host, 13f, MUTED))
			row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
			row.addView(text(if(tv.host == prefs.tvHost && prefs.tvToken.isNotEmpty()) "Đã ghép" else "Ghép đôi ›", 14f, ACCENT))
			list.addView(row)
			if(i < found.size - 1) list.addView(View(this).apply { setBackgroundColor(STROKE) }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
		}
		root.addView(list)
		root.addView(text("Không thấy TV? Nhập địa chỉ IP hiện trên TV", 14f, ACCENT).apply {
			setPadding(px(4), px(12), 0, 0)
			setOnClickListener { askIp() }
		})

		message?.let { root.addView(text(it, 15f, if(it.startsWith("✅")) ACCENT else AMBER).apply { setPadding(0, px(16), 0, 0) }) }

		// hướng dẫn cài app trên TV
		root.addView(section("CÀI APP DỊCH GAME TV LÊN TV"))
		val guide = card().apply { setPadding(px(16), px(14), px(16), px(14)) }
		listOf(
			"Trên TV, mở Google Play và cài app Downloader (của AFTVnews).",
			"Mở Downloader, nhập đường dẫn bên dưới rồi bấm Go. Cho phép Downloader cài ứng dụng khi TV hỏi.",
			"Mở Dịch Game TV → bấm \"Cấp quyền\" để cho hiện chữ trên màn hình.",
			"TV hiện mã 4 số. Quay lại đây, chọn TV và nhập mã."
		).forEachIndexed { i, s -> guide.addView(text("${i + 1}.  $s", 14f, TEXT).apply { setPadding(0, px(4), 0, px(4)) }) }
		guide.addView(text(TV_APK_URL, 13f, ACCENT).apply {
			typeface = Typeface.MONOSPACE
			setPadding(0, px(8), 0, 0)
			setOnClickListener {
				val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
				cm.setPrimaryClip(android.content.ClipData.newPlainText("Dịch Game TV", TV_APK_URL))
				Toast.makeText(this@TvConnectActivity, "Đã copy đường dẫn", Toast.LENGTH_SHORT).show()
			}
		})
		guide.addView(text("TV Samsung và LG chưa hỗ trợ — dùng màn hình điện thoại trong lúc chờ.", 13f, MUTED).apply { setPadding(0, px(10), 0, 0) })
		root.addView(guide)
	}

	private fun askCode(tv: FoundTv)
	{
		val input = EditText(this).apply {
			hint = "Mã 4 số trên TV"
			inputType = InputType.TYPE_CLASS_NUMBER
		}
		val box = FrameLayout(this).apply { setPadding(px(20), px(8), px(20), 0); addView(input) }
		MaterialAlertDialogBuilder(this)
			.setTitle("Ghép đôi ${tv.name}")
			.setView(box)
			.setPositiveButton("Ghép đôi") { _, _ ->
				message = "Đang ghép đôi…"
				render()
				lifecycleScope.launch {
					val err = TvLink.pair(this@TvConnectActivity, tv, input.text.toString())
					message = err?.let { "❌ $it" } ?: "✅ Đã ghép đôi! Vào game là phụ đề hiện trên TV."
					render()
				}
			}
			.setNegativeButton("Huỷ", null)
			.show()
	}

	private fun askIp()
	{
		val input = EditText(this).apply { hint = "Ví dụ 192.168.1.25"; inputType = InputType.TYPE_CLASS_TEXT }
		val box = FrameLayout(this).apply { setPadding(px(20), px(8), px(20), 0); addView(input) }
		MaterialAlertDialogBuilder(this)
			.setTitle("Địa chỉ IP của TV")
			.setView(box)
			.setPositiveButton("Tiếp") { _, _ ->
				val ip = input.text.toString().trim()
				if(ip.isNotEmpty()) askCode(FoundTv("TV $ip", ip, TvLink.PORT))
			}
			.setNegativeButton("Huỷ", null)
			.show()
	}

	private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
		text = t
		textSize = size
		setTextColor(color)
		if(bold) setTypeface(typeface, Typeface.BOLD)
	}

	private fun section(t: String) = text(t, 13f, MUTED).apply { setPadding(px(4), px(24), 0, px(8)) }

	private fun card() = LinearLayout(this).apply {
		orientation = LinearLayout.VERTICAL
		background = GradientDrawable().apply { cornerRadius = px(18).toFloat(); setColor(CARD); setStroke(px(1), STROKE) }
	}
}
