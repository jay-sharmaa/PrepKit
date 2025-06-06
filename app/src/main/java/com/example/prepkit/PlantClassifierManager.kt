package com.example.prepkit

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.graphics.scale
import org.pytorch.LiteModuleLoader

class PlantClassifierManager(private val context: Context) {
    private var module: Module? = null

    private val classes = listOf<String>(
        "aloevera", "banana", "bilimbi", "cantaloupe", "cassava", "coconut", "corn", "cucumber", "eggplant",
        "galangal", "ginger", "guava", "kale", "longbeans", "mongo", "melon", "orange", "paddy", "papaya", "peper chill",
        "pineapple", "pomelo", "shallot", "soybeans", "spinach", "sweet potatoes", "tobacco", "waterapple", "watermelon"
    )

    fun loadModel(): Boolean{
        return try {
            val modelFile = File(context.filesDir, "plant_model_mobile.ptl")
            if(!modelFile.exists()){
                context.assets.open("plant_model_mobile.ptl").use{ ins ->
                    FileOutputStream(modelFile).use { ots ->
                        ins.copyTo(ots)
                    }
                }
            }
            this.module = LiteModuleLoader.load(modelFile.absoluteFile.toString())
            Log.d("PlantClassifier", "Model Loaded Successfully")
            true
        } catch (e: IOException) {

            false
        }
    }

    fun classifyImage(bitmap: Bitmap): Pair<String, Float> {
        return try {
            val module = this.module ?: return Pair("Model not Loaded", 0f)

            Log.d("PlantClassifier", "Image Classified Successfully")

            val resizedBitmap = bitmap.scale(128, 128, false)

            val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                resizedBitmap,
                floatArrayOf(0.5f, 0.5f, 0.5f),
                floatArrayOf(0.5f, 0.5f, 0.5f)
            )



            val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
            val scores = outputTensor.dataAsFloatArray

            val maxScoreIdx = scores.indices.maxByOrNull{ scores[it] } ?: 0
            val maxScore = scores[maxScoreIdx]
            val predictedClass = classes[maxScoreIdx]

            val expScores = scores.map { kotlin.math.exp(it.toDouble()).toFloat() }
            val sumExp = expScores.sum()
            val probability = expScores[maxScoreIdx] / sumExp

            Pair(predictedClass, probability)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("Error during classification", 0f)
        }
    }
}