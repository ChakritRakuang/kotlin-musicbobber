package e.chakritrakhuang.kotlinmusicbobber

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Vibrator
import android.support.annotation.ColorInt
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator

import java.lang.ref.WeakReference
import java.util.Random
import java.util.WeakHashMap

@Suppress("NAME_SHADOWING" , "UNREACHABLE_CODE" , "IMPLICIT_CAST_TO_ANY" , "DEPRECATION")
class AudioWidget private constructor(builder : Builder) {

    private val playPauseButton : PlayPauseButton

    private val expandCollapseWidget : ExpandCollapseWidget

    private val removeWidgetView : RemoveWidgetView

    private var playbackState : PlaybackState? = null

    private val controller : Controller

    private val windowManager : WindowManager
    private val vibrator : Vibrator
    private val handler : Handler
    private val screenSize : Point
    private val context : Context
    private val playPauseButtonManager : TouchManager
    private val expandedWidgetManager : TouchManager
    private val ppbToExpBoundsChecker : TouchManager.BoundsChecker
    private val expToPpbBoundsChecker : TouchManager.BoundsChecker

    private val albumCoverCache = WeakHashMap<Int , WeakReference<Drawable>>()

    private val removeBounds : RectF

    private val hiddenRemWidPos : Point

    private val visibleRemWidPos : Point
    private var animatedRemBtnYPos = - 1
    private var widgetWidth : Float = 0.toFloat()
    private var widgetHeight : Float = 0.toFloat()
    private var radius : Float = 0.toFloat()
    private val onControlsClickListener : OnControlsClickListenerWrapper?

    var isShown : Boolean = false
        private set
    private var released : Boolean = false
    private var removeWidgetShown : Boolean = false
    private var onWidgetStateChangedListener : OnWidgetStateChangedListener? = null

    init {
        this.context = builder.context.applicationContext
        this.vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        this.handler = Handler()
        this.screenSize = Point()
        this.removeBounds = RectF()
        this.hiddenRemWidPos = Point()
        this.visibleRemWidPos = Point()
        this.controller = newController()
        this.windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            windowManager.defaultDisplay.getSize(screenSize)
        } else {
            screenSize.x = windowManager.defaultDisplay.width
            screenSize.y = windowManager.defaultDisplay.height
        }
        screenSize.y -= statusBarHeight() + navigationBarHeight()

        val configuration = prepareConfiguration(builder)
        playPauseButton = PlayPauseButton(configuration)
        expandCollapseWidget = ExpandCollapseWidget(configuration)
        removeWidgetView = RemoveWidgetView(configuration)
        val offsetCollapsed = context.resources.getDimensionPixelOffset(R.dimen.aw_edge_offset_collapsed)
        val offsetExpanded = context.resources.getDimensionPixelOffset(R.dimen.aw_edge_offset_expanded)
        playPauseButtonManager = TouchManager(playPauseButton , playPauseButton.newBoundsChecker(
                if (builder.edgeOffsetXCollapsedSet) builder.edgeOffsetXCollapsed else offsetCollapsed ,
                if (builder.edgeOffsetYCollapsedSet) builder.edgeOffsetYCollapsed else offsetCollapsed
        ))
                .screenWidth(screenSize.x)
                .screenHeight(screenSize.y)
        expandedWidgetManager = TouchManager(expandCollapseWidget , expandCollapseWidget.newBoundsChecker(
                if (builder.edgeOffsetXExpandedSet) builder.edgeOffsetXExpanded else offsetExpanded ,
                if (builder.edgeOffsetYExpandedSet) builder.edgeOffsetYExpanded else offsetExpanded
        ))
                .screenWidth(screenSize.x)
                .screenHeight(screenSize.y)

        expandedWidgetManager.callback(ExpandCollapseWidgetCallback())
        expandCollapseWidget.onWidgetStateChangedListener(object : OnWidgetStateChangedListener {
            override fun onWidgetStateChanged(state : State) {
                if (state == State.COLLAPSED) {
                    playPauseButton.setLayerType(View.LAYER_TYPE_SOFTWARE , null)
                    try {
                        windowManager.removeView(expandCollapseWidget)
                    } catch (e : IllegalArgumentException) {
                        // view not attached to window
                    }

                    playPauseButton.enableProgressChanges(true)
                }
                if (onWidgetStateChangedListener != null) {
                    onWidgetStateChangedListener !!.onWidgetStateChanged(state)
                }
            }

            override fun onWidgetPositionChanged(cx : Int , cy : Int) {

            }
        })
        onControlsClickListener = OnControlsClickListenerWrapper()
        expandCollapseWidget.onControlsClickListener(onControlsClickListener)
        ppbToExpBoundsChecker = playPauseButton.newBoundsChecker(
                if (builder.edgeOffsetXExpandedSet) builder.edgeOffsetXExpanded else offsetExpanded ,
                if (builder.edgeOffsetYExpandedSet) builder.edgeOffsetYExpanded else offsetExpanded
        )
        expToPpbBoundsChecker = expandCollapseWidget.newBoundsChecker(
                if (builder.edgeOffsetXCollapsedSet) builder.edgeOffsetXCollapsed else offsetCollapsed ,
                if (builder.edgeOffsetYCollapsedSet) builder.edgeOffsetYCollapsed else offsetCollapsed
        )
    }

    private fun prepareConfiguration(builder : Builder) : Configuration {

        val darkColor = if (builder.darkColorSet) builder.darkColor else ContextCompat.getColor(context , R.color.aw_dark)
        val lightColor = if (builder.lightColorSet) builder.lightColor else ContextCompat.getColor(context , R.color.aw_light)
        val progressColor = if (builder.progressColorSet) builder.progressColor else ContextCompat.getColor(context , R.color.aw_progress)
        val expandColor = if (builder.expandWidgetColorSet) builder.expandWidgetColor else ContextCompat.getColor(context , R.color.aw_expanded)
        val crossColor = if (builder.crossColorSet) builder.crossColor else ContextCompat.getColor(context , R.color.aw_cross_default)
        val crossOverlappedColor = if (builder.crossOverlappedColorSet) builder.crossOverlappedColor else ContextCompat.getColor(context , R.color.aw_cross_overlapped)
        val shadowColor = if (builder.shadowColorSet) builder.shadowColor else ContextCompat.getColor(context , R.color.aw_shadow)

        val playDrawable = if (builder.playDrawable != null) builder.playDrawable else ContextCompat.getDrawable(context , R.drawable.aw_ic_play)
        val pauseDrawable = if (builder.pauseDrawable != null) builder.pauseDrawable else ContextCompat.getDrawable(context , R.drawable.aw_ic_pause)
        val prevDrawable = if (builder.prevDrawable != null) builder.prevDrawable else ContextCompat.getDrawable(context , R.drawable.aw_ic_prev)
        val nextDrawable = if (builder.nextDrawable != null) builder.nextDrawable else ContextCompat.getDrawable(context , R.drawable.aw_ic_next)
        val playlistDrawable = if (builder.playlistDrawable != null) builder.playlistDrawable else ContextCompat.getDrawable(context , R.drawable.aw_ic_playlist)
        val albumDrawable = if (builder.defaultAlbumDrawable != null) builder.defaultAlbumDrawable else ContextCompat.getDrawable(context , R.drawable.aw_ic_default_album)

        val buttonPadding = if (builder.buttonPaddingSet) builder.buttonPadding else context.resources.getDimensionPixelSize(R.dimen.aw_button_padding)
        val crossStrokeWidth = if (builder.crossStrokeWidthSet) builder.crossStrokeWidth else context.resources.getDimension(R.dimen.aw_cross_stroke_width)
        val progressStrokeWidth = if (builder.progressStrokeWidthSet) builder.progressStrokeWidth else context.resources.getDimension(R.dimen.aw_progress_stroke_width)
        val shadowRadius = if (builder.shadowRadiusSet) builder.shadowRadius else context.resources.getDimension(R.dimen.aw_shadow_radius)
        val shadowDx = if (builder.shadowDxSet) builder.shadowDx else context.resources.getDimension(R.dimen.aw_shadow_dx)
        val shadowDy = if (builder.shadowDySet) builder.shadowDy else context.resources.getDimension(R.dimen.aw_shadow_dy)
        val bubblesMinSize = if (builder.bubblesMinSizeSet) builder.bubblesMinSize else context.resources.getDimension(R.dimen.aw_bubbles_min_size)
        val bubblesMaxSize = if (builder.bubblesMaxSizeSet) builder.bubblesMaxSize else context.resources.getDimension(R.dimen.aw_bubbles_max_size)
        val prevNextExtraPadding = context.resources.getDimensionPixelSize(R.dimen.aw_prev_next_button_extra_padding)

        widgetHeight = context.resources.getDimensionPixelSize(R.dimen.aw_player_height).toFloat()
        widgetWidth = context.resources.getDimensionPixelSize(R.dimen.aw_player_width).toFloat()
        radius = widgetHeight / 2f
        playbackState = PlaybackState()
        return Configuration.Builder()
                .context(context)
                .playbackState(playbackState !!)
                .random(Random())
                .accDecInterpolator(AccelerateDecelerateInterpolator())
                .darkColor(darkColor)
                .playColor(lightColor)
                .progressColor(progressColor)
                .expandedColor(expandColor)
                .widgetWidth(widgetWidth)
                .radius(radius)
                .playlistDrawable(playlistDrawable)
                .playDrawable(playDrawable)
                .prevDrawable(prevDrawable)
                .nextDrawable(nextDrawable)
                .pauseDrawable(pauseDrawable)
                .albumDrawable(albumDrawable)
                .buttonPadding(buttonPadding)
                .prevNextExtraPadding(prevNextExtraPadding)
                .crossStrokeWidth(crossStrokeWidth)
                .progressStrokeWidth(progressStrokeWidth)
                .shadowRadius(shadowRadius)
                .shadowDx(shadowDx)
                .shadowDy(shadowDy)
                .shadowColor(shadowColor)
                .bubblesMinSize(bubblesMinSize)
                .bubblesMaxSize(bubblesMaxSize)
                .crossColor(crossColor)
                .crossOverlappedColor(crossOverlappedColor)
                .build()
    }

    private fun statusBarHeight() : Int {
        val resourceId = context.resources.getIdentifier("status_bar_height" , "dimen" , "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else context.resources.getDimensionPixelSize(R.dimen.aw_status_bar_height)
    }

    private fun navigationBarHeight() : Int {
        if (hasNavigationBar()) {
            val resourceId = context.resources.getIdentifier("navigation_bar_height" , "dimen" , "android")
            return if (resourceId > 0) {
                context.resources.getDimensionPixelSize(resourceId)
            } else context.resources.getDimensionPixelSize(R.dimen.aw_navigation_bar_height)
        }
        return 0
    }

    private fun hasNavigationBar() : Boolean {
        val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
        val hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME)
        val id = context.resources.getIdentifier("config_showNavigationBar" , "bool" , "android")
        return ! hasBackKey && ! hasHomeKey || id > 0 && context.resources.getBoolean(id)
    }

    private fun newController() : Controller {
        return object : Controller {

            override fun start() {
                playbackState !!.start(this)
            }

            override fun pause() {
                playbackState !!.pause(this)
            }

            override fun stop() {
                playbackState !!.stop(this)
            }

            override fun duration() : Int {
                return playbackState !!.duration()
            }

            override fun duration(duration : Int) {
                playbackState !!.duration(duration)
            }

            override fun position() : Int {
                return playbackState !!.position()
            }

            override fun position(position : Int) {
                playbackState !!.position(position)
            }

            override fun onControlsClickListener(onControlsClickListener : OnControlsClickListener?) {
                this@AudioWidget.onControlsClickListener !!.onControlsClickListener(onControlsClickListener)
            }

            override fun onWidgetStateChangedListener(onWidgetStateChangedListener : OnWidgetStateChangedListener?) {
                this@AudioWidget.onWidgetStateChangedListener = onWidgetStateChangedListener
            }

            override fun albumCover(albumCover : Drawable?) {
                expandCollapseWidget.albumCover(albumCover)
                if (albumCover != null) {
                    playPauseButton.albumCover(albumCover)
                }
            }

            override fun albumCoverBitmap(albumCover : Bitmap?) {
                if (albumCover == null) {
                    expandCollapseWidget.albumCover(null)
                    playPauseButton.albumCover(null !!)
                } else {
                    val wrDrawable = albumCoverCache[albumCover.hashCode()]
                    if (wrDrawable != null) {
                        val drawable = wrDrawable.get()
                        if (drawable != null) {
                            expandCollapseWidget.albumCover(drawable)
                            playPauseButton.albumCover(drawable)
                            return
                        }
                    }

                    val albumCover = BitmapDrawable(context.resources , albumCover)
                    expandCollapseWidget.albumCover(albumCover)
                    playPauseButton.albumCover(albumCover)
                    albumCoverCache.put(albumCover.hashCode() , WeakReference(albumCover))
                }
            }
        }
    }

    fun show(cx : Int , cy : Int) {
        if (isShown) {
            return
        }
        isShown = true
        val remWidX = screenSize.x / 2f - radius * RemoveWidgetView.SCALE_LARGE
        hiddenRemWidPos.set(remWidX.toInt() , (screenSize.y.toFloat() + widgetHeight + navigationBarHeight().toFloat()).toInt())
        visibleRemWidPos.set(remWidX.toInt() , (screenSize.y.toFloat() - radius - if (hasNavigationBar()) 0 else widgetHeight).toInt())
        try {
            show(removeWidgetView , hiddenRemWidPos.x , hiddenRemWidPos.y)
        } catch (e : IllegalArgumentException) {
            // widget not removed yet, animation in progress
        }

        show(playPauseButton , (cx - widgetHeight).toInt() , (cy - widgetHeight).toInt())
        playPauseButtonManager.animateToBounds()
    }

    fun hide() {
        hideInternal(true)
    }

    private fun hideInternal(byPublic : Boolean) {
        if (! isShown) {
            return
        }
        isShown = false
        released = true
        try {
            windowManager.removeView(playPauseButton)
        } catch (e : IllegalArgumentException) {
            // view not attached to window
        }

        if (byPublic) {
            try {
                windowManager.removeView(removeWidgetView)
            } catch (e : IllegalArgumentException) {

            }

        }
        try {
            windowManager.removeView(expandCollapseWidget)
        } catch (e : IllegalArgumentException) {

        }

        if (onWidgetStateChangedListener != null) {
            onWidgetStateChangedListener !!.onWidgetStateChanged(State.REMOVED)
        }
    }

    fun expand() {
        removeWidgetShown = false
        playPauseButton.enableProgressChanges(false)
        playPauseButton.postDelayed({ this.checkSpaceAndShowExpanded() } , PlayPauseButton.PROGRESS_CHANGES_DURATION)
    }

    fun collapse() {
        expandCollapseWidget.setCollapseListener(ExpandCollapseWidget.AnimationProgressListener {
            val it = null
            playPauseButton.setAlpha(it)
        })

        val params = expandCollapseWidget.layoutParams as WindowManager.LayoutParams
        val cx = params.x + expandCollapseWidget.width / 2
        if (cx > screenSize.x / 2) {
            expandCollapseWidget.expandDirection(ExpandCollapseWidget.DIRECTION_LEFT)
        } else {
            expandCollapseWidget.expandDirection(ExpandCollapseWidget.DIRECTION_RIGHT)
        }
        updatePlayPauseButtonPosition()
        if (expandCollapseWidget.collapse()) {
            playPauseButtonManager.animateToBounds()
            expandedWidgetManager.animateToBounds(null !!)
        }
    }

    private fun updatePlayPauseButtonPosition() {
        val widgetParams = expandCollapseWidget.layoutParams as WindowManager.LayoutParams
        val params = playPauseButton.layoutParams as WindowManager.LayoutParams
        if (expandCollapseWidget.expandDirection() == ExpandCollapseWidget.DIRECTION_RIGHT) {
            params.x = (widgetParams.x - radius).toInt()
        } else {
            params.x = (widgetParams.x + widgetWidth - widgetHeight - radius).toInt()
        }
        params.y = widgetParams.y
        try {
            windowManager.updateViewLayout(playPauseButton , params)
        } catch (e : IllegalArgumentException) {
            // view not attached to window
        }

        if (onWidgetStateChangedListener != null) {
            onWidgetStateChangedListener !!.onWidgetPositionChanged((params.x + widgetHeight).toInt() , (params.y + widgetHeight).toInt())
        }
    }

    private fun checkSpaceAndShowExpanded() {
        val params = playPauseButton.layoutParams as WindowManager.LayoutParams
        val x = params.x
        params.y
        val expandDirection : Int
        if (x + widgetHeight > screenSize.x / 2) {
            expandDirection = ExpandCollapseWidget.DIRECTION_LEFT
        } else {
            expandDirection = ExpandCollapseWidget.DIRECTION_RIGHT
        }

        playPauseButtonManager.animateToBounds() {
            val params1 = playPauseButton.layoutParams as WindowManager.LayoutParams
            var x1 = params1.x
            val y1 = params1.y
            if (expandDirection == ExpandCollapseWidget.DIRECTION_LEFT) {
                x1 -= (widgetWidth - widgetHeight * 1.5f).toInt()
            } else {
                x1 += (widgetHeight / 2f).toInt()
            }
            show(expandCollapseWidget , x1 , y1)
            playPauseButton.setLayerType(View.LAYER_TYPE_NONE , null)

            expandCollapseWidget.setExpandListener { percent -> playPauseButton.alpha = 1f - percent }
            expandCollapseWidget.expand(expandDirection)
        }
    }

    fun controller() : Controller {
        return controller
    }

    private fun show(view : View , left : Int , top : Int) {
        val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT ,
                WindowManager.LayoutParams.WRAP_CONTENT ,
                WindowManager.LayoutParams.TYPE_PHONE ,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS ,
                PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.START or Gravity.TOP
        params.x = left
        params.y = top
        windowManager.addView(view , params)
    }

    internal abstract class BoundsCheckerWithOffset(private val offsetX : Int , private val offsetY : Int) : TouchManager.BoundsChecker {

        override fun stickyLeftSide(screenWidth : Float) : Float {
            return stickyLeftSideImpl(screenWidth) + offsetX
        }

        override fun stickyRightSide(screenWidth : Float) : Float {
            return stickyRightSideImpl(screenWidth) - offsetX
        }

        override fun stickyTopSide(screenHeight : Float) : Float {
            return stickyTopSideImpl(screenHeight) + offsetY
        }

        override fun stickyBottomSide(screenHeight : Float) : Float {
            return stickyBottomSideImpl(screenHeight) - offsetY
        }

        protected abstract fun stickyLeftSideImpl(screenWidth : Float) : Float
        protected abstract fun stickyRightSideImpl(screenWidth : Float) : Float
        protected abstract fun stickyTopSideImpl(screenHeight : Float) : Float
        protected abstract fun stickyBottomSideImpl(screenHeight : Float) : Float
    }

    private abstract inner class PlayPauseButtonCallback internal constructor() : TouchManager.SimpleCallback() {
        private val animatorUpdateListener : ValueAnimator.AnimatorUpdateListener = null !!
        private var readyToRemove : Boolean = false

        init {
        }

        override fun onClick(x : Float , y : Float) {
            playPauseButton.onClick()
            onControlsClickListener?.onPlayPauseClicked()
        }

        override fun onLongClick(x : Float , y : Float) {
            released = true
            expand()
        }

        override fun onTouched(x : Float , y : Float) {
            super.onTouched(x , y)
            released = false
            handler.postDelayed({
                if (! released) {
                    removeWidgetShown = true
                    val animator = ValueAnimator.ofFloat(hiddenRemWidPos.y.toFloat() , visibleRemWidPos.y.toFloat())
                    animator.duration = REMOVE_BTN_ANIM_DURATION
                    animator.addUpdateListener(animatorUpdateListener)
                    animator.start()
                }
            } , Configuration.LONG_CLICK_THRESHOLD)
            playPauseButton.onTouchDown()
        }

        override fun onMoved(diffX : Float , diffY : Float) {
            super.onMoved(diffX , diffY)
            val curReadyToRemove = isReadyToRemove
            if (curReadyToRemove != readyToRemove) {
                readyToRemove = curReadyToRemove
                removeWidgetView.setOverlapped(readyToRemove)
                if (readyToRemove && vibrator.hasVibrator()) {
                    vibrator.vibrate(VIBRATION_DURATION)
                }
            }
            updateRemoveBtnPosition()
        }

        private fun updateRemoveBtnPosition() {
            if (removeWidgetShown) {
                val playPauseBtnParams = playPauseButton.layoutParams as WindowManager.LayoutParams
                val removeBtnParams = removeWidgetView.layoutParams as WindowManager.LayoutParams

                val tgAlpha = (screenSize.x / 2.0 - playPauseBtnParams.x) / (visibleRemWidPos.y - playPauseBtnParams.y)
                val rotationDegrees = 360 - Math.toDegrees(Math.atan(tgAlpha))

                var distance = Math.sqrt(Math.pow((animatedRemBtnYPos - playPauseBtnParams.y).toDouble() , 2.0) + Math.pow((visibleRemWidPos.x - hiddenRemWidPos.x).toDouble() , 2.0)).toFloat()
                val maxDistance = Math.sqrt(Math.pow(screenSize.x.toDouble() , 2.0) + Math.pow(screenSize.y.toDouble() , 2.0)).toFloat()
                distance /= maxDistance

                if (animatedRemBtnYPos == - 1) {
                    animatedRemBtnYPos = visibleRemWidPos.y
                }

                removeBtnParams.x = DrawableUtils.rotateX(
                        visibleRemWidPos.x.toFloat() , animatedRemBtnYPos - radius * distance ,
                        hiddenRemWidPos.x.toFloat() , animatedRemBtnYPos.toFloat() , rotationDegrees.toFloat()).toInt()
                removeBtnParams.y = DrawableUtils.rotateY(
                        visibleRemWidPos.x.toFloat() , animatedRemBtnYPos - radius * distance ,
                        hiddenRemWidPos.x.toFloat() , animatedRemBtnYPos.toFloat() , rotationDegrees.toFloat()).toInt()

                try {
                    windowManager.updateViewLayout(removeWidgetView , removeBtnParams)
                } catch (e : IllegalArgumentException) {
                    // view not attached to window
                }

            }
        }

        abstract val REMOVE_BTN_ANIM_DURATION : Long

        override fun onReleased(x : Float , y : Float) {
            super.onReleased(x , y)
            playPauseButton.onTouchUp()
            released = true
            if (removeWidgetShown) {
                val animator = ValueAnimator.ofFloat(visibleRemWidPos.y.toFloat() , hiddenRemWidPos.y.toFloat())
                animator.duration = REMOVE_BTN_ANIM_DURATION
                animator.addUpdateListener(animatorUpdateListener)
                animator.addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation : Animator) {
                        removeWidgetShown = false
                        if (! isShown) {
                            try {
                                windowManager.removeView(removeWidgetView)
                            } catch (e : IllegalArgumentException) {
                                // view not attached to window
                            }

                        }
                    }
                })
                animator.start()
            }
            if (isReadyToRemove) {
                hideInternal(false)
            } else {
                if (onWidgetStateChangedListener != null) {
                    val params = playPauseButton.layoutParams as WindowManager.LayoutParams
                    onWidgetStateChangedListener !!.onWidgetPositionChanged((params.x + widgetHeight).toInt() , (params.y + widgetHeight).toInt())
                }
            }
        }

        override fun onAnimationCompleted() {
            super.onAnimationCompleted()
            if (onWidgetStateChangedListener != null) {
                val params = playPauseButton.layoutParams as WindowManager.LayoutParams
                onWidgetStateChangedListener !!.onWidgetPositionChanged((params.x + widgetHeight).toInt() , (params.y + widgetHeight).toInt())
            }
        }

        private val isReadyToRemove : Boolean
            get() {
                val removeParams = removeWidgetView.layoutParams as WindowManager.LayoutParams
                removeBounds.set(removeParams.x.toFloat() , removeParams.y.toFloat() , removeParams.x + widgetHeight , removeParams.y + widgetHeight)
                val params = playPauseButton.layoutParams as WindowManager.LayoutParams
                val cx = params.x + widgetHeight
                val cy = params.y + widgetHeight
                return removeBounds.contains(cx , cy)
            }

        companion object {

            private val REMOVE_BTN_ANIM_DURATION : Long = 200
        }
    }

    private inner class ExpandCollapseWidgetCallback : TouchManager.SimpleCallback() {

        override fun onTouched(x : Float , y : Float) {
            super.onTouched(x , y)
            expandCollapseWidget.onTouched(x , y)
        }

        override fun onReleased(x : Float , y : Float) {
            super.onReleased(x , y)
            expandCollapseWidget.onReleased(x , y)
        }

        override fun onClick(x : Float , y : Float) {
            super.onClick(x , y)
            expandCollapseWidget.onClick(x , y)
        }

        override fun onLongClick(x : Float , y : Float) {
            super.onLongClick(x , y)
            expandCollapseWidget.onLongClick(x , y)
        }

        override fun onTouchOutside() {
            if (! expandCollapseWidget.isAnimationInProgress) {
                collapse()
            }
        }

        override fun onMoved(diffX : Float , diffY : Float) {
            super.onMoved(diffX , diffY)
            updatePlayPauseButtonPosition()
        }

        override fun onAnimationCompleted() {
            super.onAnimationCompleted()
            updatePlayPauseButtonPosition()
        }
    }

    private inner class OnControlsClickListenerWrapper : OnControlsClickListener {

        private var onControlsClickListener : OnControlsClickListener? = null

        fun onControlsClickListener(inner : OnControlsClickListener?) : OnControlsClickListenerWrapper {
            this.onControlsClickListener = inner
            return this
        }

        override fun onPlaylistClicked() : Boolean {
            if (onControlsClickListener == null || ! onControlsClickListener !!.onPlaylistClicked()) {
                collapse()
                return true
            }
            return false
        }

        override fun onPlaylistLongClicked() {
            if (onControlsClickListener != null) {
                onControlsClickListener !!.onPlaylistLongClicked()
            }
        }

        override fun onPreviousClicked() {
            if (onControlsClickListener != null) {
                onControlsClickListener !!.onPreviousClicked()
            }
        }

        override fun onPreviousLongClicked() {
            if (onControlsClickListener != null) {
                onControlsClickListener !!.onPreviousLongClicked()
            }
        }

        override fun onPlayPauseClicked() : Boolean {
            if (onControlsClickListener == null || ! onControlsClickListener !!.onPlayPauseClicked()) {
                if (playbackState !!.state() != Configuration.STATE_PLAYING) {
                    playbackState !!.start(this@AudioWidget)
                } else {
                    playbackState !!.pause(this@AudioWidget)
                }
                return true
            }
            return false
        }

        override fun onPlayPauseLongClicked() {
            if (onControlsClickListener != null) {
                onControlsClickListener !!.onPlayPauseLongClicked()
            }
        }

        override fun onNextClicked() {
            if (onControlsClickListener != null) {
                onControlsClickListener !!.onNextClicked()
            }
        }

        override fun onNextLongClicked() {
            if (onControlsClickListener != null) {
                onControlsClickListener !!.onNextLongClicked()
            }
        }

        override fun onAlbumClicked() {
            if (onControlsClickListener != null) {
                onControlsClickListener !!.onAlbumClicked()
            }
        }

        override fun onAlbumLongClicked() {
            if (onControlsClickListener != null) {
                onControlsClickListener !!.onAlbumLongClicked()
            }
        }
    }

    /**
     * Builder class for [AudioWidget].
     */
    class Builder(internal val context : Context) {

        internal var darkColor : Int = 0

        internal var lightColor : Int = 0

        internal var progressColor : Int = 0

        internal var crossColor : Int = 0

        internal var crossOverlappedColor : Int = 0

        internal var shadowColor : Int = 0

        internal var expandWidgetColor : Int = 0
        internal var buttonPadding : Int = 0
        internal var crossStrokeWidth : Float = 0.toFloat()
        internal var progressStrokeWidth : Float = 0.toFloat()
        internal var shadowRadius : Float = 0.toFloat()
        internal var shadowDx : Float = 0.toFloat()
        internal var shadowDy : Float = 0.toFloat()
        internal var bubblesMinSize : Float = 0.toFloat()
        internal var bubblesMaxSize : Float = 0.toFloat()
        internal var playDrawable : Drawable? = null
        internal var prevDrawable : Drawable? = null
        internal var nextDrawable : Drawable? = null
        internal var playlistDrawable : Drawable? = null
        internal var defaultAlbumDrawable : Drawable? = null
        internal var pauseDrawable : Drawable? = null
        internal var darkColorSet : Boolean = false
        internal var lightColorSet : Boolean = false
        internal var progressColorSet : Boolean = false
        internal var crossColorSet : Boolean = false
        internal var crossOverlappedColorSet : Boolean = false
        internal var shadowColorSet : Boolean = false
        internal var expandWidgetColorSet : Boolean = false
        internal var buttonPaddingSet : Boolean = false
        internal var crossStrokeWidthSet : Boolean = false
        internal var progressStrokeWidthSet : Boolean = false
        internal var shadowRadiusSet : Boolean = false
        internal var shadowDxSet : Boolean = false
        internal var shadowDySet : Boolean = false
        internal var bubblesMinSizeSet : Boolean = false
        internal var bubblesMaxSizeSet : Boolean = false
        internal var edgeOffsetXCollapsed : Int = 0
        internal var edgeOffsetYCollapsed : Int = 0
        internal var edgeOffsetXExpanded : Int = 0
        internal var edgeOffsetYExpanded : Int = 0
        internal var edgeOffsetXCollapsedSet : Boolean = false
        internal var edgeOffsetYCollapsedSet : Boolean = false
        internal var edgeOffsetXExpandedSet : Boolean = false
        internal var edgeOffsetYExpandedSet : Boolean = false

        /**
         * Set dark color (playing state).
         * @param darkColor dark color
         */
        fun darkColor(@ColorInt darkColor : Int) : Builder {
            this.darkColor = darkColor
            darkColorSet = true
            return this
        }

        fun lightColor(@ColorInt lightColor : Int) : Builder {
            this.lightColor = lightColor
            lightColorSet = true
            return this
        }

        fun progressColor(@ColorInt progressColor : Int) : Builder {
            this.progressColor = progressColor
            progressColorSet = true
            return this
        }

        fun crossColor(@ColorInt crossColor : Int) : Builder {
            this.crossColor = crossColor
            crossColorSet = true
            return this
        }

        fun crossOverlappedColor(@ColorInt crossOverlappedColor : Int) : Builder {
            this.crossOverlappedColor = crossOverlappedColor
            crossOverlappedColorSet = true
            return this
        }

        fun shadowColor(@ColorInt shadowColor : Int) : Builder {
            this.shadowColor = shadowColor
            shadowColorSet = true
            return this
        }

        fun expandWidgetColor(@ColorInt expandWidgetColor : Int) : Builder {
            this.expandWidgetColor = expandWidgetColor
            expandWidgetColorSet = true
            return this
        }

        fun buttonPadding(buttonPadding : Int) : Builder {
            this.buttonPadding = buttonPadding
            buttonPaddingSet = true
            return this
        }

        fun crossStrokeWidth(crossStrokeWidth : Float) : Builder {
            this.crossStrokeWidth = crossStrokeWidth
            crossStrokeWidthSet = true
            return this
        }

        fun progressStrokeWidth(progressStrokeWidth : Float) : Builder {
            this.progressStrokeWidth = progressStrokeWidth
            progressStrokeWidthSet = true
            return this
        }

        fun shadowRadius(shadowRadius : Float) : Builder {
            this.shadowRadius = shadowRadius
            shadowRadiusSet = true
            return this
        }

        fun shadowDx(shadowDx : Float) : Builder {
            this.shadowDx = shadowDx
            shadowDxSet = true
            return this
        }

        fun shadowDy(shadowDy : Float) : Builder {
            this.shadowDy = shadowDy
            shadowDySet = true
            return this
        }

        fun bubblesMinSize(bubblesMinSize : Float) : Builder {
            this.bubblesMinSize = bubblesMinSize
            bubblesMinSizeSet = true
            return this
        }

        fun bubblesMaxSize(bubblesMaxSize : Float) : Builder {
            this.bubblesMaxSize = bubblesMaxSize
            bubblesMaxSizeSet = true
            return this
        }

        fun playDrawable(playDrawable : Drawable) : Builder {
            this.playDrawable = playDrawable
            return this
        }

        fun prevTrackDrawale(prevDrawable : Drawable) : Builder {
            this.prevDrawable = prevDrawable
            return this
        }

        fun nextTrackDrawable(nextDrawable : Drawable) : Builder {
            this.nextDrawable = nextDrawable
            return this
        }

        fun playlistDrawable(playlistDrawable : Drawable) : Builder {
            this.playlistDrawable = playlistDrawable
            return this
        }

        fun defaultAlbumDrawable(defaultAlbumCover : Drawable) : Builder {
            this.defaultAlbumDrawable = defaultAlbumCover
            return this
        }

        fun pauseDrawable(pauseDrawable : Drawable) : Builder {
            this.pauseDrawable = pauseDrawable
            return this
        }

        fun edgeOffsetXCollapsed(edgeOffsetX : Int) : Builder {
            this.edgeOffsetXCollapsed = edgeOffsetX
            edgeOffsetXCollapsedSet = true
            return this
        }

        fun edgeOffsetYCollapsed(edgeOffsetY : Int) : Builder {
            this.edgeOffsetYCollapsed = edgeOffsetY
            edgeOffsetYCollapsedSet = true
            return this
        }

        fun edgeOffsetYExpanded(edgeOffsetY : Int) : Builder {
            this.edgeOffsetYExpanded = edgeOffsetY
            edgeOffsetYExpandedSet = true
            return this
        }

        fun edgeOffsetXExpanded(edgeOffsetX : Int) : Builder {
            this.edgeOffsetXExpanded = edgeOffsetX
            edgeOffsetXExpandedSet = true
            return this
        }

        fun build() : AudioWidget {
            if (buttonPaddingSet) {
                checkOrThrow(buttonPadding , "Button padding")
            }
            if (shadowRadiusSet) {
                checkOrThrow(shadowRadius , "Shadow radius")
            }
            if (shadowDxSet) {
                checkOrThrow(shadowDx , "Shadow dx")
            }
            if (shadowDySet) {
                checkOrThrow(shadowDy , "Shadow dy")
            }
            if (bubblesMinSizeSet) {
                checkOrThrow(bubblesMinSize , "Bubbles min size")
            }
            if (bubblesMaxSizeSet) {
                checkOrThrow(bubblesMaxSize , "Bubbles max size")
            }
            if (bubblesMinSizeSet && bubblesMaxSizeSet && bubblesMaxSize < bubblesMinSize) {
                throw IllegalArgumentException("Bubbles max size must be greater than bubbles min size")
            }
            if (crossStrokeWidthSet) {
                checkOrThrow(crossStrokeWidth , "Cross stroke width")
            }
            if (progressStrokeWidthSet) {
                checkOrThrow(progressStrokeWidth , "Progress stroke width")
            }
            return AudioWidget(this)
        }

        private fun checkOrThrow(number : Int , name : String) {
            if (number < 0)
                throw IllegalArgumentException(name + " must be equals or greater zero.")
        }

        private fun checkOrThrow(number : Float , name : String) {
            if (number < 0)
                throw IllegalArgumentException(name + " must be equals or greater zero.")
        }

    }

    interface Controller {

        fun start()

        fun pause()

        fun stop()

        fun duration() : Int

        fun duration(duration : Int)

        fun position() : Int

        fun position(position : Int)

        fun onControlsClickListener(onControlsClickListener : OnControlsClickListener?)

        fun onWidgetStateChangedListener(onWidgetStateChangedListener : OnWidgetStateChangedListener?)

        fun albumCover(albumCover : Drawable?)

        fun albumCoverBitmap(albumCover : Bitmap?)
    }

    interface OnControlsClickListener {

        fun onPlaylistClicked() : Boolean

        fun onPlaylistLongClicked()

        fun onPreviousClicked()

        fun onPreviousLongClicked()

        fun onPlayPauseClicked() : Boolean

        fun onPlayPauseLongClicked()

        fun onNextClicked()

        fun onNextLongClicked()

        fun onAlbumClicked()

        fun onAlbumLongClicked()
    }

    interface OnWidgetStateChangedListener {

        fun onWidgetStateChanged(state : State)

        fun onWidgetPositionChanged(cx : Int , cy : Int)
    }

    enum class State {
        COLLAPSED ,
        EXPANDED ,
        REMOVED
    }

    companion object {

        private val VIBRATION_DURATION : Long = 100
    }
}