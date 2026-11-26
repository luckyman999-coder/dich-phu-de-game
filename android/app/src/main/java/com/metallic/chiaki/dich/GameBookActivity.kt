// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - màn hình Sổ tay game

package com.metallic.chiaki.dich

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.metallic.chiaki.dich.HomeActivity.Companion.ACCENT
import com.metallic.chiaki.dich.HomeActivity.Companion.BG
import com.metallic.chiaki.dich.HomeActivity.Companion.CARD
import com.metallic.chiaki.dich.HomeActivity.Companion.MUTED
import com.metallic.chiaki.dich.HomeActivity.Companion.STROKE
import com.metallic.chiaki.dich.HomeActivity.Companion.TEXT

class GameBookActivity: AppCompatActivity()
{
	private lateinit var book: GameBook
	private lateinit var root: LinearLayout
	private val dp get() = resources.displayMetrics.density
	private fun px(v: Int) = (v * dp).toInt()

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		book = GameBook(this)
		window.statusBarColor = BG
		window.navigationBarColor = BG
		val scroll = ScrollView(this).apply { setBackgroundColor(BG) }
		root = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(px(20), px(20), px(20), px(40))
		}
		scroll.addView(root)
		setContentView(scroll)
		render()
	}

	/** Dựng lại toàn bộ màn hình (dữ liệu nhỏ nên đơn giản là vẽ lại) */
	private fun render()
	{
		root.removeAllViews()
		val cur = book.current

		// tiêu đề
		val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		header.addView(text("‹", 28f, TEXT).apply { setPadding(0, 0, px(14), 0); setOnClickListener { finish() } })
		header.addView(text("Sổ tay game", 22f, TEXT, bold = true))
		root.addView(header)
		root.addView(text("AI đọc sổ tay này mỗi lần dịch để giữ đúng tên, giới tính và cách xưng hô.", 14f, MUTED).apply { setPadding(0, px(4), 0, 0) })

		// chọn game: dạng chip cuộn ngang
		root.addView(section("ĐANG DÙNG"))
		val chipScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
		val chips = LinearLayout(this)
		chips.addView(chip("Chung", cur == null) { book.select(null); render() })
		book.games.forEach { g -> chips.addView(chip(g.name, cur?.id == g.id) { book.select(g.id); render() }) }
		chips.addView(chip("＋ Thêm", false, outline = true) { addGameDialog() })
		chipScroll.addView(chips)
		root.addView(chipScroll)

		if(cur == null)
		{
			root.addView(card().apply {
				setPadding(px(16), px(16), px(16), px(16))
				addView(text("Đang dịch chế độ chung", 16f, TEXT, bold = true))
				addView(text("AI tự đoán văn phong. Chọn hoặc thêm game để bản dịch chuẩn hơn, nhất là tên nhân vật và xưng hô.", 14f, MUTED).apply { setPadding(0, px(6), 0, px(12)) })
				addView(button("📚  Chọn từ thư viện ${GamePresets.all.size} game") { libraryDialog() })
			}, lpTop(16))
			return
		}

		// văn phong
		root.addView(section("VĂN PHONG"))
		val genreScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
		val genres = LinearLayout(this)
		Genre.values().forEach { g ->
			genres.addView(chip(g.label, cur.genre == g) { book.upsert(cur.copy(genre = g)); render() })
		}
		genreScroll.addView(genres)
		root.addView(genreScroll)

		// bối cảnh
		root.addView(section("BỐI CẢNH CÂU CHUYỆN"))
		val ctxCard = card().apply { setPadding(px(14), px(10), px(14), px(14)) }
		val ctxEdit = edit("Cốt truyện, quan hệ giữa các nhân vật, thời đại…", cur.context, multiline = true)
		ctxCard.addView(ctxEdit)
		ctxCard.addView(button("Lưu bối cảnh") {
			book.upsert(cur.copy(context = ctxEdit.text.toString()))
			Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show()
		}, lpTop(10))
		root.addView(ctxCard)

		// từ điển
		val dictHeader = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
		dictHeader.addView(section("TỪ ĐIỂN (${cur.entries.size})"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		dictHeader.addView(text("＋ Thêm mục", 14f, ACCENT).apply {
			setPadding(0, px(24), 0, px(8))
			setOnClickListener { entryDialog(cur, null) }
		})
		root.addView(dictHeader)
		val dict = card()
		if(cur.entries.isEmpty())
			dict.addView(text("Chưa có mục nào. Thêm tên nhân vật kèm giới tính để giọng đọc nam/nữ đúng người.", 14f, MUTED).apply { setPadding(px(16), px(16), px(16), px(16)) })
		cur.entries.forEachIndexed { i, e ->
			val row = LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(px(16), px(12), px(16), px(12))
				setOnClickListener { entryDialog(cur, i) }
			}
			val g = when(e.gender) { Gender.MALE -> " · ♂ nam"; Gender.FEMALE -> " · ♀ nữ"; else -> "" }
			row.addView(text(e.term, 16f, TEXT, bold = true))
			row.addView(text(e.kind.label + g, 12f, ACCENT))
			if(e.note.isNotBlank()) row.addView(text(e.note, 14f, MUTED))
			dict.addView(row)
			if(i < cur.entries.size - 1) dict.addView(View(this).apply { setBackgroundColor(STROKE) }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
		}
		root.addView(dict)

		// xem trước nội dung gửi AI + xoá game
		root.addView(section("AI SẼ NHẬN ĐƯỢC"))
		root.addView(card().apply {
			setPadding(px(14), px(12), px(14), px(12))
			addView(text(GameBook.render(cur).ifEmpty { "(trống)" }, 13f, MUTED).apply { typeface = Typeface.MONOSPACE })
		})
		root.addView(text("Xoá game này khỏi sổ tay", 14f, Color.rgb(248, 113, 113)).apply {
			gravity = Gravity.CENTER
			setPadding(0, px(24), 0, 0)
			setOnClickListener {
				MaterialAlertDialogBuilder(this@GameBookActivity)
					.setMessage("Xoá \"${cur.name}\" và toàn bộ ghi chú của game này?")
					.setPositiveButton("Xoá") { _, _ -> book.delete(cur.id); render() }
					.setNegativeButton("Huỷ", null)
					.show()
			}
		})
	}

	// ---------------- hộp thoại ----------------

	private fun addGameDialog()
	{
		MaterialAlertDialogBuilder(this)
			.setTitle("Thêm game")
			.setItems(arrayOf("📚  Chọn từ thư viện (${GamePresets.all.size} game)", "✏️  Tự nhập tên game")) { _, which ->
				if(which == 0) libraryDialog() else newGameDialog()
			}
			.show()
	}

	private fun libraryDialog()
	{
		val names = GamePresets.all.map { it.name }.toTypedArray()
		MaterialAlertDialogBuilder(this)
			.setTitle("Thư viện game")
			.setItems(names) { _, i -> book.addPreset(GamePresets.all[i]); render() }
			.show()
	}

	private fun newGameDialog()
	{
		val input = EditText(this).apply { hint = "Tên game, ví dụ: Stellar Blade"; inputType = InputType.TYPE_CLASS_TEXT }
		val box = FrameLayout(this).apply { setPadding(px(20), px(8), px(20), 0); addView(input) }
		MaterialAlertDialogBuilder(this)
			.setTitle("Tên game")
			.setView(box)
			.setPositiveButton("Thêm") { _, _ ->
				val name = input.text.toString().trim()
				if(name.isNotEmpty()) { book.create(name); render() }
			}
			.setNegativeButton("Huỷ", null)
			.show()
	}

	private fun entryDialog(game: GameProfile, index: Int?)
	{
		val old = index?.let { game.entries[it] }
		val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(px(20), px(8), px(20), 0) }
		val term = EditText(this).apply { hint = "Tên / từ, ví dụ: Atreus"; setText(old?.term ?: "") }
		box.addView(term)
		val kinds = EntryKind.values()
		val kindSpinner = Spinner(this).apply {
			adapter = ArrayAdapter(this@GameBookActivity, android.R.layout.simple_spinner_dropdown_item, kinds.map { it.label })
			setSelection(kinds.indexOf(old?.kind ?: EntryKind.CHARACTER))
		}
		box.addView(kindSpinner)
		val genders = arrayOf(Gender.UNKNOWN, Gender.MALE, Gender.FEMALE)
		val genderGroup = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
		listOf("Chưa rõ", "Nam", "Nữ").forEachIndexed { i, l ->
			genderGroup.addView(RadioButton(this).apply { id = 1000 + i; text = l; isChecked = genders[i] == (old?.gender ?: Gender.UNKNOWN) })
		}
		box.addView(genderGroup)
		val note = EditText(this).apply {
			hint = "Cách dịch / vai trò / xưng hô, ví dụ: con trai Kratos, xưng \"con\", gọi Kratos là \"cha\""
			setText(old?.note ?: "")
			minLines = 2
		}
		box.addView(note)

		val dlg = MaterialAlertDialogBuilder(this)
			.setTitle(if(old == null) "Thêm vào từ điển" else "Sửa mục")
			.setView(box)
			.setPositiveButton("Lưu") { _, _ ->
				val t = term.text.toString().trim()
				if(t.isEmpty()) return@setPositiveButton
				val gi = (genderGroup.checkedRadioButtonId - 1000).coerceIn(0, 2)
				val e = BookEntry(t, kinds[kindSpinner.selectedItemPosition], genders[gi], note.text.toString().trim())
				val list = game.entries.toMutableList()
				if(index == null) list.add(e) else list[index] = e
				book.upsert(game.copy(entries = list))
				render()
			}
			.setNegativeButton("Huỷ", null)
		if(old != null)
			dlg.setNeutralButton("Xoá") { _, _ ->
				book.upsert(game.copy(entries = game.entries.filterIndexed { i, _ -> i != index }))
				render()
			}
		dlg.show()
	}

	// ---------------- thành phần giao diện ----------------

	private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
		text = t
		textSize = size
		setTextColor(color)
		if(bold) setTypeface(typeface, Typeface.BOLD)
	}

	private fun section(t: String) = text(t, 13f, MUTED).apply { setPadding(px(4), px(24), 0, px(8)) }

	private fun card() = LinearLayout(this).apply {
		orientation = LinearLayout.VERTICAL
		background = GradientDrawable().apply { cornerRadius = px(18).toFloat(); setColor(CARD); setStroke(px(1), STROKE) }
	}

	private fun lpTop(top: Int) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = px(top) }

	private fun chip(label: String, selected: Boolean, outline: Boolean = false, onClick: () -> Unit) = TextView(this).apply {
		text = label
		textSize = 14f
		setTextColor(if(selected) BG else if(outline) ACCENT else TEXT)
		setPadding(px(14), px(8), px(14), px(8))
		background = GradientDrawable().apply {
			cornerRadius = px(18).toFloat()
			setColor(if(selected) ACCENT else CARD)
			setStroke(px(1), if(outline) ACCENT else STROKE)
		}
		layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = px(8) }
		setOnClickListener { onClick() }
	}

	private fun button(label: String, onClick: () -> Unit) = TextView(this).apply {
		text = label
		textSize = 15f
		setTypeface(typeface, Typeface.BOLD)
		setTextColor(BG)
		gravity = Gravity.CENTER
		setPadding(0, px(12), 0, px(12))
		background = GradientDrawable().apply { cornerRadius = px(12).toFloat(); setColor(ACCENT) }
		setOnClickListener { onClick() }
	}

	private fun edit(hint: String, value: String, multiline: Boolean = false) = EditText(this).apply {
		this.hint = hint
		setText(value)
		setTextColor(TEXT)
		setHintTextColor(MUTED)
		textSize = 15f
		if(multiline)
		{
			inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
			minLines = 3
			gravity = Gravity.TOP or Gravity.START
		}
	}
}
