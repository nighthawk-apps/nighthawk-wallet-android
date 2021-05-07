package com.nighthawkapps.wallet.android.ui

import android.Manifest
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.ERROR_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_HW_NOT_PRESENT
import androidx.biometric.BiometricPrompt.ERROR_HW_UNAVAILABLE
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT
import androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT
import androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
import androidx.biometric.BiometricPrompt.ERROR_NO_BIOMETRICS
import androidx.biometric.BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt.ERROR_NO_SPACE
import androidx.biometric.BiometricPrompt.ERROR_TIMEOUT
import androidx.biometric.BiometricPrompt.ERROR_UNABLE_TO_PROCESS
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_VENDOR
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigator
import androidx.navigation.findNavController
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import cash.z.ecc.android.sdk.ext.twig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.DialogFirstUseMessageBinding
import com.nighthawkapps.wallet.android.di.component.MainActivitySubcomponent
import com.nighthawkapps.wallet.android.di.component.SynchronizerSubcomponent
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.showCriticalError
import com.nighthawkapps.wallet.android.ext.showCriticalProcessorError
import com.nighthawkapps.wallet.android.ext.showScanFailure
import com.nighthawkapps.wallet.android.ext.showUninitializedError
import com.nighthawkapps.wallet.android.ui.history.HistoryViewModel
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel
import com.nighthawkapps.wallet.android.ui.util.INCLUDE_MEMO_PREFIXES_RECOGNIZED
import com.nighthawkapps.wallet.android.ui.util.toUtf8Memo
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var clipboard: ClipboardManager

    @Inject
    lateinit var mainViewModel: MainViewModel

    @Inject
    lateinit var walletSetupViewModel: WalletSetupViewModel

    val isInitialized get() = ::synchronizerComponent.isInitialized

    private val historyViewModel: HistoryViewModel by activityViewModel()
    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var snackbar: Snackbar? = null
    private var dialog: Dialog? = null
    private var ignoreScanFailure: Boolean = false

    lateinit var component: MainActivitySubcomponent
    lateinit var synchronizerComponent: SynchronizerSubcomponent

    var navController: NavController? = null
    private val navInitListeners: MutableList<() -> Unit> = mutableListOf()

    private val hasCameraPermission
        get() = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    val latestHeight: Int?
        get() = if (isInitialized) {
            synchronizerComponent.synchronizer().latestHeight
        } else {
            null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        component = NighthawkWalletApp.component.mainActivitySubcomponent().create(this).also {
            it.inject(this)
        }
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)
        initNavigation()
        initLoadScreen()

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, false)
    }

    private fun setWindowFlag(bits: Int, on: Boolean) {
        val win = window
        val winParams = win.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }

    private fun initNavigation() {
        navController = findNavController(R.id.nav_host_fragment)
        navController!!.addOnDestinationChangedListener { _, _, _ ->
            // hide the keyboard anytime we change destinations
            getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(
                this@MainActivity.window.decorView.rootView.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }

        for (listener in navInitListeners) {
            listener()
        }
        navInitListeners.clear()
    }

    private fun initLoadScreen() {
        lifecycleScope.launchWhenResumed {
            mainViewModel.loadingMessage.collect { message ->
                onLoadingMessage(message)
            }
        }
    }

    private fun onLoadingMessage(message: String?) {
        twig("Applying loading message: $message")
        // TODO: replace with view binding
        findViewById<View>(R.id.container_loading).goneIf(message == null)
        findViewById<TextView>(R.id.text_message).text = message
    }

    fun safeNavigate(@IdRes destination: Int, extras: Navigator.Extras? = null) {
        if (navController == null) {
            navInitListeners.add {
                try {
                    navController?.navigate(destination, null, null, extras)
                } catch (t: Throwable) {
                    twig(
                        "WARNING: during callback, did not navigate to destination: R.id.${
                            resources.getResourceEntryName(
                                destination
                            )
                        } due to: $t"
                    )
                }
            }
        } else {
            try {
                navController?.navigate(destination, null, null, extras)
            } catch (t: Throwable) {
                twig(
                    "WARNING: did not immediately navigate to destination: R.id.${
                        resources.getResourceEntryName(
                            destination
                        )
                    } due to: $t"
                )
            }
        }
    }

    fun startSync(initializer: Initializer) {
        twig("MainActivity.startSync")
        if (!isInitialized) {
            mainViewModel.setLoading(true)
            synchronizerComponent = NighthawkWalletApp.component.synchronizerSubcomponent().create(
                initializer
            )
            twig("Synchronizer component created")
            synchronizerComponent.synchronizer().let { synchronizer ->
                lifecycleScope.launch(CoroutineExceptionHandler(::onCriticalError)) {
                    twig("starting synchronizer...")
                    synchronizer.onProcessorErrorHandler = ::onProcessorError
                    synchronizer.onChainErrorHandler = ::onChainError
                    walletSetupViewModel.onPrepareSync(synchronizer)
                    synchronizer.start(this)
                    mainViewModel.setSyncReady(true)
                    mainViewModel.setLoading(false)
                    twig("...done starting synchronizer")
                }
            }
        } else {
            twig("Ignoring request to start sync because sync has already been started!")
            mainViewModel.setLoading(false)
        }
    }

    private fun onCriticalError(coroutineContext: CoroutineContext, error: Throwable) {
        showCriticalError(
            "Critical Error",
            "An unrecognized error occurred:" +
                    "\n\n${error.message}" +
                    if (error.cause?.message != null) "\ncaused by: ${error.cause?.message}" else ""
        ) {
            throw error
        }
    }

    fun setLoading(isLoading: Boolean, message: String? = null) {
        mainViewModel.setLoading(isLoading, message)
    }

    fun authenticate(
        description: String,
        title: String = getString(R.string.biometric_prompt_title),
        block: () -> Unit
    ) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                twig("Authentication success")
                block()
            }

            override fun onAuthenticationFailed() {
                twig("Authentication failed!!!!")
                showMessage("Authentication failed :(")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                twig("Authentication Error")
                fun doNothing(message: String, interruptUser: Boolean = true) {
                    if (interruptUser) {
                        showSnackbar(message)
                    } else {
                        showMessage(message, true)
                    }
                }
                when (errorCode) {
                    ERROR_HW_NOT_PRESENT, ERROR_HW_UNAVAILABLE,
                    ERROR_NO_BIOMETRICS, ERROR_NO_DEVICE_CREDENTIAL -> {
                        twig("Warning: bypassing authentication because $errString [$errorCode]")
                        showMessage(
                            "Please enable screen lock on this device to add security here!",
                            true
                        )
                        block()
                    }
                    ERROR_LOCKOUT -> doNothing("Too many attempts. Try again in 30s.")
                    ERROR_LOCKOUT_PERMANENT -> doNothing("Whoa. Waaaay too many attempts!")
                    ERROR_CANCELED -> doNothing("I just can't right now. Please try again.")
                    ERROR_NEGATIVE_BUTTON -> doNothing("Authentication cancelled", false)
                    ERROR_USER_CANCELED -> doNothing("Cancelled", false)
                    ERROR_NO_SPACE -> doNothing("Not enough storage space!")
                    ERROR_TIMEOUT -> doNothing("Oops. It timed out.")
                    ERROR_UNABLE_TO_PROCESS -> doNothing(".")
                    ERROR_VENDOR -> doNothing("We got some weird error and you should report this.")
                }
            }
        }

        BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback).apply {
            authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setConfirmationRequired(false)
                    .setDescription(description)
                    .setDeviceCredentialAllowed(true)
                    .build()
            )
        }
    }

    fun playSound(fileName: String) {
        mediaPlayer.apply {
            if (isPlaying) stop()
            try {
                reset()
                assets.openFd(fileName).let { afd ->
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                prepare()
                start()
            } catch (t: Throwable) {
                Log.e("SDK_ERROR", "ERROR: unable to play sound due to $t")
            }
        }
    }

    // TODO: spruce this up with API 26 stuff
    fun vibrateSuccess() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(longArrayOf(0, 200, 200, 100, 100, 800), -1)
        }
    }

    fun copyAddress(view: View? = null) {
        lifecycleScope.launch {
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Z-Address",
                    synchronizerComponent.synchronizer().getAddress()
                )
            )
            showMessage("Address copied!")
        }
    }

    fun copyDonationAddress(view: View? = null) {
        lifecycleScope.launch {
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Z-Address",
                    getString(R.string.nighthawk_address)
                )
            )
            showMessage("Donation Address copied! Please return Send Zcash for sending the donation.")
        }
    }

    fun onLaunchUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (t: Throwable) {
            showMessage(getString(R.string.error_launch_url))
            twig("Warning: failed to open browser due to $t")
        }
    }

    suspend fun isValidAddress(address: String): Boolean {
        try {
            return !synchronizerComponent.synchronizer().validateAddress(address).isNotValid
        } catch (t: Throwable) {
        }
        return false
    }

    fun copyText(textToCopy: String, label: String = "ECC Wallet Text") {
        clipboard.setPrimaryClip(
            ClipData.newPlainText(label, textToCopy)
        )
        showMessage("$label copied!")
    }

    fun preventBackPress(fragment: Fragment) {
        onFragmentBackPressed(fragment) {}
    }

    fun onFragmentBackPressed(fragment: Fragment, block: () -> Unit) {
        onBackPressedDispatcher.addCallback(fragment, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                block()
            }
        })
    }

    private fun showMessage(message: String, linger: Boolean = false) {
        Toast.makeText(this, message, if (linger) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    fun showSnackbar(message: String, action: String = getString(android.R.string.ok)): Snackbar {
        return if (snackbar == null) {
            val view = findViewById<View>(R.id.main_activity_container)
            val snacks = Snackbar
                .make(view, "$message", Snackbar.LENGTH_INDEFINITE)
                .setAction(action) { /*auto-close*/ }

            val snackBarView = snacks.view as ViewGroup
            val navigationBarHeight = resources.getDimensionPixelSize(
                resources.getIdentifier(
                    "navigation_bar_height",
                    "dimen",
                    "android"
                )
            )
            val params = snackBarView.getChildAt(0).layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(
                params.leftMargin,
                params.topMargin,
                params.rightMargin,
                navigationBarHeight
            )

            snackBarView.getChildAt(0).setLayoutParams(params)
            snacks
        } else {
            snackbar!!.setText(message).setAction(action) { /*auto-close*/ }
        }.also {
            if (!it.isShownOrQueued) it.show()
        }
    }

    fun showKeyboard(focusedView: View) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(focusedView, InputMethodManager.SHOW_FORCED)
    }

    fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(findViewById<View>(android.R.id.content).windowToken, 0)
    }

    /**
     * @param popUpToInclusive the destination to remove from the stack before opening the camera.
     * This only takes effect in the common case where the permission is granted.
     */
    fun maybeOpenScan(popUpToInclusive: Int? = null) {
        if (hasCameraPermission) {
            openCamera(popUpToInclusive)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
            } else {
                onNoCamera()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                onNoCamera()
            }
        }
    }

    private fun openCamera(popUpToInclusive: Int? = null) {
        navController?.navigate(popUpToInclusive ?: R.id.action_global_nav_scan)
    }

    private fun onNoCamera() {
        showSnackbar(getString(R.string.camera_permission_denied))
    }

    // TODO: clean up this error handling
    private var ignoredErrors = 0
    private fun onProcessorError(error: Throwable?): Boolean {
        var notified = false
        when (error) {
            is CompactBlockProcessorException.Uninitialized -> {
                if (dialog == null) {
                    notified = true
                    runOnUiThread {
                        dialog = showUninitializedError(error) {
                            dialog = null
                        }
                    }
                }
            }
            is CompactBlockProcessorException.FailedScan -> {
                if (dialog == null && !ignoreScanFailure) throttle("scanFailure", 20_000L) {
                    notified = true
                    runOnUiThread {
                        dialog = showScanFailure(error,
                            onCancel = { dialog = null },
                            onDismiss = { dialog = null }
                        )
                    }
                }
            }
        }
        if (!notified) {
            ignoredErrors++
            if (ignoredErrors >= ZcashSdk.RETRIES) {
                if (dialog == null) {
                    notified = true
                    runOnUiThread {
                        dialog = showCriticalProcessorError(error, onRetry = {
                            lifecycleScope.launch {
                                // TODO: give a WIPE option here, instead of auto-erase. Perhaps a rescan popup?
//                                Initializer.erase(
//                                    NighthawkWalletApp.instance,
//                                    ZcashSdk.DEFAULT_ALIAS
//                                )
//                                walletSetupViewModel.onRestore()
                                dialog = null
                            }
                        })
                    }
                }
            }
        }
        twig("MainActivity has received an error${if (notified) " and notified the user" else ""} and reported it to bugsnag and mixpanel.")
        return true
    }

    private fun onChainError(errorHeight: Int, rewindHeight: Int) {
    }

    // TODO: maybe move this quick helper code somewhere general or throttle the dialogs differently (like with a flow and stream operators, instead)

    private val throttles = mutableMapOf<String, () -> Any>()
    private val noWork = {}
    private fun throttle(key: String, delay: Long, block: () -> Any) {
        // if the key exists, just add the block to run later and exit
        if (throttles.containsKey(key)) {
            throttles[key] = block
            return
        }
        block()

        // after doing the work, check back in later and if another request came in, throttle it, otherwise exit
        throttles[key] = noWork
        findViewById<View>(android.R.id.content).postDelayed({
            throttles[key]?.let { pendingWork ->
                throttles.remove(key)
                if (pendingWork !== noWork) throttle(key, delay, pendingWork)
            }
        }, delay)
    }

    fun toTxId(tx: ByteArray?): String? {
        if (tx == null) return null
        val sb = StringBuilder(tx.size * 2)
        for (i in (tx.size - 1) downTo 0) {
            sb.append(String.format("%02x", tx[i]))
        }
        return sb.toString()
    }

    /* Memo functions that might possibly get moved to MemoUtils */

    private val addressRegex = """zs\d\w{65,}""".toRegex()

    suspend fun getSender(transaction: ConfirmedTransaction?): String {
        if (transaction == null) return getString(R.string.unknown)
        val memo = transaction.memo.toUtf8Memo()
        return extractValidAddress(memo)?.toAbbreviatedAddress() ?: getString(R.string.unknown)
    }

    fun extractAddress(memo: String?) = addressRegex.findAll(memo ?: "").lastOrNull()?.value

    suspend fun extractValidAddress(memo: String?): String? {
        if (memo == null || memo.length < 25) return null

        // note: cannot use substringAfterLast because we need to ignore case
        try {
            INCLUDE_MEMO_PREFIXES_RECOGNIZED.forEach { prefix ->
                memo.lastIndexOf(prefix, ignoreCase = true).takeUnless { it == -1 }
                    ?.let { lastIndex ->
                        memo.substring(lastIndex + prefix.length).trimStart().validateAddress()
                            ?.let { address ->
                                return@extractValidAddress address
                            }
                    }
            }
        } catch (t: Throwable) {
        }

        return null
    }

    suspend fun String?.validateAddress(): String? {
        if (this == null) return null
        return if (isValidAddress(this)) this else null
    }

    fun showFirstUseWarning(
        prefKey: String,
        @StringRes titleResId: Int = R.string.blank,
        @StringRes msgResId: Int = R.string.blank,
        @StringRes positiveResId: Int = android.R.string.ok,
        @StringRes negativeResId: Int = android.R.string.cancel,
        action: MainActivity.() -> Unit = {}
    ) {
        historyViewModel.prefs.getBoolean(prefKey).let { doNotWarnAgain ->
            if (doNotWarnAgain) {
                action()
                return@showFirstUseWarning
            }
        }

        val dialogViewBinding = DialogFirstUseMessageBinding.inflate(layoutInflater)

        fun savePref() {
            dialogViewBinding.dialogFirstUseCheckbox.isChecked.let { wasChecked ->
                historyViewModel.prefs.setBoolean(prefKey, wasChecked)
            }
        }

        dialogViewBinding.dialogMessage.setText(msgResId)
        if (dialog != null) dialog?.dismiss()
        dialog = MaterialAlertDialogBuilder(this)
            .setTitle(titleResId)
            .setView(dialogViewBinding.root)
            .setCancelable(false)
            .setPositiveButton(positiveResId) { d, _ ->
                d.dismiss()
                dialog = null
                savePref()
                action()
            }
            .setNegativeButton(negativeResId) { d, _ ->
                d.dismiss()
                dialog = null
                savePref()
            }
            .show()
    }
}
