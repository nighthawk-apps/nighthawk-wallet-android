package com.nighthawkapps.wallet.android.ext

import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import androidx.core.content.getSystemService
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Context.showClearDataConfirmation(
    onDismiss: () -> Unit = {},
    onCancel: () -> Unit = {}
): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle("Nuke Wallet?")
        .setMessage("WARNING: Potential Loss of Funds\n\nClearing all wallet data and can result in a loss of funds, if you cannot locate your correct seed phrase.\n\nPlease confirm that you have your 24-word seed phrase available before proceeding.")
        .setCancelable(false)
        .setPositiveButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            onDismiss()
            onCancel()
        }
        .setNegativeButton("Erase Wallet") { dialog, _ ->
            dialog.dismiss()
            onDismiss()
            getSystemService<ActivityManager>()?.clearApplicationUserData()
        }
        .show()
}

fun Context.showUninitializedError(error: Throwable? = null, onDismiss: () -> Unit = {}): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle("Wallet Improperly Initialized")
        .setMessage("This wallet has not been initialized correctly! Perhaps an error occurred during install.\n\nThis can be fixed with a reset. First, locate your backup seed phrase, then CLEAR DATA and reimport it.")
        .setCancelable(false)
        .setPositiveButton("Exit") { dialog, _ ->
            dialog.dismiss()
            onDismiss()
            if (error != null) throw error
        }
        .setNegativeButton("Clear Data") { dialog, _ ->
            showClearDataConfirmation(onDismiss, onCancel = {
                // do not let the user back into the app because we cannot recover from this case
                showUninitializedError(error, onDismiss)
            })
        }
        .show()
}

fun Context.showInvalidSeedPhraseError(
    error: Throwable? = null,
    onDismiss: () -> Unit = {}
): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle("Oops! Invalid Seed Phrase")
        .setMessage("That seed phrase appears to be invalid! Please double-check it and try again.\n\n${error?.message ?: ""}")
        .setCancelable(false)
        .setPositiveButton("Retry") { dialog, _ ->
            dialog.dismiss()
            onDismiss()
        }
        .show()
}

fun Context.showScanFailure(
    error: Throwable?,
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {}
): Dialog {
    val message = if (error == null) {
        "Unknown error"
    } else {
        "${error.message}${if (error.cause != null) "\n\nCaused by: ${error.cause}" else ""}"
    }
    return MaterialAlertDialogBuilder(this)
        .setTitle("Scan Failure")
        .setMessage(message)
        .setCancelable(true)
        .setPositiveButton("Retry") { d, _ ->
            d.dismiss()
            onDismiss()
        }
        .setNegativeButton("Ignore") { d, _ ->
            d.dismiss()
            onCancel()
            onDismiss()
        }
        .show()
}

fun Context.showCriticalProcessorError(error: Throwable?, onRetry: () -> Unit = {}): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle("Processor Error")
        .setMessage(error?.message ?: "Critical error while processing blocks!")
        .setCancelable(false)
        .setPositiveButton("Retry") { d, _ ->
            d.dismiss()
            onRetry()
        }
        .setNegativeButton("Exit") { dialog, _ ->
            dialog.dismiss()
            throw error
                ?: RuntimeException("Critical error while processing blocks and the user chose to exit.")
        }
        .show()
}