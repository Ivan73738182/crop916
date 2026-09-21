package com.example.crop916

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class CropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private var originalBitmap: Bitmap? = null
    private var imageX = 0f
    private var imageY = 0f
    private var scale = 1f
    private var userScale = 1f
    private var rotation = 0f
    private var frameRect = RectF()
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private var targetRatio = 9f / 16f
    private var savedX = 0f
    private var savedY = 0f

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                userScale *= detector.scaleFactor
                userScale = userScale.coerceIn(0.5f, 3f)
                clampImage()
                invalidate()
                return true
            }
        })

    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B3000000") }
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

    fun setRatio(ratio: Float) {
        targetRatio = ratio
        updateFrame()
        invalidate()
    }

    fun setBitmap(bmp: Bitmap) {
        originalBitmap = bmp
        bitmap = bmp
        imageX = 0f
        imageY = 0f
        scale = 1f
        userScale = 1f
        rotation = 0f
        updateFrame()
        invalidate()
    }

    fun rotate() {
        val bmp = bitmap ?: return
        val matrix = Matrix()
        matrix.postRotate(90f)
        bitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        userScale = 1f
        updateFrame()
        invalidate()
    }

    fun centerImage() {
        savedX = imageX
        savedY = imageY
        updateFrame()
        invalidate()
    }

    fun undo() {
        imageX = savedX
        imageY = savedY
        clampImage()
        invalidate()
    }

    fun getCroppedBitmap(): Bitmap? {
        val bmp = bitmap ?: return null
        val imgLeft = (frameRect.left - imageX) / (scale * userScale)
        val imgTop = (frameRect.top - imageY) / (scale * userScale)
        val imgRight = (frameRect.right - imageX) / (scale * userScale)
        val imgBottom = (frameRect.bottom - imageY) / (scale * userScale)

        val left = imgLeft.coerceIn(0f, bmp.width.toFloat() - 1)
        val top = imgTop.coerceIn(0f, bmp.height.toFloat() - 1)
        val right = imgRight.coerceIn(left + 1, bmp.width.toFloat())
        val bottom = imgBottom.coerceIn(top + 1, bmp.height.toFloat())

        return Bitmap.createBitmap(bmp, left.toInt(), top.toInt(), (right - left).toInt(), (bottom - top).toInt())
    }

    private fun updateFrame() {
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        if (viewW == 0f || viewH == 0f) return

        val frameW: Float
        val frameH: Float
        if (viewW / viewH > targetRatio) {
            frameH = viewH
            frameW = frameH * targetRatio
        } else {
            frameW = viewW
            frameH = frameW / targetRatio
        }

        val left = (viewW - frameW) / 2f
        val top = (viewH - frameH) / 2f
        frameRect.set(left, top, left + frameW, top + frameH)

        val bmp = bitmap ?: return
        val scaleX = frameW / bmp.width
        val scaleY = frameH / bmp.height
        scale = maxOf(scaleX, scaleY)

        val totalScale = scale * userScale
        val scaledW = bmp.width * totalScale
        val scaledH = bmp.height * totalScale
        imageX = frameRect.centerX() - scaledW / 2f
        imageY = frameRect.centerY() - scaledH / 2f
        clampImage()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateFrame()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return

        val totalScale = scale * userScale
        canvas.save()
        canvas.translate(imageX, imageY)
        canvas.scale(totalScale, totalScale)
        canvas.drawBitmap(bmp, 0f, 0f, imagePaint)
        canvas.restore()

        canvas.drawRect(0f, 0f, width.toFloat(), frameRect.top, overlayPaint)
        canvas.drawRect(0f, frameRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawRect(0f, frameRect.top, frameRect.left, frameRect.bottom, overlayPaint)
        canvas.drawRect(frameRect.right, frameRect.top, width.toFloat(), frameRect.bottom, overlayPaint)

        canvas.drawRect(frameRect, framePaint)

        val c = 40f
        canvas.drawLine(frameRect.left, frameRect.top, frameRect.left + c, frameRect.top, cornerPaint)
        canvas.drawLine(frameRect.left, frameRect.top, frameRect.left, frameRect.top + c, cornerPaint)
        canvas.drawLine(frameRect.right - c, frameRect.top, frameRect.right, frameRect.top, cornerPaint)
        canvas.drawLine(frameRect.right, frameRect.top, frameRect.right, frameRect.top + c, cornerPaint)
        canvas.drawLine(frameRect.left, frameRect.bottom - c, frameRect.left, frameRect.bottom, cornerPaint)
        canvas.drawLine(frameRect.left, frameRect.bottom, frameRect.left + c, frameRect.bottom, cornerPaint)
        canvas.drawLine(frameRect.right, frameRect.bottom - c, frameRect.right, frameRect.bottom, cornerPaint)
        canvas.drawLine(frameRect.right - c, frameRect.bottom, frameRect.right, frameRect.bottom, cornerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (bitmap == null) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && !scaleDetector.isInProgress) {
                    imageX += event.x - lastTouchX
                    imageY += event.y - lastTouchY
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
        val totalScale = scale * userScale
        val scaledW = bmp.width * totalScale
        val scaledH = bmp.height * totalScale
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
