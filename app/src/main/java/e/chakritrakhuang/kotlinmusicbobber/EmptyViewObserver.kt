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
        this.recyclerViewWeakReference = WeakReference(recyclerView)
        recyclerView.adapter.registerAdapterDataObserver(this)
    }

    fun unbind() {
        if (recyclerViewWeakReference == null)
            return
        val recyclerView = recyclerViewWeakReference !!.get()
        if (recyclerView != null) {
            recyclerView.adapter.unregisterAdapterDataObserver(this)
            recyclerViewWeakReference !!.clear()
        }
    }

    override fun onChanged() {
        super.onChanged()
        somethingChanged()
    }

    override fun onItemRangeChanged(positionStart : Int , itemCount : Int) {
        super.onItemRangeChanged(positionStart , itemCount)
        somethingChanged()
    }

    override fun onItemRangeChanged(positionStart : Int , itemCount : Int , payload : Any?) {
        super.onItemRangeChanged(positionStart , itemCount , payload)
        somethingChanged()
    }

    override fun onItemRangeInserted(positionStart : Int , itemCount : Int) {
        super.onItemRangeInserted(positionStart , itemCount)
        somethingChanged()
    }

    override fun onItemRangeRemoved(positionStart : Int , itemCount : Int) {
        super.onItemRangeRemoved(positionStart , itemCount)
        somethingChanged()
    }

    private fun somethingChanged() {
        val view = viewWeakReference.get()
        val recyclerView = recyclerViewWeakReference !!.get()
        if (view != null && recyclerView != null) {
            if (recyclerView.adapter.itemCount == 0) {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        }
    }
}