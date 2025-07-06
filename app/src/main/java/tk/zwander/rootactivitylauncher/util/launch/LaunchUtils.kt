package tk.zwander.rootactivitylauncher.util.launch

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.data.prefs
import tk.zwander.rootactivitylauncher.util.findActionForComponent
import tk.zwander.rootactivitylauncher.util.findCategoriesForComponent
import tk.zwander.rootactivitylauncher.util.findDataForComponent
import tk.zwander.rootactivitylauncher.util.getAllIntentFiltersCompat

val launchStrategiesMap by lazy {
    mapOf(
        ComponentType.ACTIVITY to ActivityLaunchStrategy::class.sealedSubclasses
            .mapNotNull { it.objectInstance?.let { obj -> obj to it } },
        ComponentType.SERVICE to ServiceLaunchStrategy::class.sealedSubclasses
            .mapNotNull { it.objectInstance?.let { obj -> obj to it } },
        ComponentType.RECEIVER to ReceiverLaunchStrategy::class.sealedSubclasses
            .mapNotNull { it.objectInstance?.let { obj -> obj to it } },
    )
}

fun Context.createLaunchArgs(extras: List<ExtraInfo>, componentKey: String): LaunchArgs {
    val intent = Intent(prefs.findActionForComponent(componentKey))
    intent.component = ComponentName.unflattenFromString(componentKey)
    intent.data = prefs.findDataForComponent(componentKey)?.toUri()

    val filters = packageManager.getAllIntentFiltersCompat(intent.component?.packageName)

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

private suspend inline fun Context.performLaunch(args: LaunchArgs, type: ComponentType, strategy: LaunchStrategy? = null): List<Pair<String, Throwable>> {
    val errors = mutableListOf<Pair<String, Throwable>>()

    val strategies = if (strategy != null) {
        listOf(strategy to strategy::class)
    } else {
        launchStrategiesMap[type]
            ?.sortedByDescending { (obj, _) -> obj.priority }
    }

    strategies?.forEach { (obj, clazz) ->
        with (obj) {
            if (canRun(args)) {
                val latestResult = tryLaunch(args)

                Log.e("RootActivityLauncher", "$clazz $latestResult")

                if (latestResult.isEmpty()) {
                    return listOf()
                } else {
                    errors.addAll(latestResult.map { r -> resources.getString(obj.labelRes) to r })
                }
            }
        }
    }

    return errors.ifEmpty {
        listOf(
            "Unknown" to Exception(
                resources.getString(
                    R.string.unknown_launch_error,
                    args.intent.component?.flattenToString()
                )
            )
        )
    }
}

suspend fun Context.launch(
    type: ComponentType,
    extras: List<ExtraInfo>,
    componentKey: String,
    strategy: LaunchStrategy? = null,
    launchArgs: LaunchArgs = createLaunchArgs(extras, componentKey),
): List<Pair<String, Throwable>> {
    return performLaunch(launchArgs, type, strategy)
}
