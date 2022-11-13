package tk.zwander.rootactivitylauncher.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ComponentInfo
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.prefs

fun determineComponentNamePackage(componentName: String): String {
    val component = ComponentName.unflattenFromString(componentName)

    return if (component != null) component.packageName else componentName
}

fun Context.findExtrasForComponent(componentName: String): List<ExtraInfo> {
    val extras = ArrayList<ExtraInfo>()

    prefs.extras[componentName]?.let { extras.addAll(it) }

    return extras
}

fun Context.updateExtrasForComponent(componentName: String, extras: List<ExtraInfo>) {
    val map = prefs.extras

    map[componentName] = extras
    prefs.extras = map
}

fun Context.findActionForComponent(componentName: String): String {
    val pkg = determineComponentNamePackage(componentName)

    return prefs.actions[componentName] ?: (prefs.actions[pkg] ?: Intent.ACTION_MAIN)
}

fun Context.updateActionForComponent(componentName: String, action: String?) {
    val map = prefs.actions

    map[componentName] = if (action.isNullOrBlank()) null else action
    prefs.actions = map
}

fun Context.findDataForComponent(componentName: String): String? {
    val pkg = determineComponentNamePackage(componentName)

    return prefs.datas[componentName] ?: prefs.datas[pkg]
}

fun Context.updateDataForComponent(componentName: String, data: String?) {
    val map = prefs.datas

    map[componentName] = if (data.isNullOrBlank()) null else data
    prefs.datas = map
}

fun Context.findCategoriesForComponent(componentName: String): MutableList<String?> {
    val pkg = determineComponentNamePackage(componentName)

    return prefs.categories[componentName] ?: prefs.categories[pkg] ?: mutableListOf()
}

fun Context.updateCategoriesForComponent(componentName: String, categories: List<String?>) {
    val filtered = categories.filterNot { it.isNullOrBlank() }
    val map = prefs.categories

    map[componentName] = ArrayList(filtered)
    prefs.categories = map
}

fun constructComponentKey(component: PackageItemInfo): String {
    return constructComponentKey(component.packageName, component.name)
}

fun constructComponentKey(packageName: String, componentName: String): String {
    return "$packageName/$componentName"
}

fun Context.openAppInfo(packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.parse("package:$packageName")

    try {
        startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(this, resources.getString(R.string.unable_to_launch, e.localizedMessage), Toast.LENGTH_SHORT).show()
    }
}

fun ActivityInfo.persistableModeToString(): String {
    return when (persistableMode) {
        ActivityInfo.PERSIST_ROOT_ONLY -> "PERSIST_ROOT_ONLY"
        ActivityInfo.PERSIST_NEVER -> "PERSIST_NEVER"
        ActivityInfo.PERSIST_ACROSS_REBOOTS -> "PERSIST_ACROSS_REBOOTS"
        else -> "UNKNOWN=$persistableMode"
    }
}

val ActivityInfo.manifestMinAspectRatioCompat: Float
    @SuppressLint("PrivateApi")
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        manifestMinAspectRatio
    } else {
        ActivityInfo::class.java
            .getDeclaredField("mMinAspectRatio")
            .apply { isAccessible = true }
            .getFloat(this)
    }

val ActivityInfo.rMaxAspectRatio: Float
    @SuppressLint("PrivateApi")
    get() = ActivityInfo::class.java
        .getDeclaredField("mMaxAspectRatio")
        .apply { isAccessible = true }
        .getFloat(this)

fun ComponentInfo.isActuallyEnabled(context: Context): Boolean {
    return applicationInfo.isActuallyEnabled(context) && try {
        checkEnabledSetting(
            context.packageManager.getComponentEnabledSetting(safeComponentName),
            enabled
        )
    } catch (e: Exception) {
        enabled
    }
}

fun ApplicationInfo.isActuallyEnabled(context: Context): Boolean {
    return try {
        checkEnabledSetting(
            context.packageManager.getApplicationEnabledSetting(packageName),
            enabled
        )
    } catch (e: Exception) {
        enabled
    }
}

private fun checkEnabledSetting(setting: Int, default: Boolean): Boolean {
    return when (setting) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> default
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER -> false
        else -> false
    }
}

val ComponentInfo.safeComponentName: ComponentName
    get() = ComponentName(packageName, name)
