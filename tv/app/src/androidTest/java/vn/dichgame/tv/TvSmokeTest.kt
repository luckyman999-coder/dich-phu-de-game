// Test app TV: mở màn hình chính, ghép đôi qua cổng 47810 như điện thoại, gửi phụ đề, chụp ảnh
package vn.dichgame.tv

import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

@RunWith(AndroidJUnit4::class)
class TvSmokeTest
{
	private val inst get() = InstrumentationRegistry.getInstrumentation()
	private val ctx get() = inst.targetContext

	private fun shot(name: String)
	{
		Thread.sleep(800)
		val bmp = inst.uiAutomation.takeScreenshot() ?: return
		val dir = File(ctx.getExternalFilesDir(null), "shots").apply { mkdirs() }
		File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
	}

	@Test fun pairAndShowSubtitle()
	{
		ActivityScenario.launch(MainActivity::class.java).use {
			Thread.sleep(3000)
			shot("tv_01_man_hinh_chinh")

			// sai mã phải bị từ chối
			Socket("127.0.0.1", SubtitleService.PORT).use { s ->
				val out = PrintWriter(s.getOutputStream().bufferedWriter(), true)
				val inp = BufferedReader(InputStreamReader(s.getInputStream()))
				out.println(JSONObject().put("t", "hello").put("code", "0000x").toString())
				assertEquals("err", JSONObject(inp.readLine()).getString("t"))
			}

			// đúng mã → ok + token, gửi phụ đề
			Socket("127.0.0.1", SubtitleService.PORT).use { s ->
				val out = PrintWriter(s.getOutputStream().bufferedWriter(), true)
				val inp = BufferedReader(InputStreamReader(s.getInputStream()))
				out.println(JSONObject().put("t", "hello").put("code", SubtitleService.pairingCode(ctx)).put("name", "Test").toString())
				val r = JSONObject(inp.readLine())
				assertEquals("ok", r.getString("t"))
				out.println(JSONObject().put("t", "sub").put("text", "Chúng ta sẽ tìm ra cách. Cùng nhau.")
					.put("speaker", "Kael").put("gender", "MALE").put("speak", false).toString())
				Thread.sleep(1500)
				shot("tv_02_phu_de_de_len")
				out.println("{\"t\":\"ping\"}")
				assertEquals("pong", JSONObject(inp.readLine()).getString("t"))
			}
		}
	}
}
