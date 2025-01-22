package com.phamnhantucode.photoeditor.camera.effect.processor

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors.newHandlerExecutor
import androidx.camera.core.processing.OpenGlRenderer
import androidx.camera.core.processing.ShaderProvider
import androidx.camera.core.processing.util.GLUtils.InputFormat
import androidx.core.util.Preconditions.checkState
import com.phamnhantucode.photoeditor.core.model.ui.FilterType

@SuppressLint("RestrictedApi")
class FilterProcessor(
    private val value: Float = 0.25f,
    filterType: FilterType = FilterType.BRIGHTNESS,
) : SurfaceProcessor, OnFrameAvailableListener {

    companion object {
        private var value = 0.75f
        private const val GL_THREAD_NAME = "FilterProcessor"
    }


    private val glThread = HandlerThread(GL_THREAD_NAME).apply { start() }
    private var glHandler = Handler(glThread.looper)
    var glExecutor = newHandlerExecutor(glHandler)


    private var glRenderer = OpenGlRenderer()
    private val outputSurface = mutableMapOf<SurfaceOutput, Surface>()
    private val textureTransform = FloatArray(16)
    private val surfaceTransform = FloatArray(16)
    private var isReleased = false

    private var surfaceRequest = false
    private var outputSurfaceProvider = false

    private var renderWidth = 1050
    private var renderHeight = 1050

    init {
        setValueFilter(value)
        setFilter(filterType)
    }

    override fun onInputSurface(request: SurfaceRequest) {
        checkGlThread()
        if (isReleased) {
            request.willNotProvideSurface()
            return
        }

        surfaceRequest = true
        val surfaceTexture = SurfaceTexture(glRenderer.textureName)
        renderWidth = request.resolution.width
        renderHeight = request.resolution.height
        surfaceTexture.setDefaultBufferSize(
            request.resolution.width,
            request.resolution.height
        )
        val surface = Surface(surfaceTexture)
        request.provideSurface(surface, glExecutor) {
            surfaceTexture.setOnFrameAvailableListener(null)
            surfaceTexture.release()
            surface.release()
        }
        surfaceTexture.setOnFrameAvailableListener(this, glHandler)
    }

    private fun checkGlThread() {
        checkState(GL_THREAD_NAME == Thread.currentThread().name)
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        checkGlThread()
        outputSurfaceProvider = true
        if (isReleased) {
            surfaceOutput.close()
            return
        }
        val surface = surfaceOutput.getSurface(glExecutor) {
            surfaceOutput.close()
            outputSurface.remove(surfaceOutput)?.let { removedSurface ->
                glRenderer.unregisterOutputSurface(removedSurface)
            }
        }
        glRenderer.registerOutputSurface(surface)
        outputSurface[surfaceOutput] = surface
    }

    fun release() {
        glExecutor.execute {
            releaseInternal()
        }
    }

    private fun releaseInternal() {
        checkGlThread()
        if (!isReleased) {
            for ((surfaceOutput, _) in outputSurface) {
                surfaceOutput.close()
            }
            outputSurface.clear()
            glRenderer.release()
            glThread.quitSafely()
            isReleased = true
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        checkGlThread()
        if (isReleased) {
            return
        }
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(textureTransform)
        for (entry in outputSurface.entries.iterator()) {
            val surface = entry.value
            val surfaceOutput = entry.key
            surfaceOutput.updateTransformMatrix(surfaceTransform, textureTransform)
            glRenderer.render(surfaceTexture.timestamp, surfaceTransform, surface)
        }
    }

    private fun setValueFilter(value: Float) {
        FilterProcessor.value = value
    }

    private fun setFilter(filterType: FilterType) {
        val shaderProvider = when (filterType) {
            FilterType.BRIGHTNESS -> object : ShaderProvider {
                override fun createFragmentShader(
                    sampler: String,
                    fragCoords: String,
                ): String {
                    return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES $sampler;
                    uniform float uAlphaScale;
                    varying vec2 $fragCoords;
                    void main() {
                        vec4 sampleColor = texture2D($sampler, $fragCoords);                        
                        vec3 adjustedColor = sampleColor.rgb + vec3($value);
                        gl_FragColor = vec4(adjustedColor, sampleColor.a * uAlphaScale);
                    }
                """.trimIndent()
                }
            }

            FilterType.CONTRAST -> object : ShaderProvider {
                override fun createFragmentShader(
                    sampler: String,
                    fragCoords: String,
                ): String {
                    return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES $sampler;
                    uniform float uAlphaScale;
                    varying vec2 $fragCoords;
                    void main() {
                        vec4 sampleColor = texture2D($sampler, $fragCoords);                        
                        vec3 adjustedColor = (sampleColor.rgb - vec3(0.5)) * $value + vec3(0.5);
                        gl_FragColor = vec4(adjustedColor, sampleColor.a * uAlphaScale);
                    }
                """.trimIndent()
                }
            }

            FilterType.EXPOSURE -> object : ShaderProvider {
                override fun createFragmentShader(
                    sampler: String,
                    fragCoords: String,
                ): String {
                    return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES $sampler;
                    uniform float uAlphaScale;
                    varying vec2 $fragCoords;
                    void main() {
                        vec4 sampleColor = texture2D($sampler, $fragCoords);                        
                        vec3 adjustedColor = sampleColor.rgb * pow(2.0, $value);
                        gl_FragColor = vec4(adjustedColor, sampleColor.a * uAlphaScale);
                    }
                """.trimIndent()
                }
            }

            FilterType.SATURATION -> object : ShaderProvider {
                override fun createFragmentShader(
                    sampler: String,
                    fragCoords: String,
                ): String {
                    return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES $sampler;
                    uniform float uAlphaScale;
                    varying vec2 $fragCoords;
                    const vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);
                    void main() {
                        vec4 sampleColor = texture2D($sampler, $fragCoords);
                        float luminance = dot(sampleColor.rgb, luminanceWeighting);
                        vec3 adjustedColor = mix(vec3(luminance), sampleColor.rgb, $value);
                        gl_FragColor = vec4(adjustedColor, sampleColor.a * uAlphaScale);
                    }
                """.trimIndent()
                }
            }

            FilterType.HUE -> object : ShaderProvider {

                override fun createFragmentShader(
                    sampler: String,
                    fragCoords: String,
                ): String {
                    return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES $sampler;
                    uniform float uAlphaScale;
                    varying vec2 $fragCoords;
                    const vec4 kRGBToYPrime = vec4 (0.299, 0.587, 0.114, 0.0);
                    const vec4 kRGBToI = vec4 (0.595716, -0.274453, -0.321263, 0.0);
                    const vec4 kRGBToQ = vec4 (0.211456, -0.522591, 0.31135, 0.0);
                    const vec4 kYIQToR = vec4 (1.0, 0.9563, 0.6210, 0.0);
                    const vec4 kYIQToG = vec4 (1.0, -0.2721, -0.6474, 0.0);   
                    const vec4 kYIQToB = vec4 (1.0, -1.1070, 1.7046, 0.0);
                    void main() {
                        vec4 color = texture2D($sampler, $fragCoords);
                        float YPrime = dot (color, kRGBToYPrime);
                        float I = dot (color, kRGBToI);
                        float Q = dot (color, kRGBToQ);
                        float hue = atan (Q, I);
                        float chroma = sqrt (I * I + Q * Q);
                        hue += ${value};
                        Q = chroma * sin (hue);
                        I = chroma * cos (hue);
                        vec4 yIQ = vec4 (YPrime, I, Q, 0.0);
                        color.r = dot (yIQ, kYIQToR);
                        color.g = dot (yIQ, kYIQToG);
                        color.b = dot (yIQ, kYIQToB);
                        gl_FragColor = vec4(color.rgb, color.a * uAlphaScale);
                    }
                """.trimIndent()
                }
            }

            FilterType.SHARPEN -> object : ShaderProvider {
                override fun createFragmentShader(
                    sampler: String,
                    fragCoords: String,
                ): String {
                    return "#extension GL_OES_EGL_image_external : require\n" +
                            "precision mediump float;\n" +
                            "\n" +
                            "varying vec2 " + fragCoords + ";\n" +
                            "uniform samplerExternalOES " + sampler + ";\n" +
                            "uniform float uAlphaScale;\n" +
                            "\n" +
                            "void main() {\n" +

                            "float imageWidthFactor = ${(1f / renderWidth)};\n" +
                            "float imageHeightFactor = ${(1f / renderHeight)};\n" +
                            "    vec2 textureCoordinate = " + fragCoords + ";\n" +
                            "    vec2 widthStep = vec2(imageWidthFactor, 0.0);\n" +
                            "    vec2 heightStep = vec2(0.0, imageHeightFactor);\n" +
                            "\n" +
                            "    vec2 leftTextureCoordinate = textureCoordinate - widthStep;\n" +
                            "    vec2 rightTextureCoordinate = textureCoordinate + widthStep;\n" +
                            "    vec2 topTextureCoordinate = textureCoordinate + heightStep;\n" +
                            "    vec2 bottomTextureCoordinate = textureCoordinate - heightStep;\n" +
                            "\n" +
                            "    float centerMultiplier = 1.0 + 4.0 * ${value};\n" +
                            "    float edgeMultiplier = ${value};\n" +
                            "\n" +
                            "    vec3 textureColor = texture2D(" + sampler + ", textureCoordinate).rgb;\n" +
                            "    vec3 leftTextureColor = texture2D(" + sampler + ", leftTextureCoordinate).rgb;\n" +
                            "    vec3 rightTextureColor = texture2D(" + sampler + ", rightTextureCoordinate).rgb;\n" +
                            "    vec3 topTextureColor = texture2D(" + sampler + ", topTextureCoordinate).rgb;\n" +
                            "    vec3 bottomTextureColor = texture2D(" + sampler + ", bottomTextureCoordinate).rgb;\n" +
                            "\n" +
                            "    gl_FragColor = vec4((textureColor * centerMultiplier - (leftTextureColor * edgeMultiplier + " +
                            "rightTextureColor * edgeMultiplier + topTextureColor * edgeMultiplier + " +
                            "bottomTextureColor * edgeMultiplier)), texture2D(" + sampler + ", bottomTextureCoordinate).w * uAlphaScale);\n" +
                            "}"
                }
            }

            FilterType.BOX_BLUR -> object : ShaderProvider {
                override fun createFragmentShader(
                    samplerVarName: String,
                    fragCoordsVarName: String,
                ): String {
                    return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES $samplerVarName;
                    uniform float uAlphaScale;
                    varying vec2 $fragCoordsVarName;
                    void main() {
                        float texelWidthOffset = ${value / renderWidth};
                        float texelHeightOffset = ${value / renderHeight};
                        
                        vec2 firstOffset = vec2(1.5 * texelWidthOffset, 1.5 * texelHeightOffset);
                        vec2 secondOffset = vec2(3.5 * texelWidthOffset, 3.5 * texelHeightOffset);
                        
                        vec2 centerTextureCoordinate = $fragCoordsVarName;
                        vec2 oneStepLeftTextureCoordinate = centerTextureCoordinate - firstOffset;
                        vec2 twoStepsLeftTextureCoordinate = centerTextureCoordinate - secondOffset;
                        vec2 oneStepRightTextureCoordinate = centerTextureCoordinate + firstOffset;
                        vec2 twoStepsRightTextureCoordinate = centerTextureCoordinate + secondOffset;
                        
                        vec4 textureColor = texture2D($samplerVarName, centerTextureCoordinate) * 0.2;
                        textureColor += texture2D($samplerVarName, oneStepLeftTextureCoordinate) * 0.2;
                        textureColor += texture2D($samplerVarName, oneStepRightTextureCoordinate) * 0.2;
                        textureColor += texture2D($samplerVarName, twoStepsLeftTextureCoordinate) * 0.2;
                        textureColor += texture2D($samplerVarName, twoStepsRightTextureCoordinate) * 0.2;
                        
                        gl_FragColor = vec4(textureColor.rgb, textureColor.a * uAlphaScale);
                    }
                    """.trimIndent()
                }
            }

            else -> object : ShaderProvider {
                override fun createFragmentShader(
                    sampler: String,
                    fragCoords: String,
                ): String {
                    return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES $sampler;
                    uniform float uAlphaScale;
                    varying vec2 $fragCoords;
                    void main() {
                        vec4 sampleColor = texture2D($sampler, $fragCoords);
                        gl_FragColor = vec4(sampleColor.rgb, sampleColor.a * uAlphaScale);
                    }
                """.trimIndent()

                }
            }
        }
        glExecutor.execute {
            glRenderer.release()
            glRenderer.init(
                DynamicRange.SDR,
                mapOf(InputFormat.DEFAULT to shaderProvider)
            )
        }
    }
}
