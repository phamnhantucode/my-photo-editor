package com.phamnhantucode.photoeditor.album

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.album.model.MyImage
import com.phamnhantucode.photoeditor.databinding.ItemImageBinding

class ListImagesAdapter(
    private val onImageClick: (MyImage) -> Unit,
    private val onImageLongClick: (MyImage) -> Unit,
) : RecyclerView.Adapter<ListImagesAdapter.ImageViewHolder>() {

    var images: List<MyImage> = emptyList()
        set(value) {
            val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(
                ImagesDiffCallBack(field, value)
            )
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    var isSelectedMode = false
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class ImageViewHolder(private val binding: ItemImageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private lateinit var image: MyImage
        fun bind(image: MyImage) {
            this.image = image
            Glide.with(binding.root)
                .load(image.uri)
                .placeholder(R.drawable.ic_download)
                .into(binding.ivImage)
            changeViewVisibility()
            setViewAction(binding, image)
        }


        private fun changeViewVisibility() {
            binding.apply {
                ivMask.isVisible = isSelectedMode
                ivSelected.isVisible = isSelectedMode
                if (image.isSelected) {
                    ivSelected.setImageResource(R.drawable.ic_tick_filled)
                } else {
                    ivSelected.setImageDrawable(null)
                }
            }
        }
    }

    private fun setViewAction(binding: ItemImageBinding, image: MyImage) {
        binding.root.setOnClickListener {
            onImageClick(image)
        }
        binding.root.setOnLongClickListener {
            onImageLongClick(image)
            true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }
}

class ImagesDiffCallBack(
    private val oldList: List<MyImage>,
    private val newList: List<MyImage>,
) : androidx.recyclerview.widget.DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].uri == newList[newItemPosition].uri
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].hashCode() == newList[newItemPosition].hashCode()
    }
}