package com.phamnhantucode.photoeditor.editor.core.text

data class TextEditorState(
    val text: String,
    val mode: TextEditorMode,
    val color: Int,
    val size: Float,
)