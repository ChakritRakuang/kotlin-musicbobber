package e.chakritrakhuang.kotlinmusicbobber

import android.support.v7.widget.RecyclerView
import android.view.View

import java.lang.ref.WeakReference

/**
 * Simple observer for displaying and hiding empty view.
 */
internal class EmptyViewObserver(view : View) : RecyclerView.AdapterDataObserver() {

    private val viewWeakReference : WeakReference<View>
    private var recyclerViewWeakReference : WeakReference<RecyclerView>? = null

    init {
        viewWeakReference = WeakReference(view)
    }

    /**
     * Bind observer to recycler view's adapter. This method must be called after setting adapter to recycler view.
     * @param recyclerView instance of recycler view
     */
    fun bind(recyclerView : RecyclerView) {
        unbind()
        this.recyclerViewWeakReference = WeakReference<RecyclerView>(recyclerView)
        recyclerView.getAdapter().registerAdapterDataObserver(this)
    }

    fun unbind() {
        if (recyclerViewWeakReference == null)
            return
        val recyclerView = recyclerViewWeakReference !!.get()
        if (recyclerView != null) {
            recyclerView !!.getAdapter().unregisterAdapterDataObserver(this)
            recyclerViewWeakReference !!.clear()
        }
    }

    fun onChanged() {
        super.onChanged()
        somethingChanged()
    }

    fun onItemRangeChanged(positionStart : Int , itemCount : Int) {
        super.onItemRangeChanged(positionStart , itemCount)
        somethingChanged()
    }

    fun onItemRangeChanged(positionStart : Int , itemCount : Int , payload : Any) {
        super.onItemRangeChanged(positionStart , itemCount , payload)
        somethingChanged()
    }

    fun onItemRangeInserted(positionStart : Int , itemCount : Int) {
        super.onItemRangeInserted(positionStart , itemCount)
        somethingChanged()
    }

    fun onItemRangeRemoved(positionStart : Int , itemCount : Int) {
        super.onItemRangeRemoved(positionStart , itemCount)
        somethingChanged()
    }

    private fun somethingChanged() {
        val view = viewWeakReference.get()
        val recyclerView = recyclerViewWeakReference !!.get()
        if (view != null && recyclerView != null) {
            if (recyclerView !!.getAdapter().getItemCount() === 0) {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }
    }
}