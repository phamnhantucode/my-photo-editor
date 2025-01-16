package com.phamnhantucode.photoeditor.camera.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.phamnhantucode.photoeditor.core.PhotoEditorFirebaseStorage
import com.phamnhantucode.photoeditor.core.model.firebase.CameraSticker
import com.phamnhantucode.photoeditor.databinding.FragmentStickerBottomDialogBinding

class CameraStickerBottomSheetDialogFragment(
    private val onStickerClickListener: (CameraSticker) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentStickerBottomDialogBinding
    private val stickerAdapter = CameraStickerAdapter { sticker ->
        onStickerClickListener(sticker)
        dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentStickerBottomDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            var stickers = PhotoEditorFirebaseStorage.getInstance().getCameraStickers()
            emptyView.isVisible = stickers.isEmpty()
            stickerAdapter.submitList(stickers)
            rvSticker.layoutManager =
                GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
            rvSticker.adapter = stickerAdapter

            cleanTextBtn.setOnClickListener {
                etSearch.setText("")
            }

            etSearch.doOnTextChanged { text, start, before, count ->
                val searchKey = text.toString()
                stickers = PhotoEditorFirebaseStorage.getInstance().getCameraStickers(searchKey)
                emptyView.isVisible = stickers.isEmpty()
                stickerAdapter.submitList(stickers)
            }
        }
    }

}