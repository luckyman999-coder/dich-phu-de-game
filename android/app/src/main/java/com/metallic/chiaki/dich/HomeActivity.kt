// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Phụ Đề Game - màn hình chính một chạm: BẮT ĐẦU CHƠI

package com.metallic.chiaki.dich

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.metallic.chiaki.common.DiscoveredDisplayHost
import com.metallic.chiaki.common.DisplayHost
import com.metallic.chiaki.common.Preferences
import com.metallic.chiaki.common.ext.viewModelFactory
import com.metallic.chiaki.common.getDatabase
import com.metallic.chiaki.lib.ConnectInfo
import com.metallic.chiaki.lib.DiscoveryHost
import com.metallic.chiaki.main.MainActivity
import com.metallic.chiaki.main.MainViewModel
import com.metallic.chiaki.regist.RegistActivity
import com.metallic.chiaki.stream.StreamActivity

class HomeActivity: AppCompatActivity()
{
	companion object
	{
		private const val SEARCH_TIMEOUT_MS = 10_000L
		private val PURPLE = Color.rgb(108, 99, 255)
		private val PURPLE_2 = Color.rgb(139, 92, 246)
	}

	private lateinit var prefs: DichPrefs
	private lateinit var viewModel: MainViewModel
	private var hosts: List<DisplayHost> = emptyList()

	private lateinit var statusDot: View
	private lateinit var statusText: TextView
	private lateinit var gameText: TextView
	private lateinit var startButton: TextView
	private lateinit var startSub: TextView

	private val handler = Handler(Looper.getMainLooper())
	private var searching = false
	private var wakingUp = false

	private val dp get() = resources.displayMetrics.density
	private fun px(v: Int) = (v * dp).toInt()

	private val psnLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
		if(prefs.psnAccountId.isNotEmpty()) onStartPressed()
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		prefs = DichPrefs(this)
		viewModel = ViewModelProvider(this, viewModelFactory { MainViewModel(getDatabase(this), Preferences(this)) })[MainViewModel::class.java]
		viewModel.discoveryManager.active = true
		buildUi()
		viewModel.displayHosts.observe(this) {
			hosts = it
			updateStatus()
			if(searching || wakingUp) tryConnect(fromSearch = true)
		}
	}

	override fun onResume()
	{
		super.onResume()
		viewModel.discoveryManager.active = true
		gameText.text = prefs.gameName.ifEmpty { "Chung · Tự động" } + "  ›"
		updateStatus()
	}

	override fun onPause()
	{
		super.onPause()
		stopSearching()
	}

	// ---------------- luồng một chạm ----------------

	private fun onStartPressed()
	{
		// 1. chưa có mã dịch → vào cài đặt
		if(!prefs.hasAnyKey)
		{
			MaterialAlertDialogBuilder(this)
				.setTitle("Thêm mã dịch miễn phí")
				.setMessage("Cần ít nhất 1 mã Gemini (miễn phí) để dịch. Có hướng dẫn lấy mã ngay trong Cài đặt.\n\nMuốn xem thử trước thì bấm \"Xem thử\".")
				.setPositiveButton("Vào cài đặt") { _, _ -> startActivity(Intent(this, DichSettingsActivity::class.java)) }
				.setNeutralButton("Xem thử") { _, _ -> startActivity(Intent(this, DemoActivity::class.java)) }
				.show()
			return
		}
		tryConnect(fromSearch = false)
	}

	private fun tryConnect(fromSearch: Boolean)
	{
		val registered = hosts.filter { it.isRegistered }
		val ready = registered.firstOrNull { it !is DiscoveredDisplayHost || it.discoveredHost.state == DiscoveryHost.State.READY }
		if(ready != null && (ready is DiscoveredDisplayHost || !fromSearch))
		{
			stopSearching()
			connect(ready)
			return
		}
		val standby = registered.firstOrNull { it is DiscoveredDisplayHost && it.discoveredHost.state == DiscoveryHost.State.STANDBY }
		if(standby != null)
		{
			if(!wakingUp)
			{
				wakingUp = true
				val rh = standby.registeredHost!!
				viewModel.discoveryManager.sendWakeup(standby.host, rh.rpRegistKey, rh.target.isPS5)
				setBusy("Đang đánh thức máy chơi game…")
				startSearching()
			}
			return
		}
		// chưa ghép đôi: tìm thấy máy nhưng chưa đăng ký
		val unregistered = hosts.firstOrNull { it is DiscoveredDisplayHost && !it.isRegistered && it.isPS5 }
			?: hosts.firstOrNull { it is DiscoveredDisplayHost && !it.isRegistered }
		if(unregistered != null)
		{
			stopSearching()
			pair(unregistered)
			return
		}
		if(!fromSearch) startSearching()
	}

	private fun pair(host: DisplayHost)
	{
		if(prefs.psnAccountId.isEmpty())
		{
			MaterialAlertDialogBuilder(this)
				.setTitle("Tìm thấy ${host.name ?: "máy chơi game"}")
				.setMessage("Lần đầu cần đăng nhập PSN để ghép đôi (nên dùng tài khoản PSN thứ 2).")
				.setPositiveButton("Đăng nhập PSN") { _, _ ->
					psnLogin.launch(Intent(this, PsnLoginActivity::class.java))
				}
				.setNegativeButton("Để sau", null)
				.show()
			return
		}
		MaterialAlertDialogBuilder(this)
			.setTitle("Ghép đôi ${host.name ?: "máy chơi game"}")
			.setMessage("Trên máy chơi game vào: Cài đặt → Hệ thống → Chơi từ xa → Liên kết thiết bị. Máy sẽ hiện mã 8 số, nhập mã đó ở bước sau.")
			.setPositiveButton("Tiếp tục") { _, _ ->
				startActivity(Intent(this, RegistActivity::class.java)
					.putExtra(RegistActivity.EXTRA_HOST, host.host)
					.putExtra(RegistActivity.EXTRA_BROADCAST, false))
			}
			.setNegativeButton("Huỷ", null)
			.show()
	}

	private fun connect(host: DisplayHost)
	{
		val rh = host.registeredHost ?: return
		val info = ConnectInfo(host.isPS5, host.host, rh.rpRegistKey, rh.rpKey, Preferences(this).videoProfile)
		startActivity(Intent(this, StreamActivity::class.java).putExtra(StreamActivity.EXTRA_CONNECT_INFO, info))
	}

	private val searchTimeout = Runnable {
		val wasWaking = wakingUp
		stopSearching()
		MaterialAlertDialogBuilder(this)
			.setTitle(if(wasWaking) "Máy chơi game chưa thức dậy" else "Chưa tìm thấy máy chơi game")
			.setMessage("• Điện thoại và máy chơi game phải cùng Wi-Fi nhà\n• Trên máy chơi game: Cài đặt → Hệ thống → Chơi từ xa → Bật\n• Bật chế độ nghỉ: cho phép bật máy từ mạng\n\nVẫn không được thì nhập địa chỉ IP thủ công.")
			.setPositiveButton("Thử lại") { _, _ -> onStartPressed() }
			.setNeutralButton("Nhập IP thủ công") { _, _ -> startActivity(Intent(this, MainActivity::class.java)) }
			.setNegativeButton("Đóng", null)
			.show()
	}

	private fun startSearching()
	{
		searching = true
		viewModel.discoveryManager.active = true
		if(!wakingUp) setBusy("Đang tìm máy chơi game…")
		handler.removeCallbacks(searchTimeout)
		handler.postDelayed(searchTimeout, if(wakingUp) 25_000L else SEARCH_TIMEOUT_MS)
	}

	private fun stopSearching()
	{
		searching = false
		wakingUp = false
		handler.removeCallbacks(searchTimeout)
		startButton.isEnabled = true
		startSub.text = "kết nối & dịch"
		updateStatus()
	}

	private fun setBusy(msg: String)
	{
		startSub.text = msg
		startButton.isEnabled = false
	}

	private fun updateStatus()
	{
		val registered = hosts.filter { it.isRegistered }
		val online = registered.any { it is DiscoveredDisplayHost && it.discoveredHost.state == DiscoveryHost.State.READY }
		val (color, text) = when
		{
			online -> Color.rgb(34, 197, 94) to "Máy chơi game sẵn sàng"
			registered.isNotEmpty() -> Color.rgb(234, 179, 8) to "Máy chơi game đang nghỉ hoặc tắt"
			hosts.isNotEmpty() -> Color.rgb(59, 130, 246) to "Tìm thấy máy, chưa ghép đôi"
			else -> Color.rgb(156, 163, 175) to "Chưa kết nối máy chơi game"
		}
		(statusDot.background as GradientDrawable).setColor(color)
		statusText.text = text
	}

	// ---------------- giao diện ----------------

	private fun buildUi()
	{
		window.statusBarColor = Color.rgb(238, 240, 255)
		val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(238, 240, 255)); isFillViewport = true }
		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(px(20), px(20), px(20), px(32))
		}
		scroll.addView(root)

		// tiêu đề + chấm trạng thái + bánh răng
		val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		header.addView(TextView(this).apply {
			text = "Dịch Phụ Đề Game"
			textSize = 24f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(Color.rgb(17, 24, 39))
		}, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		statusDot = View(this).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.GRAY) } }
		header.addView(statusDot, LinearLayout.LayoutParams(px(12), px(12)).apply { marginEnd = px(14) })
		header.addView(ImageButton(this).apply {
			setImageResource(com.metallic.chiaki.R.drawable.ic_settings)
			setColorFilter(Color.rgb(17, 24, 39))
			background = null
			contentDescription = "Cài đặt"
			setOnClickListener { startActivity(Intent(this@HomeActivity, DichSettingsActivity::class.java)) }
		}, LinearLayout.LayoutParams(px(40), px(40)))
		root.addView(header)

		// chọn game
		gameText = TextView(this).apply {
			textSize = 15f
			setTextColor(Color.rgb(75, 85, 99))
			setPadding(0, px(4), 0, 0)
			setOnClickListener { pickGame() }
		}
		root.addView(gameText)
		statusText = TextView(this).apply { textSize = 13f; setTextColor(Color.rgb(107, 114, 128)) }
		root.addView(statusText)

		// nút tròn BẮT ĐẦU CHƠI
		val circleSize = px(230)
		val circle = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			gravity = Gravity.CENTER
			background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(PURPLE, PURPLE_2)).apply { shape = GradientDrawable.OVAL }
			elevation = px(8).toFloat()
			isClickable = true
			isFocusable = true
		}
		startButton = TextView(this).apply {
			text = "BẮT ĐẦU CHƠI"
			textSize = 26f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(Color.WHITE)
			gravity = Gravity.CENTER
		}
		startSub = TextView(this).apply {
			text = "kết nối & dịch"
			textSize = 15f
			setTextColor(Color.argb(220, 255, 255, 255))
			gravity = Gravity.CENTER
			setPadding(px(16), px(4), px(16), 0)
		}
		circle.addView(startButton)
		circle.addView(startSub)
		circle.setOnClickListener { if(startButton.isEnabled) onStartPressed() }
		root.addView(circle, LinearLayout.LayoutParams(circleSize, circleSize).apply {
			gravity = Gravity.CENTER_HORIZONTAL
			topMargin = px(40)
			bottomMargin = px(28)
		})

		// nút Xem thử
		root.addView(TextView(this).apply {
			text = "▶  Xem thử – không cần máy chơi game"
			textSize = 15f
			setTextColor(PURPLE)
			gravity = Gravity.CENTER
			setPadding(px(16), px(12), px(16), px(12))
			background = GradientDrawable().apply { cornerRadius = px(24).toFloat(); setColor(Color.WHITE) }
			setOnClickListener { startActivity(Intent(this@HomeActivity, DemoActivity::class.java)) }
		}, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

		// thẻ mẹo
		val tips = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(px(16), px(14), px(16), px(14))
			background = GradientDrawable().apply { cornerRadius = px(16).toFloat(); setColor(Color.argb(200, 255, 255, 255)) }
		}
		listOf(
			"Lần đầu: bấm BẮT ĐẦU CHƠI → đăng nhập PSN → nhập mã ghép đôi hiện trên máy chơi game",
			"Điện thoại và máy chơi game nên cùng Wi-Fi 5GHz cho hình mượt",
			"Trong game: vuốt mép màn hình → \"Chỉnh khung phụ đề\" để kéo khung trùm phụ đề",
			"Cắm sạc khi chơi lâu"
		).forEach { tip ->
			tips.addView(TextView(this).apply {
				text = "💡  $tip"
				textSize = 14f
				setTextColor(Color.rgb(75, 85, 99))
				setPadding(0, px(4), 0, px(4))
			})
		}
		root.addView(tips, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(20) })

		setContentView(scroll)
	}

	private fun pickGame()
	{
		val names = arrayOf("Chung · Tự động") + GamePresets.all.map { it.name }.toTypedArray()
		MaterialAlertDialogBuilder(this)
			.setTitle("Đang chơi game nào?")
			.setItems(names) { _, which ->
				if(which == 0)
				{
					prefs.gameName = ""
					prefs.gameNotes = ""
				}
				else
				{
					val p = GamePresets.all[which - 1]
					prefs.gameName = p.name
					prefs.gameNotes = p.notes
				}
				gameText.text = prefs.gameName.ifEmpty { "Chung · Tự động" } + "  ›"
			}
			.show()
	}
}
