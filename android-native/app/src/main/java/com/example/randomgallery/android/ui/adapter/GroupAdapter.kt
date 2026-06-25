package com.example.randomgallery.android.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.randomgallery.android.R
import com.example.randomgallery.android.data.model.GroupVO
import com.example.randomgallery.android.databinding.ItemGroupBinding
import com.example.randomgallery.android.util.ImageUrlResolver

class GroupAdapter(
    private val onClick: (GroupVO) -> Unit
) : ListAdapter<GroupVO, GroupAdapter.GroupViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GroupViewHolder(private val binding: ItemGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupVO) {
            val context = binding.root.context
            binding.groupName.text = item.groupName ?: context.getString(R.string.group_name_unnamed)
            binding.groupMeta.text = context.getString(
                R.string.group_meta_format,
                item.groupId ?: "-",
                item.groupCount ?: 0
            )
            binding.cover.load(ImageUrlResolver.displayUrl(item.groupUrl)) {
                crossfade(true)
                error(R.drawable.ic_404_placeholder)
                placeholder(R.drawable.ic_loading_placeholder)
            }
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GroupVO>() {
            override fun areItemsTheSame(oldItem: GroupVO, newItem: GroupVO): Boolean =
                oldItem.groupId == newItem.groupId

            override fun areContentsTheSame(oldItem: GroupVO, newItem: GroupVO): Boolean =
                oldItem == newItem
        }
    }
}
