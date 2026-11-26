// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Phụ Đề Game - đăng nhập PSN trong app để lấy Account ID (dùng ghép đôi PS5)
// Luồng giống chiaki-ng bản máy tính (gui/src/psnaccountid.cpp)

package com.metallic.chiaki.dich

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class PsnLoginActivity: AppCompatActivity()
{
	companion object
	{
		const val EXTRA_ACCOUNT_ID = "psn_account_id"

		private const val CLIENT_ID = "ba495a24-818c-472b-b12d-ff231c1b5745"
		private const val CLIENT_SECRET = "mvaiZkRsAsI1IBkY"
		private const val REDIRECT = "https://remoteplay.dl.playstation.net/remoteplay/redirect"
		private const val SCOPE = "psn:clientapp referenceDataService:countryConfig.read pushNotification:webSocket.desktop.connect sessionManager:remotePlaySession.system.update"
		private const val TOKEN_URL = "https://auth.api.sonyentertainmentnetwork.com/2.0/oauth/token"
		private val LOGIN_URL = "https://auth.api.sonyentertainmentnetwork.com/2.0/oauth/authorize" +
			"?service_entity=urn:service-entity:psn&response_type=code&client_id=$CLIENT_ID" +
			"&redirect_uri=$REDIRECT&scope=${Uri.encode(SCOPE)}&request_locale=en_US&ui=pr" +
			"&service_logo=ps&layout_type=popup&smcid=remoteplay&prompt=always&PlatformPrivacyWs1=minimal&"
	}

	private lateinit var webView: WebView
	private lateinit var progress: ProgressBar
	private var handled = false

	@SuppressLint("SetJavaScriptEnabled")
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		title = "Đăng nhập PSN"

		val root = FrameLayout(this)
		webView = WebView(this)
		progress = ProgressBar(this).apply { isIndeterminate = true }
		root.addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
		root.addView(progress, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
			android.view.Gravity.CENTER))
		setContentView(root)

		// Bắt đầu phiên đăng nhập mới
		CookieManager.getInstance().removeAllCookies(null)

		webView.settings.javaScriptEnabled = true
		webView.settings.domStorageEnabled = true
		// Một số trang đăng nhập chặn WebView: bỏ dấu "; wv" khỏi user agent
		webView.settings.userAgentString = webView.settings.userAgentString.replace("; wv", "")
		webView.webViewClient = object: WebViewClient()
		{
			override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
				checkRedirect(request.url.toString())

			override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?)
			{
				checkRedirect(url)
				progress.visibility = android.view.View.VISIBLE
			}

			override fun onPageFinished(view: WebView, url: String)
			{
				progress.visibility = android.view.View.GONE
			}
		}
		webView.loadUrl(LOGIN_URL)
	}

	private fun checkRedirect(url: String): Boolean
	{
		if(!url.startsWith(REDIRECT)) return false
		if(handled) return true
		handled = true
		val code = Uri.parse(url).getQueryParameter("code")
		if(code.isNullOrEmpty())
		{
			fail("Không nhận được mã đăng nhập từ PSN")
			return true
		}
		webView.stopLoading()
		progress.visibility = android.view.View.VISIBLE
		lifecycleScope.launch {
			try
			{
				val accountId = withContext(Dispatchers.IO) { fetchAccountId(code) }
				done(accountId)
			}
			catch(e: Exception)
			{
				fail("Lỗi lấy Account ID: ${e.message}")
			}
		}
		return true
	}

	private fun basicAuth(): String =
		"Basic " + Base64.encodeToString("$CLIENT_ID:$CLIENT_SECRET".toByteArray(), Base64.NO_WRAP)

	private fun fetchAccountId(code: String): String
	{
		// 1) đổi code lấy access token
		val body = "grant_type=authorization_code&code=${URLEncoder.encode(code, "UTF-8")}" +
			"&scope=${URLEncoder.encode(SCOPE, "UTF-8")}&redirect_uri=${URLEncoder.encode(REDIRECT, "UTF-8")}&"
		val tokenConn = URL(TOKEN_URL).openConnection() as HttpURLConnection
		val accessToken = try
		{
			tokenConn.requestMethod = "POST"
			tokenConn.doOutput = true
			tokenConn.setRequestProperty("Authorization", basicAuth())
			tokenConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
			tokenConn.outputStream.use { it.write(body.toByteArray()) }
			if(tokenConn.responseCode !in 200..299)
				throw Exception("token HTTP ${tokenConn.responseCode}")
			JSONObject(tokenConn.inputStream.bufferedReader().use { it.readText() }).getString("access_token")
		}
		finally { tokenConn.disconnect() }

		// 2) hỏi thông tin token để lấy user_id
		val infoConn = URL("$TOKEN_URL/$accessToken").openConnection() as HttpURLConnection
		val userId = try
		{
			infoConn.setRequestProperty("Authorization", basicAuth())
			infoConn.setRequestProperty("Content-Type", "application/json")
			if(infoConn.responseCode !in 200..299)
				throw Exception("user HTTP ${infoConn.responseCode}")
			JSONObject(infoConn.inputStream.bufferedReader().use { it.readText() }).getString("user_id")
		}
		finally { infoConn.disconnect() }

		// 3) user_id (số 64-bit) → 8 byte little-endian → Base64
		var n = userId.toULong()
		val bytes = ByteArray(8)
		for(i in 0 until 8)
		{
			bytes[i] = (n and 0xFFuL).toByte()
			n = n shr 8
		}
		return Base64.encodeToString(bytes, Base64.NO_WRAP)
	}

	private fun done(accountId: String)
	{
		progress.visibility = android.view.View.GONE
		DichPrefs(this).psnAccountId = accountId
		(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
			.setPrimaryClip(ClipData.newPlainText("PSN Account ID", accountId))
		Toast.makeText(this, "Đã lấy Account ID: $accountId", Toast.LENGTH_LONG).show()
		setResult(RESULT_OK, Intent().putExtra(EXTRA_ACCOUNT_ID, accountId))
		finish()
	}

	private fun fail(msg: String)
	{
		progress.visibility = android.view.View.GONE
		MaterialAlertDialogBuilder(this)
			.setMessage(msg)
			.setPositiveButton("Thử lại") { _, _ ->
				handled = false
				webView.loadUrl(LOGIN_URL)
			}
			.setNegativeButton("Đóng") { _, _ -> finish() }
			.show()
	}
}
