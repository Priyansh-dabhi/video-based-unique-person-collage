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
import kotlin.math.sqrt

/**
 * Generates 512-dimensional face embeddings using ArcFace MobileNetV2 trained on MS1M.
 * Input: [1, 112, 112, 3] RGB normalized to [-1.0, 1.0] via (pixel - 127.5) / 127.5.
 * Output: [1, 512] L2-normalized unit vector.
 */
class ArcFaceEmbedder(
    context: Context,
    modelPath: String = "arcface_512.tflite"
) : FaceEmbedder {

    private var interpreter: Interpreter? = null
    private val batchSize: Int
    private val embeddingSize: Int

    companion object {
        private const val TAG = "ArcFaceEmbedder"
        private const val INPUT_SIZE = 112
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f
    }

    init {
        val mappedByteBuffer = FileUtil.loadMappedFile(context, modelPath)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        val tflite = Interpreter(mappedByteBuffer, options)
        interpreter = tflite

        val inShape = tflite.getInputTensor(0).shape()
        batchSize = if (inShape.isNotEmpty() && inShape[0] > 0) inShape[0] else 1

        val outShape = tflite.getOutputTensor(0).shape()
        embeddingSize = if (outShape.isNotEmpty() && outShape.last() > 0) outShape.last() else 512
        Log.d(TAG, "ArcFace initialized. InShape: ${inShape.contentToString()}, OutShape: ${outShape.contentToString()}, batchSize=$batchSize, embeddingSize=$embeddingSize")
    }

    override fun generateEmbedding(faceBitmap: Bitmap): FloatArray {
        val tflite = interpreter ?: throw IllegalStateException("Interpreter has been closed or not initialized")

        // 1. Resize/ensure 112x112
        val resizedBitmap = BitmapUtils.resizeForEmbedding(faceBitmap, INPUT_SIZE)

        // 2. Prepare input ByteBuffer: [batchSize, 112, 112, 3] * 4 bytes
        val inputBuffer = ByteBuffer.allocateDirect(batchSize * INPUT_SIZE * INPUT_SIZE * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        resizedBitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        // 3. Normalize pixels: (pixel - 127.5) / 127.5 -> [-1.0, 1.0]
        for (b in 0 until batchSize) {
            for (pixel in intValues) {
                val r = (pixel shr 16 and 0xFF).toFloat()
                val g = (pixel shr 8 and 0xFF).toFloat()
                val blue = (pixel and 0xFF).toFloat()

                inputBuffer.putFloat((r - IMAGE_MEAN) / IMAGE_STD)
                inputBuffer.putFloat((g - IMAGE_MEAN) / IMAGE_STD)
                inputBuffer.putFloat((blue - IMAGE_MEAN) / IMAGE_STD)
            }
        }

        // 4. Allocate output container matching [batchSize, embeddingSize]
        val output = Array(batchSize) { FloatArray(embeddingSize) }

        // Rewind buffer before passing to TFLite
        inputBuffer.rewind()

        // 5. Run inference
        tflite.run(inputBuffer, output)

        val rawEmbedding = output[0]

        // 6. L2 Normalize the 512-D embedding vector
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
