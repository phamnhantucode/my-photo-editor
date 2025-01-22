package com.phamnhantucode.photoeditor.editor.core.text

import android.graphics.Typeface

data class TextEditorState(
    val text: String,
    val mode: TextEditorMode,
    val color: Int,
    val size: Float,
    val typeface: Typeface
)
