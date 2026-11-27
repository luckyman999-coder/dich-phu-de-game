// Test khói: mở từng màn hình, chụp ảnh, chạy thật chế độ Xem thử
package com.metallic.chiaki.dich

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SmokeTest
{
	private val inst get() = InstrumentationRegistry.getInstrumentation()
	private val ctx get() = inst.targetContext

	@Before
	fun setup()
	{
		// bỏ qua hướng dẫn tự mở lần đầu để test màn hình chính
		ctx.getSharedPreferences("dich_game", 0).edit().putBoolean("guide_seen", true).commit()
	}

	private fun shot(name: String)
	{
		Thread.sleep(800)
		val bmp = inst.uiAutomation.takeScreenshot() ?: return
		val dir = File(ctx.getExternalFilesDir(null), "shots").apply { mkdirs() }
		File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
	}

	private fun texts(a: Activity): List<String>
	{
		val out = ArrayList<String>()
		fun walk(v: View)
		{
			if(v is TextView) out.add(v.text.toString())
			if(v is ViewGroup) for(i in 0 until v.childCount) walk(v.getChildAt(i))
		}
		walk(a.window.decorView)
		return out
	}

	private inline fun <reified T: Activity> open(name: String, waitMs: Long = 2500, check: (List<String>) -> Unit = {})
	{
		ActivityScenario.launch(T::class.java).use { sc ->
			Thread.sleep(waitMs)
			var t: List<String> = emptyList()
			sc.onActivity { t = texts(it) }
			shot(name)
			check(t)
		}
	}

	@Test fun t01_home() = open<HomeActivity>("01_man_hinh_chinh", 4000) { t ->
		assertTrue("Thiếu nút Chơi", t.any { it.contains("CHƠI VÀ DỊCH") })
	}

	@Test fun t02_guide() = open<GuideActivity>("02_huong_dan") { t -> assertTrue(t.any { it.contains("BẮT ĐẦU NHANH") }) }

	@Test fun t03_keyguide() = open<KeyGuideActivity>("03_lay_ma") { t -> assertTrue(t.any { it.contains("Create API key") }) }

	@Test fun t04_gamebook()
	{
		val book = GameBook(ctx)
		book.addPreset(GamePresets.all.first())
		assertTrue("Sổ tay chưa đồng bộ ghi chú sang prompt", DichPrefs(ctx).gameNotes.contains("Kratos"))
		open<GameBookActivity>("04_so_tay_game") { t -> assertTrue(t.any { it.contains("God of War") }) }
		book.select(null)
	}

	@Test fun t05_settings() = open<DichSettingsActivity>("05_cai_dat")

	@Test fun t06_tvconnect() = open<TvConnectActivity>("06_ket_noi_tv") { t -> assertTrue(t.any { it.contains("Kết nối TV") }) }

	@Test fun t07_about() = open<AboutActivity>("07_gioi_thieu") { t -> assertTrue(t.any { it.contains("AGPL") }) }

	/** Chạy thật: vẽ cảnh mẫu → OCR (ML Kit) → dịch dự phòng (chưa có mã) → hiện bản dịch */
	@Test fun t08_demo_end_to_end()
	{
		DichPrefs(ctx).geminiKeys = emptyList()
		DichPrefs(ctx).groqKey = ""
		ActivityScenario.launch(DemoActivity::class.java).use { sc ->
			val viLines = DemoScript.lines.map { it.vi }
			var ok = false
			for(i in 0 until 40)
			{
				Thread.sleep(500)
				sc.onActivity { a -> ok = texts(a).any { t -> viLines.any { t.contains(it) } } }
				if(ok) break
			}
			shot("08_xem_thu")
			assertTrue("Xem thử không ra bản dịch sau 20 giây (OCR/dịch lỗi)", ok)
		}
	}
}
