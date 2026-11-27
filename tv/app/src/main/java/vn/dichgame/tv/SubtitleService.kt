// SPDX-License-Identifier: AGPL-3.0-only
// Dịch Game TV - dịch vụ nền: nhận phụ đề từ điện thoại qua Wi-Fi, vẽ đè lên màn hình, đọc bằng loa TV

package vn.dichgame.tv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.Locale
import java.util.UUID
import kotlin.concurrent.thread

class SubtitleService: Service()
{
	companion object
	{
		const val PORT = 47810
		const val SERVICE_TYPE = "_dichgame._tcp."
		private const val TAG = "DichGameTV"
		private const val CHANNEL = "dichgame_tv"
		private const val AUTO_HIDE_MS = 12_000L

		/** Trạng thái để màn hình chính hiển thị */
		@Volatile var running = false
		@Volatile var connectedPhone: String? = null
		@Volatile var lastError: String? = null
		var onStateChanged: (() -> Unit)? = null

		fun start(context: Context)
		{
			val i = Intent(context, SubtitleService::class.java)
			if(Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
		}

		fun pairingCode(context: Context): String
		{
			val sp = context.getSharedPreferences("tv", Context.MODE_PRIVATE)
			return sp.getString("code", null) ?: (1000 + (Math.random() * 9000).toInt()).toString().also {
				sp.edit().putString("code", it).apply()
			}
		}

		/** Thử hiện chữ (nút trên màn hình chính) */
		var testHook: ((String) -> Unit)? = null
	}

	private val main = Handler(Looper.getMainLooper())
	private var server: ServerSocket? = null
	private var nsd: NsdManager? = null
	private var nsdListener: NsdManager.RegistrationListener? = null
	private var overlayRoot: FrameLayout? = null
	private var subtitle: TextView? = null
	private var tts: TextToSpeech? = null
	private var ttsReady = false
	private val hide = Runnable { subtitle?.visibility = android.view.View.GONE }

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onCreate()
	{
		super.onCreate()
		startAsForeground()
		running = true
		tts = TextToSpeech(this) { status ->
			if(status == TextToSpeech.SUCCESS)
			{
				tts?.setLanguage(Locale("vi", "VN"))
				tts?.setAudioAttributes(AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ASSISTANT)
					.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
				ttsReady = true
			}
		}
		createOverlay()
		startServer()
		registerNsd()
		testHook = { text -> main.post { show(text, "", "UNKNOWN", null, true) } }
		notifyState()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		if(overlayRoot == null) createOverlay()
		return START_STICKY
	}

	override fun onDestroy()
	{
		running = false
		connectedPhone = null
		testHook = null
		try { server?.close() } catch(_: Exception) {}
		try { nsdListener?.let { nsd?.unregisterService(it) } } catch(_: Exception) {}
		overlayRoot?.let { try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch(_: Exception) {} }
		tts?.shutdown()
		notifyState()
		super.onDestroy()
	}

	private fun notifyState() = main.post { onStateChanged?.invoke() }

	private fun startAsForeground()
	{
		val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		if(Build.VERSION.SDK_INT >= 26)
			nm.createNotificationChannel(NotificationChannel(CHANNEL, "Dịch Game TV", NotificationManager.IMPORTANCE_LOW))
		val n = (if(Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else @Suppress("DEPRECATION") Notification.Builder(this))
			.setContentTitle("Dịch Game TV đang chờ phụ đề")
			.setContentText("Mã ghép đôi: ${pairingCode(this)}")
			.setSmallIcon(android.R.drawable.ic_menu_info_details)
			.build()
		if(Build.VERSION.SDK_INT >= 34)
			startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
		else
			startForeground(1, n)
	}

	// ---------------- lớp phụ đề ----------------

	private fun createOverlay()
	{
		if(Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this))
		{
			lastError = "Chưa cấp quyền hiện chữ đè lên ứng dụng khác"
			notifyState()
			return
		}
		val wm = getSystemService(WINDOW_SERVICE) as WindowManager
		val root = FrameLayout(this)
		val tv = TextView(this).apply {
			setTextColor(Color.WHITE)
			gravity = Gravity.CENTER
			typeface = Typeface.DEFAULT_BOLD
			setShadowLayer(6f, 0f, 2f, Color.BLACK)
			background = GradientDrawable().apply { cornerRadius = 12f; setColor(Color.argb(200, 0, 0, 0)) }
			setPadding(36, 14, 36, 14)
			visibility = android.view.View.GONE
		}
		root.addView(tv, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
			Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = 80 })
		val type = if(Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
			else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
		val lp = WindowManager.LayoutParams(
			WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, type,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
				WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
			PixelFormat.TRANSLUCENT)
		try
		{
			wm.addView(root, lp)
			overlayRoot = root
			subtitle = tv
			lastError = null
		}
		catch(e: Exception)
		{
			Log.e(TAG, "Không tạo được lớp phụ đề", e)
			lastError = "TV không cho hiện chữ đè: ${e.message}"
		}
		notifyState()
	}

	/** region = [trái, trên, phải, dưới] 0..1 theo khung hình game; null = mặc định đáy màn hình */
	private fun show(text: String, speaker: String, gender: String, region: DoubleArray?, speak: Boolean)
	{
		val tv = subtitle ?: return
		val root = overlayRoot ?: return
		val h = root.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
		val w = root.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
		tv.textSize = (h * 0.042f) / resources.displayMetrics.scaledDensity
		tv.text = if(speaker.isNotEmpty()) "$speaker: $text" else text
		val lp = tv.layoutParams as FrameLayout.LayoutParams
		if(region != null && region.size == 4)
		{
			lp.gravity = Gravity.TOP or Gravity.START
			lp.leftMargin = (region[0] * w).toInt()
			lp.topMargin = (region[1] * h).toInt()
			lp.width = ((region[2] - region[0]) * w).toInt()
			lp.bottomMargin = 0
		}
		else
		{
			lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
			lp.leftMargin = 0; lp.topMargin = 0
			lp.width = FrameLayout.LayoutParams.WRAP_CONTENT
			lp.bottomMargin = (h * 0.08f).toInt()
		}
		tv.layoutParams = lp
		tv.visibility = android.view.View.VISIBLE
		main.removeCallbacks(hide)
		main.postDelayed(hide, AUTO_HIDE_MS)
		if(speak && ttsReady)
		{
			tts?.setPitch(when(gender) { "MALE" -> 0.8f; "FEMALE" -> 1.15f; else -> 1f })
			tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
		}
	}

	private fun clear()
	{
		main.removeCallbacks(hide)
		subtitle?.visibility = android.view.View.GONE
	}

	// ---------------- máy chủ Wi-Fi ----------------

	private fun startServer()
	{
		thread(name = "DichGameTV-server", isDaemon = true) {
			try
			{
				val ss = ServerSocket(PORT)
				server = ss
				while(!ss.isClosed)
				{
					val s = ss.accept()
					thread(isDaemon = true) { handle(s) }
				}
			}
			catch(e: Exception)
			{
				if(running) { lastError = "Không mở được cổng $PORT: ${e.message}"; notifyState() }
			}
		}
	}

	private fun handle(s: Socket)
	{
		val sp = getSharedPreferences("tv", MODE_PRIVATE)
		var name = s.inetAddress.hostAddress ?: "điện thoại"
		try
		{
			s.tcpNoDelay = true
			val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
			val out = PrintWriter(s.getOutputStream().bufferedWriter(Charsets.UTF_8), true)
			// bắt tay: mã ghép đôi lần đầu, các lần sau dùng token
			val hello = JSONObject(reader.readLine() ?: return)
			val tokens = sp.getStringSet("tokens", emptySet())!!
			val token = hello.optString("token")
			val ok = when
			{
				token.isNotEmpty() && token in tokens -> true
				hello.optString("code") == pairingCode(this) -> true
				else -> false
			}
			if(!ok)
			{
				out.println(JSONObject().put("t", "err").put("msg", "Sai mã ghép đôi").toString())
				return
			}
			val newToken = if(token in tokens) token else UUID.randomUUID().toString().also {
				sp.edit().putStringSet("tokens", tokens + it).apply()
			}
			name = hello.optString("name").ifEmpty { name }
			out.println(JSONObject().put("t", "ok").put("token", newToken).put("tv", Build.MODEL).toString())
			connectedPhone = name
			notifyState()

			while(true)
			{
				val line = reader.readLine() ?: break
				val m = try { JSONObject(line) } catch(e: Exception) { continue }
				when(m.optString("t"))
				{
					"sub" ->
					{
						val r = m.optJSONArray("region")
						val region = if(r != null && r.length() == 4) DoubleArray(4) { r.getDouble(it) } else null
						val text = m.optString("text")
						val speaker = m.optString("speaker")
						val gender = m.optString("gender")
						val speak = m.optBoolean("speak", true)
						main.post { show(text, speaker, gender, region, speak) }
					}
					"clear" -> main.post { clear() }
					"ping" -> out.println("{\"t\":\"pong\"}")
				}
			}
		}
		catch(e: Exception)
		{
			Log.w(TAG, "Mất kết nối với $name", e)
		}
		finally
		{
			try { s.close() } catch(_: Exception) {}
			if(connectedPhone == name) connectedPhone = null
			notifyState()
		}
	}

	private fun registerNsd()
	{
		val info = NsdServiceInfo().apply {
			serviceName = "Dịch Game TV (${Build.MODEL})"
			serviceType = SERVICE_TYPE
			port = PORT
		}
		val l = object: NsdManager.RegistrationListener
		{
			override fun onServiceRegistered(i: NsdServiceInfo) {}
			override fun onRegistrationFailed(i: NsdServiceInfo, code: Int) { Log.w(TAG, "NSD lỗi $code") }
			override fun onServiceUnregistered(i: NsdServiceInfo) {}
			override fun onUnregistrationFailed(i: NsdServiceInfo, code: Int) {}
		}
		nsd = getSystemService(NSD_SERVICE) as NsdManager
		nsdListener = l
		try { nsd?.registerService(info, NsdManager.PROTOCOL_DNS_SD, l) } catch(e: Exception) { Log.w(TAG, "NSD", e) }
	}
}
