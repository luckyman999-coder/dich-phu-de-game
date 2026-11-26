// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - ghi chú mẫu cho một số game (người dùng sửa thêm được)

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
		""".trimIndent()),
		GamePreset("Stellar Blade", """
			Giữ nguyên tên: EVE, Adam, Lily, Raven, Kaya, Enya.
			Naytiba giữ nguyên. EVE nói ngắn, điềm tĩnh, xưng "tôi".
			Adam và Lily gọi EVE là "EVE", xưng "tôi" hoặc "anh"/"em" tuỳ thân mật.
			Văn phong khoa học viễn tưởng hậu tận thế.
		""".trimIndent()),
		GamePreset("Final Fantasy XVI", """
			Giữ nguyên tên: Clive Rosfield, Joshua, Jill Warrick, Cidolfus Telamon (Cid), Torgal, Barnabas, Benedikta.
			Dominant, Eikon, Bearer, Mothercrystal giữ nguyên tiếng Anh.
			Clive gọi Joshua là em trai ("em"), Joshua gọi Clive "anh".
			Văn phong trung cổ giả tưởng, trang trọng.
		""".trimIndent()),
		GamePreset("Horizon Forbidden West", """
			Giữ nguyên tên: Aloy, Varl, Zo, Erend, Sylens, Kotallo, Alva, Beta, Tilda.
			Tên máy móc (Tallneck, Thunderjaw, Sunwing...) giữ nguyên.
			Aloy xưng "tôi", nói thẳng thắn, quyết đoán.
			Tên bộ tộc Nora, Carja, Tenakth, Utaru giữ nguyên.
		""".trimIndent()),
		GamePreset("Uncharted 4", """
			Giữ nguyên tên: Nathan Drake (Nate), Sam Drake, Elena Fisher, Victor Sullivan (Sully), Rafe Adler, Nadine Ross.
			Sully gọi Nate là "kid" → "nhóc"; Nate gọi Sully "ông" hoặc "Sully".
			Nate và Sam là anh em: Sam là anh.
			Văn phong phiêu lưu, hài hước, câu đùa nhanh.
		""".trimIndent()),
		GamePreset("Cyberpunk 2077", """
			Giữ nguyên tên: V, Johnny Silverhand, Jackie Welles, Judy Alvarez, Panam Palmer, Takemura, Rogue.
			Night City, Arasaka, netrunner, cyberware giữ nguyên.
			"Choom" → "bồ"; "preem" → "xịn"; "gonk" → "đồ ngốc".
			Johnny nói cộc cằn, chửi thề nhiều. Văn phong đường phố, tiếng lóng.
		""".trimIndent()),
		GamePreset("Baldur's Gate 3", """
			Giữ nguyên tên: Shadowheart, Astarion, Gale, Lae'zel, Karlach, Wyll, Halsin, Minthara.
			Illithid, Mind Flayer, tadpole ("nòng nọc") giữ đúng.
			Astarion nói mỉa mai, kiểu cách; Lae'zel lạnh lùng, gọi người khác "istik".
			Văn phong giả tưởng D&D.
		""".trimIndent()),
		GamePreset("Death Stranding", """
			Giữ nguyên tên: Sam Porter Bridges, Fragile, Die-Hardman, Higgs, Amelie, Deadman, Heartman, Mama, Lockne.
			BB (Lou), BT, Beach, chiral, Timefall giữ nguyên.
			Sam nói ít, cộc lốc, xưng "tôi".
			Văn phong trầm, triết lý.
		""".trimIndent()),
		GamePreset("Assassin's Creed Shadows", """
			Giữ nguyên tên: Naoe, Yasuke, Oda Nobunaga, Akechi Mitsuhide, Tokugawa Ieyasu.
			Shinobi, samurai, katana, daimyo giữ nguyên.
			Văn phong cổ trang Nhật thời Chiến Quốc: kính ngữ "đại nhân", "chúa công".
		""".trimIndent()),
		GamePreset("Hogwarts Legacy", """
			Giữ nguyên tên: Professor Fig, Ranrok, Sebastian Sallow, Natsai Onai (Natty), Poppy Sweeting, Ominis Gaunt.
			Tên phép thuật (Lumos, Accio, Avada Kedavra...), Hogwarts, Muggle, Gryffindor/Slytherin/Ravenclaw/Hufflepuff giữ nguyên.
			Học sinh gọi nhau "cậu", xưng "tớ"; gọi giáo sư là "thầy"/"cô".
			Văn phong phép thuật, gần với bản dịch Harry Potter tiếng Việt.
		""".trimIndent()),
		GamePreset("Persona 5 Royal", """
			Giữ nguyên tên: Joker, Morgana (Mona), Ryuji (Skull), Ann (Panther), Yusuke (Fox), Makoto (Queen), Futaba (Oracle), Haru (Noir), Akechi.
			Phantom Thieves, Metaverse, Palace, Shadow, Persona giữ nguyên.
			Học sinh xưng "tớ"/"cậu"; senpai giữ nguyên.
			Ryuji nói bỗ bã; Morgana xưng "ta". Văn phong anime học đường.
		""".trimIndent()),
		GamePreset("Sekiro: Shadows Die Twice", """
			Wolf → "Sói" (người chơi), Kuro – Divine Heir → "Thần Tử", Isshin Ashina, Genichiro Ashina, Emma, Owl ("Cú" – cha nuôi của Sói).
			Sói gọi Kuro là "chủ nhân", xưng "tôi" hoặc "thuộc hạ".
			Văn phong cổ trang Nhật, nhân vật lớn tuổi xưng "ta", gọi "ngươi".
		""".trimIndent()),
		GamePreset("Star Wars Jedi: Survivor", """
			Giữ nguyên tên: Cal Kestis, BD-1, Cere Junda, Greez, Merrin, Bode Akuna, Dagan Gera.
			Jedi, Sith, lightsaber, Empire giữ nguyên; the Force → "Thần Lực".
			Văn phong phiêu lưu không gian, tự nhiên.
		""".trimIndent()),
		GamePreset("It Takes Two", """
			Giữ nguyên tên: Cody, May, Rose, Dr. Hakim ("Book of Love" → "Sách Tình Yêu").
			Cody và May là vợ chồng sắp ly hôn: gọi nhau "anh"/"em", hay cãi nhau.
			Rose là con gái, gọi "bố"/"mẹ". Văn phong hài hước, gia đình.
		""".trimIndent()),
		GamePreset("Ratchet & Clank: Rift Apart", """
			Giữ nguyên tên: Ratchet, Clank, Rivet, Kit, Dr. Nefarious, Captain Qwark.
			Ratchet và Clank là bạn thân, gọi nhau "cậu"/"tớ".
			Văn phong hoạt hình, vui nhộn, chơi chữ.
		""".trimIndent()),
		GamePreset("Silent Hill 2", """
			Giữ nguyên tên: James Sunderland, Mary, Maria, Angela, Eddie, Laura, Pyramid Head.
			James xưng "tôi"; Laura là bé gái, nói xấc xược.
			Văn phong kinh dị tâm lý, u buồn, câu ngắn.
		""".trimIndent()),
		GamePreset("Alan Wake 2", """
			Giữ nguyên tên: Alan Wake, Saga Anderson, Alex Casey, Mr. Scratch, Tim Breaker.
			Dark Place, Bright Falls, Cauldron Lake giữ nguyên.
			Saga và Casey là đặc vụ FBI, xưng hô công việc "tôi"/"anh"/"cô". Văn phong kinh dị trinh thám.
		""".trimIndent()),
		GamePreset("Kingdom Come: Deliverance II", """
			Giữ nguyên tên: Henry, Hans Capon, Sir Radzig Kobyla, Hanush, Mutt (chó của Henry).
			Henry gọi Hans là "ngài" (my lord), Hans gọi Henry "anh"/"ngươi" khi trêu.
			Văn phong trung cổ châu Âu, có chửi thề dân dã.
		""".trimIndent()),
		GamePreset("Ghost of Yōtei", """
			Giữ nguyên tên nhân vật và địa danh Nhật Bản (Ezo, Yōtei).
			Văn phong cổ trang Nhật, người lớn tuổi xưng "ta", gọi "ngươi"; kính ngữ "đại nhân".
		""".trimIndent()),
		GamePreset("Monster Hunter Wilds", """
			Hunter → "Thợ Săn", Palico giữ nguyên, tên quái vật giữ nguyên tiếng Anh.
			Văn phong phiêu lưu, nhẹ nhàng.
		""".trimIndent()),
		GamePreset("Dragon's Dogma 2", """
			Arisen, Pawn giữ nguyên. Tên vương quốc Vermund, Battahl giữ nguyên.
			Pawn nói lịch sự, gọi người chơi "chủ nhân". Văn phong trung cổ giả tưởng.
		""".trimIndent())
	)
}
