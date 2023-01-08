package tk.zwander.rootactivitylauncher.util.launch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.data.prefs
import tk.zwander.rootactivitylauncher.util.findActionForComponent
import tk.zwander.rootactivitylauncher.util.findCategoriesForComponent
import tk.zwander.rootactivitylauncher.util.findDataForComponent

private fun Context.createLaunchArgs(extras: List<ExtraInfo>, componentKey: String, filters: List<IntentFilter>): LaunchArgs {
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

    return LaunchArgs(intent, extras, filters)
}

private suspend inline fun <reified T : LaunchStrategy> Context.performLaunch(args: LaunchArgs): List<Pair<String, Throwable>> {
    val errors = mutableListOf<Pair<String, Throwable>>()

    T::class.sealedSubclasses.forEach {
        with (it.objectInstance!!) {
            if (canRun()) {
                val latestResult = tryLaunch(args)
                if (latestResult.isEmpty()) {
                    return listOf()
                } else {
                    errors.addAll(latestResult.map { r -> it.simpleName!! to r })
                }
            }
        }
    }

    return errors.ifEmpty {
        listOf(
            "Unknown" to Exception(resources.getString(R.string.unknown_launch_error, args.intent.component?.flattenToString()))
        )
    }
}

suspend fun Context.launch(type: ComponentType, extras: List<ExtraInfo>, componentKey: String, filters: List<IntentFilter>): List<Pair<String, Throwable>> {
    val args = createLaunchArgs(extras, componentKey, filters)

    return when (type) {
        ComponentType.ACTIVITY -> performLaunch<ActivityLaunchStrategy>(args)
        ComponentType.SERVICE -> performLaunch<ServiceLaunchStrategy>(args)
        ComponentType.RECEIVER -> performLaunch<ReceiverLaunchStrategy>(args)
    }
}
