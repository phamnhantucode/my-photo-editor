package com.phamnhantucode.photoeditor.editor.adapter

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.core.model.ui.ImageFilter
import com.phamnhantucode.photoeditor.databinding.ItemImageFilterBinding
import jp.co.cyberagent.android.gpuimage.GPUImage

class FilterAdapter(
    private val demoBitmap : Bitmap,
    private val onFilterClickListener: (imgFilter: ImageFilter) -> Unit,
) : ListAdapter<ImageFilter, FilterAdapter.FilterViewHolder>(FilterDiffCallback) {

    var selectedFilter: ImageFilter? = null

    override fun submitList(list: List<ImageFilter>?) {
        super.submitList(list)
        selectedFilter = list?.firstOrNull()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        return FilterViewHolder(
            ItemImageFilterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FilterViewHolder(
        private val binding: ItemImageFilterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val gpuImage = GPUImage(binding.root.context)
        @SuppressLint("NotifyDataSetChanged")
        fun bind(filter: ImageFilter) {
            if (selectedFilter == filter) {
                binding.root.background = AppCompatResources.getDrawable(binding.root.context, R.drawable.bg_box_round)
            } else {
                binding.root.setBackgroundColor(0)
            }
            binding.tvFilterName.text = filter.name
            Glide.with(binding.root.context)
                .load(filter.demoBitmap)
                .into(binding.ivFilterDemo)
            binding.root.setOnClickListener {
                selectedFilter = filter
                notifyDataSetChanged()
                onFilterClickListener(filter)
            }
        }
    }

    companion object {
        private const val TAG = "FilterAdapter"
        private const val STICKER_DOWNLOADING_ALPHA = 0.75f
    }
}

private object FilterDiffCallback : DiffUtil.ItemCallback<ImageFilter>() {
    override fun areItemsTheSame(oldItem: ImageFilter, newItem: ImageFilter): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: ImageFilter, newItem: ImageFilter): Boolean {
        return oldItem == newItem
    }
}