package com.example.crop916

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class CropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private var imageX = 0f
    private var imageY = 0f
    private var scale = 1f
    private var frameRect = RectF()
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B3000000")
    }

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    fun setBitmap(bmp: Bitmap) {
        bitmap = bmp
        imageX = 0f
        imageY = 0f
        scale = 1f
        updateFrame()
        invalidate()
    }

    fun getCroppedBitmap(): Bitmap? {
        val bmp = bitmap ?: return null

        val imgLeft = (frameRect.left - imageX) / scale
        val imgTop = (frameRect.top - imageY) / scale
        val imgRight = (frameRect.right - imageX) / scale
        val imgBottom = (frameRect.bottom - imageY) / scale

        val left = imgLeft.coerceIn(0f, bmp.width.toFloat() - 1)
        val top = imgTop.coerceIn(0f, bmp.height.toFloat() - 1)
        val right = imgRight.coerceIn(left + 1, bmp.width.toFloat())
        val bottom = imgBottom.coerceIn(top + 1, bmp.height.toFloat())

        val x = left.toInt()
        val y = top.toInt()
        val w = (right - left).toInt()
        val h = (bottom - top).toInt()

        return Bitmap.createBitmap(bmp, x, y, w, h)
    }

    private fun updateFrame() {
        val viewW = width.toFloat()
        val viewH = height.toFloat()

        if (viewW == 0f || viewH == 0f) return

        val frameW: Float
        val frameH: Float

        if (viewW / viewH > 9f / 16f) {
            frameH = viewH
            frameW = frameH * 9f / 16f
        } else {
            frameW = viewW
            frameH = frameW * 16f / 9f
        }

        val left = (viewW - frameW) / 2f
        val top = (viewH - frameH) / 2f
        frameRect.set(left, top, left + frameW, top + frameH)

        val bmp = bitmap ?: return
        val scaleX = frameW / bmp.width
        val scaleY = frameH / bmp.height
        scale = maxOf(scaleX, scaleY)

        val scaledW = bmp.width * scale
        val scaledH = bmp.height * scale
        imageX = frameRect.centerX() - scaledW / 2f
        imageY = frameRect.centerY() - scaledH / 2f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateFrame()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val bmp = bitmap ?: return

        canvas.save()
        canvas.translate(imageX, imageY)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bmp, 0f, 0f, imagePaint)
        canvas.restore()

        canvas.drawRect(0f, 0f, width.toFloat(), frameRect.top, overlayPaint)
        canvas.drawRect(0f, frameRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawRect(0f, frameRect.top, frameRect.left, frameRect.bottom, overlayPaint)
        canvas.drawRect(frameRect.right, frameRect.top, width.toFloat(), frameRect.bottom, overlayPaint)

        canvas.drawRect(frameRect, framePaint)

        val cornerLen = 40f
        canvas.drawLine(frameRect.left, frameRect.top, frameRect.left + cornerLen, frameRect.top, cornerPaint)
        canvas.drawLine(frameRect.left, frameRect.top, frameRect.left, frameRect.top + cornerLen, cornerPaint)
        canvas.drawLine(frameRect.right - cornerLen, frameRect.top, frameRect.right, frameRect.top, cornerPaint)
        canvas.drawLine(frameRect.right, frameRect.top, frameRect.right, frameRect.top + cornerLen, cornerPaint)
        canvas.drawLine(frameRect.left, frameRect.bottom - cornerLen, frameRect.left, frameRect.bottom, cornerPaint)
        canvas.drawLine(frameRect.left, frameRect.bottom, frameRect.left + cornerLen, frameRect.bottom, cornerPaint)
        canvas.drawLine(frameRect.right, frameRect.bottom - cornerLen, frameRect.right, frameRect.bottom, cornerPaint)
        canvas.drawLine(frameRect.right - cornerLen, frameRect.bottom, frameRect.right, frameRect.bottom, cornerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bitmap == null) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    imageX += dx
                    imageY += dy
                    clampImage()
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun clampImage() {
        val bmp = bitmap ?: return
        val scaledW = bmp.width * scale
        val scaledH = bmp.height * scale

        val minX = frameRect.right - scaledW
        val maxX = frameRect.left
        val minY = frameRect.bottom - scaledH
        val maxY = frameRect.top

        if (imageX < minX) imageX = minX
        if (imageX > maxX) imageX = maxX
        if (imageY < minY) imageY = minY
        if (imageY > maxY) imageY = maxY
    }
}
