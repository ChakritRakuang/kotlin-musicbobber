package e.chakritrakhuang.kotlinmusicbobber

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.FloatEvaluator
import android.animation.IntEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator

internal class TouchManager(private val view : View , private val boundsChecker : BoundsChecker) : View.OnTouchListener {
    private val windowManager : WindowManager
    private val stickyEdgeAnimator : StickyEdgeAnimator
    private val velocityAnimator : FlingGestureAnimator

    private var gestureListener : GestureListener? = null
    private val gestureDetector : GestureDetector
    private var callback : Callback? = null
    private var screenWidth : Int = 0
    private var screenHeight : Int = 0
    private var lastRawX : Float? = null
    private var lastRawY : Float? = null
    private var touchCanceled : Boolean = false

    init {
        this.gestureDetector = GestureDetector()
        gestureDetector.setIsLongpressEnabled(true)
        this.view.setOnTouchListener(this)
        val context = view.context.applicationContext
        this.windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        this.screenWidth = context.resources.displayMetrics.widthPixels
        this.screenHeight = context.resources.displayMetrics.heightPixels - context.resources.getDimensionPixelSize(R.dimen.aw_status_bar_height)
        stickyEdgeAnimator = StickyEdgeAnimator()
        velocityAnimator = FlingGestureAnimator()
    }

    private fun GestureDetector() : GestureDetector {}

    fun screenWidth(screenWidth : Int) : TouchManager {
        this.screenWidth = screenWidth
        return this
    }

    fun screenHeight(screenHeight : Int) : TouchManager {
        this.screenHeight = screenHeight
        return this
    }

    fun callback(callback : Callback) : TouchManager {
        this.callback = callback
        return this
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v : View , event : MotionEvent) : Boolean {
        val res = (! touchCanceled || event.action == MotionEvent.ACTION_UP) && gestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_DOWN) {
            touchCanceled = false
            gestureListener !!.onDown(event)
        } else if (event.action == MotionEvent.ACTION_UP) {
            if (! touchCanceled) {
                gestureListener !!.onUpEvent(event)
            }
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            if (! touchCanceled) {
                gestureListener !!.onMove(event)
            }
        } else if (event.action == MotionEvent.ACTION_OUTSIDE) {
            gestureListener !!.onTouchOutsideEvent(event)
            touchCanceled = false
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            touchCanceled = true
        }
        return res
    }

    /**
     * Touch manager callback.
     */
    internal interface Callback {

        /**
         * Called when user clicks on view.
         * @param x click x coordinate
         * @param y click y coordinate
         */
        fun onClick(x : Float , y : Float)

        /**
         * Called when user long clicks on view.
         * @param x click x coordinate
         * @param y click y coordinate
         */
        fun onLongClick(x : Float , y : Float)

        /**
         * Called when user touches screen outside view's bounds.
         */
        fun onTouchOutside()

        /**
         * Called when user touches widget but not removed finger from it.
         * @param x x coordinate
         * @param y y coordinate
         */
        fun onTouched(x : Float , y : Float)

        /**
         * Called when user drags widget.
         * @param diffX movement by X axis
         * @param diffY movement by Y axis
         */
        fun onMoved(diffX : Float , diffY : Float)

        /**
         * Called when user releases finger from widget.
         * @param x x coordinate
         * @param y y coordinate
         */
        fun onReleased(x : Float , y : Float)

        /**
         * Called when sticky edge animation completed.
         */
        fun onAnimationCompleted()
    }

    internal open class SimpleCallback : Callback {

        override fun onClick(x : Float , y : Float) {

        }

        override fun onLongClick(x : Float , y : Float) {

        }

        override fun onTouchOutside() {

        }

        override fun onTouched(x : Float , y : Float) {

        }

        override fun onMoved(diffX : Float , diffY : Float) {

        }

        override fun onReleased(x : Float , y : Float) {

        }

        override fun onAnimationCompleted() {

        }

    }

    /**
     * Interface that return sticky bounds for widget.
     */
    internal interface BoundsChecker {

        /**
         * Get sticky left position.
         * @param screenWidth screen width
         * @return sticky left position
         */
        fun stickyLeftSide(screenWidth : Float) : Float

        /**
         * Get sticky right position.
         * @param screenWidth screen width
         * @return sticky right position
         */
        fun stickyRightSide(screenWidth : Float) : Float

        /**
         * Get sticky top position.
         * @param screenHeight screen height
         * @return sticky top position
         */
        fun stickyTopSide(screenHeight : Float) : Float

        /**
         * Get sticky bottom position.
         * @param screenHeight screen height
         * @return sticky bottom position
         */
        fun stickyBottomSide(screenHeight : Float) : Float
    }

    /**
     * View's gesture listener.
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private var prevX : Int = 0
        private var prevY : Int = 0
        private var velX : Float = 0.toFloat()
        private var velY : Float = 0.toFloat()
        private var lastEventTime : Long = 0

        override fun onDown(e : MotionEvent) : Boolean {
            val params = view.layoutParams as WindowManager.LayoutParams
            prevX = params.x
            prevY = params.y
            val result = ! stickyEdgeAnimator.isAnimating
            if (result) {
                if (callback != null) {
                    callback !!.onTouched(e.x , e.y)
                }
            }
            return result
        }

        override fun onSingleTapUp(e : MotionEvent) : Boolean {
            if (callback != null) {
                callback !!.onClick(e.x , e.y)
            }
            return true
        }

        override fun onScroll(e1 : MotionEvent , e2 : MotionEvent , distanceX : Float , distanceY : Float) : Boolean {
            val diffX = e2.rawX - e1.rawX
            val diffY = e2.rawY - e1.rawY
            val l = prevX + diffX
            val t = prevY + diffY
            val params = view.layoutParams as WindowManager.LayoutParams
            params.x = l.toInt()
            params.y = t.toInt()
            try {
                windowManager.updateViewLayout(view , params)
            } catch (e : IllegalArgumentException) {
                // view not attached to window
            }

            if (callback != null) {
                callback !!.onMoved(distanceX , distanceY)
            }
            return true
        }

        override fun onLongPress(e : MotionEvent) {
            if (callback != null) {
                callback !!.onLongClick(e.x , e.y)
            }
            val downTime = SystemClock.uptimeMillis()
            val eventTime = SystemClock.uptimeMillis() + 100
            val x = 0.0f
            val y = 0.0f
            val metaState = 0
            val event = MotionEvent.obtain(
                    downTime ,
                    eventTime ,
                    MotionEvent.ACTION_CANCEL ,
                    x ,
                    y ,
                    metaState
            )
            view.dispatchTouchEvent(event)
            //            onUpEvent(e);
        }

        override fun onFling(e1 : MotionEvent , e2 : MotionEvent , velocityX : Float , velocityY : Float) : Boolean {
            velocityAnimator.animate(velX , velY)
            return true
        }

        fun onMove(e2 : MotionEvent) {
            if (lastRawX != null && lastRawY != null) {
                val diff = e2.eventTime - lastEventTime
                val dt = if (diff == 0) 0 else 1000f / diff
                val newVelX = (e2.rawX - lastRawX !!) * dt
                val newVelY = (e2.rawY - lastRawY !!) * dt
                velX = DrawableUtils.smooth(velX , newVelX , 0.2f)
                velY = DrawableUtils.smooth(velY , newVelY , 0.2f)
            }
            lastRawX = e2.rawX
            lastRawY = e2.rawY
            lastEventTime = e2.eventTime
        }

        fun onUpEvent(e : MotionEvent) {
            if (callback != null) {
                callback !!.onReleased(e.x , e.y)
            }
            lastRawX = null
            lastRawY = null
            lastEventTime = 0
            velY = 0f
            velX = velY
            if (! velocityAnimator.isAnimating) {
                stickyEdgeAnimator.animate(boundsChecker)
            }
        }

        fun onTouchOutsideEvent(e : MotionEvent) {
            if (callback != null) {
                callback !!.onTouchOutside()
            }
        }
    }

    /**
     * Helper class for animating fling gesture.
     */
    private inner class FlingGestureAnimator internal constructor() {
        private val flingGestureAnimator : ValueAnimator
        private val dxHolder : PropertyValuesHolder
        private val dyHolder : PropertyValuesHolder
        private val interpolator : Interpolator
        private var params : WindowManager.LayoutParams? = null

        init {
            interpolator = DecelerateInterpolator()
            dxHolder = PropertyValuesHolder.ofFloat("x" , 0 , 0)
            dyHolder = PropertyValuesHolder.ofFloat("y" , 0 , 0)
            dxHolder.setEvaluator(FloatEvaluator())
            dyHolder.setEvaluator(FloatEvaluator())
            flingGestureAnimator = ValueAnimator.ofPropertyValuesHolder(dxHolder , dyHolder)
            flingGestureAnimator.interpolator = interpolator
            flingGestureAnimator.duration = DEFAULT_ANIM_DURATION
            flingGestureAnimator.addUpdateListener { animation ->
                val newX = animation.getAnimatedValue("x") as Float
                val newY = animation.getAnimatedValue("y") as Float
                if (callback != null) {
                    callback !!.onMoved(newX - params !!.x , newY - params !!.y)
                }
                params !!.x = newX.toInt()
                params !!.y = newY.toInt()

                try {
                    windowManager.updateViewLayout(view , params)
                } catch (e : IllegalArgumentException) {
                    animation.cancel()
                }
            }
            flingGestureAnimator.addListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation : Animator) {
                    super.onAnimationEnd(animation)
                    stickyEdgeAnimator.animate(boundsChecker)
                }

                override fun onAnimationCancel(animation : Animator) {
                    super.onAnimationCancel(animation)
                    stickyEdgeAnimator.animate(boundsChecker)
                }
            })
        }

        internal fun animate(velocityX : Float , velocityY : Float) {
            if (isAnimating) {
                return
            }

            params = view.layoutParams as WindowManager.LayoutParams

            val dx = velocityX / 1000f * DEFAULT_ANIM_DURATION
            val dy = velocityY / 1000f * DEFAULT_ANIM_DURATION

            val newX : Float
            val newY : Float

            if (dx + params !!.x > screenWidth / 2f) {
                newX = boundsChecker.stickyRightSide(screenWidth.toFloat()) + Math.min(view.width , view.height) / 2f
            } else {
                newX = boundsChecker.stickyLeftSide(screenWidth.toFloat()) - Math.min(view.width , view.height) / 2f
            }

            newY = params !!.y + dy

            dxHolder.setFloatValues(params !!.x.toFloat() , newX)
            dyHolder.setFloatValues(params !!.y.toFloat() , newY)

            flingGestureAnimator.start()
        }

        internal val isAnimating : Boolean
            get() = flingGestureAnimator.isRunning

        companion object {
            private val DEFAULT_ANIM_DURATION : Long = 200
        }
    }

    /**
     * Helper class for animating sticking to screen edge.
     */
    private inner class StickyEdgeAnimator {
        private val dxHolder : PropertyValuesHolder
        private val dyHolder : PropertyValuesHolder
        private val edgeAnimator : ValueAnimator
        private val interpolator : Interpolator
        private var params : WindowManager.LayoutParams? = null

        init {
            interpolator = OvershootInterpolator()
            dxHolder = PropertyValuesHolder.ofInt("x" , 0 , 0)
            dyHolder = PropertyValuesHolder.ofInt("y" , 0 , 0)
            dxHolder.setEvaluator(IntEvaluator())
            dyHolder.setEvaluator(IntEvaluator())
            edgeAnimator = ValueAnimator.ofPropertyValuesHolder(dxHolder , dyHolder)
            edgeAnimator.interpolator = interpolator
            edgeAnimator.duration = DEFAULT_ANIM_DURATION
            edgeAnimator.addUpdateListener { animation ->
                val x = animation.getAnimatedValue("x") as Int
                val y = animation.getAnimatedValue("y") as Int
                if (callback != null) {
                    callback !!.onMoved((x - params !!.x).toFloat() , (y - params !!.y).toFloat())
                }
                params !!.x = x
                params !!.y = y
                try {
                    windowManager.updateViewLayout(view , params)
                } catch (e : IllegalArgumentException) {
                    // view not attached to window
                    animation.cancel()
                }
            }
            edgeAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation : Animator) {
                    super.onAnimationEnd(animation)
                    if (callback != null) {
                        callback !!.onAnimationCompleted()
                    }
                }
            })
        }

        fun animate(boundsChecker : BoundsChecker) {
            animate(boundsChecker , null)
        }

        fun animate(boundsChecker : BoundsChecker , afterAnimation : Runnable?) {
            if (edgeAnimator.isRunning) {
                return
            }
            params = view.layoutParams as WindowManager.LayoutParams
            val cx = params !!.x + view.width / 2f
            val cy = params !!.y + view.width / 2f
            val x : Int
            if (cx < screenWidth / 2f) {
                x = boundsChecker.stickyLeftSide(screenWidth.toFloat()).toInt()
            } else {
                x = boundsChecker.stickyRightSide(screenWidth.toFloat()).toInt()
            }
            var y = params !!.y
            val top = boundsChecker.stickyTopSide(screenHeight.toFloat()).toInt()
            val bottom = boundsChecker.stickyBottomSide(screenHeight.toFloat()).toInt()
            if (params !!.y > bottom || params !!.y < top) {
                if (cy < screenHeight / 2f) {
                    y = top
                } else {
                    y = bottom
                }
            }
            dxHolder.setIntValues(params !!.x , x)
            dyHolder.setIntValues(params !!.y , y)
            edgeAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation : Animator) {
                    super.onAnimationCancel(animation)
                    edgeAnimator.removeListener(this)
                    afterAnimation?.run()
                }

                override fun onAnimationEnd(animation : Animator) {
                    super.onAnimationEnd(animation)
                    edgeAnimator.removeListener(this)
                    afterAnimation?.run()
                }
            })
            edgeAnimator.start()
        }

        internal val isAnimating : Boolean
            get() = edgeAnimator.isRunning

        companion object {
            private val DEFAULT_ANIM_DURATION : Long = 300
        }
    }

    fun animateToBounds(boundsChecker : BoundsChecker , afterAnimation : Runnable?) {
        stickyEdgeAnimator.animate(boundsChecker , afterAnimation)
    }

    fun animateToBounds() {
        stickyEdgeAnimator.animate(boundsChecker , null)
    }
}