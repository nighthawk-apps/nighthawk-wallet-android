package com.nighthawkapps.wallet.android.ui.scan

import android.content.res.Resources
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Reader
import com.google.zxing.common.HybridBinarizer

class QrAnalyzer(val scanCallback: (qrContent: String, image: ImageProxy) -> Unit) :
    ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader()

    override fun analyze(image: ImageProxy) {
        image.toBinaryBitmap().let { bitmap ->
            val qrContent = bitmap.decodeWith(reader) ?: bitmap.flip().decodeWith(reader)
            if (qrContent == null) {
                image.close()
            } else {
                onImageScan(qrContent, image)
            }
        }
    }

    private fun ImageProxy.toBinaryBitmap(): BinaryBitmap {
        return planes[0].buffer.let { buffer ->
            ByteArray(buffer.remaining()).also { buffer.get(it) }
        }.let { bytes ->
            PlanarYUVLuminanceSource(bytes, width, height, 0, 0, width, height, false)
        }.let { source ->
            BinaryBitmap(HybridBinarizer(source))
        }
    }

    private fun BinaryBitmap.decodeWith(reader: Reader): String? {
        return try {
            reader.decode(this).toString()
        } catch (e: Resources.NotFoundException) {
            // these happen frequently. Whenever no QR code is found in the frame. No need to log.
            null
        } catch (e: Throwable) {
            null
        }
    }

    private fun BinaryBitmap.flip(): BinaryBitmap {
        blackMatrix.apply {
            repeat(width) { w ->
                repeat(height) { h ->
                    flip(w, h)
                }
            }
        }
        return this
    }

    private fun onImageScan(result: String, image: ImageProxy) {
        scanCallback(result, image)
    }
}
