package com.example.video_basedunique_personcollage.data.ml

import android.content.Context
import android.graphics.Bitmap
import com.example.video_basedunique_personcollage.domain.ml.FaceEmbedder
import com.example.video_basedunique_personcollage.utils.BitmapUtils
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class MobileFaceNetEmbedder(
    context: Context,
    modelPath: String = "mobilefacenet.tflite"
) : FaceEmbedder {

    private var interpreter: Interpreter? = null
    private val batchSize: Int
    private val embeddingSize: Int

    companion object {
        private const val INPUT_SIZE = 112
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 128.0f
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
        embeddingSize = if (outShape.isNotEmpty() && outShape.last() > 0) outShape.last() else 192
    }

    override fun generateEmbedding(faceBitmap: Bitmap): FloatArray {
        val tflite = interpreter ?: throw IllegalStateException("Interpreter has been closed or not initialized")

        // 1. Resize to 112x112
        val resizedBitmap = BitmapUtils.resizeForEmbedding(faceBitmap, INPUT_SIZE)

        // 2. Prepare input ByteBuffer: [batchSize, 112, 112, 3] * 4 bytes
        val inputBuffer = ByteBuffer.allocateDirect(batchSize * INPUT_SIZE * INPUT_SIZE * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        resizedBitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        // Fill buffer for each batch slot (e.g. batch size 2)
        for (b in 0 until batchSize) {
            for (pixel in intValues) {
                val r = (pixel shr 16 and 0xFF)
                val g = (pixel shr 8 and 0xFF)
                val blue = (pixel and 0xFF)

                inputBuffer.putFloat((r - IMAGE_MEAN) / IMAGE_STD)
                inputBuffer.putFloat((g - IMAGE_MEAN) / IMAGE_STD)
                inputBuffer.putFloat((blue - IMAGE_MEAN) / IMAGE_STD)
            }
        }

        // 3. Allocate output container matching [batchSize, embeddingSize]
        val output = Array(batchSize) { FloatArray(embeddingSize) }

        // CRITICAL: Rewind the buffer before passing to TFLite
        inputBuffer.rewind()

        // 4. Run inference
        tflite.run(inputBuffer, output)

        val rawEmbedding = output[0]

        // 5. L2 Normalize the embedding vector
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
