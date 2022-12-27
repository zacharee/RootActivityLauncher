package tk.zwander.rootactivitylauncher.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.os.UserHandle
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo

private suspend fun Context.tryShizukuEnable(pkg: String, enabled: Boolean): Throwable? {
    if (!Shizuku.pingBinder()) return Exception(resources.getString(R.string.shizuku_not_running))

    if (!hasShizukuPermission && !requestShizukuPermission()) {
        return Exception(resources.getString(R.string.no_shizuku_access))
    }

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

        null
    } catch (e: Exception) {
        e
    }
}

private fun Context.tryRootEnable(pkg: String, enabled: Boolean): Throwable? {
    if (!Shell.SU.available()) return Exception(resources.getString(R.string.no_root_access))

    val errorOutput = mutableListOf<String>()

    return if (Shell.Pool.SU.run("pm ${if (enabled) "enable" else "disable"} $pkg", null, errorOutput, false) == 0) {
        null
    } else {
        Exception(errorOutput.joinToString("\n"))
    }
}

suspend fun Context.setPackageEnabled(info: ApplicationInfo, enabled: Boolean): Throwable? {
    val pkg = info.packageName
    if (pkg == packageName) return Exception(resources.getString(R.string.self_disable_error))

    return try {
        val newState = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER

        packageManager.setApplicationEnabledSetting(
            pkg,
            newState,
            0
        )

        return if (info.isActuallyEnabled(this) == enabled) {
            null
        } else {
            Exception(
                resources.getString(R.string.unknown_state_change_error, info.packageName)
            )
        }
    } catch (e: Exception) {
        val shizukuResult = tryShizukuEnable(pkg, enabled)
        if (shizukuResult == null) {
            if (info.isActuallyEnabled(this) == enabled) {
                return null
            }
        }

        val rootResult = tryRootEnable(pkg, enabled)
        if (rootResult == null) {
            if (info.isActuallyEnabled(this) == enabled) {
                return null
            }
        }

        e
    }
}

suspend fun Context.setComponentEnabled(info: BaseComponentInfo, enabled: Boolean): Throwable? {
    val hasRoot = withContext(Dispatchers.IO) {
        Shell.SU.available()
    }

    if (!hasRoot && Shizuku.pingBinder() && !hasShizukuPermission) {
        requestShizukuPermission()
    }

    return if (hasRoot || (Shizuku.pingBinder() && hasShizukuPermission)) {
        val result = withContext(Dispatchers.IO) {
            if (hasRoot) {
                try {
                    if (Shell.Pool.SU.run("pm ${if (enabled) "enable" else "disable"} ${info.component.flattenToString()}") == 0
                        && info.info.isActuallyEnabled(this@setComponentEnabled) == enabled) {
                        null
                    } else {
                        Exception(
                            resources.getString(R.string.unknown_state_change_error, info.info.safeComponentName.flattenToString())
                        )
                    }
                } catch (e: Exception) {
                    e
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

                    if (info.info.isActuallyEnabled(this@setComponentEnabled) == enabled) {
                        null
                    } else {
                        Exception(
                            resources.getString(R.string.unknown_state_change_error, info.info.safeComponentName.flattenToString())
                        )
                    }
                } catch (e: Exception) {
                    e
                }
            }
        }

        result
    } else {
        Exception(
            resources.getString(R.string.root_or_shizuku_required)
        )
    }
}
