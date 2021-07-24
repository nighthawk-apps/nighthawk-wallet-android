package com.nighthawkapps.wallet.android.ui.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import cash.z.ecc.android.sdk.ext.twig
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.EncryptionConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.File
import java.io.FileOutputStream
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.Date

object PdfUtil {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        twig("Exception $throwable in coroutine scope $coroutineContext")
    }

    fun exportPasswordProtectedPdf(
        context: Context,
        password: String,
        seedWords: List<CharArray>,
        birthDay: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch(coroutineExceptionHandler) {
            val stringBuilder = StringBuilder()
            try {
                seedWords.forEachIndexed { index, chars ->
                    stringBuilder.append(index + 1).append(". ").append(chars.concatToString()).append("     ")
                }
                val filePath = "${context.cacheDir.absolutePath}/SeedWords.pdf"
                if (File(filePath).exists()) {
                    File(filePath).delete()
                }
                val writerProperties = WriterProperties().setStandardEncryption(
                    password.toByteArray(),
                    password.toByteArray(),
                    EncryptionConstants.ALLOW_PRINTING,
                    EncryptionConstants.ENCRYPTION_AES_256
                )
                val pdfWriter = PdfWriter(FileOutputStream(filePath), writerProperties)
                val document = Document(PdfDocument(pdfWriter), PageSize.A4, true)
                val headingFontSize = 16f
                val headingColor = DeviceRgb(0, 0, 0)

                // Add seed words
                document.add(
                    Paragraph(
                        Text("These are seed phrase used to restore Zcash based Nighthawk Wallet: \n")
                            .setFontSize(headingFontSize)
                            .setFontColor(headingColor)
                    )
                )
                document.add(Paragraph(stringBuilder.toString()))
                document.add(Paragraph("\n"))

                // Add birthday
                val birthdayParaGraph = Paragraph()
                birthdayParaGraph.add(
                    Text("Birthday: ")
                        .setFontSize(headingFontSize)
                        .setFontColor(headingColor)
                )
                birthdayParaGraph.add("$birthDay")
                document.add(birthdayParaGraph)

                // Add pdf generated time
                val generatedAtParaGraph = Paragraph()
                generatedAtParaGraph.add(
                    Text("GeneratedAt: ")
                        .setFontSize(headingFontSize)
                        .setFontColor(headingColor)
                )
                val dateFormatter = SimpleDateFormat(context.getString(R.string.transaction_history_format_date_time_brief), context.locale())
                generatedAtParaGraph.add(dateFormatter.format(Date()))
                document.add(generatedAtParaGraph)

                // Close the doc
                document.close()

                // Share the file
                withContext(Dispatchers.Main) {
                    if (File(filePath).exists()) {
                        shareFile(context, File(filePath))
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Pdf creation failed $e", Toast.LENGTH_SHORT).show()
                twig(e)
            }
        }
    }

    private fun shareFile(context: Context, file: File) {
        try {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                val fileURI = FileProvider.getUriForFile(
                    context, context.packageName + ".fileprovider",
                    file
                )
                putExtra(Intent.EXTRA_STREAM, fileURI)
            }.run {
                context.startActivity(Intent.createChooser(this, "Share File"))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Pdf sharing failed $e", Toast.LENGTH_SHORT).show()
            twig(e)
        }
    }
}
