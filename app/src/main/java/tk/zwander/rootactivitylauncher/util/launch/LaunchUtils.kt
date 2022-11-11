package tk.zwander.rootactivitylauncher.util.launch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.util.*

private fun Context.createLaunchArgs(extras: List<ExtraInfo>, componentKey: String): LaunchArgs {
    val intent = Intent(prefs.findActionForComponent(componentKey))
    intent.component = ComponentName.unflattenFromString(componentKey)
    intent.data = prefs.findDataForComponent(componentKey)?.let { Uri.parse(it) }

    prefs.findCategoriesForComponent(componentKey).forEach { category ->
        intent.addCategory(category)
    }

    if (extras.isNotEmpty()) {
        extras.forEach { extra ->
            extra.safeType.putExtra(intent, extra.key, extra.value)
        }
    }

    return LaunchArgs(intent, extras)
}

private suspend inline fun <reified T : LaunchStrategy> Context.performLaunch(args: LaunchArgs): Boolean {
    T::class.sealedSubclasses.forEach {
        with (it.objectInstance!!) {
            if (canRun() && tryLaunch(args)) {
                return true
            }
        }
    }

    showRootToast()
    return false
}

private suspend inline fun <reified T : LaunchStrategy> Context.launch(extras: List<ExtraInfo>, componentKey: String): Boolean {
    val args = createLaunchArgs(extras, componentKey)

    return performLaunch<T>(args)
}

suspend fun Context.launchService(extras: List<ExtraInfo>, componentKey: String): Boolean {
    return launch<ServiceLaunchStrategy>(extras, componentKey)
}

suspend fun Context.launchActivity(extras: List<ExtraInfo>, componentKey: String): Boolean {
    return launch<ActivityLaunchStrategy>(extras, componentKey)
}

suspend fun Context.launchReceiver(extras: List<ExtraInfo>, componentKey: String): Boolean {
    return launch<ReceiverLaunchStrategy>(extras, componentKey)
}