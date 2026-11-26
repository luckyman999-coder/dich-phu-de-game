// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - màn hình chính một chạm: BẮT ĐẦU CHƠI

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
		// Bảng màu riêng của Dịch Game: nền tối, nhấn xanh ngọc
		val BG = Color.rgb(14, 17, 22)
		val CARD = Color.rgb(24, 28, 36)
		val STROKE = Color.rgb(40, 46, 58)
		val TEXT = Color.rgb(236, 239, 244)
		val MUTED = Color.rgb(140, 148, 162)
		val ACCENT = Color.rgb(45, 212, 167)
		val GREEN = Color.rgb(34, 197, 94)
		val AMBER = Color.rgb(245, 180, 40)
		val BLUE = Color.rgb(96, 165, 250)
	}

	private lateinit var prefs: DichPrefs
	private lateinit var viewModel: MainViewModel
	private var hosts: List<DisplayHost> = emptyList()

	private lateinit var statusDot: View
	private lateinit var statusText: TextView
	private lateinit var gameText: TextView
	private lateinit var startButton: TextView
	private lateinit var startSub: TextView
	private lateinit var consoleName: TextView
	private lateinit var checklist: LinearLayout

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
		gameText.text = "🎮  " + (GameBook(this).current?.let { "${it.name} · ${it.entries.size} mục" } ?: "Chung · tự nhận diện")
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
				.setPositiveButton("Lấy mã ngay") { _, _ -> startActivity(Intent(this, KeyGuideActivity::class.java)) }
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
		val online = registered.firstOrNull { it is DiscoveredDisplayHost && it.discoveredHost.state == DiscoveryHost.State.READY }
		val (color, text) = when
		{
			online != null -> GREEN to "Sẵn sàng · cùng Wi-Fi"
			registered.isNotEmpty() -> AMBER to "Đang nghỉ hoặc tắt · bấm Chơi để đánh thức"
			hosts.isNotEmpty() -> BLUE to "Tìm thấy máy · chưa ghép đôi"
			else -> MUTED to "Chưa thấy máy chơi game"
		}
		(statusDot.background as GradientDrawable).setColor(color)
		statusText.text = text
		consoleName.text = (online ?: registered.firstOrNull() ?: hosts.firstOrNull())?.name ?: "Máy chơi game của bạn"
		refreshChecklist()
	}

	private fun refreshChecklist()
	{
		if(!::checklist.isInitialized) return
		checklist.removeAllViews()
		val paired = hosts.any { it.isRegistered }
		addCheck("Mã dịch AI", prefs.hasAnyKey, if(prefs.hasAnyKey) "${prefs.geminiKeys.size} mã Gemini" + (if(prefs.groqKey.isNotEmpty()) " + Groq" else "") else "Lấy mã miễn phí – 2 phút") {
			startActivity(Intent(this, if(prefs.hasAnyKey) DichSettingsActivity::class.java else KeyGuideActivity::class.java))
		}
		addCheck("Tài khoản PSN", prefs.psnAccountId.isNotEmpty(), if(prefs.psnAccountId.isNotEmpty()) "Đã lấy Account ID" else "Đăng nhập để ghép đôi") {
			psnLogin.launch(Intent(this, PsnLoginActivity::class.java))
		}
		addCheck("Ghép đôi máy chơi game", paired, if(paired) "Đã ghép đôi" else "Bấm Chơi để tự tìm và ghép đôi") {
			onStartPressed()
		}
	}

	private fun addCheck(title: String, ok: Boolean, sub: String, onClick: () -> Unit)
	{
		val row = LinearLayout(this).apply {
			gravity = Gravity.CENTER_VERTICAL
			setPadding(px(14), px(12), px(14), px(12))
			setOnClickListener { onClick() }
		}
		row.addView(TextView(this).apply {
			text = if(ok) "✓" else "!"
			textSize = 15f
			setTypeface(typeface, Typeface.BOLD)
			gravity = Gravity.CENTER
			setTextColor(if(ok) BG else Color.BLACK)
			background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(if(ok) ACCENT else AMBER) }
		}, LinearLayout.LayoutParams(px(26), px(26)).apply { marginEnd = px(12) })
		val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
		texts.addView(TextView(this).apply { text = title; textSize = 15f; setTextColor(TEXT) })
		texts.addView(TextView(this).apply { text = sub; textSize = 13f; setTextColor(MUTED) })
		row.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		row.addView(TextView(this).apply { text = "›"; textSize = 20f; setTextColor(MUTED) })
		checklist.addView(row)
	}

	// ---------------- giao diện (nền tối, thẻ máy chơi game) ----------------

	private fun card(): LinearLayout = LinearLayout(this).apply {
		orientation = LinearLayout.VERTICAL
		background = GradientDrawable().apply { cornerRadius = px(20).toFloat(); setColor(CARD); setStroke(px(1), STROKE) }
	}

	private fun sectionTitle(t: String) = TextView(this).apply {
		text = t
		textSize = 13f
		setTextColor(MUTED)
		setPadding(px(4), px(24), 0, px(8))
	}

	private fun buildUi()
	{
		window.statusBarColor = BG
		window.navigationBarColor = BG
		val scroll = ScrollView(this).apply { setBackgroundColor(BG); isFillViewport = true }
		val root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(px(20), px(20), px(20), px(32))
		}
		scroll.addView(root)

		// thanh trên: logo chữ + cài đặt
		val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		header.addView(TextView(this).apply {
			text = "Dịch"
			textSize = 22f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(TEXT)
		})
		header.addView(TextView(this).apply {
			text = "Game"
			textSize = 22f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(BG)
			setPadding(px(8), 0, px(8), 0)
			background = GradientDrawable().apply { cornerRadius = px(8).toFloat(); setColor(ACCENT) }
		}, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = px(6) })
		header.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
		header.addView(ImageButton(this).apply {
			setImageResource(com.metallic.chiaki.R.drawable.ic_settings)
			setColorFilter(TEXT)
			background = null
			contentDescription = "Cài đặt"
			setOnClickListener { startActivity(Intent(this@HomeActivity, DichSettingsActivity::class.java)) }
		}, LinearLayout.LayoutParams(px(40), px(40)))
		root.addView(header)
		root.addView(TextView(this).apply {
			text = "Phụ đề và giọng đọc tiếng Việt cho game PS5"
			textSize = 14f
			setTextColor(MUTED)
			setPadding(0, px(4), 0, 0)
		})

		// thẻ máy chơi game
		val console = card().apply { setPadding(px(18), px(18), px(18), px(18)) }
		val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		top.addView(TextView(this).apply {
			text = "PS5"
			textSize = 14f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(TEXT)
			gravity = Gravity.CENTER
			background = GradientDrawable().apply { cornerRadius = px(12).toFloat(); setColor(STROKE) }
		}, LinearLayout.LayoutParams(px(52), px(52)).apply { marginEnd = px(14) })
		val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
		consoleName = TextView(this).apply { textSize = 17f; setTypeface(typeface, Typeface.BOLD); setTextColor(TEXT) }
		info.addView(consoleName)
		val statusRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, px(4), 0, 0) }
		statusDot = View(this).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(MUTED) } }
		statusRow.addView(statusDot, LinearLayout.LayoutParams(px(8), px(8)).apply { marginEnd = px(6) })
		statusText = TextView(this).apply { textSize = 13f; setTextColor(MUTED) }
		statusRow.addView(statusText)
		info.addView(statusRow)
		top.addView(info, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		console.addView(top)

		val play = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			gravity = Gravity.CENTER
			setPadding(0, px(14), 0, px(14))
			background = GradientDrawable().apply { cornerRadius = px(14).toFloat(); setColor(ACCENT) }
			isClickable = true
		}
		startButton = TextView(this).apply {
			text = "▶  CHƠI VÀ DỊCH"
			textSize = 18f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(BG)
			gravity = Gravity.CENTER
		}
		startSub = TextView(this).apply {
			text = "kết nối & dịch"
			textSize = 13f
			setTextColor(Color.argb(170, 0, 0, 0))
			gravity = Gravity.CENTER
		}
		play.addView(startButton)
		play.addView(startSub)
		play.setOnClickListener { if(startButton.isEnabled) onStartPressed() }
		console.addView(play, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(18) })
		console.addView(TextView(this).apply {
			text = "Chưa có máy? Xem thử bằng cảnh mẫu"
			textSize = 14f
			setTextColor(ACCENT)
			gravity = Gravity.CENTER
			setPadding(0, px(14), 0, 0)
			setOnClickListener { startActivity(Intent(this@HomeActivity, DemoActivity::class.java)) }
		})
		root.addView(console, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(20) })

		// game đang chơi
		root.addView(sectionTitle("GAME ĐANG CHƠI"))
		val gameCard = card().apply {
			setPadding(px(16), px(14), px(16), px(14))
			setOnClickListener { pickGame() }
		}
		val gameRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		gameText = TextView(this).apply { textSize = 16f; setTextColor(TEXT) }
		gameRow.addView(gameText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		gameRow.addView(TextView(this).apply { text = "Sổ tay ›"; textSize = 14f; setTextColor(ACCENT) })
		gameCard.addView(gameRow)
		gameCard.addView(TextView(this).apply {
			text = "Chọn đúng game để AI giữ tên nhân vật và cách xưng hô"
			textSize = 13f
			setTextColor(MUTED)
			setPadding(0, px(4), 0, 0)
		})
		root.addView(gameCard)

		// danh sách việc cần làm
		root.addView(sectionTitle("CHUẨN BỊ"))
		checklist = card()
		root.addView(checklist)

		setContentView(scroll)
		refreshChecklist()
	}

	private fun pickGame()
	{
		startActivity(Intent(this, GameBookActivity::class.java))
	}
}
