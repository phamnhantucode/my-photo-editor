package com.phamnhantucode.photoeditor.editor.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.core.helper.PhotoEditorFirebaseStorage
import com.phamnhantucode.photoeditor.core.model.firebase.Sticker
import com.phamnhantucode.photoeditor.databinding.ItemStickerBinding

class StickerAdapter(
    private val onStickerClickListener: (Uri) -> Unit,
) : ListAdapter<Sticker, StickerAdapter.StickerViewHolder>(StickerDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        return StickerViewHolder(
            ItemStickerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StickerViewHolder(
        private val binding: ItemStickerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(sticker: Sticker) {
            setupStickerImage(sticker)
            setupClickListeners(sticker)
        }

        private fun setupStickerImage(sticker: Sticker) {
            val firebaseStorageInstance = PhotoEditorFirebaseStorage.getInstance()
            sticker.path?.let { path ->
                Glide.with(itemView.context)
                    .load(
                        firebaseStorageInstance.getLocalUriIfExists(itemView.context, path)
                            ?: firebaseStorageInstance.getImageRef(path)
                    )
                    .placeholder(R.drawable.sample_img)
                    .into(binding.stickerIv)

                if (sticker.isDownloaded) {
                    binding.downloadBtn.setImageResource(0)
                }
            }
        }

        private fun setupClickListeners(sticker: Sticker) {
            binding.apply {
                stickerIv.setOnClickListener {
                    handleStickerClick(sticker)
                }

                downloadBtn.setOnClickListener {
                    handleDownloadClick(sticker)
                }
            }
        }

        private fun handleStickerClick(sticker: Sticker) {
            sticker.path?.let { path ->
                if (sticker.isDownloaded) {
                    PhotoEditorFirebaseStorage.getInstance()
                        .getFileUrl(itemView.context, path) { uri ->
                            onStickerClickListener(uri)
                        }
                } else {
                    updateDownloadingState(isDownloading = true)
                    PhotoEditorFirebaseStorage.getInstance()
                        .downloadFile(itemView.context, path) { uri ->
                            updateDownloadingState(isDownloading = false)
                            onStickerClickListener(
                                uri
                            )
                        }
                }
            }
        }

        private fun handleDownloadClick(sticker: Sticker) {
            sticker.path?.let { path ->
                updateDownloadingState(isDownloading = true)
                PhotoEditorFirebaseStorage.getInstance().downloadFile(itemView.context, path) {
                    updateDownloadingState(isDownloading = false)
                    binding.downloadBtn.setImageResource(0)
                }
            }
        }

        private fun updateDownloadingState(isDownloading: Boolean) {
            binding.apply {
                progressBar.isVisible = isDownloading
                stickerIv.animate().alpha(
                    if (isDownloading) STICKER_DOWNLOADING_ALPHA else 1f
                ).start()
            }
        }
    }

    companion object {
        private const val STICKER_DOWNLOADING_ALPHA = 0.75f
    }
}

private object StickerDiffCallback : DiffUtil.ItemCallback<Sticker>() {
    override fun areItemsTheSame(oldItem: Sticker, newItem: Sticker): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Sticker, newItem: Sticker): Boolean {
        return oldItem == newItem
    }
}
