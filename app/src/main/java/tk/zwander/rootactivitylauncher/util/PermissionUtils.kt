package tk.zwander.rootactivitylauncher.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

val Context.hasShizukuPermission: Boolean
    get() {
        if (!Shizuku.pingBinder()) {
            return false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        return try {
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                checkCallingOrSelfPermission(ShizukuProvider.PERMISSION) == PackageManager.PERMISSION_GRANTED
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: IllegalStateException) {
            false
        }
    }

suspend fun requestShizukuPermission(): Boolean {
    return suspendCoroutine { coroutine ->
        requestShizukuPermission { granted ->
            coroutine.resume(granted)
        }
    }
}

fun requestShizukuPermission(resultListener: (Boolean) -> Unit): Boolean {
    return if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
        false
    } else {
        val code = Random(System.currentTimeMillis()).nextInt()
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                resultListener(grantResult == PackageManager.PERMISSION_GRANTED)
                Shizuku.removeRequestPermissionResultListener(this)
            }
        }

        try {
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(code)
            true
        } catch (e: IllegalStateException) {
            false
        }
    }
}
