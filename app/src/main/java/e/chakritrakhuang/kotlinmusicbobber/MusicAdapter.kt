package e.chakritrakhuang.kotlinmusicbobber

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.bumptech.glide.Glide

import java.util.Locale

import butterknife.Bind
import butterknife.ButterKnife
import jp.wasabeef.glide.transformations.CropCircleTransformation

internal class MusicAdapter(context : Context) : BaseRecyclerViewAdapter<MusicItem , MusicAdapter.MusicViewHolder>(context) {

    private val cropCircleTransformation : CropCircleTransformation

    init {
        cropCircleTransformation = CropCircleTransformation(context)
    }

    override fun onCreateViewHolder(parent : ViewGroup , viewType : Int) : MusicViewHolder {
        val view = inflater.inflate(R.layout.item_music , parent , false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder : MusicViewHolder , position : Int) {
        val item = getItem(position)
        holder.title !!.text = filter !!.highlightFilteredSubstring(item.title())
        holder.artist !!.text = filter !!.highlightFilteredSubstring(item.artist())
        holder.album !!.text = filter !!.highlightFilteredSubstring(item.album())
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

        @Bind(R.id.title)
        var title : TextView? = null

        @Bind(R.id.artist)
        var artist : TextView? = null

        @Bind(R.id.album)
        var album : TextView? = null

        @Bind(R.id.duration)
        var duration : TextView? = null

        @Bind(R.id.album_cover)
        var albumCover : ImageView? = null


        init {
            ButterKnife.bind(this , itemView)
        }
    }
}