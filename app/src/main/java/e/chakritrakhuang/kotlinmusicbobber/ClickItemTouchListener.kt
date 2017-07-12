package e.chakritrakhuang.kotlinmusicbobber

import android.content.Context
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.OnItemTouchListener
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View

internal abstract class ClickItemTouchListener(hostView : RecyclerView) : OnItemTouchListener {

    private val mGestureDetector : GestureDetector

    init {
        mGestureDetector = ItemClickGestureDetector(hostView.context ,
                ItemClickGestureListener(hostView))
    }

    private fun isAttachedToWindow(hostView : RecyclerView) : Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hostView.isAttachedToWindow
        } else {
            hostView.handler != null
        }
    }

    private fun hasAdapter(hostView : RecyclerView) : Boolean {
        return hostView.adapter != null
    }

    override fun onInterceptTouchEvent(recyclerView : RecyclerView , event : MotionEvent) : Boolean {
        if (! isAttachedToWindow(recyclerView) || ! hasAdapter(recyclerView)) {
            return false
        }

        mGestureDetector.onTouchEvent(event)
        return false
    }

    override fun onTouchEvent(recyclerView : RecyclerView , event : MotionEvent) {

    }

    internal abstract fun performItemClick(parent : RecyclerView , view : View , position : Int , id : Long) : Boolean
    internal abstract fun performItemLongClick(parent : RecyclerView , view : View , position : Int , id : Long) : Boolean

    private class ItemClickGestureDetector(context : Context , private val mGestureListener : ItemClickGestureListener) : GestureDetector(context , mGestureListener) {

        override fun onTouchEvent(event : MotionEvent) : Boolean {
            val handled = super.onTouchEvent(event)

            return handled
        }
    }

    private inner class ItemClickGestureListener(private val mHostView : RecyclerView) : SimpleOnGestureListener() {
        private var mTargetChild : View? = null

        fun dispatchSingleTapUpIfNeeded(event : MotionEvent) {

            if (mTargetChild != null) {
                onSingleTapUp(event)
            }
        }

        override fun onDown(event : MotionEvent) : Boolean {
            val x = event.x.toInt()
            val y = event.y.toInt()

            mTargetChild = mHostView.findChildViewUnder(x.toFloat() , y.toFloat())
            return mTargetChild != null
        }

        override fun onShowPress(event : MotionEvent) {
            if (mTargetChild != null) {
                mTargetChild !!.isPressed = true
            }
        }

        override fun onSingleTapUp(event : MotionEvent) : Boolean {
            var handled = false

            if (mTargetChild != null) {
                mTargetChild !!.isPressed = false

                val position = mHostView.getChildPosition(mTargetChild)
                val id = mHostView.adapter.getItemId(position)
                handled = performItemClick(mHostView , mTargetChild !! , position , id)

                mTargetChild = null
            }

            return handled
        }

        override fun onScroll(event : MotionEvent , event2 : MotionEvent , v : Float , v2 : Float) : Boolean {
            if (mTargetChild != null) {
                mTargetChild !!.isPressed = false
                mTargetChild = null

                return true
            }

            return false
        }

        override fun onLongPress(event : MotionEvent) {
            if (mTargetChild == null) {
                return
            }

            val position = mHostView.getChildPosition(mTargetChild)
            val id = mHostView.adapter.getItemId(position)
            val handled = performItemLongClick(mHostView , mTargetChild !! , position , id)

            if (handled) {
                mTargetChild !!.isPressed = false
                mTargetChild = null
            }
        }
    }
}
