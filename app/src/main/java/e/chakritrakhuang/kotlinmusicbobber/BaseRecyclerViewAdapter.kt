package e.chakritrakhuang.kotlinmusicbobber

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.view.LayoutInflater

import java.util.ArrayList

/**
 * Base adapter for recycler view
 */
internal abstract class BaseRecyclerViewAdapter<TData , TViewHolder : RecyclerView.ViewHolder> : RecyclerView.Adapter<TViewHolder> , BaseFilter.FilterableAdapter<TData> {

    protected val context : Context
    protected val inflater : LayoutInflater
    private val data : MutableList<TData>
    var filter : BaseFilter<TData>? = null
        private set

    constructor(context : Context) {
        this.context = context.applicationContext
        this.inflater = LayoutInflater.from(context)
        data = ArrayList()
    }

    constructor(context : Context , data : List<TData>) {
        this.context = context.applicationContext
        this.inflater = LayoutInflater.from(context)
        this.data = ArrayList(data)
    }

    override fun withFilter(filter : BaseFilter<TData>?) {
        if (this.filter != null)
            unregisterAdapterDataObserver(this.filter !!.adapterDataObserver)
        this.filter = filter
        if (this.filter != null) {
            this.filter !!.init(this)
            registerAdapterDataObserver(this.filter !!.adapterDataObserver)
        }
    }

    override fun getItemCount() : Int {
        return if (filter != null && filter !!.isFiltered) filter !!.count else data.size
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    fun getItem(position : Int) : TData {
        return if (filter != null && filter !!.isFiltered) filter !!.getItem(position) else data[position]
    }

    override val isFiltered : Boolean
        get() = filter != null && filter !!.isFiltered

    override fun highlightFilteredSubstring(text : String) : Spannable {
        return if (isFiltered) filter !!.highlightFilteredSubstring(text) else SpannableString(text)
    }

    override fun getNonFilteredItem(position : Int) : TData {
        return data[position]
    }

    override val nonFilteredCount : Int
        get() = data.size

    fun add(`object` : TData) : Boolean {
        return data.add(`object`)
    }

    fun remove(`object` : TData) : Boolean {
        return data.remove(`object`)
    }

    fun remove(position : Int) : TData {
        return data.removeAt(position)
    }

    fun clear() {
        data.clear()
    }

    fun addAll(collection : Collection<TData>) : Boolean {
        return data.addAll(collection)
    }

    val snapshot : List<TData>
        get() = ArrayList(data)
}
