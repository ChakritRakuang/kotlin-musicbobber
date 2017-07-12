@file:Suppress("NAME_SHADOWING")

package e.chakritrakhuang.kotlinmusicbobber

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

/**
 * Remove widget view.
 */
@SuppressLint("ViewConstructor")
internal class RemoveWidgetView(configuration : Configuration) : View(configuration.context()) {

    private val size : Float
    private val radius : Float
    private val paint : Paint
    private val defaultColor : Int
    private val overlappedColor : Int
    private val sizeAnimator : ValueAnimator
    private var scale = 1.0f

    init {
        this.radius = configuration.radius()
        this.size = configuration.radius() * SCALE_LARGE * 2f
        this.paint = Paint()
        this.defaultColor = configuration.crossColor()
        this.overlappedColor = configuration.crossOverlappedColor()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = configuration.crossStrokeWidth()
        paint.color = configuration.crossColor()
        paint.strokeCap = Paint.Cap.ROUND
        sizeAnimator = ValueAnimator()
        sizeAnimator.addUpdateListener { animation ->
            scale = animation.animatedValue as Float
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec : Int , heightMeasureSpec : Int) {
        val size = View.MeasureSpec.makeMeasureSpec(this.size.toInt() , View.MeasureSpec.EXACTLY)
        super.onMeasure(size , size)
    }

    override fun onDraw(canvas : Canvas) {
        super.onDraw(canvas)
        val cx = canvas.width shr 1
        val cy = canvas.height shr 1
        val rad = radius * 0.75f
        canvas.save()
        canvas.scale(scale , scale , cx.toFloat() , cy.toFloat())
        canvas.drawCircle(cx.toFloat() , cy.toFloat() , rad , paint)
        drawCross(canvas , cx.toFloat() , cy.toFloat() , rad * 0.5f , 45f)
        canvas.restore()
    }

    private fun drawCross(canvas : Canvas , cx : Float , cy : Float , radius : Float , startAngle : Float) {
        drawLine(canvas , cx , cy , radius , startAngle)
        drawLine(canvas , cx , cy , radius , startAngle + 90)
    }

    private fun drawLine(canvas : Canvas , cx : Float , cy : Float , radius : Float , angle : Float) {
        var angle = angle
        val x1 = DrawableUtils.rotateX(cx , cy + radius , cx , cy , angle)
        val y1 = DrawableUtils.rotateY(cx , cy + radius , cx , cy , angle)
        angle += 180f
        val x2 = DrawableUtils.rotateX(cx , cy + radius , cx , cy , angle)
        val y2 = DrawableUtils.rotateY(cx , cy + radius , cx , cy , angle)
        canvas.drawLine(x1 , y1 , x2 , y2 , paint)
    }

    /**
     * Set overlapped state.
     * @param overlapped true if widget overlapped, false otherwise
     */
    fun setOverlapped(overlapped : Boolean) {
        sizeAnimator.cancel()
        if (overlapped) {
            sizeAnimator.setFloatValues(scale , SCALE_LARGE)
            if (paint.color != overlappedColor) {
                paint.color = overlappedColor
                invalidate()
            }
        } else {
            sizeAnimator.setFloatValues(scale , SCALE_DEFAULT)
            if (paint.color != defaultColor) {
                paint.color = defaultColor
                invalidate()
            }
        }
        sizeAnimator.start()
    }

    companion object {

        val SCALE_DEFAULT = 1.0f
        val SCALE_LARGE = 1.5f
    }
}