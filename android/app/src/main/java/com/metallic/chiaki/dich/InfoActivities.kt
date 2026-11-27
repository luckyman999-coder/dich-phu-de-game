// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - màn hình Hướng dẫn và Giới thiệu

package com.metallic.chiaki.dich

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.metallic.chiaki.dich.HomeActivity.Companion.ACCENT
import com.metallic.chiaki.dich.HomeActivity.Companion.BG
import com.metallic.chiaki.dich.HomeActivity.Companion.CARD
import com.metallic.chiaki.dich.HomeActivity.Companion.MUTED
import com.metallic.chiaki.dich.HomeActivity.Companion.STROKE
import com.metallic.chiaki.dich.HomeActivity.Companion.TEXT

/** Khung chung: nền tối, tiêu đề có nút quay lại */
abstract class DarkPageActivity: AppCompatActivity()
{
	protected lateinit var root: LinearLayout
	protected val dp get() = resources.displayMetrics.density
	protected fun px(v: Int) = (v * dp).toInt()

	abstract val pageTitle: String
	abstract fun build()

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		window.statusBarColor = BG
		window.navigationBarColor = BG
		val scroll = ScrollView(this).apply { setBackgroundColor(BG) }
		root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(px(20), px(20), px(20), px(40)) }
		scroll.addView(root)
		setContentView(scroll)
		val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		header.addView(text("‹", 28f, TEXT).apply { setPadding(0, 0, px(14), 0); setOnClickListener { finish() } })
		header.addView(text(pageTitle, 22f, TEXT, true))
		root.addView(header)
		build()
	}

	protected fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
		text = t
		textSize = size
		setTextColor(color)
		if(bold) setTypeface(typeface, Typeface.BOLD)
		setLineSpacing(0f, 1.15f)
	}

	protected fun section(t: String) = text(t, 13f, MUTED).apply { setPadding(px(4), px(24), 0, px(8)) }

	protected fun card() = LinearLayout(this).apply {
		orientation = LinearLayout.VERTICAL
		setPadding(px(16), px(14), px(16), px(14))
		background = GradientDrawable().apply { cornerRadius = px(18).toFloat(); setColor(CARD); setStroke(px(1), STROKE) }
	}

	protected fun addCard(c: LinearLayout) = root.addView(c, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = px(10) })

	protected fun link(label: String, onClick: () -> Unit) = text(label, 15f, ACCENT, true).apply {
		setPadding(0, px(8), 0, 0)
		setOnClickListener { onClick() }
	}

	protected fun openUrl(url: String) { try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch(_: Exception) {} }
}

class GuideActivity: DarkPageActivity()
{
	override val pageTitle = "Hướng dẫn"

	override fun build()
	{
		root.addView(text("4 bước là chơi được. Làm một lần, những lần sau chỉ cần bấm Chơi.", 14f, MUTED).apply { setPadding(0, px(4), 0, 0) })

		root.addView(section("BẮT ĐẦU NHANH"))
		step(1, "Lấy mã dịch miễn phí", "Mã Gemini (Google) để AI dịch. Mất khoảng 2 phút, không cần thẻ thanh toán.",
			"Lấy mã ngay ›") { startActivity(Intent(this, KeyGuideActivity::class.java)) }
		step(2, "Chuẩn bị máy PS5",
			"Trên PS5: Cài đặt → Hệ thống → Chơi từ xa → Bật Chơi từ xa.\n" +
			"Muốn bật máy từ xa: Cài đặt → Hệ thống → Tiết kiệm năng lượng → Tính năng khi ở chế độ nghỉ → bật \"Duy trì kết nối Internet\" và \"Cho phép bật PS5 từ mạng\".", null) {}
		step(3, "Ghép đôi lần đầu",
			"Ở màn hình chính bấm CHƠI VÀ DỊCH. App tự tìm PS5, mời đăng nhập PSN (nên dùng tài khoản PSN thứ 2), rồi hỏi mã 8 số.\n" +
			"Lấy mã trên PS5: Cài đặt → Hệ thống → Chơi từ xa → Liên kết thiết bị.", null) {}
		step(4, "Chọn game và chỉnh khung phụ đề",
			"Chọn game trong Sổ tay để AI giữ đúng tên nhân vật và xưng hô. Khi vào game: vuốt từ mép màn hình → \"Chỉnh khung phụ đề\" → kéo khung xanh trùm lên chỗ phụ đề → Xong.",
			"Mở Sổ tay game ›") { startActivity(Intent(this, GameBookActivity::class.java)) }

		root.addView(section("HIỆN PHỤ ĐỀ TRÊN TV"))
		addCard(card().apply {
			addView(text("Với Android TV / Google TV: cài app Dịch Game TV lên TV, ghép đôi bằng mã 4 số. Phụ đề hiện đè lên hình PS5, giọng đọc ra loa TV.", 14f, TEXT))
			addView(text("TV Samsung, LG: chưa hỗ trợ, phụ đề hiện trên điện thoại.", 13f, MUTED).apply { setPadding(0, px(6), 0, 0) })
			addView(link("Kết nối TV ›") { startActivity(Intent(this@GuideActivity, TvConnectActivity::class.java)) })
		})

		root.addView(section("MẸO"))
		addCard(card().apply {
			listOf(
				"Điện thoại và PS5 nên cùng Wi-Fi 5GHz, hoặc cắm dây mạng cho PS5.",
				"Bật hiện tên người nói trong cài đặt phụ đề của game: AI chọn xưng hô và giọng nam/nữ chuẩn hơn.",
				"Phụ đề hiện chậm hơn lời thoại khoảng 1–2 giây, vì phải đọc chữ và hỏi AI.",
				"Thêm 2–3 mã Gemini (tạo ở các dự án khác nhau) để ít bị hết lượt.",
				"Cắm sạc khi chơi lâu."
			).forEach { addView(text("•  $it", 14f, TEXT).apply { setPadding(0, px(3), 0, px(3)) }) }
		})

		root.addView(section("GẶP LỖI THƯỜNG GẶP"))
		faq("Báo \"Gemini lỗi 429 (hết lượt)\"", "Mã miễn phí đã hết lượt trong phút/ngày. Thêm mã Gemini khác hoặc mã Groq dự phòng, app tự chuyển.")
		faq("Không tìm thấy máy chơi game", "Kiểm tra cùng Wi-Fi, đã bật Chơi từ xa trên PS5. Vẫn không được thì dùng \"Nhập IP thủ công\".")
		faq("Dịch sai tên hoặc xưng hô", "Thêm nhân vật vào Sổ tay game kèm giới tính và cách xưng hô.")
		faq("Không có giọng đọc", "Cài giọng tiếng Việt cho máy: Cài đặt dịch → \"Cài giọng đọc tiếng Việt\".")

		root.addView(link("Xem thử bằng cảnh mẫu ›") { startActivity(Intent(this, DemoActivity::class.java)) }.apply { gravity = Gravity.CENTER; setPadding(0, px(20), 0, 0) })
	}

	private fun step(n: Int, title: String, desc: String, action: String?, onClick: () -> Unit)
	{
		val c = card()
		val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		row.addView(text("$n", 15f, BG, true).apply {
			gravity = Gravity.CENTER
			background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(ACCENT) }
		}, LinearLayout.LayoutParams(px(28), px(28)).apply { marginEnd = px(12) })
		row.addView(text(title, 16f, TEXT, true))
		c.addView(row)
		c.addView(text(desc, 14f, MUTED).apply { setPadding(px(40), px(6), 0, 0) })
		if(action != null) c.addView(link(action, onClick).apply { setPadding(px(40), px(8), 0, 0) })
		addCard(c)
	}

	private fun faq(q: String, a: String) = addCard(card().apply {
		addView(text(q, 15f, TEXT, true))
		addView(text(a, 14f, MUTED).apply { setPadding(0, px(4), 0, 0) })
	})
}

class AboutActivity: DarkPageActivity()
{
	override val pageTitle = "Giới thiệu"

	override fun build()
	{
		val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch(_: Exception) { "?" }
		addCard(card().apply {
			val name = LinearLayout(this@AboutActivity).apply { gravity = Gravity.CENTER_VERTICAL }
			name.addView(text("Dịch", 24f, TEXT, true))
			name.addView(text("Game", 24f, BG, true).apply {
				setPadding(px(8), 0, px(8), 0)
				background = GradientDrawable().apply { cornerRadius = px(8).toFloat(); setColor(ACCENT) }
			}, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = px(6) })
			addView(name)
			addView(text("Phiên bản Android $version", 13f, MUTED).apply { setPadding(0, px(4), 0, px(10)) })
			addView(text("Dịch Game đọc phụ đề tiếng Anh trong game PS5, dịch sang tiếng Việt bằng AI theo đúng văn phong từng game, hiện chữ và đọc to bằng giọng Việt — trên điện thoại hoặc trên TV.", 14f, TEXT))
		})

		root.addView(section("DỮ LIỆU VÀ QUYỀN RIÊNG TƯ"))
		addCard(card().apply {
			listOf(
				"Chữ phụ đề đọc được trên màn hình được gửi tới Google Gemini hoặc Groq bằng chính mã dịch của bạn để dịch. Hình ảnh game không được gửi đi.",
				"Đăng nhập PSN chỉ để lấy Account ID phục vụ ghép đôi máy. App không thấy và không lưu mật khẩu PSN.",
				"Mã dịch, sổ tay game và cài đặt chỉ lưu trên máy của bạn.",
				"Kết nối tới máy chơi game và TV diễn ra trong mạng Wi-Fi nhà bạn."
			).forEach { addView(text("•  $it", 14f, TEXT).apply { setPadding(0, px(3), 0, px(3)) }) }
		})

		root.addView(section("MÃ NGUỒN MỞ"))
		addCard(card().apply {
			addView(text("Phần kết nối máy chơi game dựa trên chiaki-ng, phát hành theo giấy phép GNU AGPL-3.0. Toàn bộ mã nguồn của Dịch Game được công khai theo cùng giấy phép.", 14f, TEXT))
			addView(link("Mã nguồn Dịch Game ›") { openUrl("https://github.com/luckyman999-coder/dich-phu-de-game") })
			addView(link("Dự án chiaki-ng ›") { openUrl("https://github.com/streetpea/chiaki-ng") })
			addView(link("Giấy phép AGPL-3.0 ›") { openUrl("https://www.gnu.org/licenses/agpl-3.0.html") })
		})

		root.addView(section("LƯU Ý"))
		addCard(card().apply {
			addView(text("Dịch Game là ứng dụng độc lập, không liên kết với Sony Interactive Entertainment. PlayStation, PS5 và PSN là thương hiệu của Sony Interactive Entertainment. Gemini là thương hiệu của Google; Groq là thương hiệu của Groq, Inc.", 13f, MUTED))
		})

		root.addView(link("Xem hướng dẫn ›") { startActivity(Intent(this, GuideActivity::class.java)) }.apply { gravity = Gravity.CENTER; setPadding(0, px(16), 0, 0) })
	}
}
