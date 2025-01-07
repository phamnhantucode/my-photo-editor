package com.phamnhantucode.photoeditor.camera

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phamnhantucode.photoeditor.databinding.ItemCameraFilterBinding

class FilterAdapter(
    var onFilterSelected: (FilterCamera) -> Unit
): ListAdapter<FilterCamera , FilterAdapter.FilterViewHolder>(FilterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = ItemCameraFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FilterViewHolder(
        private val binding: ItemCameraFilterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(filter: FilterCamera) {
            binding.ivFilter.setImageBitmap(filter.applyFilter())
            binding.ivFilter.setOnClickListener {
                onFilterSelected(filter)
            }
        }
    }

    class FilterDiffCallback: DiffUtil.ItemCallback<FilterCamera>() {
        override fun areItemsTheSame(oldItem: FilterCamera, newItem: FilterCamera): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: FilterCamera, newItem: FilterCamera): Boolean {
            return oldItem == newItem
        }

    }
}