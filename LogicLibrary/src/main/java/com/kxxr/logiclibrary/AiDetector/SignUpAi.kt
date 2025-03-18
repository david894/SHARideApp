package com.kxxr.logiclibrary.AiDetector

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream

fun extractInfoFromIdCard(
    context: Context,
    imageUri: Uri,
    onResult: (String, String, String) -> Unit // Name, Student ID, Profile Picture
) {
    val inputImage = InputImage.fromFilePath(context, imageUri)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val extractedText = visionText.text
            if (!extractedText.contains("TARUMT", ignoreCase = true)) {
                onResult("", "", "Error") // Not a valid TARUMT ID
                return@addOnSuccessListener
            }

            var name = ""
            var studentId = ""

            // Loop through the text blocks and lines to extract Name and Student ID
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    val text = line.text
                    if (Regex("[0-9]{2}[A-z]{1}").containsMatchIn(text)){
                        studentId = text // Matches Student ID pattern
                    } else if (text.contains(" ") && !Regex("\\d").containsMatchIn(text)) {
                        name = text // Heuristics for Name
                    }
                }
            }

            // Return results
            onResult(name, studentId, "Verified")
        }
        .addOnFailureListener { e ->
            onResult("", "", "Error" ) // Failed to process image
        }
}

fun detectFaceFromIdCard(
    idCardBitmap: Bitmap,
    onResult: (Bitmap?) -> Unit
) {
    val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .build()

    val detector = FaceDetection.getClient(options)
    val image = InputImage.fromBitmap(idCardBitmap, 0)

    detector.process(image)
        .addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                val face = faces[0].boundingBox

                // Padding in pixels
                val padding = 300 // Adjust this value as needed (e.g., dp converted to pixels)

                // Crop the face region
                val faceBitmap = Bitmap.createBitmap(
                    idCardBitmap,
                    (face.left - 150).coerceAtLeast(0),
                    (face.top - 170).coerceAtLeast(0),
                    (face.width() + padding).coerceAtMost(idCardBitmap.width),
                    (face.height() + padding).coerceAtMost(idCardBitmap.height)
                )
                onResult(faceBitmap)
            } else {
                onResult(null) // No face detected
            }
        }
        .addOnFailureListener {
            onResult(null) // Handle error
        }
}

fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): String {
    // Create a file in the cache directory
    val cacheDir = context.cacheDir
    val file = File(cacheDir, fileName)

    FileOutputStream(file).use { outputStream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream) // Save as PNG
        outputStream.flush()
    }

    return file.absolutePath // Return the file path for later use
}
