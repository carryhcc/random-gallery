package com.example.randomgallery.android.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.randomgallery.android.R
import com.example.randomgallery.android.data.model.PicVO
import com.example.randomgallery.android.databinding.ItemPicBinding
import com.example.randomgallery.android.util.ImageUrlResolver
import com.example.randomgallery.android.util.buildDownloadFileName
import com.example.randomgallery.android.util.downloadToPublic
import com.example.randomgallery.android.util.toast

class PicAdapter(
    private val onClick: (Int) -> Unit = {}
) : ListAdapter<PicVO, PicAdapter.PicViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PicViewHolder {
        val binding = ItemPicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PicViewHolder(private val binding: ItemPicBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PicVO) {
            val imageUrl = ImageUrlResolver.displayUrl(item.picUrl)
            binding.image.load(imageUrl) {
                crossfade(true)
                error(R.drawable.ic_404_placeholder)
                placeholder(R.drawable.ic_loading_placeholder)
            }
            binding.root.setOnClickListener { onClick(bindingAdapterPosition) }
            binding.root.setOnLongClickListener {
                val context = binding.root.context
                PopupMenu(context, binding.root).apply {
                    menu.add(context.getString(R.string.save_image))
                    setOnMenuItemClickListener {
                        context.downloadToPublic(
                            imageUrl,
                            buildDownloadFileName(imageUrl, "gallery_${item.id ?: bindingAdapterPosition}", "jpg")
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
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PicVO>() {
            override fun areItemsTheSame(oldItem: PicVO, newItem: PicVO): Boolean =
                if (oldItem.id != null || newItem.id != null) {
                    oldItem.id == newItem.id
                } else {
                    oldItem.picUrl == newItem.picUrl
                }

            override fun areContentsTheSame(oldItem: PicVO, newItem: PicVO): Boolean =
                oldItem == newItem
        }
    }
}
