package com.phamnhantucode.photoeditor.extension

import android.os.SystemClock
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

fun <T> Flow<T>.throttle(waitMillis: Int) = flow {
    coroutineScope {
        val context = coroutineContext
        var nextMillis = 0L
        var delayPost: Deferred<Unit>? = null
        collect {
            val current = SystemClock.uptimeMillis()
            if (nextMillis < current) {
                nextMillis = current + waitMillis
                emit(it)
                delayPost?.cancel()
            } else {
                val delayNext = nextMillis
                delayPost?.cancel()
                delayPost = async(Dispatchers.Default) {
                    delay(nextMillis - current)
                    if (delayNext == nextMillis) {
                        nextMillis = SystemClock.uptimeMillis() + waitMillis
                        withContext(context) {
                            emit(it)
                        }
                    }
                }
            }
        }
    }
}

fun <T> Flow<T>.debounce(waitMillis: Long) = flow {
    coroutineScope {
        val context = coroutineContext
        var delayPost: Deferred<Unit>? = null
        collect {
            delayPost?.cancel()
            delayPost = async(Dispatchers.Default) {
                delay(waitMillis)
                withContext(context) {
                    emit(it)
                }
            }
        }
    }
}
