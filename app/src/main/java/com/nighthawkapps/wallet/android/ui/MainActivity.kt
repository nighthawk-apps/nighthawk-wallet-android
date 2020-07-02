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
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.twig
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nighthawkapps.wallet.android.NighthawkWalletApp
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.di.component.MainActivitySubcomponent
import com.nighthawkapps.wallet.android.di.component.SynchronizerSubcomponent
import com.nighthawkapps.wallet.android.feedback.Feedback
import com.nighthawkapps.wallet.android.feedback.FeedbackCoordinator
import com.nighthawkapps.wallet.android.feedback.LaunchMetric
import com.nighthawkapps.wallet.android.feedback.Report
import com.nighthawkapps.wallet.android.feedback.Report.Error.NonFatal.Reorg
import com.nighthawkapps.wallet.android.feedback.Report.NonUserAction.FEEDBACK_STOPPED
import com.nighthawkapps.wallet.android.feedback.Report.NonUserAction.SYNC_START
import com.nighthawkapps.wallet.android.feedback.Report.Tap.COPY_ADDRESS
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ERROR_DIALOG_REQUEST_CODE = 1

class MainActivity : AppCompatActivity(), ProviderInstaller.ProviderInstallListener {

    @Inject
    lateinit var feedback: Feedback

    @Inject
    lateinit var feedbackCoordinator: FeedbackCoordinator

    @Inject
    lateinit var clipboard: ClipboardManager

    private var retryProviderInstall: Boolean = false
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

    override fun onCreate(savedInstanceState: Bundle?) {
        component = NighthawkWalletApp.component.mainActivitySubcomponent().create(this).also {
            it.inject(this)
        }
        super.onCreate(savedInstanceState)
        ProviderInstaller.installIfNeededAsync(this, this)
        setContentView(R.layout.main_activity)
        initNavigation()

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, false)

        lifecycleScope.launch {
            feedback.start()
        }
    }

    override fun onResume() {
        super.onResume()
        // keep track of app launch metrics
        // (how long does it take the app to open when it is not already in the foreground)
        NighthawkWalletApp.instance.let { app ->
            if (!app.creationMeasured) {
                app.creationMeasured = true
                feedback.report(LaunchMetric())
            }
        }
    }

    override fun onDestroy() {
        lifecycleScope.launch {
            feedback.report(FEEDBACK_STOPPED)
            feedback.stop()
        }
        super.onDestroy()
    }

    /**
     * This method is only called if the provider is successfully updated
     * (or is already up-to-date).
     */
    override fun onProviderInstalled() {
        // Provider is up-to-date, app can make secure network calls.
    }

    /**
     * This method is called if updating fails; the error code indicates
     * whether the error is recoverable.
     */
    override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent) {
        GoogleApiAvailability.getInstance().apply {
            if (isUserResolvableError(errorCode)) {
                // Recoverable error. Show a dialog prompting the user to
                // install/update/enable Google Play services.
                showErrorDialogFragment(this@MainActivity, errorCode, ERROR_DIALOG_REQUEST_CODE) {
                    // The user chose not to take the recovery action
                    onProviderInstallerNotAvailable()
                }
            } else {
                onProviderInstallerNotAvailable()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ERROR_DIALOG_REQUEST_CODE) {
            // Adding a fragment via GoogleApiAvailability.showErrorDialogFragment
            // before the instance state is restored throws an error. So instead,
            // set a flag here, which will cause the fragment to delay until
            // onPostResume.
            retryProviderInstall = true
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (retryProviderInstall) {
            // We can now safely retry installation.
            ProviderInstaller.installIfNeededAsync(this, this)
        }
        retryProviderInstall = false
    }

    private fun onProviderInstallerNotAvailable() {
        // This is reached if the provider cannot be updated for some reason.
        // App should consider all HTTP communication to be vulnerable, and take
        // appropriate action.
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
        navController?.addOnDestinationChangedListener { _, _, _ ->
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

    fun safeNavigate(@IdRes destination: Int) {
        if (navController == null) {
            navInitListeners.add {
                try {
                    navController?.navigate(destination)
                } catch (t: Throwable) {
                    twig(
                        "WARNING: during callback, did not navigate to destination: R.id.${resources.getResourceEntryName(
                            destination
                        )} due to: $t"
                    )
                }
            }
        } else {
            try {
                navController?.navigate(destination)
            } catch (t: Throwable) {
                twig(
                    "WARNING: did not immediately navigate to destination: R.id.${resources.getResourceEntryName(
                        destination
                    )} due to: $t"
                )
            }
        }
    }

    fun startSync(initializer: Initializer) {
        if (!::synchronizerComponent.isInitialized) {
            synchronizerComponent =
                NighthawkWalletApp.component.synchronizerSubcomponent().create(initializer)
            feedback.report(SYNC_START)
            synchronizerComponent.synchronizer().let { synchronizer ->
                synchronizer.onProcessorErrorHandler = ::onProcessorError
                synchronizer.onChainErrorHandler = ::onChainError
                synchronizer.start(lifecycleScope)
            }
        } else {
            twig("Ignoring request to start sync because sync has already been started!")
        }
    }

    fun reportScreen(screen: Report.Screen?) = reportAction(screen)

    fun reportTap(tap: Report.Tap?) = reportAction(tap)

    fun reportFunnel(step: Feedback.Funnel?) = reportAction(step)

    private fun reportAction(action: Feedback.Action?) {
        action?.let { feedback.report(it) }
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
        reportTap(COPY_ADDRESS)
        lifecycleScope.launch {
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    "Z-Address",
                    synchronizerComponent.synchronizer().getAddress()
                )
            )
            showMessage("Address copied!", "Sweet")
        }
    }

    suspend fun isValidAddress(address: String): Boolean {
        try {
            return !synchronizerComponent.synchronizer().validateAddress(address).isNotValid
        } catch (t: Throwable) {
        }
        return false
    }

    fun copyText(textToCopy: String, label: String = "Nighthawk Wallet Text") {
        clipboard.setPrimaryClip(
            ClipData.newPlainText(label, textToCopy)
        )
        showMessage("$label copied!", "Sweet")
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

    private fun showMessage(message: String, action: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun showSnackbar(message: String, action: String = "OK"): Snackbar? {
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

            snackBarView.getChildAt(0).layoutParams = params
            snacks
        } else {
            snackbar?.setText(message)?.setAction(action) {/*auto-close*/ }
        }.also {
            if (it != null && !it.isShownOrQueued) it.show()
        }
    }

    fun showKeyboard(focusedView: View?) {
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
        showSnackbar("Well, this is awkward. You denied permission for the camera.")
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
                        dialog = MaterialAlertDialogBuilder(this)
                            .setTitle("Wallet Improperly Initialized")
                            .setMessage("This wallet has not been initialized correctly! Perhaps an error occurred during install.\n\nThis can be fixed with a reset. Please reimport using your backup seed phrase.")
                            .setCancelable(false)
                            .setPositiveButton("Exit") { dialog, _ ->
                                dialog.dismiss()
                                throw error
                            }
                            .show()
                    }
                }
            }
            is CompactBlockProcessorException.FailedScan -> {
                if (dialog == null && !ignoreScanFailure) throttle("scanFailure", 20_000L) {
                    notified = true
                    runOnUiThread {
                        dialog = MaterialAlertDialogBuilder(this)
                            .setTitle("Scan Failure")
                            .setMessage("${error.message}${if (error.cause != null) "\n\nCaused by: ${error.cause}" else ""}")
                            .setCancelable(true)
                            .setPositiveButton("Retry") { d, _ ->
                                d.dismiss()
                                dialog = null
                            }
                            .setNegativeButton("Ignore") { d, _ ->
                                d.dismiss()
                                ignoreScanFailure = true
                                dialog = null
                            }
                            .show()
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
                        dialog = MaterialAlertDialogBuilder(this)
                            .setTitle("Processor Error")
                            .setMessage(error?.message ?: "Critical error while processing blocks!")
                            .setCancelable(false)
                            .setPositiveButton("Retry") { d, _ ->
                                d.dismiss()
                                dialog = null
                            }
                            .setNegativeButton("Exit") { dialog, _ ->
                                dialog.dismiss()
                                throw error
                                    ?: RuntimeException("Critical error while processing blocks and the user chose to exit.")
                            }
                            .show()
                    }
                }
            }
        }
        twig("MainActivity has received an error${if (notified) " and notified the user" else ""} and reported it to crashlytics and mixpanel.")
        feedback.report(error)
        return true
    }

    private fun onChainError(errorHeight: Int, rewindHeight: Int) {
        feedback.report(Reorg(errorHeight, rewindHeight))
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
}
