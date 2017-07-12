package e.chakritrakhuang.kotlinmusicbobber

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import android.view.ViewConfiguration
import android.view.animation.Interpolator

import java.util.Random

/**
 * Audio widget configuration class.
 */
internal class Configuration private constructor(builder : Builder) {

    private val lightColor : Int
    private val darkColor : Int
    private val progressColor : Int
    private val expandedColor : Int
    private val random : Random?
    private val width : Float
    private val height : Float
    private val playDrawable : Drawable?
    private val pauseDrawable : Drawable?
    private val prevDrawable : Drawable?
    private val nextDrawable : Drawable?
    private val playlistDrawable : Drawable?
    private val albumDrawable : Drawable?
    private val context : Context?
    private val playbackState : PlaybackState?
    private val buttonPadding : Int
    private val crossStrokeWidth : Float
    private val progressStrokeWidth : Float
    private val shadowRadius : Float
    private val shadowDx : Float
    private val shadowDy : Float
    private val shadowColor : Int
    private val bubblesMinSize : Float
    private val bubblesMaxSize : Float
    private val crossColor : Int
    private val crossOverlappedColor : Int
    private val accDecInterpolator : Interpolator?
    private val prevNextExtraPadding : Int

    init {
        this.context = builder.context
        this.random = builder.random
        this.width = builder.width
        this.height = builder.radius
        this.lightColor = builder.lightColor
        this.darkColor = builder.darkColor
        this.progressColor = builder.progressColor
        this.expandedColor = builder.expandedColor
        this.playlistDrawable = builder.playlistDrawable
        this.playDrawable = builder.playDrawable
        this.pauseDrawable = builder.pauseDrawable
        this.prevDrawable = builder.prevDrawable
        this.nextDrawable = builder.nextDrawable
        this.albumDrawable = builder.albumDrawable
        this.playbackState = builder.playbackState
        this.buttonPadding = builder.buttonPadding
        this.crossStrokeWidth = builder.crossStrokeWidth
        this.progressStrokeWidth = builder.progressStrokeWidth
        this.shadowRadius = builder.shadowRadius
        this.shadowDx = builder.shadowDx
        this.shadowDy = builder.shadowDy
        this.shadowColor = builder.shadowColor
        this.bubblesMinSize = builder.bubblesMinSize
        this.bubblesMaxSize = builder.bubblesMaxSize
        this.crossColor = builder.crossColor
        this.crossOverlappedColor = builder.crossOverlappedColor
        this.accDecInterpolator = builder.accDecInterpolator
        this.prevNextExtraPadding = builder.prevNextExtraPadding
    }

    fun context() : Context? {
        return context
    }

    fun random() : Random? {
        return random
    }

    @ColorInt
    fun lightColor() : Int {
        return lightColor
    }

    @ColorInt
    fun darkColor() : Int {
        return darkColor
    }

    @ColorInt
    fun progressColor() : Int {
        return progressColor
    }

    @ColorInt
    fun expandedColor() : Int {
        return expandedColor
    }

    fun widgetWidth() : Float {
        return width
    }

    fun radius() : Float {
        return height
    }

    fun playDrawable() : Drawable? {
        return playDrawable
    }

    fun pauseDrawable() : Drawable? {
        return pauseDrawable
    }

    fun prevDrawable() : Drawable? {
        return prevDrawable
    }

    fun nextDrawable() : Drawable? {
        return nextDrawable
    }

    fun playlistDrawable() : Drawable? {
        return playlistDrawable
    }

    fun albumDrawable() : Drawable? {
        return albumDrawable
    }

    fun playbackState() : PlaybackState? {
        return playbackState
    }

    fun crossStrokeWidth() : Float {
        return crossStrokeWidth
    }

    fun progressStrokeWidth() : Float {
        return progressStrokeWidth
    }

    fun buttonPadding() : Int {
        return buttonPadding
    }

    fun shadowRadius() : Float {
        return shadowRadius
    }

    fun shadowDx() : Float {
        return shadowDx
    }

    fun shadowDy() : Float {
        return shadowDy
    }

    fun shadowColor() : Int {
        return shadowColor
    }

    fun bubblesMinSize() : Float {
        return bubblesMinSize
    }

    fun bubblesMaxSize() : Float {
        return bubblesMaxSize
    }

    fun crossColor() : Int {
        return crossColor
    }

    fun crossOverlappedColor() : Int {
        return crossOverlappedColor
    }

    fun accDecInterpolator() : Interpolator? {
        return accDecInterpolator
    }

    fun prevNextExtraPadding() : Int {
        return prevNextExtraPadding
    }

    internal class Builder {

        private var lightColor : Int = 0
        private var darkColor : Int = 0
        private var progressColor : Int = 0
        private var expandedColor : Int = 0
        private var width : Float = 0.toFloat()
        private var radius : Float = 0.toFloat()
        private var context : Context? = null
        private var random : Random? = null
        private var playDrawable : Drawable? = null
        private var pauseDrawable : Drawable? = null
        private var prevDrawable : Drawable? = null
        private var nextDrawable : Drawable? = null
        private var playlistDrawable : Drawable? = null
        private var albumDrawable : Drawable? = null
        private var playbackState : PlaybackState? = null
        private var buttonPadding : Int = 0
        private var crossStrokeWidth : Float = 0.toFloat()
        private var progressStrokeWidth : Float = 0.toFloat()
        private var shadowRadius : Float = 0.toFloat()
        private var shadowDx : Float = 0.toFloat()
        private var shadowDy : Float = 0.toFloat()
        private var shadowColor : Int = 0
        private var bubblesMinSize : Float = 0.toFloat()
        private var bubblesMaxSize : Float = 0.toFloat()
        private var crossColor : Int = 0
        private var crossOverlappedColor : Int = 0
        private var accDecInterpolator : Interpolator? = null
        private var prevNextExtraPadding : Int = 0

        fun context(context : Context) : Builder {
            this.context = context
            return this
        }

        fun playColor(@ColorInt pauseColor : Int) : Builder {
            this.lightColor = pauseColor
            return this
        }

        fun darkColor(@ColorInt playColor : Int) : Builder {
            this.darkColor = playColor
            return this
        }

        fun progressColor(@ColorInt progressColor : Int) : Builder {
            this.progressColor = progressColor
            return this
        }

        fun expandedColor(@ColorInt expandedColor : Int) : Builder {
            this.expandedColor = expandedColor
            return this
        }

        fun random(random : Random) : Builder {
            this.random = random
            return this
        }

        fun widgetWidth(width : Float) : Builder {
            this.width = width
            return this
        }

        fun radius(radius : Float) : Builder {
            this.radius = radius
            return this
        }

        fun playDrawable(playDrawable : Drawable?) : Builder {
            this.playDrawable = playDrawable
            return this
        }

        fun pauseDrawable(pauseDrawable : Drawable?) : Builder {
            this.pauseDrawable = pauseDrawable
            return this
        }

        fun prevDrawable(prevDrawable : Drawable?) : Builder {
            this.prevDrawable = prevDrawable
            return this
        }

        fun nextDrawable(nextDrawable : Drawable?) : Builder {
            this.nextDrawable = nextDrawable
            return this
        }

        fun playlistDrawable(plateDrawable : Drawable?) : Builder {
            this.playlistDrawable = plateDrawable
            return this
        }

        fun albumDrawable(albumDrawable : Drawable?) : Builder {
            this.albumDrawable = albumDrawable
            return this
        }

        fun playbackState(playbackState : PlaybackState) : Builder {
            this.playbackState = playbackState
            return this
        }

        fun buttonPadding(buttonPadding : Int) : Builder {
            this.buttonPadding = buttonPadding
            return this
        }

        fun crossStrokeWidth(crossStrokeWidth : Float) : Builder {
            this.crossStrokeWidth = crossStrokeWidth
            return this
        }

        fun progressStrokeWidth(progressStrokeWidth : Float) : Builder {
            this.progressStrokeWidth = progressStrokeWidth
            return this
        }

        fun shadowRadius(shadowRadius : Float) : Builder {
            this.shadowRadius = shadowRadius
            return this
        }

        fun shadowDx(shadowDx : Float) : Builder {
            this.shadowDx = shadowDx
            return this
        }

        fun shadowDy(shadowDy : Float) : Builder {
            this.shadowDy = shadowDy
            return this
        }

        fun shadowColor(@ColorInt shadowColor : Int) : Builder {
            this.shadowColor = shadowColor
            return this
        }

        fun bubblesMinSize(bubblesMinSize : Float) : Builder {
            this.bubblesMinSize = bubblesMinSize
            return this
        }

        fun bubblesMaxSize(bubblesMaxSize : Float) : Builder {
            this.bubblesMaxSize = bubblesMaxSize
            return this
        }

        fun crossColor(@ColorInt crossColor : Int) : Builder {
            this.crossColor = crossColor
            return this
        }

        fun crossOverlappedColor(@ColorInt crossOverlappedColor : Int) : Builder {
            this.crossOverlappedColor = crossOverlappedColor
            return this
        }

        fun accDecInterpolator(accDecInterpolator : Interpolator) : Builder {
            this.accDecInterpolator = accDecInterpolator
            return this
        }

        fun prevNextExtraPadding(prevNextExtraPadding : Int) : Builder {
            this.prevNextExtraPadding = prevNextExtraPadding
            return this
        }

        fun build() : Configuration {
            return Configuration(this)
        }
    }

    companion object {

        val FRAME_SPEED = 70.0f

        val LONG_CLICK_THRESHOLD = (ViewConfiguration.getLongPressTimeout() + 128).toLong()
        val STATE_STOPPED = 0
        val STATE_PLAYING = 1
        val STATE_PAUSED = 2
        val TOUCH_ANIMATION_DURATION : Long = 100
    }
}
