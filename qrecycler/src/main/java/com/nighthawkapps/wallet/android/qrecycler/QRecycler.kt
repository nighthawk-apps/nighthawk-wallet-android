package com.nighthawkapps.wallet.android.qrecycler

import android.graphics.Bitmap
import android.graphics.Color
import android.widget.ImageView
import androidx.core.view.doOnLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType.ERROR_CORRECTION
import com.google.zxing.EncodeHintType.MARGIN
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.*


class QRecycler {
    fun load(content: String): Builder {
        return Builder(content)
    }

    // TODO: make this call async such that action can be taken once it is complete
    fun encode(builder: Builder) {
        builder.target.doOnLayout { measuredView ->
            val w = measuredView.width
            val h = measuredView.height
            val hints = mapOf(ERROR_CORRECTION to builder.errorCorrection, MARGIN to builder.quietZone)
            val bitMatrix = QRCodeWriter().encode(builder.content, BarcodeFormat.QR_CODE, w, h, hints)
            val pixels = IntArray(w * h)
            for (y in 0 until h) {
                val offset = y * w
                for (x in 0 until w) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            // TODO: RECYCLE THIS BITMAP MEMORY!!! Do it in a way that is lifecycle-aware and disposes of the memory when the fragment is off-screen
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
            (measuredView as ImageView).setImageBitmap(bitmap)
        }
    }

    inner class Builder(val content: String) {
        lateinit var target: ImageView
        var errorCorrection: ErrorCorrectionLevel = Q
        var quietZone: Int = 4
        fun into(imageView: ImageView) {
            target = imageView
            encode(this)
        }
        fun withQuietZoneSize(customQuietZone: Int): Builder {
            quietZone = customQuietZone
            return this
        }
        fun withCorrectionLevel(level: CorrectionLevel): Builder {
            errorCorrection = level.errorCorrectionLevel
            return this
        }
    }

    enum class CorrectionLevel(val errorCorrectionLevel: ErrorCorrectionLevel) {
        LOW(L), DEFAULT(M), MEDIUM(Q), HIGH(H);
    }
}
