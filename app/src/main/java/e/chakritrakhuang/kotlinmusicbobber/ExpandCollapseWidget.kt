package e.chakritrakhuang.kotlinmusicbobber

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView

import java.util.Random

@SuppressLint("ViewConstructor")
internal class ExpandCollapseWidget(configuration : Configuration) : ImageView(configuration.context()) , PlaybackState.PlaybackStateListener {

    private val paint : Paint
    private val radius : Float
    private val widgetWidth : Float
    private val widgetHeight : Float
    private val colorChanger : ColorChanger
    private val playColor : Int
    private val pauseColor : Int
    private val widgetColor : Int
    private val drawables : Array<Drawable>
    private val buttonBounds : Array<Rect>
    private val sizeStep : Float
    private val bubbleSizes : FloatArray
    private val bubbleSpeeds : FloatArray
    private val bubblePositions : FloatArray
    private val bubblesMinSize : Float
    private val bubblesMaxSize : Float
    private val random : Random?
    private val bubblesPaint : Paint
    private val bounds : RectF
    private val tmpRect : Rect
    private val playbackState : PlaybackState?
    private val expandAnimator : ValueAnimator
    private val collapseAnimator : ValueAnimator
    private val defaultAlbumCover : Drawable
    private val buttonPadding : Int
    private val prevNextExtraPadding : Int
    private val accDecInterpolator : Interpolator?
    private val touchDownAnimator : ValueAnimator
    private val touchUpAnimator : ValueAnimator
    private val bubblesTouchAnimator : ValueAnimator

    private var bubblesTime : Float = 0.toFloat()
    private var expanded : Boolean = false
    private var animatingExpand : Boolean = false
    private var animatingCollapse : Boolean = false
    private var expandDirection : Int = 0
    private var onWidgetStateChangedListener : AudioWidget.OnWidgetStateChangedListener? = null
    private val padding : Int
    private var onControlsClickListener : AudioWidget.OnControlsClickListener? = null
    private var touchedButtonIndex : Int = 0

    private var expandListener : AnimationProgressListener? = null
    private var collapseListener : AnimationProgressListener? = null

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE , null)
        this.playbackState = configuration.playbackState()
        this.accDecInterpolator = configuration.accDecInterpolator()
        this.random = configuration.random()
        this.bubblesPaint = Paint()
        this.bubblesPaint.style = Paint.Style.FILL
        this.bubblesPaint.isAntiAlias = true
        this.bubblesPaint.color = configuration.expandedColor()
        this.bubblesPaint.alpha = 0
        this.paint = Paint()
        this.paint.color = configuration.expandedColor()
        this.paint.isAntiAlias = true
        this.paint.setShadowLayer(
                configuration.shadowRadius() ,
                configuration.shadowDx() ,
                configuration.shadowDy() ,
                configuration.shadowColor()
        )
        this.radius = configuration.radius()
        this.widgetWidth = configuration.widgetWidth()
        this.colorChanger = ColorChanger()
        this.playColor = configuration.darkColor()
        this.pauseColor = configuration.lightColor()
        this.widgetColor = configuration.expandedColor()
        this.buttonPadding = configuration.buttonPadding()
        this.prevNextExtraPadding = configuration.prevNextExtraPadding()
        this.bubblesMinSize = configuration.bubblesMinSize()
        this.bubblesMaxSize = configuration.bubblesMaxSize()
        this.tmpRect = Rect()
        this.buttonBounds = arrayOfNulls(5)
        this.drawables = arrayOfNulls(6)
        this.bounds = RectF()
        this.drawables[INDEX_PLAYLIST] = configuration.playlistDrawable() !!.constantState !!.newDrawable().mutate()
        this.drawables[INDEX_PREV] = configuration.prevDrawable() !!.constantState !!.newDrawable().mutate()
        this.drawables[INDEX_PLAY] = configuration.playDrawable() !!.constantState !!.newDrawable().mutate()
        this.drawables[INDEX_PAUSE] = configuration.pauseDrawable() !!.constantState !!.newDrawable().mutate()
        this.drawables[INDEX_NEXT] = configuration.nextDrawable() !!.constantState !!.newDrawable().mutate()
        defaultAlbumCover = configuration.albumDrawable() !!.constantState !!.newDrawable().mutate()
        this.drawables[INDEX_ALBUM] = defaultAlbumCover
        this.sizeStep = widgetWidth / 5f
        this.widgetHeight = radius * 2
        for (i in buttonBounds.indices) {
            buttonBounds[i] = Rect()
        }
        this.bubbleSizes = FloatArray(TOTAL_BUBBLES_COUNT)
        this.bubbleSpeeds = FloatArray(TOTAL_BUBBLES_COUNT)
        this.bubblePositions = FloatArray(TOTAL_BUBBLES_COUNT * 2)
        this.playbackState !!.addPlaybackStateListener(this)

        this.expandAnimator = ValueAnimator.ofPropertyValuesHolder(
                PropertyValuesHolder.ofFloat("percent" , 0f , 1f) ,
                PropertyValuesHolder.ofInt("expandPosition" , 0 , EXPAND_DURATION_L.toInt()) ,
                PropertyValuesHolder.ofFloat("alpha" , 0f , 1f , 1f , 1f , 1f , 1f , 1f , 1f , 1f , 1f , 1f , 1f , 1f)
        ).setDuration(EXPAND_DURATION_L)

        val interpolator = LinearInterpolator()
        this.expandAnimator.interpolator = interpolator
        this.expandAnimator.addUpdateListener { animation ->
            updateExpandAnimation((animation.getAnimatedValue("expandPosition") as Int).toLong())
            alpha = animation.getAnimatedValue("alpha") as Float
            invalidate()

            if (expandListener != null) {
                expandListener !!.onValueChanged(animation.getAnimatedValue("percent") as Float)
            }
        }
        this.expandAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation : Animator) {
                super.onAnimationStart(animation)
                animatingExpand = true
            }

            override fun onAnimationEnd(animation : Animator) {
                super.onAnimationEnd(animation)
                animatingExpand = false
                expanded = true
                if (onWidgetStateChangedListener != null) {
                    onWidgetStateChangedListener !!.onWidgetStateChanged(AudioWidget.State.EXPANDED)
                }
            }

            override fun onAnimationCancel(animation : Animator) {
                super.onAnimationCancel(animation)
                animatingExpand = false
            }
        })
        this.collapseAnimator = ValueAnimator.ofPropertyValuesHolder(
                PropertyValuesHolder.ofFloat("percent" , 0f , 1f) ,
                PropertyValuesHolder.ofInt("expandPosition" , 0 , COLLAPSE_DURATION_L.toInt()) ,
                PropertyValuesHolder.ofFloat("alpha" , 1f , 1f , 1f , 1f , 1f , 1f , 1f , 1f , 1f , 1f , 0f)
        ).setDuration(COLLAPSE_DURATION_L)
        this.collapseAnimator.interpolator = interpolator
        this.collapseAnimator.addUpdateListener { animation ->
            updateCollapseAnimation((animation.getAnimatedValue("expandPosition") as Int).toLong())
            alpha = animation.getAnimatedValue("alpha") as Float
            invalidate()

            if (collapseListener != null) {
                collapseListener !!.onValueChanged(animation.getAnimatedValue("percent") as Float)
            }
        }
        this.collapseAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation : Animator) {
                super.onAnimationStart(animation)
                animatingCollapse = true
            }

            override fun onAnimationEnd(animation : Animator) {
                super.onAnimationEnd(animation)
                animatingCollapse = false
                expanded = false
                if (onWidgetStateChangedListener != null) {
                    onWidgetStateChangedListener !!.onWidgetStateChanged(AudioWidget.State.COLLAPSED)
                }
            }

            override fun onAnimationCancel(animation : Animator) {
                super.onAnimationCancel(animation)
                animatingCollapse = false
            }
        })
        this.padding = configuration.context() !!.resources.getDimensionPixelSize(R.dimen.aw_expand_collapse_widget_padding)
        val listener = { animation ->
            if (touchedButtonIndex == - 1 || touchedButtonIndex >= buttonBounds.size) {
                return
            }
            calculateBounds(touchedButtonIndex , tmpRect)
            val rect = buttonBounds[touchedButtonIndex]
            val width = tmpRect.width() * animation.getAnimatedValue() as Float / 2
            val height = tmpRect.height() * animation.getAnimatedValue() as Float / 2
            val l = (tmpRect.centerX() - width).toInt()
            val r = (tmpRect.centerX() + width).toInt()
            val t = (tmpRect.centerY() - height).toInt()
            val b = (tmpRect.centerY() + height).toInt()
            rect.set(l , t , r , b)
            invalidate(rect)
        }
        touchDownAnimator = ValueAnimator.ofFloat(1F , 0.9f).setDuration(Configuration.TOUCH_ANIMATION_DURATION)
        touchDownAnimator.addUpdateListener(listener)
        touchUpAnimator = ValueAnimator.ofFloat(0.9f , 1f).setDuration(Configuration.TOUCH_ANIMATION_DURATION)
        touchUpAnimator.addUpdateListener(listener)
        bubblesTouchAnimator = ValueAnimator.ofFloat(0F , EXPAND_BUBBLES_END_F - EXPAND_BUBBLES_START_F)
                .setDuration((EXPAND_BUBBLES_END_F - EXPAND_BUBBLES_START_F).toLong())
        bubblesTouchAnimator.interpolator = interpolator
        bubblesTouchAnimator.addUpdateListener { animation ->
            bubblesTime = animation.animatedFraction
            bubblesPaint.alpha = DrawableUtils.customFunction(bubblesTime , 0F , 0F , 255F , 0.33f , 255F , 0.66f , 0F , 1f).toInt()
            invalidate()
        }
        bubblesTouchAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation : Animator) {
                super.onAnimationStart(animation)
            }

            override fun onAnimationEnd(animation : Animator) {
                super.onAnimationEnd(animation)
                bubblesTime = 0f
            }

            override fun onAnimationCancel(animation : Animator) {
                super.onAnimationCancel(animation)
                bubblesTime = 0f
            }
        })
    }

    override fun onMeasure(widthMeasureSpec : Int , heightMeasureSpec : Int) {
        val w = View.MeasureSpec.makeMeasureSpec(widgetWidth.toInt() + padding * 2 , View.MeasureSpec.EXACTLY)
        val h = View.MeasureSpec.makeMeasureSpec((widgetHeight * 2).toInt() + padding * 2 , View.MeasureSpec.EXACTLY)
        super.onMeasure(w , h)
    }

    override fun onDraw(canvas : Canvas) {
        if (bubblesTime >= 0) {
            val half = TOTAL_BUBBLES_COUNT / 2
            for (i in 0 .. TOTAL_BUBBLES_COUNT - 1) {
                val radius = bubbleSizes[i]
                val speed = bubbleSpeeds[i] * bubblesTime
                val cx = bubblePositions[2 * i]
                var cy = bubblePositions[2 * i + 1]
                if (i < half)
                    cy *= 1 - speed
                else
                    cy *= 1 + speed
                canvas.drawCircle(cx , cy , radius , bubblesPaint)
            }
        }
        canvas.drawRoundRect(bounds , radius , radius , paint)
        drawMediaButtons(canvas)
    }

    private fun drawMediaButtons(canvas : Canvas) {
        for (i in buttonBounds.indices) {
            val drawable : Drawable
            if (i == INDEX_PLAY) {
                if (playbackState !!.state() == Configuration.STATE_PLAYING) {
                    drawable = drawables[INDEX_PAUSE]
                } else {
                    drawable = drawables[INDEX_PLAY]
                }
            } else {
                drawable = drawables[i]
            }
            drawable.bounds = buttonBounds[i]
            drawable.draw(canvas)
        }
    }

    private fun updateExpandAnimation(position : Long) {
        if (DrawableUtils.isBetween(position.toFloat() , 0f , EXPAND_COLOR_END_F)) {
            val t = DrawableUtils.normalize(position.toFloat() , 0f , EXPAND_COLOR_END_F)
            paint.color = colorChanger.nextColor(t)
        }
        if (DrawableUtils.isBetween(position.toFloat() , 0f , EXPAND_SIZE_END_F)) {
            var time = DrawableUtils.normalize(position.toFloat() , 0f , EXPAND_SIZE_END_F)
            time = accDecInterpolator !!.getInterpolation(time)
            val l : Float
            val r : Float
            val t : Float
            val b : Float
            val height = radius * 2
            t = radius
            b = t + height
            if (expandDirection == DIRECTION_LEFT) {
                r = widgetWidth
                l = r - height - (widgetWidth - height) * time
            } else {
                l = 0f
                r = l + height + (widgetWidth - height) * time
            }
            bounds.set(l , t , r , b)
        } else if (position > EXPAND_SIZE_END_F) {
            if (expandDirection == DIRECTION_LEFT) {
                bounds.left = 0f
            } else {
                bounds.right = widgetWidth
            }

        }
        if (DrawableUtils.isBetween(position.toFloat() , 0f , EXPAND_POSITION_START_F)) {
            if (expandDirection == DIRECTION_LEFT) {
                calculateBounds(INDEX_ALBUM , buttonBounds[INDEX_PLAY])
            } else {
                calculateBounds(INDEX_PLAYLIST , buttonBounds[INDEX_PLAY])
            }
        }
        if (DrawableUtils.isBetween(position.toFloat() , 0f , EXPAND_ELEMENTS_START_F)) {
            for (i in buttonBounds.indices) {
                if (i != INDEX_PLAY) {
                    drawables[i].alpha = 0
                }
            }
        }
        if (DrawableUtils.isBetween(position.toFloat() , EXPAND_ELEMENTS_START_F , EXPAND_ELEMENTS_END_F)) {
            val time = DrawableUtils.normalize(position.toFloat() , EXPAND_ELEMENTS_START_F , EXPAND_ELEMENTS_END_F)
            expandCollapseElements(time)
        }
        if (DrawableUtils.isBetween(position.toFloat() , EXPAND_POSITION_START_F , EXPAND_POSITION_END_F)) {
            var time = DrawableUtils.normalize(position.toFloat() , EXPAND_POSITION_START_F , EXPAND_POSITION_END_F)
            time = accDecInterpolator !!.getInterpolation(time)
            val playBounds = buttonBounds[INDEX_PLAY]
            calculateBounds(INDEX_PLAY , playBounds)
            val l : Int
            val t : Int
            val r : Int
            val b : Int
            t = playBounds.top
            b = playBounds.bottom
            if (expandDirection == DIRECTION_LEFT) {
                calculateBounds(INDEX_ALBUM , tmpRect)
                l = DrawableUtils.reduce(tmpRect.left.toFloat() , playBounds.left.toFloat() , time).toInt()
                r = l + playBounds.width()
            } else {
                calculateBounds(INDEX_PLAYLIST , tmpRect)
                l = DrawableUtils.enlarge(tmpRect.left.toFloat() , playBounds.left.toFloat() , time).toInt()
                r = l + playBounds.width()
            }
            playBounds.set(l , t , r , b)
        } else if (position >= EXPAND_POSITION_END_F) {
            calculateBounds(INDEX_PLAY , buttonBounds[INDEX_PLAY])
        }
        if (DrawableUtils.isBetween(position.toFloat() , EXPAND_BUBBLES_START_F , EXPAND_BUBBLES_END_F)) {
            val time = DrawableUtils.normalize(position.toFloat() , EXPAND_BUBBLES_START_F , EXPAND_BUBBLES_END_F)
            bubblesPaint.alpha = DrawableUtils.customFunction(time , 0 , 0 , 255 , 0.33f , 255 , 0.66f , 0 , 1f).toInt()
        } else {
            bubblesPaint.alpha = 0
        }
        if (DrawableUtils.isBetween(position.toFloat() , EXPAND_BUBBLES_START_F , EXPAND_BUBBLES_END_F)) {
            bubblesTime = DrawableUtils.normalize(position.toFloat() , EXPAND_BUBBLES_START_F , EXPAND_BUBBLES_END_F)
        }
    }

    private fun calculateBounds(index : Int , bounds : Rect) {
        var padding = buttonPadding
        if (index == INDEX_PREV || index == INDEX_NEXT) {
            padding += prevNextExtraPadding
        }
        calculateBounds(index , bounds , padding)
    }

    private fun calculateBounds(index : Int , bounds : Rect , padding : Int) {
        val l = (index * sizeStep + padding).toInt()
        val t = (radius + padding).toInt()
        val r = ((index + 1) * sizeStep - padding).toInt()
        val b = (radius * 3 - padding).toInt()
        bounds.set(l , t , r , b)
    }

    private fun updateCollapseAnimation(position : Long) {
        if (DrawableUtils.isBetween(position.toFloat() , 0f , COLLAPSE_ELEMENTS_END_F)) {
            val time = 1 - DrawableUtils.normalize(position.toFloat() , 0f , COLLAPSE_ELEMENTS_END_F)
            expandCollapseElements(time)
        }
        if (position > COLLAPSE_ELEMENTS_END_F) {
            for (i in buttonBounds.indices) {
                if (i != INDEX_PLAY) {
                    drawables[i].alpha = 0
                }
            }
        }
        if (DrawableUtils.isBetween(position.toFloat() , COLLAPSE_POSITION_START_F , COLLAPSE_POSITION_END_F)) {
            var time = DrawableUtils.normalize(position.toFloat() , COLLAPSE_POSITION_START_F , COLLAPSE_POSITION_END_F)
            time = accDecInterpolator !!.getInterpolation(time)
            val playBounds = buttonBounds[INDEX_PLAY]
            calculateBounds(INDEX_PLAY , playBounds)
            val l : Int
            val t : Int
            val r : Int
            val b : Int
            t = playBounds.top
            b = playBounds.bottom
            if (expandDirection == DIRECTION_LEFT) {
                calculateBounds(INDEX_ALBUM , tmpRect)
                l = DrawableUtils.enlarge(playBounds.left.toFloat() , tmpRect.left.toFloat() , time).toInt()
                r = l + playBounds.width()
            } else {
                calculateBounds(INDEX_PLAYLIST , tmpRect)
                l = DrawableUtils.reduce(playBounds.left.toFloat() , tmpRect.left.toFloat() , time).toInt()
                r = l + playBounds.width()
            }
            buttonBounds[INDEX_PLAY].set(l , t , r , b)
        }
        if (DrawableUtils.isBetween(position.toFloat() , COLLAPSE_SIZE_START_F , COLLAPSE_SIZE_END_F)) {
            var time = DrawableUtils.normalize(position.toFloat() , COLLAPSE_SIZE_START_F , COLLAPSE_SIZE_END_F)
            time = accDecInterpolator !!.getInterpolation(time)
            paint.color = colorChanger.nextColor(time)
            val l : Float
            val r : Float
            val t : Float
            val b : Float
            val height = radius * 2
            t = radius
            b = t + height
            if (expandDirection == DIRECTION_LEFT) {
                r = widgetWidth
                l = r - height - (widgetWidth - height) * (1 - time)
            } else {
                l = 0f
                r = l + height + (widgetWidth - height) * (1 - time)
            }
            bounds.set(l , t , r , b)
        }
    }

    private fun expandCollapseElements(time : Float) {
        val alpha = DrawableUtils.between(time * 255 , 0f , 255f).toInt()
        for (i in buttonBounds.indices) {
            if (i != INDEX_PLAY) {
                var padding = buttonPadding
                if (i == INDEX_PREV || i == INDEX_NEXT) {
                    padding += prevNextExtraPadding
                }
                calculateBounds(i , buttonBounds[i])
                val size = time * (sizeStep / 2f - padding)
                val cx = buttonBounds[i].centerX()
                val cy = buttonBounds[i].centerY()
                buttonBounds[i].set((cx - size).toInt() , (cy - size).toInt() , (cx + size).toInt() , (cy + size).toInt())
                drawables[i].alpha = alpha
            }
        }
    }

    fun onClick(x : Float , y : Float) {
        if (isAnimationInProgress)
            return
        val index = getTouchedAreaIndex(x.toInt() , y.toInt())
        if (index == INDEX_PLAY || index == INDEX_PREV || index == INDEX_NEXT) {
            if (! bubblesTouchAnimator.isRunning) {
                randomizeBubblesPosition()
                bubblesTouchAnimator.start()
            }
        }
        when (index) {
            INDEX_PLAYLIST -> {
                if (onControlsClickListener != null) {
                    onControlsClickListener !!.onPlaylistClicked()
                }
            }
            INDEX_PREV -> {
                if (onControlsClickListener != null) {
                    onControlsClickListener !!.onPreviousClicked()
                }
            }
            INDEX_PLAY -> {
                if (onControlsClickListener != null) {
                    onControlsClickListener !!.onPlayPauseClicked()
                }
            }
            INDEX_NEXT -> {
                if (onControlsClickListener != null) {
                    onControlsClickListener !!.onNextClicked()
                }
            }
            INDEX_ALBUM -> {
                if (onControlsClickListener != null) {
                    onControlsClickListener !!.onAlbumClicked()
                }
            }
            else -> {
                Log.w(ExpandCollapseWidget::class.java.simpleName , "Unknown index: " + index)
            }
        }
    }

    fun onLongClick(x : Float , y : Float) {
        if (isAnimationInProgress)
            return
        val index = getTouchedAreaIndex(x.toInt() , y.toInt())
        when (index) {
            INDEX_PLAYLIST -> {
                if (onControlsClickListener != null) {
                    onControlsClickListener !!.onPlaylistLongClicked()
                }
            }
            INDEX_PREV -> {
                if (onControlsClickListener != null) {
                    onControlsClickListener !!.onPreviousLongClicked()
                }
            }
            INDEX_PLAY -> {
                if (onControlsClickListener != null) {
                    onControlsClickListener !!.onPlayPauseLongClicked()
                }
            }
            INDEX_NEXT -> {
                if (onControlsClickListener != null) {
                    onControlsClickListener !!.onNextLongClicked()
                }
            }
            INDEX_ALBUM -> {
                if (onControlsClickListener != null) {
                    onControlsClickListener !!.onAlbumLongClicked()
                }
            }
            else -> {
                Log.w(ExpandCollapseWidget::class.java.simpleName , "Unknown index: " + index)
            }
        }
    }

    private fun getTouchedAreaIndex(x : Int , y : Int) : Int {
        var index = - 1
        for (i in buttonBounds.indices) {
            calculateBounds(i , tmpRect , 0)
            if (tmpRect.contains(x , y)) {
                index = i
                break
            }
        }
        return index
    }

    fun expand(expandDirection : Int) {
        if (expanded) {
            return
        }
        this.expandDirection = expandDirection
        startExpandAnimation()
    }

    private fun startExpandAnimation() {
        if (isAnimationInProgress)
            return
        animatingExpand = true
        if (playbackState !!.state() == Configuration.STATE_PLAYING) {
            colorChanger
                    .fromColor(playColor)
                    .toColor(widgetColor)

        } else {
            colorChanger
                    .fromColor(pauseColor)
                    .toColor(widgetColor)
        }
        randomizeBubblesPosition()
        expandAnimator.start()
    }

    private fun randomizeBubblesPosition() {
        val half = TOTAL_BUBBLES_COUNT / 2
        val step = widgetWidth / half
        for (i in 0 .. TOTAL_BUBBLES_COUNT - 1) {
            val index = i % half
            val speed = 0.3f + 0.7f * random !!.nextFloat()
            val size = bubblesMinSize + (bubblesMaxSize - bubblesMinSize) * random.nextFloat()
            val radius = size / 2f
            val cx = padding.toFloat() + index * step + step * random.nextFloat() * (if (random.nextBoolean()) 1 else - 1).toFloat()
            val cy = widgetHeight + padding
            bubbleSpeeds[i] = speed
            bubbleSizes[i] = radius
            bubblePositions[2 * i] = cx
            bubblePositions[2 * i + 1] = cy
        }
    }

    private fun startCollapseAnimation() {
        if (isAnimationInProgress) {
            return
        }
        collapseAnimator.start()
    }

    val isAnimationInProgress : Boolean
        get() = animatingCollapse || animatingExpand

    fun collapse() : Boolean {
        if (! expanded) {
            return false
        }
        if (playbackState !!.state() == Configuration.STATE_PLAYING) {
            colorChanger
                    .fromColor(widgetColor)
                    .toColor(playColor)
        } else {
            colorChanger
                    .fromColor(widgetColor)
                    .toColor(pauseColor)
        }
        startCollapseAnimation()
        return true
    }

    override fun onStateChanged(oldState : Int , newState : Int , initiator : Any) {
        invalidate()
    }

    override fun onProgressChanged(position : Int , duration : Int , percentage : Float) {

    }

    fun onWidgetStateChangedListener(onWidgetStateChangedListener : AudioWidget.OnWidgetStateChangedListener) : ExpandCollapseWidget {
        this.onWidgetStateChangedListener = onWidgetStateChangedListener
        return this
    }

    fun expandDirection() : Int {
        return expandDirection
    }

    fun expandDirection(expandDirection : Int) {
        this.expandDirection = expandDirection
    }

    fun onControlsClickListener(onControlsClickListener : AudioWidget.OnControlsClickListener) {
        this.onControlsClickListener = onControlsClickListener
    }

    fun albumCover(albumCover : Drawable?) {
        if (drawables[INDEX_ALBUM] === albumCover)
            return
        if (albumCover == null) {
            drawables[INDEX_ALBUM] = defaultAlbumCover
        } else {
            if (albumCover.constantState != null)
                drawables[INDEX_ALBUM] = albumCover.constantState !!.newDrawable().mutate()
            else
                drawables[INDEX_ALBUM] = albumCover
        }
        val bounds = buttonBounds[INDEX_ALBUM]
        invalidate(bounds.left , bounds.top , bounds.right , bounds.bottom)
    }

    fun onTouched(x : Float , y : Float) {
        val index = getTouchedAreaIndex(x.toInt() , y.toInt())
        if (index == INDEX_PLAY || index == INDEX_NEXT || index == INDEX_PREV) {
            touchedButtonIndex = index
            touchDownAnimator.start()
        }
    }

    fun onReleased(x : Float , y : Float) {
        val index = getTouchedAreaIndex(x.toInt() , y.toInt())
        if (index == INDEX_PLAY || index == INDEX_NEXT || index == INDEX_PREV) {
            touchedButtonIndex = index
            touchUpAnimator.start()
        }
    }

    fun newBoundsChecker(offsetX : Int , offsetY : Int) : TouchManager.BoundsChecker {
        return BoundsCheckerImpl(radius , padding.toFloat() , widgetWidth , widgetHeight , offsetX , offsetY)
    }

    fun setCollapseListener(collapseListener : AnimationProgressListener?) {
        this.collapseListener = collapseListener
    }

    fun setExpandListener(expandListener : (Any) -> Unit) {
        this.expandListener = expandListener
    }

    internal interface AnimationProgressListener {
        fun onValueChanged(percent : Float)
    }

    private class BoundsCheckerImpl internal constructor(private val radius : Float , private val padding : Float , private val widgetWidth : Float , private val widgetHeight : Float , offsetX : Int , offsetY : Int) : AudioWidget.BoundsCheckerWithOffset(offsetX , offsetY) {

        public override fun stickyLeftSideImpl(screenWidth : Float) : Float {
            return 0f
        }

        public override fun stickyRightSideImpl(screenWidth : Float) : Float {
            return screenWidth - widgetWidth
        }

        public override fun stickyBottomSideImpl(screenHeight : Float) : Float {
            return screenHeight - 3 * radius
        }

        public override fun stickyTopSideImpl(screenHeight : Float) : Float {
            return - radius
        }
    }

    companion object {

        val DIRECTION_LEFT = 1
        val DIRECTION_RIGHT = 2

        private val EXPAND_DURATION_F = 34 * Configuration.FRAME_SPEED
        private val EXPAND_DURATION_L = EXPAND_DURATION_F.toLong()
        private val EXPAND_COLOR_END_F = 9 * Configuration.FRAME_SPEED
        private val EXPAND_SIZE_END_F = 12 * Configuration.FRAME_SPEED
        private val EXPAND_POSITION_START_F = 10 * Configuration.FRAME_SPEED
        private val EXPAND_POSITION_END_F = 18 * Configuration.FRAME_SPEED
        private val EXPAND_BUBBLES_START_F = 18 * Configuration.FRAME_SPEED
        private val EXPAND_BUBBLES_END_F = 32 * Configuration.FRAME_SPEED
        private val EXPAND_ELEMENTS_START_F = 20 * Configuration.FRAME_SPEED
        private val EXPAND_ELEMENTS_END_F = 27 * Configuration.FRAME_SPEED

        private val COLLAPSE_DURATION_F = 12 * Configuration.FRAME_SPEED
        private val COLLAPSE_DURATION_L = COLLAPSE_DURATION_F.toLong()
        private val COLLAPSE_ELEMENTS_END_F = 3 * Configuration.FRAME_SPEED
        private val COLLAPSE_SIZE_START_F = 2 * Configuration.FRAME_SPEED
        private val COLLAPSE_SIZE_END_F = 12 * Configuration.FRAME_SPEED
        private val COLLAPSE_POSITION_START_F = 3 * Configuration.FRAME_SPEED
        private val COLLAPSE_POSITION_END_F = 12 * Configuration.FRAME_SPEED


        private val INDEX_PLAYLIST = 0
        private val INDEX_PREV = 1
        private val INDEX_PLAY = 2
        private val INDEX_NEXT = 3
        private val INDEX_ALBUM = 4
        private val INDEX_PAUSE = 5

        private val TOTAL_BUBBLES_COUNT = 30
    }
}
