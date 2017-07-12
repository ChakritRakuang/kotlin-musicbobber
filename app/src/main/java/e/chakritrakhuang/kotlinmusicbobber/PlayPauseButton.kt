package e.chakritrakhuang.kotlinmusicbobber

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.support.v7.graphics.Palette
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView

import java.util.HashMap
import java.util.Random

/**
 * Collapsed state view.
 */
@SuppressLint("ViewConstructor")
internal class PlayPauseButton(configuration : Configuration) : ImageView(configuration.context()) , PlaybackState.PlaybackStateListener {

    private val albumPlaceholderPaint : Paint
    private val buttonPaint : Paint
    private val bubblesPaint : Paint
    private val progressPaint : Paint
    private val pausedColor : Int
    private val playingColor : Int
    private val bubbleSizes : FloatArray
    private val bubbleSpeeds : FloatArray
    private val bubbleSpeedCoefficients : FloatArray
    private val random : Random?
    private val colorChanger : ColorChanger
    private val playDrawable : Drawable
    private val pauseDrawable : Drawable
    private val bounds : RectF
    private val radius : Float
    private val playbackState : PlaybackState?
    private val touchDownAnimator : ValueAnimator
    private val touchUpAnimator : ValueAnimator
    private val bubblesAnimator : ValueAnimator
    private val progressAnimator : ValueAnimator
    private val buttonPadding : Float
    private val bubblesMinSize : Float
    private val bubblesMaxSize : Float
    @SuppressLint("UseSparseArrays")
    private val isNeedToFillAlbumCoverMap = HashMap<Int , Boolean>()

    var isAnimationInProgress : Boolean = false
        private set
    private var randomStartAngle : Float = 0.toFloat()
    private var buttonSize = 1.0f
    private var progress = 0.0f
    private var animatedProgress = 0f
    private var progressChangesEnabled : Boolean = false

    private var albumCover : Drawable? = null
    private var lastPaletteAsyncTask : AsyncTask<* , * , *>? = null
    private val hsvArray = FloatArray(3)

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE , null)
        this.playbackState = configuration.playbackState()
        this.random = configuration.random()
        this.buttonPaint = Paint()
        this.buttonPaint.color = configuration.lightColor()
        this.buttonPaint.style = Paint.Style.FILL
        this.buttonPaint.isAntiAlias = true
        this.buttonPaint.setShadowLayer(
                configuration.shadowRadius() ,
                configuration.shadowDx() ,
                configuration.shadowDy() ,
                configuration.shadowColor()
        )
        this.bubblesMinSize = configuration.bubblesMinSize()
        this.bubblesMaxSize = configuration.bubblesMaxSize()
        this.bubblesPaint = Paint()
        this.bubblesPaint.style = Paint.Style.FILL
        this.progressPaint = Paint()
        this.progressPaint.isAntiAlias = true
        this.progressPaint.style = Paint.Style.STROKE
        this.progressPaint.strokeWidth = configuration.progressStrokeWidth()
        this.progressPaint.color = configuration.progressColor()
        this.albumPlaceholderPaint = Paint()
        this.albumPlaceholderPaint.style = Paint.Style.FILL
        this.albumPlaceholderPaint.color = configuration.lightColor()
        this.albumPlaceholderPaint.isAntiAlias = true
        this.albumPlaceholderPaint.alpha = ALBUM_COVER_PLACEHOLDER_ALPHA
        this.pausedColor = configuration.lightColor()
        this.playingColor = configuration.darkColor()
        this.radius = configuration.radius()
        this.buttonPadding = configuration.buttonPadding().toFloat()
        this.bounds = RectF()
        this.bubbleSizes = FloatArray(TOTAL_BUBBLES_COUNT)
        this.bubbleSpeeds = FloatArray(TOTAL_BUBBLES_COUNT)
        this.bubbleSpeedCoefficients = FloatArray(TOTAL_BUBBLES_COUNT)
        this.colorChanger = ColorChanger()
        this.playDrawable = configuration.playDrawable() !!.constantState !!.newDrawable().mutate()
        this.pauseDrawable = configuration.pauseDrawable() !!.constantState !!.newDrawable().mutate()
        this.pauseDrawable.alpha = 0
        this.playbackState !!.addPlaybackStateListener(this)
        val listener = { animation ->
            buttonSize = animation.getAnimatedValue()
            invalidate()
        }
        this.touchDownAnimator = ValueAnimator.ofFloat(1 , 0.9f).setDuration(Configuration.TOUCH_ANIMATION_DURATION)
        this.touchDownAnimator.addUpdateListener(listener)
        this.touchUpAnimator = ValueAnimator.ofFloat(0.9f , 1).setDuration(Configuration.TOUCH_ANIMATION_DURATION)
        this.touchUpAnimator.addUpdateListener(listener)
        this.bubblesAnimator = ValueAnimator.ofInt(0 , ANIMATION_TIME_L.toInt()).setDuration(ANIMATION_TIME_L)
        this.bubblesAnimator.interpolator = LinearInterpolator()
        this.bubblesAnimator.addUpdateListener { animation ->
            val position = animation.currentPlayTime
            val fraction = animation.animatedFraction
            updateBubblesPosition(position , fraction)
            invalidate()
        }
        this.bubblesAnimator.addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationStart(animation : Animator) {
                super.onAnimationStart(animation)
                isAnimationInProgress = true
            }

            override fun onAnimationEnd(animation : Animator) {
                super.onAnimationEnd(animation)
                isAnimationInProgress = false
            }

            override fun onAnimationCancel(animation : Animator) {
                super.onAnimationCancel(animation)
                isAnimationInProgress = false
            }
        })
        this.progressAnimator = ValueAnimator()
        this.progressAnimator.addUpdateListener { animation ->
            animatedProgress = animation.animatedValue as Float
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec : Int , heightMeasureSpec : Int) {
        val size = View.MeasureSpec.makeMeasureSpec((radius * 4).toInt() , View.MeasureSpec.EXACTLY)
        super.onMeasure(size , size)
    }

    private fun updateBubblesPosition(position : Long , fraction : Float) {
        val alpha = DrawableUtils.customFunction(fraction , 0F , 0F , 0F , 0.3f , 255F , 0.5f , 225F , 0.7f , 0F , 1f).toInt()
        bubblesPaint.alpha = alpha
        if (DrawableUtils.isBetween(position.toFloat() , COLOR_ANIMATION_TIME_START_F , COLOR_ANIMATION_TIME_END_F)) {
            val colorDt = DrawableUtils.normalize(position.toFloat() , COLOR_ANIMATION_TIME_START_F , COLOR_ANIMATION_TIME_END_F)
            buttonPaint.color = colorChanger.nextColor(colorDt)
            if (playbackState !!.state() == Configuration.STATE_PLAYING) {
                pauseDrawable.alpha = DrawableUtils.between(255 * colorDt , 0f , 255f).toInt()
                playDrawable.alpha = DrawableUtils.between(255 * (1 - colorDt) , 0f , 255f).toInt()
            } else {
                playDrawable.alpha = DrawableUtils.between(255 * colorDt , 0f , 255f).toInt()
                pauseDrawable.alpha = DrawableUtils.between(255 * (1 - colorDt) , 0f , 255f).toInt()
            }
        }
        for (i in 0 .. TOTAL_BUBBLES_COUNT - 1) {
            bubbleSpeeds[i] = fraction * bubbleSpeedCoefficients[i]
        }
    }

    fun onClick() {
        if (isAnimationInProgress) {
            return
        }
        if (playbackState !!.state() == Configuration.STATE_PLAYING) {
            colorChanger
                    .fromColor(playingColor)
                    .toColor(pausedColor)
            bubblesPaint.color = pausedColor
        } else {
            colorChanger
                    .fromColor(pausedColor)
                    .toColor(playingColor)
            bubblesPaint.color = playingColor
        }
        startBubblesAnimation()
    }

    private fun startBubblesAnimation() {
        randomStartAngle = 360 * random !!.nextFloat()
        for (i in 0 .. TOTAL_BUBBLES_COUNT - 1) {
            val speed = 0.5f + 0.5f * random.nextFloat()
            val size = bubblesMinSize + (bubblesMaxSize - bubblesMinSize) * random.nextFloat()
            val radius = size / 2f
            bubbleSizes[i] = radius
            bubbleSpeedCoefficients[i] = speed
        }
        bubblesAnimator.start()
    }

    fun onTouchDown() {
        touchDownAnimator.start()
    }

    fun onTouchUp() {
        touchUpAnimator.start()
    }

    public override fun onDraw(canvas : Canvas) {
        val cx = (width shr 1).toFloat()
        val cy = (height shr 1).toFloat()
        canvas.scale(buttonSize , buttonSize , cx , cy)
        if (isAnimationInProgress) {
            for (i in 0 .. TOTAL_BUBBLES_COUNT - 1) {
                val angle = randomStartAngle + BUBBLES_ANGLE_STEP * i
                val speed = bubbleSpeeds[i]
                val x = DrawableUtils.rotateX(cx , cy * (1 - speed) , cx , cy , angle)
                val y = DrawableUtils.rotateY(cx , cy * (1 - speed) , cx , cy , angle)
                canvas.drawCircle(x , y , bubbleSizes[i] , bubblesPaint)
            }
        } else if (playbackState !!.state() != Configuration.STATE_PLAYING) {
            playDrawable.alpha = 255
            pauseDrawable.alpha = 0
            // in case widget was drawn without animation in different state
            if (buttonPaint.color != pausedColor) {
                buttonPaint.color = pausedColor
            }
        } else {
            playDrawable.alpha = 0
            pauseDrawable.alpha = 255
            // in case widget was drawn without animation in different state
            if (buttonPaint.color != playingColor) {
                buttonPaint.color = playingColor
            }
        }

        canvas.drawCircle(cx , cy , radius , buttonPaint)
        if (albumCover != null) {
            canvas.drawCircle(cx , cy , radius , buttonPaint)
            albumCover !!.setBounds((cx - radius).toInt() , (cy - radius).toInt() , (cx + radius).toInt() , (cy + radius).toInt())
            albumCover !!.draw(canvas)
            val isNeedToFillAlbumCover = isNeedToFillAlbumCoverMap[albumCover !!.hashCode()]
            if (isNeedToFillAlbumCover != null && isNeedToFillAlbumCover) {
                canvas.drawCircle(cx , cy , radius , albumPlaceholderPaint)
            }
        }

        val padding = progressPaint.strokeWidth / 2f
        bounds.set(cx - radius + padding , cy - radius + padding , cx + radius - padding , cy + radius - padding)
        canvas.drawArc(bounds , - 90f , animatedProgress , false , progressPaint)

        val l = (cx - radius + buttonPadding).toInt()
        val t = (cy - radius + buttonPadding).toInt()
        val r = (cx + radius - buttonPadding).toInt()
        val b = (cy + radius - buttonPadding).toInt()
        if (isAnimationInProgress || playbackState !!.state() != Configuration.STATE_PLAYING) {
            playDrawable.setBounds(l , t , r , b)
            playDrawable.draw(canvas)
        }
        if (isAnimationInProgress || playbackState !!.state() == Configuration.STATE_PLAYING) {
            pauseDrawable.setBounds(l , t , r , b)
            pauseDrawable.draw(canvas)
        }
    }

    override fun onStateChanged(oldState : Int , newState : Int , initiator : Any) {
        if (initiator is AudioWidget)
            return
        if (newState == Configuration.STATE_PLAYING) {
            buttonPaint.color = playingColor
            pauseDrawable.alpha = 255
            playDrawable.alpha = 0
        } else {
            buttonPaint.color = pausedColor
            pauseDrawable.alpha = 0
            playDrawable.alpha = 255
        }
        postInvalidate()
    }

    override fun onProgressChanged(position : Int , duration : Int , percentage : Float) {
        if (percentage > progress) {
            val old = progress
            post {
                if (animateProgressChanges(old * 360 , percentage * 360 , PROGRESS_STEP_DURATION)) {
                    progress = percentage
                }
            }
        } else {
            this.progress = percentage
            this.animatedProgress = percentage * 360
            postInvalidate()
        }

    }

    fun enableProgressChanges(enable : Boolean) {
        if (progressChangesEnabled == enable)
            return
        progressChangesEnabled = enable
        if (progressChangesEnabled) {
            animateProgressChangesForce(0f , progress * 360 , PROGRESS_CHANGES_DURATION)
        } else {
            animateProgressChangesForce(progress * 360 , 0f , PROGRESS_CHANGES_DURATION)
        }
    }

    private fun animateProgressChangesForce(oldValue : Float , newValue : Float , duration : Long) {
        if (progressAnimator.isRunning) {
            progressAnimator.cancel()
        }
        animateProgressChanges(oldValue , newValue , duration)
    }

    private fun animateProgressChanges(oldValue : Float , newValue : Float , duration : Long) : Boolean {
        if (progressAnimator.isRunning) {
            return false
        }
        progressAnimator.setFloatValues(oldValue , newValue)
        progressAnimator.duration = duration
        progressAnimator.start()
        return true
    }

    fun newBoundsChecker(offsetX : Int , offsetY : Int) : TouchManager.BoundsChecker {
        return BoundsCheckerImpl(radius , offsetX , offsetY)
    }

    fun albumCover(newAlbumCover : Drawable) {
        if (this.albumCover === newAlbumCover) return
        this.albumCover = newAlbumCover

        if (albumCover is BitmapDrawable && ! isNeedToFillAlbumCoverMap.containsKey(albumCover !!.hashCode())) {
            val bitmap = (albumCover as BitmapDrawable).bitmap
            if (bitmap != null && ! bitmap.isRecycled) {
                if (lastPaletteAsyncTask != null && ! lastPaletteAsyncTask !!.isCancelled) {
                    lastPaletteAsyncTask !!.cancel(true)
                }
                lastPaletteAsyncTask = Palette.from(bitmap).generate { palette ->
                    val dominantColor = palette.getDominantColor(Integer.MAX_VALUE)
                    if (dominantColor != Integer.MAX_VALUE) {
                        Color.colorToHSV(dominantColor , hsvArray)
                        isNeedToFillAlbumCoverMap.put(albumCover !!.hashCode() , hsvArray[2] > 0.65f)
                        postInvalidate()
                    }
                }
            }
        }
        postInvalidate()
    }

    private class BoundsCheckerImpl internal constructor(private val radius : Float , offsetX : Int , offsetY : Int) : AudioWidget.BoundsCheckerWithOffset(offsetX , offsetY) {

        public override fun stickyLeftSideImpl(screenWidth : Float) : Float {
            return - radius
        }

        public override fun stickyRightSideImpl(screenWidth : Float) : Float {
            return screenWidth - radius * 3
        }

        public override fun stickyBottomSideImpl(screenHeight : Float) : Float {
            return screenHeight - radius * 3
        }

        public override fun stickyTopSideImpl(screenHeight : Float) : Float {
            return - radius
        }
    }

    companion object {

        private val BUBBLES_ANGLE_STEP = 18.0f
        private val ANIMATION_TIME_F = 8 * Configuration.FRAME_SPEED
        private val ANIMATION_TIME_L = ANIMATION_TIME_F.toLong()
        private val COLOR_ANIMATION_TIME_F = ANIMATION_TIME_F / 4f
        private val COLOR_ANIMATION_TIME_START_F = (ANIMATION_TIME_F - COLOR_ANIMATION_TIME_F) / 2
        private val COLOR_ANIMATION_TIME_END_F = COLOR_ANIMATION_TIME_START_F + COLOR_ANIMATION_TIME_F
        private val TOTAL_BUBBLES_COUNT = (360 / BUBBLES_ANGLE_STEP).toInt()
        val PROGRESS_CHANGES_DURATION = (6 * Configuration.FRAME_SPEED).toLong()
        private val PROGRESS_STEP_DURATION = (3 * Configuration.FRAME_SPEED).toLong()
        private val ALBUM_COVER_PLACEHOLDER_ALPHA = 100
    }
}