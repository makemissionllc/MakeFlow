package com.makemission.makeflow.core.ml

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TfliteCategoryClassifier private constructor(
    private val interpreter: Interpreter,
    private val labels: List<String>,
    private val vocab: Map<String, Int>,
    private val maxFeatures: Int
) : CategoryClassifier {

    override fun classify(description: String): CategoryPrediction? {
        val inputVec = vectorize(description, vocab, maxFeatures)
        if (inputVec.none { it > 0f }) return null
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(inputVec, output)
        val probs = output[0]
        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: return null
        val confidence = probs[maxIdx]
        return CategoryPrediction(
            categoryName = labels[maxIdx],
            confidence = confidence
        )
    }

    fun close() {
        interpreter.close()
    }

    companion object {
        private const val TAG = "TfliteCategoryClassifier"
        private const val MODEL_FILENAME = "ml/category_classifier.tflite"
        private const val VOCAB_FILENAME = "ml/vocab.json"

        suspend fun create(context: Context): TfliteCategoryClassifier? =
            withContext(Dispatchers.IO) {
                try {
                    val modelBytes = loadModelFile(context)
                    val interpreter = Interpreter(modelBytes)

                    val (vocab, labels, maxFeatures) = loadVocab(context)

                    Log.i(TAG, "Model loaded successfully (${labels.size} labels, $maxFeatures features)")
                    TfliteCategoryClassifier(interpreter, labels, vocab, maxFeatures)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load model", e)
                    null
                }
            }

        private fun loadModelFile(context: Context): ByteBuffer {
            val afd = context.assets.openFd(MODEL_FILENAME)
            val inputStream = FileInputStream(afd.fileDescriptor)
            val channel = inputStream.channel
            val startOffset = afd.startOffset
            val declaredLength = afd.declaredLength
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
            ).also {
                it.order(ByteOrder.nativeOrder())
            }
        }

        private data class VocabData(
            val vocabulary: Map<String, Int>,
            val labels: List<String>,
            @Suppress("unused") val ngram_range: List<Int>,
            @Suppress("unused") val analyzer: String
        )

        private fun loadVocab(context: Context): Triple<Map<String, Int>, List<String>, Int> {
            val json = context.assets.open(VOCAB_FILENAME).bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val raw: Map<String, Any> = Gson().fromJson(json, type)

            val vocabRaw = raw["vocabulary"] as Map<String, Double>
            val vocab = vocabRaw.mapValues { it.value.toInt() }
            val labels = (raw["labels"] as List<*>).map { it as String }
            val maxFeatures = vocab.values.maxOrNull()?.plus(1) ?: 2000
            return Triple(vocab, labels, maxFeatures)
        }
    }
}
