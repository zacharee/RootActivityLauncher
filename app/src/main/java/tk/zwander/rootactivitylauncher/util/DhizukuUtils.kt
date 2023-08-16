package tk.zwander.rootactivitylauncher.util

import android.content.pm.PackageManager
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object DhizukuUtils {
    suspend fun requestDhizukuPermission(): Boolean {
        return suspendCoroutine { continuation ->
            Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                override fun onRequestPermission(grantResult: Int) {
                    continuation.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            })
        }
    }
}
