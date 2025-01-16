package com.phamnhantucode.photoeditor.camera.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult

class FaceDetectorHelper(
    var threshold: Float = THRESHOLD_DEFAULT,
    var currentDelegate: Int = DELEGATE_CPU,
    var runningMode: RunningMode = RunningMode.LIVE_STREAM,
    val context: Context,
    // The listener is only used when running in RunningMode.LIVE_STREAM
    var faceDetectorListener: DetectorListener? = null
) {

    // For this example this needs to be a var so it can be reset on changes. If the faceDetector
    // will not change, a lazy val would be preferable.
    private var faceDetector: FaceDetector? = null

    init {
//        setupFaceDetector()
    }

    fun clearFaceDetector() {
        faceDetector?.close()
        faceDetector = null
    }

    // Initialize the face detector using current settings on the
    // thread that is using it. CPU can be used with detectors
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the detector
    fun setupFaceDetector() {
        // Set general detection options, including number of used threads
        val baseOptionsBuilder = BaseOptions.builder()

        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                baseOptionsBuilder.setDelegate(Delegate.CPU)
            }
            DELEGATE_GPU -> {
                // Is there a check for GPU being supported?
                baseOptionsBuilder.setDelegate(Delegate.GPU)
            }
        }

        val modelName = "blaze_face_short_range.tflite"

        baseOptionsBuilder.setModelAssetPath(modelName)

        // Check if runningMode is consistent with faceDetectorListener
        when (runningMode) {
            RunningMode.LIVE_STREAM -> {
                if (faceDetectorListener == null) {
                    throw IllegalStateException(
                        "faceDetectorListener must be set when runningMode is LIVE_STREAM."
                    )
                }
            }
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                // no-op
            }
        }

        try {
            val optionsBuilder =
                FaceDetector.FaceDetectorOptions.builder()
                    .setBaseOptions(baseOptionsBuilder.build())
                    .setMinDetectionConfidence(threshold)
                    .setRunningMode(runningMode)

            when (runningMode) {
                RunningMode.IMAGE,
                RunningMode.VIDEO -> optionsBuilder.setRunningMode(runningMode)
                RunningMode.LIVE_STREAM ->
                    optionsBuilder.setRunningMode(runningMode)
                        .setResultListener(this::returnLivestreamResult)
                        .setErrorListener(this::returnLivestreamError)
            }

            val options = optionsBuilder.build()
            faceDetector = FaceDetector.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            faceDetectorListener?.onError(
                "Face detector failed to initialize. See error logs for details"
            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        } catch (e: RuntimeException) {
            faceDetectorListener?.onError(
                "Face detector failed to initialize. See error logs for " +
                        "details", GPU_ERROR
            )
            Log.e(
                TAG,
                "Face detector failed to load model with error: " + e.message
            )
        }
    }

    // Return running status of recognizer helper
    fun isClosed(): Boolean {
        return faceDetector == null
    }

    // Accepts the URI for a video file loaded from the user's gallery and attempts to run
    // face detection inference on the video. This process will evaluate every frame in
    // the video and attach the results to a bundle that will be returned.
    fun detectVideoFile(
        videoUri: Uri,
        inferenceIntervalMs: Long
    ): ResultBundle? {

        if (runningMode != RunningMode.VIDEO) {
            throw IllegalArgumentException(
                "Attempting to call detectVideoFile" +
                        " while not using RunningMode.VIDEO"
            )
        }

        if (faceDetector == null) return null

        // Inference time is the difference between the system time at the start and finish of the
        // process
        val startTime = SystemClock.uptimeMillis()

        var didErrorOccurred = false

        // Load frames from the video and run the face detection model.
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val videoLengthMs =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLong()

        // Note: We need to read width/height from frame instead of getting the width/height
        // of the video directly because MediaRetriever returns frames that are smaller than the
        // actual dimension of the video file.
        val firstFrame = retriever.getFrameAtTime(0)
        val width = firstFrame?.width
        val height = firstFrame?.height

        // If the video is invalid, returns a null detection result
        if ((videoLengthMs == null) || (width == null) || (height == null)) return null

        // Next, we'll get one frame every frameInterval ms, then run detection on these frames.
        val resultList = mutableListOf<FaceDetectorResult>()
        val numberOfFrameToRead = videoLengthMs.div(inferenceIntervalMs)

        for (i in 0..numberOfFrameToRead) {
            val timestampMs = i * inferenceIntervalMs // ms

            retriever
                .getFrameAtTime(
                    timestampMs * 1000, // convert from ms to micro-s
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                ?.let { frame ->
                    // Convert the video frame to ARGB_8888 which is required by the MediaPipe
                    val argb8888Frame =
                        if (frame.config == Bitmap.Config.ARGB_8888) frame
                        else frame.copy(Bitmap.Config.ARGB_8888, false)

                    // Convert the input Bitmap face to an MPImage face to run inference
                    val mpImage = BitmapImageBuilder(argb8888Frame).build()

                    // Run face detection using MediaPipe Face Detector API
                    faceDetector?.detectForVideo(mpImage, timestampMs)
                        ?.let { detectionResult ->
                            resultList.add(detectionResult)
                        }
                        ?: {
                            didErrorOccurred = true
                            faceDetectorListener?.onError(
                                "ResultBundle could not be returned" +
                                        " in detectVideoFile"
                            )
                        }
                }
                ?: run {
                    didErrorOccurred = true
                    faceDetectorListener?.onError(
                        "Frame at specified time could not be" +
                                " retrieved when detecting in video."
                    )
                }
        }

        retriever.release()

        val inferenceTimePerFrameMs =
            (SystemClock.uptimeMillis() - startTime).div(numberOfFrameToRead)

        return if (didErrorOccurred) {
            null
        } else {
            ResultBundle(resultList, inferenceTimePerFrameMs, height, width)
        }
    }

    // Runs face detection on live streaming cameras frame-by-frame and returns the results
    // asynchronously to the caller.
    fun detectLivestreamFrame(imageProxy: ImageProxy) {
//        val frameTime = SystemClock.uptimeMillis()
//        val bitmap = imageProxy.toBitmap()
//        val matrix =
//            Matrix().apply {
//                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
//            }
//
//        val rotatedBitmap =
//            Bitmap.createBitmap(
//                bitmap,
//                0,
//                0,
//                bitmap.width,
//                bitmap.height,
//                matrix,
//                true
//            )
//
//        // Convert the input Bitmap face to an MPImage face to run inference
//        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
//
//        detectAsync(mpImage, frameTime)
    }

    // Run face detection using MediaPipe Face Detector API
    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        // As we're using running mode LIVE_STREAM, the detection result will be returned in
        // returnLivestreamResult function
        faceDetector?.detectAsync(mpImage, frameTime)
    }

    // Return the detection result to this FaceDetectorHelper's caller
    private fun returnLivestreamResult(
        result: FaceDetectorResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        faceDetectorListener?.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width
            )
        )
    }

    // Return errors thrown during detection to this FaceDetectorHelper's caller
    private fun returnLivestreamError(error: RuntimeException) {
        faceDetectorListener?.onError(
            error.message ?: "An unknown error has occurred"
        )
    }

    // Accepted a Bitmap and runs face detection inference on it to return results back
    // to the caller
    fun detectImage(image: Bitmap): ResultBundle? {

        if (runningMode != RunningMode.IMAGE) {
            throw IllegalArgumentException(
                "Attempting to call detectImage" +
                        " while not using RunningMode.IMAGE"
            )
        }

        if (faceDetector == null) return null

        val startTime = SystemClock.uptimeMillis()

        val mpImage = BitmapImageBuilder(image).build()

        faceDetector?.detect(mpImage)?.also { detectionResult ->
            val inferenceTimeMs = SystemClock.uptimeMillis() - startTime
            return ResultBundle(
                listOf(detectionResult),
                inferenceTimeMs,
                image.height,
                image.width
            )
        }

        return null
    }

    data class ResultBundle(
        val results: List<FaceDetectorResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val THRESHOLD_DEFAULT = 0.5F
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1

        const val TAG = "FaceDetectorHelper"
    }

    // Used to pass results or errors back to the calling class
    interface DetectorListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
    }
}