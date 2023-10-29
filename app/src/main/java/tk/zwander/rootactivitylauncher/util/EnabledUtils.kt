package tk.zwander.rootactivitylauncher.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.ServiceManager
import android.os.UserHandle
import android.util.Log
import com.rosan.dhizuku.api.Dhizuku
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.util.launch.ReceiverLaunchStrategy.ShizukuJava.getUidAndPackage

private fun tryWrappedBinderEnable(pkg: String, enabled: Boolean, wrap: (IBinder) -> IBinder): Throwable? {
    return try {
        val ipm = IPackageManager.Stub.asInterface(
            wrap(ServiceManager.getService("package"))
        )

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

private suspend fun Context.tryShizukuEnable(pkg: String, enabled: Boolean): Throwable? {
    if (!Shizuku.pingBinder()) return Exception(resources.getString(R.string.shizuku_not_running))

    if (!hasShizukuPermission && !requestShizukuPermission()) {
        return Exception(resources.getString(R.string.no_shizuku_access))
    }

    return tryWrappedBinderEnable(pkg, enabled) { ShizukuBinderWrapper(it) }
}

private suspend fun Context.tryDhizukuEnable(pkg: String, enabled: Boolean): Throwable? {
    if (!Dhizuku.init(this)) return Exception(resources.getString(R.string.dhizuku_not_running))

    if (!Dhizuku.isPermissionGranted() && !DhizukuUtils.requestDhizukuPermission()) {
        return Exception(resources.getString(R.string.no_dhizuku_access))
    }

    return tryWrappedBinderEnable(pkg, enabled) { Dhizuku.binderWrapper(it) }
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

        val dhizukuResult = tryDhizukuEnable(pkg, enabled)
        if (dhizukuResult == null) {
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

    val hasShizuku = Shizuku.pingBinder() && hasShizukuPermission
    val hasDhizuku = Dhizuku.init(this) && Dhizuku.isPermissionGranted()

    if (!hasRoot && Shizuku.pingBinder() && !hasShizukuPermission) {
        requestShizukuPermission()
    }

    if (!hasRoot && Dhizuku.init(this) && !Dhizuku.isPermissionGranted()) {
        DhizukuUtils.requestDhizukuPermission()
    }

    val (_, pkg) = getUidAndPackage()

    return if (hasRoot || hasShizuku || hasDhizuku) {
        fun binderWrapper(wrap: (IBinder) -> IBinder): Throwable? {
            val binder = SystemServiceHelper.getSystemService("package")
            val ipm = IPackageManager.Stub.asInterface(wrap(binder))

            return try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    IPackageManager::class.java
                        .getDeclaredMethod(
                            "setComponentEnabledSetting",
                            ComponentName::class.java, Int::class.java,
                            Int::class.java, Int::class.java,
                        )
                        .invoke(
                            ipm,
                            info.component,
                            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            0,
                            UserHandle.USER_SYSTEM,
                        )
                } else {
                    ipm.setComponentEnabledSetting(
                        info.component,
                        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        0,
                        UserHandle.USER_SYSTEM,
                        pkg,
                    )
                }

                if (info.info.isActuallyEnabled(this@setComponentEnabled) == enabled) {
                    null
                } else {
                    Exception(
                        resources.getString(R.string.unknown_state_change_error, info.info.safeComponentName.flattenToString())
                    )
                }
            } catch (e: Exception) {
                Log.e("RootActivityLauncher", "Error changing component state", e)
                e
            }
        }

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
                if (hasShizuku) {
                    if (binderWrapper { ShizukuBinderWrapper(it) } == null) {
                        return@withContext null
                    }
                }

                if (hasDhizuku) {
                    return@withContext binderWrapper { Dhizuku.binderWrapper(it) }
                }

                Exception(
                    resources.getString(R.string.unknown_state_change_error, info.info.safeComponentName.flattenToString())
                )
            }
        }

        result
    } else {
        Exception(
            resources.getString(R.string.root_or_shizuku_required)
        )
    }
}
