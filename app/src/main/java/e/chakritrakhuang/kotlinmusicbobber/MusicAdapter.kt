package e.chakritrakhuang.kotlinmusicbobber

import android.content.Context
import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import java.util.Locale
import butterknife.ButterKnife
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.SimpleTarget
import jp.wasabeef.glide.transformations.CropCircleTransformation

internal class MusicAdapter(context : Context) : BaseRecyclerViewAdapter<MusicItem , MusicAdapter.MusicViewHolder>(context) {

    private val cropCircleTransformation : CropCircleTransformation = CropCircleTransformation(context)

    override fun onCreateViewHolder(parent : ViewGroup , viewType : Int) : MusicViewHolder {
        val view = inflater.inflate(R.layout.item_music , parent , false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder : MusicViewHolder , position : Int) {
        val item = getItem(position)
        holder.title !!.text = item.title()?.let { filter !!.highlightFilteredSubstring(it) }
        holder.artist !!.text = item.artist()?.let { filter !!.highlightFilteredSubstring(it) }
        holder.album !!.text = item.album()?.let { filter !!.highlightFilteredSubstring(it) }
        holder.duration !!.text = convertDuration(item.duration())
        Glide.with(context)
                .load(item.albumArtUri())
                .asBitmap()
                .transform(cropCircleTransformation)
                .placeholder(R.drawable.aw_ic_default_album)
                .error(R.drawable.aw_ic_default_album)
                .into(holder.albumCover)
    }

    private fun convertDuration(durationInMs : Long) : String {
        val durationInSeconds = durationInMs / 1000
        val seconds = durationInSeconds % 60
        val minutes = durationInSeconds % 3600 / 60
        val hours = durationInSeconds / 3600
        return if (hours > 0) {
            String.format(Locale.US , "%02d:%02d:%02d" , hours , minutes , seconds)
        } else String.format(Locale.US , "%02d:%02d" , minutes , seconds)
    }

    internal class MusicViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

        var title : TextView? = null

        var artist : TextView? = null

        var album : TextView? = null

        var duration : TextView? = null

        var albumCover : ImageView? = null

        init {
            ButterKnife.bind(this , itemView)
        }
    }
}

internal fun Any.into(albumCover : SimpleTarget<Bitmap>) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

private fun Any.error(aw_ic_default_album : Int) : Any {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

private fun Any.placeholder(aw_ic_default_album : Int) : Any {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

internal fun Any.transform(cropCircleTransformation : CropCircleTransformation) : Any {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

internal fun <TranscodeType> RequestBuilder<TranscodeType>.asBitmap() : Any {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}
