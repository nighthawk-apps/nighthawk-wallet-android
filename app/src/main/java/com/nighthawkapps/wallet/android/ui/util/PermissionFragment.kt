package com.nighthawkapps.wallet.android.ui.util
//
//import android.Manifest
//import android.content.Context
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.widget.Toast
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import cash.z.ecc.android.ui.MainActivity
//
//class PermissionFragment : Fragment() {
//
//    val activity get() = context as MainActivity
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        if (!hasPermissions(activity)) {
//            requestPermissions(PERMISSIONS, REQUEST_CODE)
//        } else {
//            activity.openCamera()
//        }
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int, permissions: Array<String>, grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == REQUEST_CODE) {
//            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
//                activity.openCamera()
//            } else {
//                Toast.makeText(context, "Camera request denied", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    companion object {
//        private const val REQUEST_CODE = 101
//        private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
//
//        fun hasPermissions(context: Context) = PERMISSIONS.all {
//            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
//        }
//    }
//}