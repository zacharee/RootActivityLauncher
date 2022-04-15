package tk.zwander.rootactivitylauncher.util.launch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.util.*

fun Context.launchService(extras: List<ExtraInfo>, componentKey: String): Boolean {
    val intent = Intent(prefs.findActionForComponent(componentKey))
    intent.component = ComponentName.unflattenFromString(componentKey)

    if (extras.isNotEmpty()) extras.forEach {
        intent.putExtra(it.key, it.value)
    }

    val args = LaunchArgs(
        intent = intent,
        extras = extras
    )

    ServiceLaunchStrategy::class.sealedSubclasses.forEach {
        with (it.objectInstance!!) {
            if (canRun() && tryLaunch(args)) {
                return true
            }
        }
    }

    showRootToast()
    return false
}

fun Context.launchActivity(extras: List<ExtraInfo>, componentKey: String): Boolean {
    val intent = Intent(prefs.findActionForComponent(componentKey))
    intent.component = ComponentName.unflattenFromString(componentKey)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    if (extras.isNotEmpty()) extras.forEach {
        intent.putExtra(it.key, it.value)
    }

    val args = LaunchArgs(
        intent = intent,
        extras = extras
    )

    ActivityLaunchStrategy::class.sealedSubclasses.forEach {
        with (it.objectInstance!!) {
            if (canRun() && tryLaunch(args)) {
                return true
            }
        }
    }

    showRootToast()
    return false
}

fun Context.launchReceiver(extras: List<ExtraInfo>, componentKey: String): Boolean {
    val intent = Intent(prefs.findActionForComponent(componentKey))
    intent.component = ComponentName.unflattenFromString(componentKey)

    if (extras.isNotEmpty()) extras.forEach {
        intent.putExtra(it.key, it.value)
    }

    val args = LaunchArgs(
        intent = intent,
        extras = extras
    )

    ReceiverLaunchStrategy::class.sealedSubclasses.forEach {
        with (it.objectInstance!!) {
            if (canRun() && tryLaunch(args)) {
                return true
            }
        }
    }

    showRootToast()
    return false
}