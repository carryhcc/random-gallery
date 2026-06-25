package com.example.randomgallery.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.randomgallery.android.R
import com.example.randomgallery.android.data.model.XhsWorkMedia
import com.example.randomgallery.android.databinding.ItemMediaBinding
import com.example.randomgallery.android.util.ImageUrlResolver
import com.example.randomgallery.android.util.buildDownloadFileName
import com.example.randomgallery.android.util.downloadToPublic
import com.example.randomgallery.android.util.toast

class MediaAdapter(
    private val isGif: Boolean,
    private val onDelete: (XhsWorkMedia) -> Unit,
    private val onPreview: (XhsWorkMedia) -> Unit,
    private val onDownload: (XhsWorkMedia) -> Unit
) : ListAdapter<XhsWorkMedia, MediaAdapter.MediaViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class MediaViewHolder(private val binding: ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: XhsWorkMedia, position: Int) {
            // GIF（实为 mp4）只在列表里展示首帧静态缩略图 + 播放角标，
            // 点击进入全屏预览才真正播放，避免列表内多个 VideoView 同时拉流播放导致解码器/内存耗尽。
            val mediaUrl = if (isGif) {
                ImageUrlResolver.absoluteUrl(item.mediaUrl)
            } else {
                ImageUrlResolver.displayUrl(item.mediaUrl)
            }
            binding.index.text = binding.root.context.getString(R.string.media_index_number, position + 1)
            binding.playBadge.visibility = if (isGif) View.VISIBLE else View.GONE
            binding.videoView.visibility = View.GONE
            binding.image.visibility = View.VISIBLE
            binding.image.load(mediaUrl) {
                crossfade(true)
                error(R.drawable.ic_404_placeholder)
                placeholder(R.drawable.ic_loading_placeholder)
            }
            binding.previewBtn.setOnClickListener { onPreview(item) }
            binding.downloadBtn.setOnClickListener { onDownload(item) }
            binding.deleteBtn.setOnClickListener { onDelete(item) }
            binding.root.setOnClickListener { onPreview(item) }
            binding.root.setOnLongClickListener {
                val context = binding.root.context
                PopupMenu(context, binding.root).apply {
                    menu.add(context.getString(R.string.save_image))
                    setOnMenuItemClickListener {
                        context.downloadToPublic(
                            mediaUrl,
                            buildDownloadFileName(
                                mediaUrl,
                                if (isGif) "live_${item.id ?: position}" else "image_${item.id ?: position}",
                                if (isGif) "mp4" else "jpg"
                            )
                        )
                        context.toast(context.getString(R.string.save_started))
                        true
                    }
                    show()
                }
                true
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<XhsWorkMedia>() {
            override fun areItemsTheSame(oldItem: XhsWorkMedia, newItem: XhsWorkMedia): Boolean =
                if (oldItem.id != null || newItem.id != null) {
                    oldItem.id == newItem.id
                } else {
                    oldItem.mediaUrl == newItem.mediaUrl
                }

            override fun areContentsTheSame(oldItem: XhsWorkMedia, newItem: XhsWorkMedia): Boolean =
                oldItem == newItem
        }
    }
}
