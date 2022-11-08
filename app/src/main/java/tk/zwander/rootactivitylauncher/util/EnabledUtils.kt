package tk.zwander.rootactivitylauncher.util

import android.content.Context
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.os.UserHandle
import android.widget.Toast
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo

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

private fun tryRootEnable(pkg: String, enabled: Boolean): Boolean {
    if (!Shell.SU.available()) return false

    return Shell.Pool.SU.run("pm ${if (enabled) "enable" else "disable"} $pkg") == 0
}

fun Context.setPackageEnabled(pkg: String, enabled: Boolean): Boolean {
    if (pkg == packageName) return false

    try {
        val newState = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER

        packageManager.setApplicationEnabledSetting(
            pkg,
            newState,
            0
        )

        assert(packageManager.getApplicationEnabledSetting(pkg) == newState)

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

suspend fun Context.setComponentEnabled(info: BaseComponentInfo, enabled: Boolean): Boolean {
    val hasRoot = withContext(Dispatchers.IO) {
        Shell.SU.available()
    }

    return if (hasRoot || (Shizuku.pingBinder() && hasShizukuPermission)) {
        val result = withContext(Dispatchers.IO) {
            if (hasRoot) {
                try {
                    Shell.Pool.SU.run("pm ${if (enabled) "enable" else "disable"} ${info.component.flattenToString()}") == 0
                } catch (e: Exception) {
                    false
                }
            } else {
                val ipm = IPackageManager.Stub.asInterface(
                    ShizukuBinderWrapper(
                        SystemServiceHelper.getSystemService("package")
                    )
                )

                try {
                    ipm.setComponentEnabledSetting(
                        info.component,
                        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        0,
                        UserHandle.USER_SYSTEM
                    )

                    true
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(
                            this@setComponentEnabled,
                            R.string.requires_root,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    false
                }
            }
        }

        result
    } else {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@setComponentEnabled, R.string.requires_root, Toast.LENGTH_SHORT)
                .show()
        }
        false
    }
}
