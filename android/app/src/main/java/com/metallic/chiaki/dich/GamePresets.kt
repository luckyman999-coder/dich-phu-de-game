// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Phụ Đề Game - ghi chú mẫu cho một số game (người dùng sửa thêm được)

package com.metallic.chiaki.dich

data class GamePreset(val name: String, val notes: String)

object GamePresets
{
	val all = listOf(
		GamePreset("God of War Ragnarök", """
			Giữ nguyên tên: Kratos, Atreus, Freya, Mimir, Thor, Odin, Brok, Sindri, Angrboda.
			Kratos gọi Atreus là "Boy" → dịch "nhóc"/"con"; Kratos xưng "ta", gọi Atreus "con".
			Atreus gọi Kratos "Father" → "Cha", xưng "con".
			Mimir nói kiểu dí dỏm, gọi Kratos "brother" → "người anh em".
			Văn phong thần thoại Bắc Âu, trang trọng nhưng gọn.
		""".trimIndent()),
		GamePreset("The Last of Us Part I / II", """
			Giữ nguyên tên: Joel, Ellie, Tommy, Tess, Abby, Dina, Jesse, Lev.
			Joel – Ellie: Joel xưng "tôi"/"chú", gọi Ellie "nhóc"/"cháu"; Ellie xưng "cháu"/"tôi" tuỳ lúc, giọng tuổi teen, hay chửi thề nhẹ.
			Fireflies → "Đom Đóm", Infected → "Kẻ nhiễm", Clicker giữ nguyên.
			Văn phong đời thường, ngắn, có chất bụi bặm.
		""".trimIndent()),
		GamePreset("Ghost of Tsushima", """
			Giữ nguyên tên: Jin Sakai, Lord Shimura, Yuna, Taka, Khotun Khan, Ishikawa, Masako, Kenji.
			Jin gọi Lord Shimura là "Uncle" → "Chú", xưng "cháu"; Shimura gọi Jin "cháu", xưng "ta".
			Mongols → "quân Mông Cổ"; samurai, katana giữ nguyên; "The Ghost" → "Bóng Ma".
			Văn phong cổ trang Nhật, trang trọng.
		""".trimIndent()),
		GamePreset("Elden Ring", """
			Giữ nguyên tên riêng: Melina, Ranni, Radahn, Malenia, Godrick, Margit, Marika, Erdtree (Cây Erd).
			Tarnished → "Kẻ Tàn Tạ"; Lands Between → "Vùng Đất Giữa"; Great Rune → "Đại Cổ Tự".
			NPC nói giọng cổ: xưng "ta", gọi người chơi "ngươi".
		""".trimIndent()),
		GamePreset("Black Myth: Wukong", """
			Dùng tên Hán Việt quen thuộc theo Tây Du Ký: Sun Wukong → Tôn Ngộ Không, Destined One → Thiên Mệnh Nhân,
			Zhu Bajie → Trư Bát Giới, Tang Sanzang → Đường Tăng, Erlang Shen → Nhị Lang Thần, Guanyin → Quan Âm, Yaoguai → Yêu quái.
			Văn phong cổ trang Trung Hoa: "ta – ngươi", "lão phu", "tiểu tử".
		""".trimIndent()),
		GamePreset("Marvel's Spider-Man 2", """
			Giữ nguyên tên: Peter Parker, Miles Morales, Mary Jane (MJ), Harry Osborn, Kraven, Venom, Ganke, Rio.
			Peter – Miles: Peter xưng "anh", gọi Miles "em"/"nhóc"; Miles gọi Peter "anh".
			Văn phong trẻ trung, hài hước, câu đùa ngắn gọn.
		""".trimIndent()),
		GamePreset("Red Dead Redemption 2", """
			Giữ nguyên tên: Arthur Morgan, Dutch van der Linde, John Marston, Hosea, Sadie, Micah, Charles, Abigail.
			Dutch gọi Arthur "son" → "con trai"/"chú em"; Arthur xưng "tôi".
			Văn phong miền Tây cao bồi, thô ráp, có chửi thề.
		""".trimIndent()),
		GamePreset("The Witcher 3", """
			Giữ nguyên tên: Geralt, Ciri, Yennefer, Triss, Dandelion, Vesemir, Eredin.
			Witcher giữ nguyên; Wild Hunt → "Săn Hoang"; Geralt nói cộc lốc, xưng "tôi"/"ta".
			Geralt – Ciri: Ciri gọi Geralt "Geralt", Geralt gọi Ciri "con"/"Ciri".
		""".trimIndent()),
		GamePreset("Final Fantasy VII Rebirth", """
			Giữ nguyên tên: Cloud, Tifa, Aerith, Barret, Red XIII, Yuffie, Cait Sith, Sephiroth, Zack.
			Shinra, SOLDIER, Materia giữ nguyên. Cloud nói ngắn, lạnh lùng, xưng "tôi".
			Văn phong anime JRPG, tự nhiên.
		""".trimIndent()),
		GamePreset("Resident Evil 4", """
			Giữ nguyên tên: Leon, Ashley, Luis, Ada, Krauser, Salazar, Saddler, Hunnigan.
			Leon xưng "tôi", gọi Ashley "cô"; Ashley gọi Leon "anh".
			Los Illuminados giữ nguyên; Ganado → "dân làng bị nhiễm". Văn phong kinh dị hành động, câu ngắn.
		""".trimIndent())
	)
}
