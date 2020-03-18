package com.sandipan.paul.graphics

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.graphics.toRectF
import androidx.core.graphics.xor

class PhotoCropy(context: Context) : FrameLayout(context) {
    companion object {
        private const val MARGIN: Float = 50F
        private const val CENTRAL_VIEW_SIDE_LENGTH = MARGIN.toInt().times(4)
        private val BORDER_MARGIN = MARGIN.div(2)
    }

    private var viewInitialised = false
    private val centralView: LinearLayout
    private val pointers: List<LinearLayout>

    init {
        val width = this.resources.displayMetrics.widthPixels
        addView(
            getTouchLayout(
                (width.div(2) - CENTRAL_VIEW_SIDE_LENGTH.div(2)),
                (height.div(2) - CENTRAL_VIEW_SIDE_LENGTH.div(2)),
                CENTRAL_VIEW_SIDE_LENGTH,
                4
            )
        )
        this.centralView =
            this@PhotoCropy.getChildAt(this@PhotoCropy.childCount.minus(1)) as LinearLayout
        this.pointers = MutableList(0) { LinearLayout(this.context) }
        (0..1).map { i ->
            (0..1).map { j ->
                val index = j + i * 2
                this@PhotoCropy.addView(
                    getTouchLayout(
                        width * j,
                        width * i,
                        MARGIN.toInt().times(2),
                        index
                    )
                )
                this.pointers.add(this@PhotoCropy.getChildAt(this@PhotoCropy.childCount.minus(1)) as LinearLayout)
            }
        }
    }

    fun getCropRect(): RectF {
        val width = pointers[0].width.div(2)
        return RectF(
            pointers[0].x + width,
            pointers[0].y + width,
            pointers[3].x + width,
            pointers[3].y + width
        )
    }

    override fun attachViewToParent(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.attachViewToParent(child, index, params)
    }

    private fun initPolygon() {
        if (pointers[0].x < BORDER_MARGIN) {
            pointers[0].x =
                BORDER_MARGIN
            pointers[2].x =
                BORDER_MARGIN
        }
        if (pointers[0].y < BORDER_MARGIN) {
            pointers[0].y =
                BORDER_MARGIN
            pointers[1].y =
                BORDER_MARGIN
        }
        val width = this.resources.displayMetrics.widthPixels
        if (pointers[1].x + pointers[1].width > width - BORDER_MARGIN) {
            pointers[1].x = (width - pointers[1].width - BORDER_MARGIN)
            pointers[3].x = (width - pointers[3].width - BORDER_MARGIN)
        }
        if (pointers[3].y + pointers[3].height > width - BORDER_MARGIN) {
            pointers[3].y = (width - pointers[3].width - BORDER_MARGIN)
            pointers[2].y = (width - pointers[2].width - BORDER_MARGIN)
        }
    }

    private val minSideLengthCropRect = MARGIN * 6
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (!viewInitialised) {
            initPolygon()
            viewInitialised = true
        }
        var currentRect = getCropRect()
        if (currentRect.width() < minSideLengthCropRect) {
            if (tag == 0 || tag == 2) {
                pointers[0].x = pointers[1].x - minSideLengthCropRect
                pointers[2].x = pointers[0].x
            }
            if (tag == 1 || tag == 3) {
                pointers[1].x = pointers[0].x + minSideLengthCropRect
                pointers[3].x = pointers[1].x
            }
            currentRect = getCropRect()
        }
        if (currentRect.height() < minSideLengthCropRect) {
            if (tag == 0 || tag == 1) {
                pointers[0].y = pointers[2].y - minSideLengthCropRect
                pointers[1].y = pointers[0].y
            }
            if (tag == 2 || tag == 3) {
                pointers[2].y = pointers[0].y + minSideLengthCropRect
                pointers[3].y = pointers[2].y
            }
            currentRect = getCropRect()
        }
        drawRectAtCenter(canvas, currentRect)
        drawUnCroppedArea(canvas, currentRect)
        drawGrid(canvas, currentRect)
        drawCropRect(canvas, currentRect)
        drawPlusMarkAtCenter(
            canvas,
            currentRect.left + currentRect.width().div(2),
            currentRect.top + currentRect.width().div(2)
        )
        drawPointerCircles(canvas)
    }

    private fun drawRectAtCenter(canvas: Canvas, cropRect: RectF) {
        keepPointer4AtCenter(cropRect)
        val rect = RectF(
            centralView.x,
            centralView.y,
            centralView.x + centralView.width,
            centralView.y + centralView.height
        )
        val paint = Paint().apply {
            this.style = Paint.Style.FILL
            this.strokeWidth = 1f
            this.color = Color.parseColor("#20FFFFFF")
            this.isAntiAlias = true
        }
        canvas.drawPath(
            Path().apply { this.addRoundRect(rect, 10f, 10F, Path.Direction.CW) },
            paint
        )
        canvas.drawPath(
            Path().apply { this.addRoundRect(rect, 10f, 10F, Path.Direction.CW) },
            paint.apply {
                this.style = Paint.Style.STROKE
                this.color = Color.parseColor("#FFFFFF")
            })

        rect.apply {
            this.left = centralView.x.plus(7)
            this.top = centralView.y.plus(7)
            this.right = centralView.x.minus(7) + centralView.width
            this.bottom = centralView.y.minus(7) + centralView.height
        }
        paint.apply {
            this.style = Paint.Style.FILL
            this.color = Color.parseColor("#20FFFFFF")
            this.strokeWidth = 1f
            this.isAntiAlias = true
        }
        canvas.drawPath(
            Path().apply { this.addRoundRect(rect, 10f, 10F, Path.Direction.CW) },
            paint
        )
        canvas.drawPath(
            Path().apply { this.addRoundRect(rect, 10f, 10F, Path.Direction.CW) },
            paint.apply {
                this.style = Paint.Style.STROKE
                this.color = Color.parseColor("#FFFFFF")
            })
    }

    private fun keepPointer4AtCenter(cropRect: RectF) {
        centralView.x = cropRect.left + (cropRect.width().div(2) - CENTRAL_VIEW_SIDE_LENGTH.div(2))
        centralView.y = cropRect.top + (cropRect.height().div(2) - CENTRAL_VIEW_SIDE_LENGTH.div(2))
    }

    private fun drawUnCroppedArea(canvas: Canvas, cropRect: RectF) {
        val paint = Paint().apply {
            this.style = Paint.Style.FILL
            this.color = Color.parseColor("#46000000")
            this.isAntiAlias = true
        }
        val path = Rect(0, 0, width, width).toRectF().xor(cropRect).boundaryPath
        canvas.drawPath(path, paint)
    }

    private fun drawGrid(canvas: Canvas, cropRect: RectF) {
        if (isGridEnabled) {
            //0 until 3
            val paint = Paint().apply {
                this.style = Paint.Style.STROKE
                this.color = Color.parseColor("#F3F3F3")
                this.strokeWidth = 2f
                this.isAntiAlias = true
            }
            (0 until 3).map { i ->
                (0 until 3).map { j ->
                    val startx = cropRect.left + i * cropRect.width().div(3)
                    val starty = cropRect.top + j * cropRect.height().div(3)
                    canvas.drawLine(startx, starty, startx + cropRect.width().div(3), starty, paint)
                    canvas.drawLine(startx, starty, startx, starty + cropRect.width().div(3), paint)
                }
            }
        }
    }

    private fun drawCropRect(canvas: Canvas, cropRect: RectF) {
        val paint = Paint().apply {
            this.style = Paint.Style.STROKE
            this.color = Color.parseColor("#F3F3F3")
            this.strokeWidth = 2f
            this.isAntiAlias = true
        }
        val offset = 6.5F
        canvas.drawRect(
            RectF(
                cropRect.left + offset,
                cropRect.top + offset,
                cropRect.right - offset,
                cropRect.bottom - offset
            ), paint
        )
        canvas.drawRect(
            RectF(
                cropRect.left - offset,
                cropRect.top - offset,
                cropRect.right + offset,
                cropRect.bottom + offset
            ), paint
        )
        canvas.drawRect(cropRect, paint.apply {
            this.strokeWidth = 7f
        })
    }

    private fun drawPlusMarkAtCenter(canvas: Canvas, cx: Float, cy: Float) {
        val paint = Paint().apply {
            this.style = Paint.Style.STROKE
            this.color = Color.parseColor("#F3F3F3")
            this.strokeWidth = 5f
            this.isAntiAlias = true
        }
        val path = Path().apply {
            this.addRoundRect(
                RectF(cx.minus(20), cy.minus(3), cx.plus(20), cy.plus(3)),
                3f,
                3F,
                Path.Direction.CW
            )
            this.addRoundRect(
                RectF(cx.minus(3), cy.minus(20), cx.plus(3), cy.plus(20)),
                3f,
                3F,
                Path.Direction.CW
            )
        }
        canvas.drawPath(path, paint)
        paint.apply {
            this.style = Paint.Style.FILL
            this.color = Color.parseColor("#F3F3F3")
            this.strokeWidth = 3f
            this.isAntiAlias = true
        }
        canvas.drawLine(cx.minus(20), cy, cx.plus(20), cy, paint)
        canvas.drawLine(cx, cy.minus(20), cx, cy.plus(20), paint)
    }

    private fun drawPointerCircles(canvas: Canvas) {
        pointers.map {
            canvas.drawCircle(it.x + it.width.div(2), it.y + it.width.div(2), 25F, Paint().apply {
                this.style = Paint.Style.STROKE
                this.color = Color.parseColor("#FFFFFF")
                this.strokeWidth = 1f
                this.isAntiAlias = true
            })
            canvas.drawCircle(it.x + it.width.div(2), it.y + it.width.div(2), 24F, Paint().apply {
                this.style = Paint.Style.FILL
                this.color = Color.parseColor("#4BFFFFFF")
                this.isAntiAlias = true
            })
            canvas.drawCircle(it.x + it.width.div(2), it.y + it.width.div(2), 18F, Paint().apply {
                this.style = Paint.Style.STROKE
                this.color = Color.parseColor("#FFFFFF")
                this.strokeWidth = 1f
                this.isAntiAlias = true
            })
            canvas.drawCircle(it.x + it.width.div(2), it.y + it.width.div(2), 17F, Paint().apply {
                this.style = Paint.Style.FILL
                this.color = Color.parseColor("#4BFFFFFF")
                this.isAntiAlias = true
            })
            canvas.drawCircle(it.x + it.width.div(2), it.y + it.width.div(2), 10F, Paint().apply {
                this.style = Paint.Style.FILL
                this.color = Color.parseColor("#FFFFFF")
                this.isAntiAlias = true
            })
        }
    }

    private var isGridEnabled: Boolean = false
    @SuppressLint("ClickableViewAccessibility")
    private fun getTouchLayout(x: Int, y: Int, side: Int, tag: Int): LinearLayout {
        return LinearLayout(context).apply {
            this.layoutParams = LayoutParams(side, side)
            this.tag = tag
            this.x = x.toFloat()
            this.y = y.toFloat()
            this.setOnTouchListener(TouchListenerImpl())
        }
    }

    private var tag: Int = -1
    private var multiplierMap = hashMapOf(
        0 to intArrayOf(1, 1),
        1 to intArrayOf(1, -1),
        2 to intArrayOf(1, -1),
        3 to intArrayOf(1, 1)
    )

    private inner class TouchListenerImpl : OnTouchListener {
        private var cursorPositionOnDown = PointF()
        private var viewPositionOnDown = PointF()
        private var limitExceeds = false
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val currentPositionAfterDown = PointF(event.x, event.y)
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    tag = (view as LinearLayout).tag as Int
                    val pixcelsMoved = PointF(
                        currentPositionAfterDown.x - cursorPositionOnDown.x,
                        currentPositionAfterDown.y - cursorPositionOnDown.y
                    )
                    var tempX: Float
                    var tempY: Float
                    val multiplierPair =
                        multiplierMap.toList().firstOrNull { it.first == view.tag as Int }
                    if (multiplierPair == null) {
                        tempX = viewPositionOnDown.x + pixcelsMoved.x
                        tempY = viewPositionOnDown.y + pixcelsMoved.y
                        val diff = view.x - pointers[0].x

                        if (tempX - diff < BORDER_MARGIN) {
                            tempX = BORDER_MARGIN + diff
                        }
                        if (tempY - diff < BORDER_MARGIN) {
                            tempY = BORDER_MARGIN + diff
                        }
                        if (tempX + view.width + diff >= width - BORDER_MARGIN) {
                            tempX = width - view.width - BORDER_MARGIN - diff
                        }
                        if (tempY + view.width + diff >= width - BORDER_MARGIN) {
                            tempY = width - view.width - BORDER_MARGIN - diff
                        }
                        view.x = tempX
                        view.y = tempY

                        val cropRectWidth = pointers[1].x - pointers[0].x

                        pointers[0].x = view.x - diff
                        pointers[0].y = view.y - diff

                        pointers[1].x = pointers[0].x + cropRectWidth
                        pointers[1].y = pointers[0].y

                        pointers[2].x = pointers[0].x
                        pointers[2].y = pointers[0].y + cropRectWidth

                        pointers[3].x = pointers[2].x + cropRectWidth
                        pointers[3].y = pointers[2].y
                    } else {
                        tempX = viewPositionOnDown.x + pixcelsMoved.x * multiplierPair.second[0]
                        tempY = viewPositionOnDown.y + pixcelsMoved.x * multiplierPair.second[1]
                        if (tempX < BORDER_MARGIN) {
                            if (!limitExceeds) {
                                view.x =
                                    BORDER_MARGIN
                                limitExceeds = true
                            }
                        } else if (tempY < BORDER_MARGIN) {
                            if (!limitExceeds) {
                                view.y =
                                    BORDER_MARGIN
                                limitExceeds = true
                            }
                        } else if (tempX + view.width >= width - BORDER_MARGIN) {
                            if (!limitExceeds) {
                                view.x = width - view.width - BORDER_MARGIN
                                limitExceeds = true
                            }
                        } else if (tempY + view.width >= width - BORDER_MARGIN) {
                            if (!limitExceeds) {
                                view.y = width - view.width - BORDER_MARGIN
                                limitExceeds = true
                            }
                        } else {
                            view.x = tempX
                            view.y = tempY
                        }

                        when (multiplierPair.first) {
                            0 -> {
                                pointers[1].y = view.y
                                pointers[2].x = view.x
                            }
                            1 -> {
                                pointers[0].y = view.y
                                pointers[3].x = view.x
                            }
                            2 -> {
                                pointers[0].x = view.x
                                pointers[3].y = view.y
                            }
                            3 -> {
                                pointers[1].x = view.x
                                pointers[2].y = view.y
                            }
                        }
                    }
                    viewPositionOnDown = PointF(view.x, view.y)
                }
                MotionEvent.ACTION_DOWN -> {
                    cursorPositionOnDown.x = currentPositionAfterDown.x
                    cursorPositionOnDown.y = currentPositionAfterDown.y
                    viewPositionOnDown = PointF(view.x, view.y)
                    isGridEnabled = true
                }
                MotionEvent.ACTION_UP -> {
                    isGridEnabled = false
                }
            }
            this@PhotoCropy.invalidate()
            return true
        }
    }
}