package com.examples.storeaphoto2

import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager {

    companion object {

        fun checkPermission(activity: Activity, permission: String): Boolean {

            return if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d("PermissionManager", "Permission Denied")
                ActivityCompat.requestPermissions(activity, arrayOf(permission), 0)
                false
            }
            else true
        }
    }
}