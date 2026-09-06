package com.example.video_basedunique_personcollage.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.video_basedunique_personcollage.domain.ml.FaceEmbedder
import com.example.video_basedunique_personcollage.utils.BitmapUtils
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Generates 512-dimensional face embeddings using FaceNet-512 (Inception-ResNet-v1 architecture)
 * trained on VGGFace2.
 * Expects input: [1, 160, 160, 3] RGB normalized via standard prewhitening (zero mean, unit variance).
 * Outputs: [1, 512] L2-normalized embedding vector.
 */
class FaceNetEmbedder(
    context: Context,
    modelPath: String = "facenet_512.tflite"
) : FaceEmbedder {

    private var interpreter: Interpreter? = null
    private val embeddingSize: Int

    companion object {
        private const val TAG = "FaceNetEmbedder"
        private const val INPUT_SIZE = 160
    }

    init {
        val mappedByteBuffer = FileUtil.loadMappedFile(context, modelPath)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        val tflite = Interpreter(mappedByteBuffer, options)
        interpreter = tflite

        val inShape = tflite.getInputTensor(0).shape()
        val outShape = tflite.getOutputTensor(0).shape()
        embeddingSize = if (outShape.isNotEmpty() && outShape.last() > 0) outShape.last() else 512
        Log.d(TAG, "FaceNet-512 initialized. InShape: ${inShape.contentToString()}, OutShape: ${outShape.contentToString()}")
    }

    override fun generateEmbedding(faceBitmap: Bitmap): FloatArray {
        val tflite = interpreter ?: throw IllegalStateException("Interpreter has been closed or not initialized")

        // 1. Resize to 160x160
        val resizedBitmap = BitmapUtils.resizeForEmbedding(faceBitmap, INPUT_SIZE)

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        resizedBitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        // 2. Compute mean and standard deviation for FaceNet prewhitening (standardize)
        val totalElements = INPUT_SIZE * INPUT_SIZE * 3
        var sum = 0.0
        var sumSq = 0.0

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF).toDouble()
            val g = (pixel shr 8 and 0xFF).toDouble()
            val b = (pixel and 0xFF).toDouble()

            sum += r + g + b
            sumSq += r * r + g * g + b * b
        }

        val mean = sum / totalElements
        val variance = (sumSq / totalElements) - (mean * mean)
        val std = sqrt(max(0.0, variance))
        val stdAdj = max(std, 1.0 / sqrt(totalElements.toDouble())).toFloat()
        val meanFloat = mean.toFloat()

        // 3. Prepare direct ByteBuffer [1, 160, 160, 3] * 4 bytes
        val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        for (pixel in intValues) {
            val r = (pixel shr 16 and 0xFF).toFloat()
            val g = (pixel shr 8 and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()

            inputBuffer.putFloat((r - meanFloat) / stdAdj)
            inputBuffer.putFloat((g - meanFloat) / stdAdj)
            inputBuffer.putFloat((b - meanFloat) / stdAdj)
        }

        // 4. Allocate output container matching [1, embeddingSize]
        val output = Array(1) { FloatArray(embeddingSize) }

        // Rewind buffer before passing to TFLite
        inputBuffer.rewind()

        // 5. Run inference
        tflite.run(inputBuffer, output)

        val rawEmbedding = output[0]

        // 6. L2 Normalize the embedding vector
        return l2Normalize(rawEmbedding)
    }

    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sumSquares = 0.0f
        for (value in embedding) {
            sumSquares += value * value
        }
        val norm = sqrt(sumSquares.toDouble()).toFloat()
        if (norm == 0f) return embedding

        val normalized = FloatArray(embedding.size)
        for (i in embedding.indices) {
            normalized[i] = embedding[i] / norm
        }
        return normalized
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
    }
}
