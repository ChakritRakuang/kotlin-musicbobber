package e.chakritrakhuang.kotlinmusicbobber

import android.support.v7.widget.RecyclerView
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View

internal class ItemClickSupport private constructor(private val mRecyclerView : RecyclerView) {

    interface OnItemClickListener {

        fun onItemClick(parent : RecyclerView , view : View , position : Int , id : Long)
    }

    interface OnItemLongClickListener {

        fun onItemLongClick(parent : RecyclerView , view : View , position : Int , id : Long) : Boolean
    }

    private val mTouchListener : TouchListener

    private var mItemClickListener : OnItemClickListener? = null
    private var mItemLongClickListener : OnItemLongClickListener? = null

    init {

        mTouchListener = TouchListener(mRecyclerView)
        mRecyclerView.addOnItemTouchListener(mTouchListener)
    }

    fun setOnItemClickListener(listener : (Any , Any , Any , Any) -> Unit) {
    }

    fun setOnItemLongClickListener(listener : OnItemLongClickListener) {
        if (! mRecyclerView.isLongClickable) {
            mRecyclerView.isLongClickable = true
        }

        mItemLongClickListener = listener
    }

    private inner class TouchListener internal constructor(recyclerView : RecyclerView) : ClickItemTouchListener(recyclerView) {

        override fun performItemClick(parent : RecyclerView , view : View , position : Int , id : Long) : Boolean {
            if (mItemClickListener != null) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                mItemClickListener !!.onItemClick(parent , view , position , id)
                return true
            }

            return false
        }

        override fun performItemLongClick(parent : RecyclerView , view : View , position : Int , id : Long) : Boolean {
            if (mItemLongClickListener != null) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                return mItemLongClickListener !!.onItemLongClick(parent , view , position , id)
            }

            return false
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept : Boolean) {

        }
    }

    companion object {

        fun addTo(recyclerView : RecyclerView) : ItemClickSupport {
            var itemClickSupport = from(recyclerView)
            if (itemClickSupport == null) {
                itemClickSupport = ItemClickSupport(recyclerView)
                recyclerView.setTag(R.id.twowayview_item_click_support , itemClickSupport)
            }

            return itemClickSupport
        }

        fun removeFrom(recyclerView : RecyclerView) {
            val itemClickSupport = from(recyclerView) ?: return

            recyclerView.removeOnItemTouchListener(itemClickSupport.mTouchListener)
            recyclerView.setTag(R.id.twowayview_item_click_support , null)
        }

        fun from(recyclerView : RecyclerView?) : ItemClickSupport? {
            return if (recyclerView == null) {
                null
            } else recyclerView.getTag(R.id.twowayview_item_click_support) as ItemClickSupport

        }
    }
}