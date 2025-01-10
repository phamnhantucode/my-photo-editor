package com.phamnhantucode.photoeditor.extension

fun Boolean.then(block: () -> Unit): Boolean {
    if (this) {
        block()
    }
    return this
}

fun Boolean.otherwise(block: () -> Unit): Boolean {
    if (!this) {
        block()
    }
    return this
}