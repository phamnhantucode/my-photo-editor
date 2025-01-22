package com.phamnhantucode.photoeditor.editor.fragment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.phamnhantucode.photoeditor.core.helper.PhotoEditorFirebaseStorage
import com.phamnhantucode.photoeditor.databinding.FragmentStickerBottomDialogBinding
import com.phamnhantucode.photoeditor.editor.adapter.StickerAdapter

class StickerBottomSheetDialogFragment(
    private val onStickerClickListener: (Bitmap) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentStickerBottomDialogBinding
    private val stickerAdapter = StickerAdapter {uri ->
            onStickerClickListener(BitmapFactory.decodeFile(uri.path))
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
            var stickers = PhotoEditorFirebaseStorage.getInstance().getStickers()
            emptyView.isVisible = stickers.isEmpty()
            stickerAdapter.submitList(stickers)
            rvSticker.layoutManager =
                GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
            rvSticker.adapter = stickerAdapter

            cleanTextBtn.setOnClickListener {
                etSearch.setText("")
            }

            etSearch.doOnTextChanged { text, _, _, _ ->
                val searchKey = text.toString()
                stickers = PhotoEditorFirebaseStorage.getInstance().getStickers(searchKey)
                emptyView.isVisible = stickers.isEmpty()
                stickerAdapter.submitList(stickers)
            }
        }
    }
}
