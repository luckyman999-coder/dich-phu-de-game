// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - Sổ tay game: hồ sơ từng game (văn phong, bối cảnh, từ điển nhân vật/tên riêng)

package com.metallic.chiaki.dich

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class Genre(val label: String, val style: String)
{
	AUTO("Tự nhận diện", ""),
	ACTION("Hành động", "Văn phong hành động: câu ngắn, dứt khoát, giàu cảm xúc."),
	MYTH("Thần thoại / sử thi", "Văn phong sử thi, trang trọng; thần linh xưng \"ta\", gọi \"ngươi\"."),
	WUXIA("Kiếm hiệp / cổ trang", "Văn phong cổ trang, dùng từ Hán Việt; xưng hô \"tại hạ\", \"huynh\", \"muội\", \"lão phu\", \"ngươi\" theo vai vế."),
	HORROR("Kinh dị", "Văn phong căng thẳng, u ám, câu ngắn."),
	SCIFI("Khoa học viễn tưởng", "Văn phong hiện đại, thuật ngữ công nghệ giữ nguyên tiếng Anh nếu thông dụng."),
	MODERN("Đời thường / thành phố", "Văn phong đời thường, khẩu ngữ tự nhiên, được dùng tiếng lóng nhẹ."),
	FANTASY("Giả tưởng / JRPG", "Văn phong phiêu lưu giả tưởng, tự nhiên như phim hoạt hình lồng tiếng."),
	COMEDY("Hài hước", "Văn phong dí dỏm, giữ được câu đùa, chơi chữ theo kiểu Việt.");

	companion object
	{
		fun of(name: String?) = values().firstOrNull { it.name == name } ?: AUTO
	}
}

enum class EntryKind(val label: String)
{
	CHARACTER("Nhân vật"), NAME("Tên riêng"), PLACE("Địa danh"), ITEM("Kỹ năng / vật phẩm"), TERM("Thuật ngữ");

	companion object
	{
		fun of(name: String?) = values().firstOrNull { it.name == name } ?: NAME
	}
}

data class BookEntry(
	val term: String,
	val kind: EntryKind,
	val gender: Gender,
	/** Cách dịch / mô tả / xưng hô, ví dụ: "con của Kratos, xưng 'con', gọi Kratos là 'cha'" */
	val note: String)

data class GameProfile(
	val id: String,
	val name: String,
	val genre: Genre = Genre.AUTO,
	val context: String = "",
	val entries: List<BookEntry> = emptyList())

/** Lưu sổ tay trong SharedPreferences (JSON) và đồng bộ sang DichPrefs để Translator dùng */
class GameBook(context: Context)
{
	private val sp = context.applicationContext.getSharedPreferences("dich_game_book", Context.MODE_PRIVATE)
	private val prefs = DichPrefs(context)

	var games: List<GameProfile>
		get() = parse(sp.getString("games", "[]") ?: "[]")
		private set(v) { sp.edit().putString("games", toJson(v).toString()).apply() }

	/** null = Chung (không chọn game) */
	var currentId: String?
		get() = sp.getString("current", null)
		private set(v) { sp.edit().putString("current", v).apply() }

	val current: GameProfile? get() = currentId?.let { id -> games.firstOrNull { it.id == id } }

	fun select(id: String?)
	{
		currentId = id
		sync()
	}

	fun upsert(p: GameProfile)
	{
		val list = games.toMutableList()
		val i = list.indexOfFirst { it.id == p.id }
		if(i >= 0) list[i] = p else list.add(0, p)
		games = list
		if(currentId == p.id) sync()
	}

	fun delete(id: String)
	{
		games = games.filter { it.id != id }
		if(currentId == id) select(null)
	}

	fun create(name: String, genre: Genre = Genre.AUTO, context: String = "", entries: List<BookEntry> = emptyList()): GameProfile
	{
		val p = GameProfile(UUID.randomUUID().toString(), name.trim(), genre, context, entries)
		upsert(p)
		select(p.id)
		return p
	}

	/** Thêm game từ thư viện mẫu (nếu đã có thì chọn lại) */
	fun addPreset(preset: GamePreset): GameProfile
	{
		games.firstOrNull { it.name.equals(preset.name, ignoreCase = true) }?.let { select(it.id); return it }
		return create(preset.name, Genre.AUTO, preset.notes)
	}

	/** Ghi văn bản ghi chú + giới tính nhân vật sang DichPrefs (Translator, Speaker đọc từ đó) */
	fun sync()
	{
		val p = current
		if(p == null)
		{
			prefs.gameName = ""
			prefs.gameNotes = ""
			prefs.speakerGenders = emptyMap()
			return
		}
		prefs.gameName = p.name
		prefs.gameNotes = render(p)
		prefs.speakerGenders = p.entries
			.filter { it.kind == EntryKind.CHARACTER && it.gender != Gender.UNKNOWN }
			.associate { it.term.lowercase() to it.gender.name }
	}

	companion object
	{
		fun render(p: GameProfile): String
		{
			val sb = StringBuilder()
			if(p.genre.style.isNotEmpty()) sb.append("Thể loại: ").append(p.genre.label).append(". ").append(p.genre.style).append('\n')
			if(p.context.isNotBlank()) sb.append(p.context.trim()).append('\n')
			if(p.entries.isNotEmpty())
			{
				sb.append("Từ điển của game (bắt buộc dùng đúng):\n")
				for(e in p.entries)
				{
					sb.append("- ").append(e.term).append(" [").append(e.kind.label)
					when(e.gender)
					{
						Gender.MALE -> sb.append(", nam")
						Gender.FEMALE -> sb.append(", nữ")
						Gender.UNKNOWN -> {}
					}
					sb.append("]")
					if(e.note.isNotBlank()) sb.append(": ").append(e.note.trim())
					sb.append('\n')
				}
			}
			return sb.toString().trim()
		}

		private fun toJson(list: List<GameProfile>) = JSONArray().apply {
			list.forEach { p ->
				put(JSONObject()
					.put("id", p.id).put("name", p.name).put("genre", p.genre.name).put("context", p.context)
					.put("entries", JSONArray().apply {
						p.entries.forEach { e ->
							put(JSONObject().put("term", e.term).put("kind", e.kind.name).put("gender", e.gender.name).put("note", e.note))
						}
					}))
			}
		}

		private fun parse(s: String): List<GameProfile> = try
		{
			val a = JSONArray(s)
			(0 until a.length()).map { i ->
				val o = a.getJSONObject(i)
				val ea = o.optJSONArray("entries") ?: JSONArray()
				GameProfile(
					id = o.getString("id"),
					name = o.optString("name"),
					genre = Genre.of(o.optString("genre")),
					context = o.optString("context"),
					entries = (0 until ea.length()).map { j ->
						val e = ea.getJSONObject(j)
						BookEntry(e.optString("term"), EntryKind.of(e.optString("kind")),
							runCatching { Gender.valueOf(e.optString("gender")) }.getOrDefault(Gender.UNKNOWN), e.optString("note"))
					})
			}
		}
		catch(e: Exception) { emptyList() }
	}
}
