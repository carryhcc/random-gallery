package com.example.randomgallery.android.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.randomgallery.android.R
import com.example.randomgallery.android.data.model.XhsWorkListVO
import com.example.randomgallery.android.databinding.ItemWorkBinding
import com.example.randomgallery.android.util.ImageUrlResolver

class WorkAdapter(
    private val onClick: (XhsWorkListVO) -> Unit
) : ListAdapter<XhsWorkListVO, WorkAdapter.WorkViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkViewHolder {
        val binding = ItemWorkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WorkViewHolder(private val binding: ItemWorkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: XhsWorkListVO) {
            val context = binding.root.context
            binding.title.text = item.workTitle ?: context.getString(R.string.common_untitled)
            binding.author.text = item.authorNickname ?: context.getString(R.string.common_unknown_author)
            binding.count.text = context.getString(
                R.string.work_count_format,
                item.imageCount ?: 0,
                item.gifCount ?: 0
            )
            binding.cover.load(ImageUrlResolver.displayUrl(item.coverImageUrl)) {
                crossfade(true)
                error(R.drawable.ic_404_placeholder)
                placeholder(R.drawable.ic_loading_placeholder)
            }
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<XhsWorkListVO>() {
            override fun areItemsTheSame(oldItem: XhsWorkListVO, newItem: XhsWorkListVO): Boolean =
                oldItem.workId == newItem.workId && oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: XhsWorkListVO, newItem: XhsWorkListVO): Boolean =
                oldItem == newItem
        }
    }
}
