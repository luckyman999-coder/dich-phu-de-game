// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - cấu hình

package com.metallic.chiaki.dich

import android.content.Context
import android.graphics.RectF

open class DichPrefs(context: Context)
{
	private val sp = context.applicationContext.getSharedPreferences("dich_game", Context.MODE_PRIVATE)

	/** Tối đa 3 mã Gemini, xoay vòng khi một mã hết lượt */
	var geminiKeys: List<String>
		get() = (sp.getString("gemini_keys", "") ?: "").split("\n").map { it.trim() }.filter { it.isNotEmpty() }
		set(v) { sp.edit().putString("gemini_keys", v.joinToString("\n")).apply() }

	var groqKey: String
		get() = sp.getString("groq_key", "") ?: ""
		set(v) { sp.edit().putString("groq_key", v.trim()).apply() }

	var geminiModel: String
		get() = sp.getString("gemini_model", DEFAULT_GEMINI_MODEL) ?: DEFAULT_GEMINI_MODEL
		set(v) { sp.edit().putString("gemini_model", v.trim().ifEmpty { DEFAULT_GEMINI_MODEL }).apply() }

	var groqModel: String
		get() = sp.getString("groq_model", DEFAULT_GROQ_MODEL) ?: DEFAULT_GROQ_MODEL
		set(v) { sp.edit().putString("groq_model", v.trim().ifEmpty { DEFAULT_GROQ_MODEL }).apply() }

	/** Tên game + ghi chú (tên nhân vật, cách xưng hô, văn phong) */
	var gameName: String
		get() = sp.getString("game_name", "") ?: ""
		set(v) { sp.edit().putString("game_name", v.trim()).apply() }

	var gameNotes: String
		get() = sp.getString("game_notes", "") ?: ""
		set(v) { sp.edit().putString("game_notes", v.trim()).apply() }

	open var enabled: Boolean
		get() = sp.getBoolean("enabled", true)
		set(v) { sp.edit().putBoolean("enabled", v).apply() }

	var speak: Boolean
		get() = sp.getBoolean("speak", true)
		set(v) { sp.edit().putBoolean("speak", v).apply() }

	/** Tắt tiếng game trên điện thoại (tiếng game đã có trên TV) */
	var muteGame: Boolean
		get() = sp.getBoolean("mute_game", true)
		set(v) { sp.edit().putBoolean("mute_game", v).apply() }

	var showOriginal: Boolean
		get() = sp.getBoolean("show_original", false)
		set(v) { sp.edit().putBoolean("show_original", v).apply() }

	var speechRate: Float
		get() = sp.getFloat("speech_rate", 1.1f)
		set(v) { sp.edit().putFloat("speech_rate", v).apply() }

	/** Vùng phụ đề, toạ độ 0..1 so với khung hình game */
	open var region: RectF
		get() = RectF(
			sp.getFloat("r_l", 0.12f), sp.getFloat("r_t", 0.78f),
			sp.getFloat("r_r", 0.88f), sp.getFloat("r_b", 0.95f))
		set(v) { sp.edit()
			.putFloat("r_l", v.left).putFloat("r_t", v.top)
			.putFloat("r_r", v.right).putFloat("r_b", v.bottom).apply() }

	/** tên nhân vật (chữ thường) → MALE/FEMALE, lấy từ Sổ tay game */
	var speakerGenders: Map<String, String>
		get() = try
		{
			val o = org.json.JSONObject(sp.getString("speaker_genders", "{}") ?: "{}")
			o.keys().asSequence().associateWith { o.getString(it) }
		}
		catch(e: Exception) { emptyMap() }
		set(v) { sp.edit().putString("speaker_genders", org.json.JSONObject(v as Map<*, *>).toString()).apply() }

	var psnAccountId: String
		get() = sp.getString("psn_account_id", "") ?: ""
		set(v) { sp.edit().putString("psn_account_id", v).apply() }

	val hasAnyKey get() = geminiKeys.isNotEmpty() || groqKey.isNotEmpty()

	companion object
	{
		const val DEFAULT_GEMINI_MODEL = "gemini-flash-lite-latest"
		const val DEFAULT_GROQ_MODEL = "openai/gpt-oss-120b"
	}
}
