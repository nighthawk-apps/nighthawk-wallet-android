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
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigator
import androidx.navigation.findNavController
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException
import cash.z.ecc.android.sdk.ext.BatchMetrics
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.toAbbreviatedAddress
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.DialogFirstUseMessageBinding
import com.nighthawkapps.wallet.android.di.component.MainActivitySubcomponent
import com.nighthawkapps.wallet.android.di.component.SynchronizerSubcomponent
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.ext.goneIf
import com.nighthawkapps.wallet.android.ext.showCriticalMessage
import com.nighthawkapps.wallet.android.ext.showCriticalProcessorError
import com.nighthawkapps.wallet.android.ext.showScanFailure
import com.nighthawkapps.wallet.android.ext.showSharedLibraryCriticalError
import com.nighthawkapps.wallet.android.ext.showUninitializedError
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.history.HistoryViewModel
import com.nighthawkapps.wallet.android.ui.setup.PasswordViewModel
import com.nighthawkapps.wallet.android.ui.setup.WalletSetupViewModel
import com.nighthawkapps.wallet.android.ui.util.MemoUtil
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : AppCompatActivity(R.layout.main_activity) {

    @Inject
    lateinit var clipboard: ClipboardManager
    lateinit var component: MainActivitySubcomponent
    lateinit var synchronizerComponent: SynchronizerSubcomponent

    val mainViewModel: MainViewModel by viewModels()

    val isInitialized get() = ::synchronizerComponent.isInitialized

    val historyViewModel: HistoryViewModel by activityViewModel()
    private val walletSetup: WalletSetupViewModel by viewModels()
    private val passwordViewModel: PasswordViewModel by activityViewModel()

    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var snackbar: Snackbar? = null
    private var dialog: Dialog? = null
    private var ignoreScanFailure: Boolean = false

    var navController: NavController? = null
    private val navInitListeners: MutableList<() -> Unit> = mutableListOf()

    private val hasCameraPermission
        get() = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    val latestHeight: Int? get() = if (isInitialized) {
        synchronizerComponent.synchronizer().latestHeight
    } else {
        null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        component = NighthawkWalletApp.component.mainActivitySubcomponent().create(this).also {
            it.inject(this)
        }
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        initNavigation()
        initLoadScreen()

        splashScreen.setKeepOnScreenCondition {
            getNavStartingPoint()
            mainViewModel.isAppStarting.value
        }

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, false)

        mainViewModel.setIntentData(intent?.data)
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

    private fun getNavStartingPoint() {
        // this will call startSync either now or later (after initializing with newly created seed)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                walletSetup.checkSeed()
                    .catch {
                        twig(it)
                    }
                    .collect {
                        twig("Checking seed")
                        setLoading(false)
                        var startDestination: Int = R.id.nav_home
                        if (it == WalletSetupViewModel.WalletSetupState.NO_SEED) {
                            // interact with user to create, backup and verify seed
                            // leads to a call to startSync(), later (after accounts are created from seed)
                            twig("Seed not found, therefore, launching seed creation flow")
                            startDestination = R.id.nav_landing
                        } else {
                            twig("Found seed. Re-opening existing wallet")
                            try {
                                startSync(walletSetup.openStoredWallet())
                                startDestination = if (passwordViewModel.isPinCodeEnabled() && passwordViewModel.needToCheckPin()) {
                                    R.id.nav_enter_pin_fragment
                                } else {
                                    R.id.nav_home
                                }
                            } catch (e: UnsatisfiedLinkError) {
                                showSharedLibraryCriticalError(e)
                            }
                        }
                        mainViewModel.setStartingDestination(startDestination)
                    }
            }
        }
    }

    private fun initNavigation() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                mainViewModel.startDestination.collect { startDestination ->
                    startDestination?.let {
                        setLoading(false)
                        navController = findNavController(R.id.nav_host_fragment)
                        val navGraph = navController?.navInflater?.inflate(R.navigation.mobile_navigation)
                        navGraph?.setStartDestination(it)
                        navController?.setGraph(navGraph!!, null)
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
                }
            }
        }
    }

    fun popBackTo(@IdRes destination: Int, inclusive: Boolean = false) {
        navController?.popBackStack(destination, inclusive)
    }

    fun safeNavigate(navDirections: NavDirections) = safeNavigate(navDirections.actionId, navDirections.arguments, null)

    fun safeNavigate(@IdRes destination: Int, args: Bundle? = null, extras: Navigator.Extras? = null) {
        if (navController == null) {
            navInitListeners.add {
                try {
                    navController?.navigate(destination, args, null, extras)
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
                navController?.navigate(destination, args, null, extras)
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

    fun startSync(initializer: Initializer, isRestart: Boolean = false) {
        twig("MainActivity.startSync")
        if (!isInitialized || isRestart) {
            mainViewModel.setLoading(true)
            synchronizerComponent = NighthawkWalletApp.component.synchronizerSubcomponent().create(
                initializer
            )
            twig("Synchronizer component created")
            synchronizerComponent.synchronizer().let { synchronizer ->
                synchronizer.onProcessorErrorHandler = ::onProcessorError
                synchronizer.onCriticalErrorHandler = ::onCriticalError
                (synchronizer as SdkSynchronizer).processor.onScanMetricCompleteListener = ::onScanMetricComplete

                synchronizer.start(lifecycleScope)
                mainViewModel.setSyncReady(true)
            }
        } else {
            twig("Ignoring request to start sync because sync has already been started!")
        }
        mainViewModel.setLoading(false)
        twig("MainActivity.startSync COMPLETE")
    }

    private fun onScanMetricComplete(batchMetrics: BatchMetrics, isComplete: Boolean) {
        val reportingThreshold = 100
        if (isComplete) {
            if (batchMetrics.cumulativeItems > reportingThreshold) {
                val network = synchronizerComponent.synchronizer().network.networkName
            }
        }
    }

    private fun onCriticalError(error: Throwable?): Boolean {
        val errorMessage = error?.message
            ?: error?.cause?.message
            ?: error?.toString()
            ?: "A critical error has occurred but no details were provided. Please report and consider submitting logs to help track this one down."
        showCriticalMessage(
            title = "Unrecoverable Error",
            message = errorMessage
        ) {
            throw error ?: RuntimeException("A critical error occurred but it was null")
        }
        return false
    }

    fun setLoading(isLoading: Boolean, message: String? = null) {
        mainViewModel.setLoading(isLoading, message)
    }

    fun authenticate(description: String, title: String = getString(R.string.biometric_prompt_title), block: () -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                twig("Authentication success with type: ${if (result.authenticationType == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL) "DEVICE_CREDENTIAL" else if (result.authenticationType == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC) "BIOMETRIC" else "UNKNOWN"}  object: ${result.cryptoObject}")
                block()
                twig("Done authentication block")
                // we probably only need to do this if the type is DEVICE_CREDENTIAL
                // but it doesn't hurt to hide the keyboard every time
                hideKeyboard()
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
                        showMessage("Please enable screen lock on this device to add security here!", true)
                        block()
                    }
                    ERROR_LOCKOUT -> doNothing("Too many attempts. Try again in 30s.")
                    ERROR_LOCKOUT_PERMANENT -> doNothing("Whoa. Waaaay too many attempts!")
                    ERROR_CANCELED -> doNothing("I just can't right now. Please try again.")
                    ERROR_NEGATIVE_BUTTON -> doNothing("Authentication cancelled", false)
                    ERROR_USER_CANCELED -> doNothing("Face/Touch ID Authentication Cancelled", false)
                    ERROR_NO_SPACE -> doNothing("Not enough storage space!")
                    ERROR_TIMEOUT -> doNothing("Oops. It timed out.")
                    ERROR_UNABLE_TO_PROCESS -> doNothing(".")
                    ERROR_VENDOR -> doNothing("We got some weird error and you should report this.")
                    else -> {
                        twig("Warning: unrecognized authentication error $errorCode")
                        doNothing("Authentication failed with error code $errorCode")
                    }
                }
            }
        }

        BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback).apply {
            authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setConfirmationRequired(false)
                    .setDescription(description)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
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
                twig("SDK_ERROR: unable to play sound due to $t")
            }
        }
    }

    // TODO: spruce this up with API 26 stuff
    fun vibrateSuccess() = vibrate(0, 200, 200, 100, 100, 800)

    fun vibrate(initialDelay: Long, vararg durations: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(longArrayOf(initialDelay, *durations), -1)
        }
    }

    fun copyAddress(view: View? = null) {
        lifecycleScope.launch {
            copyText(synchronizerComponent.synchronizer().getAddress(), "Address")
        }
    }

    fun copyTransparentAddress(view: View? = null) {
        lifecycleScope.launch {
            copyText(synchronizerComponent.synchronizer().getTransparentAddress(), "T-Address")
        }
    }

    fun copyText(textToCopy: String, label: String = "Nighthawk Wallet Text") {
        clipboard.setPrimaryClip(
            ClipData.newPlainText(label, textToCopy)
        )
        showMessage("$label copied!")
        vibrate(0, 50)
    }

    fun shareText(textToShare: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textToShare)
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    suspend fun isValidAddress(address: String): Boolean {
        try {
            return !synchronizerComponent.synchronizer().validateAddress(address).isNotValid
        } catch (t: Throwable) { }
        return false
    }

    fun preventBackPress(fragment: Fragment) {
        onFragmentBackPressed(fragment) {}
    }

    fun onFragmentBackPressed(fragment: Fragment, block: () -> Unit) {
        onBackPressedDispatcher.addCallback(
            fragment,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    block()
                }
            }
        )
    }

    fun showMessage(message: String, linger: Boolean = false) {
        twig("toast: $message")
        Toast.makeText(this, message, if (linger) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    fun showSnackbar(message: String, actionLabel: String = getString(android.R.string.ok), action: () -> Unit = {}): Snackbar {
        return if (snackbar == null) {
            val view = findViewById<View>(R.id.main_activity_container)
            val snacks = Snackbar
                .make(view, "$message", Snackbar.LENGTH_INDEFINITE)
                .setAction(actionLabel) { action() }

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
            snackbar!!.setText(message).setAction(actionLabel) { action() }
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
                        dialog = showScanFailure(
                            error,
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
                        dialog = showCriticalProcessorError(error) {
                            dialog = null
                        }
                    }
                }
            }
        }
        twig("MainActivity has received an error${if (notified) " and notified the user" else ""} and reported it to bugsnag and mixpanel.")
        return true
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
        findViewById<View>(android.R.id.content).postDelayed(
            {
                throttles[key]?.let { pendingWork ->
                    throttles.remove(key)
                    if (pendingWork !== noWork) throttle(key, delay, pendingWork)
                }
            },
            delay
        )
    }

    /* Memo functions that might possibly get moved to MemoUtils */

    suspend fun getSender(transaction: ConfirmedTransaction?): String {
        if (transaction == null) return getString(R.string.unknown)
        return MemoUtil.findAddressInMemo(transaction, ::isValidAddress)?.toAbbreviatedAddress() ?: getString(R.string.unknown)
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

    fun onLaunchUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (t: Throwable) {
            showMessage(getString(R.string.error_launch_url))
            twig("Warning: failed to open browser due to $t")
        }
    }
}
