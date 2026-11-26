// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL
// Dịch Game - khung kéo/thả để chọn vùng phụ đề

package com.metallic.chiaki.dich

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class RegionSelectView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null): View(context, attrs)
{
	/** Vùng chọn, toạ độ 0..1 so với view (view phủ đúng khung hình game) */
	var region = RectF(0.12f, 0.78f, 0.88f, 0.95f)
		set(v) { field = RectF(v); invalidate() }

	var editing = false
		set(v) { field = v; invalidate() }

	var onRegionChanged: ((RectF) -> Unit)? = null

	private val density = resources.displayMetrics.density
	private val handleRadius = 14 * density
	private val touchSlop = 28 * density
	private val minSize = 0.04f

	private val dimPaint = Paint().apply { color = Color.argb(110, 0, 0, 0) }
	private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.rgb(80, 220, 120); style = Paint.Style.STROKE; strokeWidth = 3 * density
	}
	private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80, 220, 120) }
	private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.WHITE; textSize = 14 * density; setShadowLayer(4f, 0f, 0f, Color.BLACK)
	}

	private enum class Drag { NONE, MOVE, TL, TR, BL, BR }
	private var drag = Drag.NONE
	private var lastX = 0f
	private var lastY = 0f

	private fun px(r: RectF) = RectF(r.left * width, r.top * height, r.right * width, r.bottom * height)

	override fun onDraw(canvas: Canvas)
	{
		super.onDraw(canvas)
		if(!editing) return
		val r = px(region)
		// làm tối phần ngoài khung
		canvas.drawRect(0f, 0f, width.toFloat(), r.top, dimPaint)
		canvas.drawRect(0f, r.bottom, width.toFloat(), height.toFloat(), dimPaint)
		canvas.drawRect(0f, r.top, r.left, r.bottom, dimPaint)
		canvas.drawRect(r.right, r.top, width.toFloat(), r.bottom, dimPaint)
		canvas.drawRect(r, borderPaint)
		for(p in listOf(PointF(r.left, r.top), PointF(r.right, r.top), PointF(r.left, r.bottom), PointF(r.right, r.bottom)))
			canvas.drawCircle(p.x, p.y, handleRadius, handlePaint)
		val label = "Kéo khung trùm lên chỗ phụ đề"
		val ty = if(r.top > 40 * density) r.top - 10 * density else r.bottom + 24 * density
		canvas.drawText(label, r.left, ty, labelPaint)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean
	{
		if(!editing) return false
		val r = px(region)
		when(event.actionMasked)
		{
			MotionEvent.ACTION_DOWN ->
			{
				val x = event.x; val y = event.y
				drag = when
				{
					near(x, y, r.left, r.top) -> Drag.TL
					near(x, y, r.right, r.top) -> Drag.TR
					near(x, y, r.left, r.bottom) -> Drag.BL
					near(x, y, r.right, r.bottom) -> Drag.BR
					r.contains(x, y) -> Drag.MOVE
					else -> Drag.NONE
				}
				lastX = x; lastY = y
				return true
			}
			MotionEvent.ACTION_MOVE ->
			{
				if(width == 0 || height == 0) return true
				val dx = (event.x - lastX) / width
				val dy = (event.y - lastY) / height
				lastX = event.x; lastY = event.y
				val n = RectF(region)
				when(drag)
				{
					Drag.MOVE ->
					{
						val ox = dx.coerceIn(-n.left, 1f - n.right)
						val oy = dy.coerceIn(-n.top, 1f - n.bottom)
						n.offset(ox, oy)
					}
					Drag.TL -> { n.left = clampLo(n.left + dx, n.right); n.top = clampLo(n.top + dy, n.bottom) }
					Drag.TR -> { n.right = clampHi(n.right + dx, n.left); n.top = clampLo(n.top + dy, n.bottom) }
					Drag.BL -> { n.left = clampLo(n.left + dx, n.right); n.bottom = clampHi(n.bottom + dy, n.top) }
					Drag.BR -> { n.right = clampHi(n.right + dx, n.left); n.bottom = clampHi(n.bottom + dy, n.top) }
					Drag.NONE -> {}
				}
				region = n
				return true
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
			{
				if(drag != Drag.NONE) onRegionChanged?.invoke(RectF(region))
				drag = Drag.NONE
				return true
			}
		}
		return true
	}

	private fun near(x: Float, y: Float, px: Float, py: Float) = abs(x - px) < touchSlop && abs(y - py) < touchSlop
	private fun clampLo(v: Float, other: Float) = max(0f, min(v, other - minSize))
	private fun clampHi(v: Float, other: Float) = min(1f, max(v, other + minSize))
}
