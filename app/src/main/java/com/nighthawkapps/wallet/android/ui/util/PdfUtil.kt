package com.nighthawkapps.wallet.android.ui.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.ext.locale
import com.nighthawkapps.wallet.android.ext.twig
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineExceptionHandler
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.IOException
import java.security.Security
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
            PDFBoxResourceLoader.init(NighthawkWalletApp.instance.applicationContext)
            val stringBuilder = StringBuilder()
            val listOfSeeds = mutableListOf<String>()
            val noOfWordsPerLineToPrint = 6
            try {
                seedWords.forEachIndexed { index, chars ->
                    stringBuilder.append(index + 1).append(". ").append(chars.concatToString()).append("     ")
                    if ((index + 1) % noOfWordsPerLineToPrint == 0) {
                        listOfSeeds.add(stringBuilder.toString())
                        stringBuilder.clear()
                    }
                }
                listOfSeeds.add(stringBuilder.toString())
                val filePath = "${context.cacheDir.absolutePath}/NighthawkSeedWords.pdf"
                if (File(filePath).exists()) {
                    File(filePath).delete()
                }

                val keyLength = 128 // 128 bit is the highest currently supported

                // Limit permissions of those without the password
                val accessPermission = AccessPermission()
                accessPermission.setCanPrint(false)

                // Sets the owner password and user password
                val standardProtectionPolicy = StandardProtectionPolicy(password, password, accessPermission)

                // Setups up the encryption parameters

                // Setups up the encryption parameters
                standardProtectionPolicy.encryptionKeyLength = keyLength
                standardProtectionPolicy.permissions = accessPermission
                val provider = BouncyCastleProvider()
                Security.addProvider(provider)

                val font: PDFont = PDType1Font.HELVETICA
                val document = PDDocument()
                val page = PDPage()

                document.addPage(page)

                try {
                    val contentStream = PDPageContentStream(document, page)
                    val headingFontSize = 16f
                    val subHeadingFontSize = 14f
                    val bodyFontSize = 12f
                    val leading: Float = 1.5f * headingFontSize
                    val seedWordXOffset = 50f

                    contentStream.beginText()
                    contentStream.setNonStrokingColor(0, 0, 0) // Write Text in black color
                    contentStream.setFont(font, headingFontSize)
                    contentStream.newLineAtOffset(50f, 700f)

                    // Title
                    contentStream.showText("These are seed words used to restore your Zcash in Nighthawk Wallet: ")

                    // Seed Words
                    contentStream.setFont(font, bodyFontSize)
                    contentStream.newLineAtOffset(seedWordXOffset, -leading)
                    for (wordLine in listOfSeeds) {
                        contentStream.newLineAtOffset(0f, -leading)
                        contentStream.showText(wordLine)
                    }

                    // Wallet Birthday
                    contentStream.setFont(font, subHeadingFontSize)
                    contentStream.newLineAtOffset(-seedWordXOffset, -leading)
                    contentStream.showText("Wallet Birthday: $birthDay")

                    // Pdf generated time
                    val dateFormatter = SimpleDateFormat(context.getString(R.string.transaction_history_format_date_time_brief), context.locale())
                    contentStream.newLineAtOffset(0f, -leading)
                    contentStream.showText("Generated At: ${dateFormatter.format(Date())}")

                    contentStream.endText()
                    contentStream.close()

                    // Save the final pdf document to a file
                    document.protect(standardProtectionPolicy) // Apply the protections to the PDF
                    document.save(filePath)
                    document.close()
                } catch (e: IOException) {
                    twig("PdfBox Exception thrown while creating PDF for encryption $e")
                }

                // Share the file
                if (File(filePath).exists()) {
                    val fileURI = FileProvider.getUriForFile(
                        context, context.packageName + ".fileprovider",
                        File(filePath)
                    )
                    withContext(Dispatchers.Main) {
                        shareFile(context, fileURI)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDF creation failed", Toast.LENGTH_SHORT).show()
                    twig(e)
                }
            }
        }
    }

    private fun shareFile(context: Context, fileURI: Uri) {
        try {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Intent.EXTRA_STREAM, fileURI)
            }.run {
                val intentChooser = Intent.createChooser(this, "Share File")

                val resInfoList: List<ResolveInfo> = context.packageManager
                    .queryIntentActivities(intentChooser, PackageManager.MATCH_DEFAULT_ONLY)

                for (resolveInfo in resInfoList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(
                        packageName,
                        fileURI,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                context.startActivity(intentChooser)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "PDF sharing failed", Toast.LENGTH_SHORT).show()
            twig(e)
        }
    }
}
