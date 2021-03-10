package tk.zwander.rootactivitylauncher.util

import android.content.Context
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.util.Log
import eu.chainfire.libsuperuser.Shell
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

private fun Context.tryShizukuEnable(pkg: String, enabled: Boolean): Boolean {
    if (!hasShizukuPermission || !Shizuku.pingBinder()) return false

    return try {
        val ipm = IPackageManager.Stub.asInterface(ShizukuBinderWrapper(
            SystemServiceHelper.getSystemService("package")))

        ipm.setApplicationEnabledSetting(
            pkg,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            0,
            0,
            "com.android.shell"
        )

        true
    } catch (e: Exception) {
        false
    }
}

private fun Context.tryRootEnable(pkg: String, enabled: Boolean): Boolean {
    if (!Shell.SU.available()) return false

    return Shell.Pool.SU.run("pm ${if (enabled) "enable" else "disable"} $pkg") == 0
}

fun Context.setPackageEnabled(pkg: String, enabled: Boolean): Boolean {
    if (pkg == packageName) return false

    try {
        packageManager.setApplicationEnabledSetting(
            pkg,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            0
        )
        return true
    } catch (e: Exception) {
        if (tryShizukuEnable(pkg, enabled)) {
            return true
        }

        if (tryRootEnable(pkg, enabled)) {
            return true
        }
    }

    showRootToast()
    return false
}