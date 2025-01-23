package com.phamnhantucode.photoeditor.camera.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.core.helper.PhotoEditorFirebaseStorage
import com.phamnhantucode.photoeditor.core.model.firebase.CameraSticker
import com.phamnhantucode.photoeditor.databinding.ItemStickerBinding

class CameraStickerAdapter(
    private val onStickerClickListener: (CameraSticker) -> Unit,
) : ListAdapter<CameraSticker, CameraStickerAdapter.StickerViewHolder>(CameraStickerDiffCallback()) {

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

        fun bind(sticker: CameraSticker) {
            setupStickerImage(sticker)
            setupClickListeners(sticker)
        }

        private fun setupStickerImage(sticker: CameraSticker) {
            val firebaseStorageInstance = PhotoEditorFirebaseStorage.getInstance()
            sticker.partials.first().path?.let { path ->
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

        private fun setupClickListeners(sticker: CameraSticker) {
            binding.apply {
                stickerIv.setOnClickListener {
                    handleStickerClick(sticker)
                }

                downloadBtn.setOnClickListener {
                    handleDownloadClick(sticker)
                }
            }
        }

        private fun handleStickerClick(sticker: CameraSticker) {
            var count = 0
            sticker.partials.forEach { partial ->
                if (sticker.isDownloaded) {
                    partial.path?.let {
                        PhotoEditorFirebaseStorage.getInstance()
                            .getFileUrl(itemView.context, it) { uri ->
                                partial.uri = uri
                                onStickerClickListener(sticker)
                            }
                    }
                } else {
                    updateDownloadingState(isDownloading = true)
                    PhotoEditorFirebaseStorage.getInstance()
                        .downloadFile(itemView.context, partial.path!!) {
                            count++
                            partial.uri = it
                            if (count == sticker.partials.size) {
                                updateDownloadingState(isDownloading = false)
                                onStickerClickListener(sticker)
                            }
                        }
                }
            }
        }

        private fun handleDownloadClick(sticker: CameraSticker) {
            var count = 0
            updateDownloadingState(isDownloading = true)
            sticker.partials.forEach { partial ->
                PhotoEditorFirebaseStorage.getInstance()
                    .downloadFile(itemView.context, partial.path!!) {
                        count++
                        partial.uri = it
                        if (count == sticker.partials.size) {
                            updateDownloadingState(isDownloading = false)
                            binding.downloadBtn.setImageResource(0)
                        }
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

    class CameraStickerDiffCallback : DiffUtil.ItemCallback<CameraSticker>() {
        override fun areItemsTheSame(oldItem: CameraSticker, newItem: CameraSticker): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CameraSticker, newItem: CameraSticker): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val STICKER_DOWNLOADING_ALPHA = 0.75f
    }
}
