package com.nighthawkapps.wallet.android.ext

import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import androidx.core.content.getSystemService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nighthawkapps.wallet.android.R

fun Context.showClearDataConfirmation(onDismiss: () -> Unit = {}, onCancel: () -> Unit = {}): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dialog_nuke_wallet_title)
        .setMessage(R.string.dialog_nuke_wallet_message)
        .setCancelable(false)
        .setPositiveButton(R.string.dialog_nuke_wallet_button_positive) { dialog, _ ->
            dialog.dismiss()
            onDismiss()
            onCancel()
        }
        .setNegativeButton(R.string.dialog_nuke_wallet_button_negative) { dialog, _ ->
            dialog.dismiss()
            onDismiss()
            getSystemService<ActivityManager>()?.clearApplicationUserData()
        }
        .show()
}

fun Context.showUninitializedError(error: Throwable? = null, onDismiss: () -> Unit = {}): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dialog_error_uninitialized_title)
        .setMessage(R.string.dialog_error_uninitialized_message)
        .setCancelable(false)
        .setPositiveButton(getString(R.string.dialog_error_uninitialized_button_positive)) { dialog, _ ->
            dialog.dismiss()
            onDismiss()
            if (error != null) throw error
        }
        .setNegativeButton(getString(R.string.dialog_error_uninitialized_button_negative)) { dialog, _ ->
            showClearDataConfirmation(onDismiss, onCancel = {
                // do not let the user back into the app because we cannot recover from this case
                showUninitializedError(error, onDismiss)
                }
            )
        }
        .show()
}

fun Context.showInvalidSeedPhraseError(error: Throwable? = null, onDismiss: () -> Unit = {}): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dialog_error_invalid_seed_phrase_title)
        .setMessage(getString(R.string.dialog_error_invalid_seed_phrase_message, error?.message ?: ""))
        .setCancelable(false)
        .setPositiveButton(getString(R.string.dialog_error_invalid_seed_phrase_button_positive)) { dialog, _ ->
            dialog.dismiss()
            onDismiss()
        }
        .show()
}

fun Context.showScanFailure(error: Throwable?, onCancel: () -> Unit = {}, onDismiss: () -> Unit = {}): Dialog {
    val message = if (error == null) {
        "Unknown error"
    } else {
        "${error.message}${if (error.cause != null) "\n\nCaused by: ${error.cause}" else ""}"
    }
    return MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dialog_error_scan_failure_title)
        .setMessage(message)
        .setCancelable(true)
        .setPositiveButton(R.string.dialog_error_scan_failure_button_positive) { d, _ ->
            d.dismiss()
            onDismiss()
        }
        .setNegativeButton(R.string.dialog_error_scan_failure_button_negative) { d, _ ->
            d.dismiss()
            onCancel()
            onDismiss()
        }
        .show()
}

fun Context.showCriticalProcessorError(
    error: Throwable?,
    onRetry: () -> Unit
): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dialog_error_processor_critical_title)
        .setMessage(error?.message ?: getString(R.string.dialog_error_processor_critical_message))
        .setCancelable(false)
        .setPositiveButton(R.string.dialog_error_processor_critical_button_rescan) { d, _ ->
            d.dismiss()
            onRetry()
        }
        .setNeutralButton(R.string.dialog_error_processor_critical_button_wait) { d, _ ->
            d.dismiss()
        }
        .setNegativeButton(R.string.dialog_error_processor_critical_button_negative) { dialog, _ ->
            dialog.dismiss()
            throw error ?: RuntimeException("Critical error while processing blocks and the user chose to exit.")
        }
        .show()
}

fun Context.showUpdateServerCriticalError(userFacingMessage: String, onConfirm: () -> Unit = {}): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dialog_error_change_server_title)
        .setMessage(userFacingMessage)
        .setCancelable(false)
        .setPositiveButton(R.string.dialog_error_change_server_button_positive) { d, _ ->
            d.dismiss()
            onConfirm()
        }
        .show()
}

fun Context.showUpdateServerDialog(positiveResId: Int = R.string.dialog_modify_server_button_positive, onCancel: () -> Unit = {}, onUpdate: () -> Unit = {}): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dialog_modify_server_title)
        .setMessage(R.string.dialog_modify_server_message)
        .setCancelable(false)
        .setPositiveButton(positiveResId) { dialog, _ ->
            dialog.dismiss()
            onUpdate()
        }
        .setNegativeButton(R.string.dialog_modify_server_button_negative) { dialog, _ ->
            dialog.dismiss()
            onCancel
        }
        .show()
}
