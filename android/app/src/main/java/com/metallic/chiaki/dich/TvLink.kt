// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - gửi phụ đề sang app Dịch Game TV qua Wi-Fi

package com.metallic.chiaki.dich

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

data class FoundTv(val name: String, val host: String, val port: Int)

/** Tìm TV trong mạng nhà bằng mDNS (_dichgame._tcp) */
class TvFinder(context: Context, private val onFound: (FoundTv) -> Unit)
{
	private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
	private var listener: NsdManager.DiscoveryListener? = null

	fun start()
	{
		val l = object: NsdManager.DiscoveryListener
		{
			override fun onDiscoveryStarted(t: String) {}
			override fun onDiscoveryStopped(t: String) {}
			override fun onStartDiscoveryFailed(t: String, e: Int) {}
			override fun onStopDiscoveryFailed(t: String, e: Int) {}
			override fun onServiceLost(i: NsdServiceInfo) {}
			override fun onServiceFound(i: NsdServiceInfo)
			{
				@Suppress("DEPRECATION")
				nsd.resolveService(i, object: NsdManager.ResolveListener
				{
					override fun onResolveFailed(i: NsdServiceInfo, e: Int) {}
					override fun onServiceResolved(r: NsdServiceInfo)
					{
						@Suppress("DEPRECATION")
						val host = r.host?.hostAddress ?: return
						onFound(FoundTv(r.serviceName, host, r.port))
					}
				})
			}
		}
		listener = l
		try { nsd.discoverServices("_dichgame._tcp.", NsdManager.PROTOCOL_DNS_SD, l) } catch(e: Exception) { Log.w("TvFinder", e) }
	}

	fun stop()
	{
		listener?.let { try { nsd.stopServiceDiscovery(it) } catch(_: Exception) {} }
		listener = null
	}
}

/**
 * Kết nối tới TV đã ghép đôi và gửi phụ đề. Tự kết nối lại khi rớt mạng.
 * Mọi hàm gọi được từ luồng chính; việc mạng chạy ở Dispatchers.IO.
 */
class TvLink(private val context: Context)
{
	companion object
	{
		private const val TAG = "TvLink"
		const val PORT = 47810

		/** Bắt tay lần đầu bằng mã ghép đôi hiện trên TV. Trả về null nếu thành công, hoặc lời báo lỗi. */
		suspend fun pair(context: Context, tv: FoundTv, code: String): String? = withContext(Dispatchers.IO) {
			try
			{
				Socket().use { s ->
					s.connect(InetSocketAddress(tv.host, tv.port), 4000)
					s.soTimeout = 5000
					val out = PrintWriter(s.getOutputStream().bufferedWriter(Charsets.UTF_8), true)
					val inp = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
					out.println(JSONObject().put("t", "hello").put("code", code.trim()).put("name", Build.MODEL).toString())
					val r = JSONObject(inp.readLine() ?: return@withContext "TV không trả lời")
					if(r.optString("t") != "ok") return@withContext r.optString("msg", "Sai mã ghép đôi")
					val p = DichPrefs(context)
					p.tvHost = tv.host
					p.tvPort = tv.port
					p.tvName = tv.name
					p.tvToken = r.optString("token")
					null
				}
			}
			catch(e: Exception) { "Không kết nối được TV: ${e.message}" }
		}
	}

	private val prefs = DichPrefs(context)
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val queue = Channel<String>(Channel.CONFLATED)
	@Volatile var connected = false
		private set
	var onState: ((Boolean) -> Unit)? = null

	val enabled get() = prefs.tvHost.isNotEmpty() && prefs.tvToken.isNotEmpty()

	fun start()
	{
		if(!enabled) return
		scope.launch {
			while(isActive)
			{
				try { session() }
				catch(e: CancellationException) { throw e }
				catch(e: Exception) { Log.w(TAG, "TV rớt kết nối: ${e.message}") }
				setConnected(false)
				delay(3000)
			}
		}
	}

	fun stop()
	{
		scope.cancel()
		setConnected(false)
	}

	fun sendSubtitle(t: Translation, region: android.graphics.RectF?, speakOnTv: Boolean)
	{
		if(!enabled) return
		val o = JSONObject().put("t", "sub").put("text", t.vietnamese).put("speaker", t.speaker)
			.put("gender", t.gender.name).put("speak", speakOnTv)
		if(region != null) o.put("region", JSONArray().put(region.left.toDouble()).put(region.top.toDouble()).put(region.right.toDouble()).put(region.bottom.toDouble()))
		queue.trySend(o.toString())
	}

	fun clear()
	{
		if(enabled) queue.trySend("{\"t\":\"clear\"}")
	}

	private suspend fun session()
	{
		Socket().use { s ->
			s.connect(InetSocketAddress(prefs.tvHost, prefs.tvPort), 4000)
			s.tcpNoDelay = true
			val out = PrintWriter(s.getOutputStream().bufferedWriter(Charsets.UTF_8), true)
			val inp = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
			out.println(JSONObject().put("t", "hello").put("token", prefs.tvToken).put("name", Build.MODEL).toString())
			s.soTimeout = 5000
			val r = JSONObject(inp.readLine() ?: return)
			if(r.optString("t") != "ok") { Log.w(TAG, "TV từ chối: $r"); return }
			setConnected(true)
			while(currentCoroutineContext().isActive)
			{
				// ping mỗi 10 giây để phát hiện rớt mạng
				val msg = withTimeoutOrNull(10_000) { queue.receive() } ?: "{\"t\":\"ping\"}"
				out.println(msg)
				if(out.checkError()) throw java.io.IOException("ghi lỗi")
				if(msg.contains("\"ping\"")) inp.readLine() ?: throw java.io.IOException("TV đóng kết nối")
			}
		}
	}

	private fun setConnected(c: Boolean)
	{
		if(connected == c) return
		connected = c
		CoroutineScope(Dispatchers.Main).launch { onState?.invoke(c) }
	}
}
