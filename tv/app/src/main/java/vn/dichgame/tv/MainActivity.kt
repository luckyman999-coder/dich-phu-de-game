// SPDX-License-Identifier: AGPL-3.0-only
// Dịch Game TV - màn hình hướng dẫn trên TV: mã ghép đôi, trạng thái, cấp quyền, thử chữ

package vn.dichgame.tv

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity: Activity()
{
	private val BG = Color.rgb(14, 17, 22)
	private val CARD = Color.rgb(24, 28, 36)
	private val STROKE = Color.rgb(40, 46, 58)
	private val TEXT = Color.rgb(236, 239, 244)
	private val MUTED = Color.rgb(140, 148, 162)
	private val ACCENT = Color.rgb(45, 212, 167)
	private val AMBER = Color.rgb(245, 180, 40)

	private lateinit var root: LinearLayout
	private val dp get() = resources.displayMetrics.density
	private fun px(v: Int) = (v * dp).toInt()

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		val scroll = ScrollView(this).apply { setBackgroundColor(BG) }
		root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(px(56), px(32), px(56), px(32))
		}
		scroll.addView(root)
		setContentView(scroll)
	}

	override fun onResume()
	{
		super.onResume()
		if(canOverlay()) SubtitleService.start(this)
		SubtitleService.onStateChanged = { render() }
		render()
	}

	override fun onPause()
	{
		super.onPause()
		SubtitleService.onStateChanged = null
	}

	private fun canOverlay() = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)

	private fun render()
	{
		root.removeAllViews()
		val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		header.addView(text("Dịch", 30f, TEXT, true))
		header.addView(text("Game TV", 30f, BG, true).apply {
			setPadding(px(10), 0, px(10), 0)
			background = round(ACCENT, 10)
		}, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = px(8) })
		root.addView(header)
		root.addView(text("Hiện phụ đề tiếng Việt từ app Dịch Game trên điện thoại, đè lên hình PS5.", 18f, MUTED).apply { setPadding(0, px(6), 0, px(20)) })

		val cols = LinearLayout(this)
		// cột trái: mã ghép đôi + trạng thái
		val left = card().apply { setPadding(px(24), px(20), px(24), px(20)) }
		left.addView(text("MÃ GHÉP ĐÔI", 16f, MUTED))
		left.addView(text(SubtitleService.pairingCode(this).toCharArray().joinToString(" "), 64f, ACCENT, true))
		left.addView(text("Tên TV: ${Build.MODEL}", 16f, TEXT).apply { setPadding(0, px(8), 0, 0) })
		left.addView(text("Địa chỉ: ${localIp() ?: "chưa có Wi-Fi"} : ${SubtitleService.PORT}", 16f, TEXT))
		val phone = SubtitleService.connectedPhone
		val (stColor, stText) = when
		{
			!canOverlay() -> AMBER to "Cần cấp quyền hiện chữ (bước 1)"
			SubtitleService.lastError != null -> AMBER to SubtitleService.lastError!!
			phone != null -> ACCENT to "Đã kết nối: $phone"
			SubtitleService.running -> MUTED to "Đang chờ điện thoại kết nối…"
			else -> MUTED to "Đang khởi động…"
		}
		left.addView(text("● $stText", 18f, stColor, true).apply { setPadding(0, px(14), 0, 0) })
		cols.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = px(20) })

		// cột phải: hướng dẫn 3 bước
		val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
		step(right, 1, "Cho phép hiện chữ trên màn hình",
			if(canOverlay()) "✓ Đã cấp quyền." else "Bấm nút dưới → chọn Dịch Game TV → Cho phép.",
			if(canOverlay()) null else "Cấp quyền" to { openOverlaySettings() })
		step(right, 2, "Ghép đôi với điện thoại",
			"Trên điện thoại: Dịch Game → Cài đặt → Kết nối TV → chọn TV này → nhập mã bên trái. Điện thoại và TV phải cùng Wi-Fi.", null)
		step(right, 3, "Chuyển sang cổng HDMI của PS5 và chơi",
			"App chạy nền, phụ đề tự hiện đè lên game. Bật TV lần sau không cần mở lại app.",
			if(canOverlay()) "Thử hiện chữ" to { testSubtitle() } else null)
		cols.addView(right, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f))
		root.addView(cols)

		root.addView(text("Mã nguồn mở theo giấy phép AGPL-3.0 · github.com/luckyman999-coder/dich-phu-de-game", 14f, MUTED).apply {
			setPadding(0, px(24), 0, 0)
		})
	}

	private fun step(parent: LinearLayout, n: Int, title: String, desc: String, action: Pair<String, () -> Unit>?)
	{
		val c = card().apply { setPadding(px(20), px(16), px(20), px(16)) }
		val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		row.addView(text("$n", 20f, BG, true).apply {
			gravity = Gravity.CENTER
			background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(ACCENT) }
		}, LinearLayout.LayoutParams(px(36), px(36)).apply { marginEnd = px(14) })
		row.addView(text(title, 20f, TEXT, true))
		c.addView(row)
		c.addView(text(desc, 16f, MUTED).apply { setPadding(px(50), px(4), 0, 0) })
		if(action != null)
		{
			val b = text(action.first, 18f, BG, true).apply {
				gravity = Gravity.CENTER
				setPadding(px(24), px(10), px(24), px(10))
				isFocusable = true
				isClickable = true
				background = StateListDrawable().apply {
					addState(intArrayOf(android.R.attr.state_focused), round(Color.WHITE, 10))
					addState(intArrayOf(), round(ACCENT, 10))
				}
				setOnClickListener { action.second() }
			}
			c.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
				topMargin = px(10); marginStart = px(50)
			})
			b.post { if(!root.hasFocus()) b.requestFocus() }
		}
		parent.addView(c, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = px(12) })
	}

	private fun openOverlaySettings()
	{
		val intents = listOf(
			Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
			Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
			Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
		for(i in intents)
		{
			try { startActivity(i); return } catch(_: Exception) {}
		}
		Toast.makeText(this, "TV này không có màn hình cấp quyền. Vào Cài đặt → Ứng dụng → Quyền đặc biệt → Hiển thị trên ứng dụng khác.", Toast.LENGTH_LONG).show()
	}

	private fun testSubtitle()
	{
		SubtitleService.start(this)
		val hook = SubtitleService.testHook
		if(hook == null) { Toast.makeText(this, "Dịch vụ đang khởi động, thử lại sau 2 giây", Toast.LENGTH_SHORT).show(); return }
		hook("Chúng ta sẽ tìm ra cách. Cùng nhau.")
		Toast.makeText(this, "Đã hiện chữ thử ở đáy màn hình. Chuyển sang cổng HDMI để xem chữ đè lên game.", Toast.LENGTH_LONG).show()
	}

	private fun localIp(): String? = try
	{
		NetworkInterface.getNetworkInterfaces().toList()
			.filter { it.isUp && !it.isLoopback }
			.flatMap { it.inetAddresses.toList() }
			.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
	}
	catch(_: Exception) { null }

	private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
		text = t
		textSize = size
		setTextColor(color)
		if(bold) typeface = Typeface.DEFAULT_BOLD
	}

	private fun round(color: Int, r: Int) = GradientDrawable().apply { cornerRadius = px(r).toFloat(); setColor(color) }

	private fun card() = LinearLayout(this).apply {
		orientation = LinearLayout.VERTICAL
		background = GradientDrawable().apply { cornerRadius = px(18).toFloat(); setColor(CARD); setStroke(px(1), STROKE) }
	}
}
