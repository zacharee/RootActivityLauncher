package tk.zwander.rootactivitylauncher.util

import android.app.AppOpsManager
import android.app.IActivityManager
import android.app.IApplicationThread
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import eu.chainfire.libsuperuser.Shell
import rikka.shizuku.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo

private fun Context.showRootToast() {
    Toast.makeText(this, R.string.requires_root, Toast.LENGTH_SHORT).show()
}

private fun tryRootServiceLaunch(componentKey: String, extras: List<ExtraInfo>): Boolean {
    val command = StringBuilder("am startservice $componentKey")

    if (extras.isNotEmpty()) extras.forEach {
        command.append(" -e \"${it.key}\" \"${it.value}\"")
    }

    return Shell.Pool.SU.run(command.toString()) == 0
}

private fun tryRootActivityLaunch(componentKey: String, extras: List<ExtraInfo>): Boolean {
    val command = StringBuilder("am start -n $componentKey")

    if (extras.isNotEmpty()) extras.forEach {
        command.append(" -e \"${it.key}\" \"${it.value}\"")
    }

    return Shell.Pool.SU.run(command.toString()) == 0
}

private fun tryRootBroadcastLaunch(componentKey: String, extras: List<ExtraInfo>): Boolean {
    val command = StringBuilder("am broadcast -n $componentKey")

    if (extras.isNotEmpty()) {
        extras.forEach {
            command.append(" -e \"${it.key}\" \"${it.value}\"")
        }
    }

    return Shell.Pool.SU.run(command.toString()) == 0
}

private fun tryShizukuServiceLaunch(intent: Intent, extras: List<ExtraInfo>): Boolean {
    return try {
        val iam = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(
                SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))

        try {
            iam.startService(
                null, intent, null, false, "com.android.shell",
                null, UserHandle.USER_CURRENT
            )
            true
        } catch (e: Exception) {
            val command = StringBuilder("am start -n ${intent.component}")

            if (extras.isNotEmpty()) extras.forEach {
                command.append(" -e \"${it.key}\" \"${it.value}\"")
            }

            Shizuku.newProcess(arrayOf("sh", "-c", command.toString()), null, null).exitValue() == 0
        }
    } catch (e: Exception) {
        false
    }
}

private fun tryAdminActivityLaunch(componentKey: String): Boolean {
    return try {
        val cInfoClass = Class.forName("com.samsung.android.knox.ContextInfo")
        val cInfo = cInfoClass.getDeclaredConstructor(Int::class.java)
                .newInstance(Process.myUid())

        val iapClass = Class.forName("com.samsung.android.knox.application.IApplicationPolicy\$Stub")
        val iap = iapClass.getDeclaredMethod("asInterface", IBinder::class.java)
                .invoke(null, (SystemServiceHelper.getSystemService("application_policy")))

        val cmp = ComponentName.unflattenFromString(componentKey)

        iapClass.getMethod("startApp", cInfoClass, String::class.java, String::class.java)
                .invoke(iap, cInfo, cmp.packageName, cmp.className)

        true
    } catch (e: Exception) {
        Log.e("RootActivityLauncher", "error", e)
        false
    }
}

private fun tryShizukuActivityLaunch(intent: Intent): Boolean {
    return try {
        val iam = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(
                SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))

        iam.startActivity(
                null, "com.android.shell", intent,
                null, null, null, 0, 0,
                null, null
        )
        true
    } catch (e: Exception) {
        false
    }
}

private fun tryShizukuBroadcastLaunch(intent: Intent): Boolean {
    return try {
        val iam = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(
                SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))

        iam.broadcastIntent(
                null, intent, null, null, 0, null,
                null, null, AppOpsManager.OP_NONE, null, false, false,
                UserHandle.USER_CURRENT
        )
        true
    } catch (e: Exception) {
        false
    }
}

fun Context.launchService(extras: List<ExtraInfo>, componentKey: String) {
    val intent = Intent(prefs.findActionForComponent(componentKey))
    intent.component = ComponentName.unflattenFromString(componentKey)

    if (extras.isNotEmpty()) extras.forEach {
        intent.putExtra(it.key, it.value)
    }

    try {
        ContextCompat.startForegroundService(this, intent)
        return
    } catch (e: SecurityException) {
    }

    if (Shizuku.pingBinder() && hasShizukuPermission) {
        if (tryShizukuServiceLaunch(intent)) {
            return
        }
    }

    if (Shell.SU.available()) {
        tryRootServiceLaunch(componentKey, extras)
        return
    }

    showRootToast()
}

fun Context.launchActivity(extras: List<ExtraInfo>, componentKey: String) {
    val intent = Intent(prefs.findActionForComponent(componentKey))
    intent.component = ComponentName.unflattenFromString(componentKey)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    if (extras.isNotEmpty()) extras.forEach {
        intent.putExtra(it.key, it.value)
    }

    try {
        startActivity(intent)
        return
    } catch (e: SecurityException) {
    } catch (e: ActivityNotFoundException) {
    }

    if (Shizuku.pingBinder() && hasShizukuPermission) {
        if (tryShizukuActivityLaunch(intent)) {
            return
        }
    }

    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    if (dpm.isAdminActive(ComponentName(this, AdminReceiver::class.java))) {
        if (tryAdminActivityLaunch(componentKey)) {
            return
        }
    }

    if (Shell.SU.available()) {
        tryRootActivityLaunch(componentKey, extras)
        return
    }

    showRootToast()
}

fun Context.launchReceiver(extras: List<ExtraInfo>, componentKey: String) {
    val intent = Intent(prefs.findActionForComponent(componentKey))
    intent.component = ComponentName.unflattenFromString(componentKey)

    if (extras.isNotEmpty()) extras.forEach {
        intent.putExtra(it.key, it.value)
    }

    try {
        sendBroadcast(intent)
    } catch (e: SecurityException) {
    }

    if (Shizuku.pingBinder() && hasShizukuPermission) {
        if (tryShizukuBroadcastLaunch(intent)) {
            return
        }
    }

    if (Shell.SU.available()) {
        tryRootBroadcastLaunch(componentKey, extras)
        return
    }

    showRootToast()
}