package com.nighthawkapps.wallet.android.ui.scan

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.nighthawkapps.wallet.android.R
import com.nighthawkapps.wallet.android.databinding.FragmentScanBinding
import com.nighthawkapps.wallet.android.di.viewmodel.activityViewModel
import com.nighthawkapps.wallet.android.di.viewmodel.viewModel
import com.nighthawkapps.wallet.android.ext.onClickNavBack
import com.nighthawkapps.wallet.android.ext.twig
import com.nighthawkapps.wallet.android.ui.MainViewModel
import com.nighthawkapps.wallet.android.ui.base.BaseFragment
import com.nighthawkapps.wallet.android.ui.send.SendViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : BaseFragment<FragmentScanBinding>() {

    private val viewModel: ScanViewModel by viewModel()

    private val sendViewModel: SendViewModel by activityViewModel()
    private val mainViewModel: MainViewModel by activityViewModel()

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private var cameraExecutor: ExecutorService? = null

    override fun inflate(inflater: LayoutInflater): FragmentScanBinding =
        FragmentScanBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (cameraExecutor != null) cameraExecutor?.shutdown()
        cameraExecutor = Executors.newSingleThreadExecutor()
        binding.backButtonHitArea.onClickNavBack()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!allPermissionsGranted()) getRuntimePermissions()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(Runnable {
            bindPreview(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(context))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor?.shutdown()
        cameraExecutor = null
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        // Most of the code here is adapted from: https://github.com/android/camera-samples/blob/master/CameraXBasic/app/src/main/java/com/android/example/cameraxbasic/fragments/CameraFragment.kt
        // it's worth keeping tabs on that implementation because they keep making breaking changes to these APIs!

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { binding.preview.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = binding.preview.display.rotation

        val preview =
            Preview.Builder().setTargetName("Preview").setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation).build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val imageAnalysis = ImageAnalysis.Builder().setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor!!, QrAnalyzer { q, i ->
            onQrScanned(q, i)
        })

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            preview.setSurfaceProvider(binding.preview.surfaceProvider)
        } catch (t: Throwable) {
            twig("Error while opening the camera: $t")
        }
}

    /**
     * Adapted from: https://github.com/android/camera-samples/blob/master/CameraXBasic/app/src/main/java/com/android/example/cameraxbasic/fragments/CameraFragment.kt#L350
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = kotlin.math.max(width, height).toDouble() / kotlin.math.min(
            width,
            height
        )
        if (kotlin.math.abs(previewRatio - (4.0 / 3.0))
            <= kotlin.math.abs(previewRatio - (16.0 / 9.0))) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun onQrScanned(qrContent: String, image: ImageProxy) {
        resumedScope.launch {
            val parsed = viewModel.parse(qrContent)
            if (parsed == null) {
                val network = viewModel.networkName
                binding.textScanError.text = getString(R.string.scan_invalid_address, network, qrContent)
                image.close()
            } else { /* continue scanning*/
                binding.textScanError.text = ""
                sendViewModel.toAddress = parsed.address
                sendViewModel.memo = parsed.memo ?: ""
                sendViewModel.zatoshiAmount = parsed.amount
                sendViewModel.setSendZecDeepLinkData(parsed)
                mainActivity?.navController?.popBackStack()
            }
        }
    }

    //
    // Permissions
    //

    private val requiredPermissions: Array<String?>
        get() {
            return try {
                val info = mainActivity?.packageManager
                    ?.getPackageInfo(mainActivity!!.packageName, PackageManager.GET_PERMISSIONS)
                val ps = info?.requestedPermissions
                if (ps != null && ps.isNotEmpty()) {
                    ps
                } else {
                    arrayOfNulls(0)
                }
            } catch (e: Exception) {
                arrayOfNulls(0)
            }
        }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(mainActivity!!, permission!!)) {
                return false
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = arrayListOf<String>()
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(mainActivity!!, permission!!)) {
                allNeededPermissions.add(permission)
            }
        }

        if (allNeededPermissions.isNotEmpty()) {
            requestPermissions(allNeededPermissions.toTypedArray(), CAMERA_PERMISSION_REQUEST)
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 1002

        private fun isPermissionGranted(context: Context, permission: String): Boolean {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
