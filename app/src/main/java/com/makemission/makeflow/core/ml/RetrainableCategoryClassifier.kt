package com.makemission.makeflow.core.ml

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class RetrainableCategoryClassifier private constructor(
    private val interpreter: Interpreter,
    private val labels: List<String>,
    private val labelSet: List<String>,
    private val vocab: Map<String, Int>,
    private val maxFeatures: Int,
    private val checkpointFile: File,
    private val hasTrainSignature: Boolean
) : CategoryClassifier {

    override fun classify(description: String): CategoryPrediction? {
        val inputVec = vectorize(description, vocab, maxFeatures)
        if (inputVec.none { it > 0f }) return null
        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(inputVec, output)
        val probs = output[0]
        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: return null
        return CategoryPrediction(labels[maxIdx], probs[maxIdx])
    }

    fun train(descriptions: List<String>, labelIndices: IntArray): Boolean {
        if (!hasTrainSignature || descriptions.isEmpty() || descriptions.size != labelIndices.size) return false
        return try {
            val inputTensor = Array(descriptions.size) { i ->
                vectorize(descriptions[i], vocab, maxFeatures)
            }
            val labelTensor = Array(labelIndices.size) { i ->
                val oneHot = FloatArray(labelSet.size)
                if (labelIndices[i] in labelSet.indices) oneHot[labelIndices[i]] = 1.0f
                oneHot
            }
            val outputs = HashMap<String, Any>()
            outputs["loss"] = FloatArray(1)
            interpreter.runSignature(
                mapOf("input" to inputTensor, "labels" to labelTensor),
                outputs,
                "train"
            )
            Log.i(TAG, "Training step completed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Training signature failed", e)
            false
        }
    }

    fun saveCheckpoint(): Boolean {
        if (!hasTrainSignature) return false
        return try {
            checkpointFile.parentFile?.mkdirs()
            interpreter.runSignature(emptyMap(), HashMap(), "save")
            checkpointFile.writeText(System.currentTimeMillis().toString())
            true
        } catch (e: Exception) {
            Log.w(TAG, "Save signature failed", e)
            false
        }
    }

    fun restoreCheckpoint(): Boolean {
        if (!hasTrainSignature || !checkpointFile.exists()) return false
        return try {
            interpreter.runSignature(emptyMap(), HashMap(), "restore")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Restore signature failed", e)
            false
        }
    }

    fun resetCheckpoint(): Boolean {
        return try {
            if (checkpointFile.exists()) checkpointFile.delete()
            true
        } catch (e: Exception) { false }
    }

    fun close() {
        interpreter.close()
    }

    companion object {
        private const val TAG = "RetrainableClassifier"
        private const val MODEL_FILENAME = "ml/category_classifier.tflite"
        private const val TRAINABLE_MODEL_FILENAME = "ml/category_classifier_trainable.tflite"
        private const val VOCAB_FILENAME = "ml/vocab.json"
        private const val LABEL_SET_FILENAME = "ml/label_set.json"

        suspend fun create(context: Context): RetrainableCategoryClassifier? =
            withContext(Dispatchers.IO) {
                try {
                    val (vocab, labels, maxFeatures) = loadVocab(context)
                    val loadedLabelSet = loadLabelSet(context)

                    var trainableModelBytes: ByteBuffer? = null
                    var trainable = false
                    try {
                        trainableModelBytes = loadModelFile(context, TRAINABLE_MODEL_FILENAME)
                        trainable = true
                    } catch (_: Exception) {
                        Log.w(TAG, "Trainable model not found, using inference model")
                        trainableModelBytes = null
                    }
                    val modelBytes = trainableModelBytes ?: loadModelFile(context, MODEL_FILENAME)

                    val interpreter = Interpreter(modelBytes)
                    val checkpointDir = File(context.filesDir, "ml/checkpoint").also { it.mkdirs() }
                    val cpFile = File(checkpointDir, "weights")

                    Log.i(TAG, "Loaded (trainable=$trainable, ${labels.size} labels)")
                    RetrainableCategoryClassifier(
                        interpreter, labels, loadedLabelSet, vocab, maxFeatures, cpFile, trainable
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create classifier", e)
                    null
                }
            }

        fun loadLabelSet(context: Context): List<String> {
            val json = context.assets.open(LABEL_SET_FILENAME).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<String>>() {}.type
            return Gson().fromJson(json, type)
        }

        private fun loadModelFile(context: Context, filename: String): ByteBuffer {
            val afd = context.assets.openFd(filename)
            val inputStream = FileInputStream(afd.fileDescriptor)
            val channel = inputStream.channel
            val startOffset = afd.startOffset
            val declaredLength = afd.declaredLength
            return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength).also {
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
