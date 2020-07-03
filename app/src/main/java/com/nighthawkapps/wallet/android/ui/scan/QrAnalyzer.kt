package com.nighthawkapps.wallet.android.ui.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import cash.z.ecc.android.sdk.ext.retrySimple
import cash.z.ecc.android.sdk.ext.twig
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata

class QrAnalyzer(val scanCallback: (qrContent: String, image: ImageProxy) -> Unit) :
    ImageAnalysis.Analyzer {
    private val detector: FirebaseVisionBarcodeDetector by lazy {
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build()
        FirebaseVision.getInstance().getVisionBarcodeDetector(options)
    }

    var pendingTask: Task<out Any>? = null

    override fun analyze(image: ImageProxy) {
        var rotation = image.imageInfo.rotationDegrees % 360
        if (rotation < 0) {
            rotation += 360
        }

        retrySimple {
            val mediaImage = FirebaseVisionImage.fromMediaImage(
                image.image!!, when (rotation) {
                    0 -> FirebaseVisionImageMetadata.ROTATION_0
                    90 -> FirebaseVisionImageMetadata.ROTATION_90
                    180 -> FirebaseVisionImageMetadata.ROTATION_180
                    270 -> FirebaseVisionImageMetadata.ROTATION_270
                    else -> {
                        FirebaseVisionImageMetadata.ROTATION_0
                    }
                }
            )
            pendingTask = detector.detectInImage(mediaImage).also {
                it.addOnSuccessListener { result ->
                    onImageScan(result, image)
                }
                it.addOnFailureListener(::onImageScanFailure)
            }
        }
    }

    private fun onImageScan(result: List<FirebaseVisionBarcode>, image: ImageProxy) {
        result.firstOrNull()?.rawValue?.let {
            scanCallback(it, image)
        } ?: runCatching { image.close() }
    }

    private fun onImageScanFailure(e: Exception) {
        twig("Warning: Image scan failed")
    }
}
