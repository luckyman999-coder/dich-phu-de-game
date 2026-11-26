// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - gọi Gemini (xoay vòng nhiều mã) rồi Groq dự phòng

package com.metallic.chiaki.dich

import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class Gender { MALE, FEMALE, UNKNOWN }

data class Translation(
	val original: String,
	val vietnamese: String,
	val speaker: String,
	val gender: Gender
)

class Translator(private val prefs: DichPrefs)
{
	companion object
	{
		private const val TAG = "DichTranslator"
		private const val KEY_COOLDOWN_MS = 60_000L
		private const val HISTORY_SIZE = 6
	}

	private class HttpError(val code: Int, msg: String): IOException(msg)

	private val cache = LruCache<String, Translation>(300)
	private val history = ArrayDeque<Pair<String, String>>()
	private val keyBlockedUntil = HashMap<String, Long>()
	private var keyIndex = 0

	/** Lời người dùng hay thấy nhất khi lỗi, hiện lên màn hình */
	@Volatile var lastError: String? = null
		private set

	suspend fun translate(text: String): Translation? = withContext(Dispatchers.IO) {
		cache.get(text)?.let { return@withContext it }
		val prompt = buildPrompt(text)
		val json = tryGemini(prompt) ?: tryGroq(prompt) ?: return@withContext null
		val result = parse(text, json) ?: return@withContext null
		cache.put(text, result)
		synchronized(history) {
			history.addLast(text to result.vietnamese)
			while(history.size > HISTORY_SIZE) history.removeFirst()
		}
		lastError = null
		result
	}

	private fun buildPrompt(text: String): String
	{
		val sb = StringBuilder()
		sb.append("Bạn là dịch giả phụ đề game chuyên nghiệp. Dịch câu thoại tiếng Anh sau sang tiếng Việt tự nhiên, ")
		sb.append("đúng giọng nhân vật và văn phong của game, ngắn gọn như phụ đề. ")
		sb.append("Giữ nguyên tên riêng (nhân vật, địa danh, kỹ năng) trừ khi ghi chú yêu cầu khác. ")
		sb.append("Chọn cách xưng hô tiếng Việt (anh/em, tôi/cậu, ta/ngươi...) phù hợp quan hệ nhân vật và giữ nhất quán với các câu trước.\n")
		if(prefs.gameName.isNotEmpty())
			sb.append("Game: ").append(prefs.gameName).append('\n')
		if(prefs.gameNotes.isNotEmpty())
			sb.append("Ghi chú về game (bắt buộc tuân theo):\n").append(prefs.gameNotes).append('\n')
		val hist = synchronized(history) { history.toList() }
		if(hist.isNotEmpty())
		{
			sb.append("Các câu vừa dịch trước đó (để giữ mạch hội thoại):\n")
			hist.forEach { (en, vi) -> sb.append("- ").append(en).append(" => ").append(vi).append('\n') }
		}
		sb.append("Văn bản do OCR đọc từ màn hình nên có thể sai vài ký tự; hãy hiểu theo nghĩa hợp lý nhất. ")
		sb.append("Nếu dòng đầu có dạng \"TÊN: lời thoại\" thì TÊN là người nói, không dịch tên đó.\n")
		sb.append("Trả về DUY NHẤT một JSON: {\"speaker\": \"tên người nói hoặc rỗng\", ")
		sb.append("\"gender\": \"male\" | \"female\" | \"unknown\", \"vi\": \"bản dịch tiếng Việt\"}\n\n")
		sb.append("Văn bản:\n").append(text)
		return sb.toString()
	}

	private fun tryGemini(prompt: String): String?
	{
		val keys = prefs.geminiKeys
		if(keys.isEmpty()) return null
		val now = System.currentTimeMillis()
		for(i in keys.indices)
		{
			val key = keys[(keyIndex + i) % keys.size]
			if((keyBlockedUntil[key] ?: 0L) > now) continue
			try
			{
				val body = JSONObject()
					.put("contents", JSONArray().put(JSONObject()
						.put("role", "user")
						.put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
					.put("generationConfig", JSONObject()
						.put("temperature", 0.3)
						.put("responseMimeType", "application/json"))
				val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
					URLEncoder.encode(prefs.geminiModel, "UTF-8") + ":generateContent"
				val resp = post(url, body.toString(), mapOf("x-goog-api-key" to key))
				keyIndex = (keyIndex + i) % keys.size
				return JSONObject(resp)
					.getJSONArray("candidates").getJSONObject(0)
					.getJSONObject("content").getJSONArray("parts").getJSONObject(0)
					.getString("text")
			}
			catch(e: HttpError)
			{
				Log.w(TAG, "Gemini key #${(keyIndex + i) % keys.size + 1} lỗi ${e.code}: ${e.message}")
				lastError = "Gemini lỗi ${e.code}" + if(e.code == 429) " (hết lượt)" else ""
				if(e.code == 429 || e.code == 403 || e.code == 401 || e.code == 400)
					keyBlockedUntil[key] = now + KEY_COOLDOWN_MS
			}
			catch(e: Exception)
			{
				Log.w(TAG, "Gemini lỗi", e)
				lastError = "Gemini: ${e.message}"
			}
		}
		return null
	}

	private fun tryGroq(prompt: String): String?
	{
		val key = prefs.groqKey
		if(key.isEmpty()) return null
		return try
		{
			val body = JSONObject()
				.put("model", prefs.groqModel)
				.put("temperature", 0.3)
				.put("response_format", JSONObject().put("type", "json_object"))
				.put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
			if(prefs.groqModel.startsWith("openai/gpt-oss"))
				body.put("reasoning_effort", "low") // dịch phụ đề cần nhanh
			val resp = post("https://api.groq.com/openai/v1/chat/completions", body.toString(),
				mapOf("Authorization" to "Bearer $key"))
			JSONObject(resp).getJSONArray("choices").getJSONObject(0)
				.getJSONObject("message").getString("content")
		}
		catch(e: Exception)
		{
			Log.w(TAG, "Groq lỗi", e)
			lastError = if(e is HttpError) "Groq lỗi ${e.code}" else "Groq: ${e.message}"
			null
		}
	}

	private fun parse(original: String, raw: String): Translation?
	{
		return try
		{
			val start = raw.indexOf('{')
			val end = raw.lastIndexOf('}')
			val o = JSONObject(raw.substring(start, end + 1))
			val vi = o.optString("vi").trim()
			if(vi.isEmpty()) return null
			Translation(
				original = original,
				vietnamese = vi,
				speaker = o.optString("speaker").trim(),
				gender = when(o.optString("gender").lowercase())
				{
					"male" -> Gender.MALE
					"female" -> Gender.FEMALE
					else -> Gender.UNKNOWN
				})
		}
		catch(e: Exception)
		{
			// Mô hình không trả JSON: dùng nguyên văn
			val t = raw.trim()
			if(t.isEmpty()) null else Translation(original, t, "", Gender.UNKNOWN)
		}
	}

	private fun post(url: String, body: String, headers: Map<String, String>): String
	{
		val conn = URL(url).openConnection() as HttpURLConnection
		try
		{
			conn.requestMethod = "POST"
			conn.connectTimeout = 8000
			conn.readTimeout = 15000
			conn.doOutput = true
			conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
			headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
			conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
			val code = conn.responseCode
			if(code !in 200..299)
			{
				val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
				throw HttpError(code, err.take(300))
			}
			return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
		}
		finally
		{
			conn.disconnect()
		}
	}
}
