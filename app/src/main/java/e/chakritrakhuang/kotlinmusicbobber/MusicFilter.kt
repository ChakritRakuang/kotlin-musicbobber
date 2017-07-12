package e.chakritrakhuang.kotlinmusicbobber

import android.text.TextUtils

import java.util.ArrayList

/**
 * Filter for list of tracks.
 */
internal class MusicFilter @Throws(AssertionError::class)
constructor(highlightColor : Int) : BaseFilter<MusicItem>(highlightColor) {

    override fun performFilteringImpl(constraint : CharSequence?) : Filter.FilterResults {
        val results = Filter.FilterResults()
        if (TextUtils.isEmpty(constraint) || TextUtils.isEmpty(constraint !!.toString().trim { it <= ' ' })) {
            results.count = - 1
            return results
        }
        val str = constraint.toString().trim { it <= ' ' }
        val result = ArrayList<MusicItem>()
        val size = nonFilteredCount
        for (i in 0 .. size - 1) {
            val item = getNonFilteredItem(i)
            if (check(str , item.title())
                    || check(str , item.album())
                    || check(str , item.artist())) {
                result.add(item)
            }
        }
        results.count = result.size
        results.values = result
        return results
    }

    private fun check(what : String , where : String?) : Boolean {
        var what = what
        var where = where
        if (TextUtils.isEmpty(where))
            return false
        where = where !!.toLowerCase()
        what = what.toLowerCase()
        return where.contains(what)
    }
}
