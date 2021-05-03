package com.nighthawkapps.wallet.android.ext

import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.text.Html
import androidx.annotation.StringRes
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

fun Context.showCriticalMessage(@StringRes titleResId: Int, @StringRes messageResId: Int, onDismiss: () -> Unit = {}): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle(titleResId)
        .setMessage(messageResId)
        .setCancelable(false)
        .setPositiveButton(android.R.string.ok) { d, _ ->
            d.dismiss()
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

fun Context.showRescanWalletDialog(distance: String, estimate: String, onWipe: () -> Unit = {}, onFullRescan: () -> Unit = {}, onQuickRescan: () -> Unit = {}): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle(R.string.dialog_rescan_wallet_title)
        .setMessage(Html.fromHtml(getString(R.string.dialog_rescan_wallet_message, distance, estimate)))
        .setCancelable(true)
        .setNeutralButton(R.string.dialog_rescan_wallet_button_neutral) { dialog, _ ->
            dialog.dismiss()
            onWipe()
        }
        .setNegativeButton(R.string.dialog_rescan_wallet_button_negative) { dialog, _ ->
            dialog.dismiss()
            onFullRescan()
        }
        .show()
}

fun Context.showConfirmation(title: String, message: String, positiveButton: String, negativeButton: String = "Cancel", onPositive: () -> Unit = {}): Dialog {
    return MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positiveButton) { dialog, _ ->
            dialog.dismiss()
            onPositive()
        }
        .setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}

/**
 * Error to show when the Rust libraries did not properly link. This problem can happen pretty often
 * during development when a build of the SDK failed to compile and resulted in an AAR file with no
 * shared libraries (*.so files) inside. In theory, this should never be seen by an end user but if
 * it does occur it is better to show a clean message explaining the situation. Nothing can be done
 * other than rebuilding the SDK or switching to a functional version.
 * As a developer, this error probably means that you need to comment out mavenLocal() as a repo.
 */
fun Context.showSharedLibraryCriticalError(e: Throwable): Dialog = showCriticalMessage(
    titleResId = R.string.dialog_error_critical_link_title,
    messageResId = R.string.dialog_error_critical_link_message,
    onDismiss = { throw e }
)
