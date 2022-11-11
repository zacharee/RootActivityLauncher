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
import android.util.SparseArray
import android.util.TypedValue
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.util.forEach
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.activities.ShortcutLaunchActivity
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.PrefManager
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.regex.PatternSyntaxException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

inline fun <T, R> SparseArray<out T>.map(transform: (T) -> R): List<R> {
    return mapTo(ArrayList(), transform)
}

inline fun <T, R, C : MutableCollection<in R>> SparseArray<out T>.mapTo(destination: C, transform: (T) -> R): C {
    forEach { _, any ->
        destination.add(transform(any))
    }

    return destination
}

val Int.hexString: String
    get() = Integer.toHexString(this)

val Context.prefs: PrefManager
    get() = PrefManager.getInstance(this)

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

fun Context.createShortcut(
    label: CharSequence,
    icon: IconCompat,
    componentKey: String,
    componentType: ComponentType
) {
    val shortcut = Intent(this, ShortcutLaunchActivity::class.java)
    shortcut.action = Intent.ACTION_MAIN
    shortcut.putExtra(ShortcutLaunchActivity.EXTRA_COMPONENT_KEY, componentKey)
    shortcut.putExtra(ShortcutLaunchActivity.EXTRA_COMPONENT_TYPE, componentType.serialize())

    val info = ShortcutInfoCompat.Builder(this, componentKey)
        .setIcon(icon)
        .setShortLabel(label)
        .setLongLabel("$label: $componentKey")
        .setIntent(shortcut)
        .build()

    ShortcutManagerCompat.requestPinShortcut(
        this,
        info,
        null
    )
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

fun Context.launchUrl(url: String) {
    try {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    } catch (e: Exception) {
        Toast.makeText(this, resources.getString(R.string.unable_to_launch, e.localizedMessage), Toast.LENGTH_SHORT).show()
    }
}

fun constructComponentKey(component: PackageItemInfo): String {
    return constructComponentKey(component.packageName, component.name)
}

fun constructComponentKey(packageName: String, componentName: String): String {
    return "$packageName/$componentName"
}

suspend fun <T> Collection<T>.forEachParallel(context: CoroutineContext = Dispatchers.IO, block: suspend CoroutineScope.(T) -> Unit) = coroutineScope {
    val jobs = ArrayList<Deferred<*>>(size)
    forEach {
        jobs.add(
            async(context) {
                block(it)
            }
        )
    }
    jobs.awaitAll()
}

fun String.isValidRegex(): Boolean {
    return try {
        Regex(this)
        true
    } catch (e: PatternSyntaxException) {
        false
    }
}

val Context.hasShizukuPermission: Boolean
    get() {
        if (!Shizuku.pingBinder()) {
            return false
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        return try {
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                checkCallingOrSelfPermission(ShizukuProvider.PERMISSION) == PackageManager.PERMISSION_GRANTED
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: IllegalStateException) {
            false
        }
    }

suspend fun requestShizukuPermission(): Boolean {
    return suspendCoroutine { coroutine ->
        requestShizukuPermission { granted ->
            coroutine.resume(granted)
        }
    }
}

fun requestShizukuPermission(resultListener: (Boolean) -> Unit): Boolean {
    return if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
        false
    } else {
        val code = Random(System.currentTimeMillis()).nextInt()
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                resultListener(grantResult == PackageManager.PERMISSION_GRANTED)
                Shizuku.removeRequestPermissionResultListener(this)
            }
        }

        try {
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(code)
            true
        } catch (e: IllegalStateException) {
            false
        }
    }
}

fun Context.showRootToast() {
    try {
        Toast.makeText(this, R.string.requires_root, Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {}
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

val Context.isTouchWiz: Boolean
    get() = packageManager.hasSystemFeature("com.samsung.feature.samsung_experience_mobile")

val ComponentInfo.safeComponentName: ComponentName
    get() = ComponentName(packageName, name)

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

@Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
suspend inline fun <reified Loaded : BaseComponentInfo, reified Input : PackageItemInfo> Array<Input>?.loadItems(
    pm: PackageManager,
    appLabel: CharSequence,
    progress: suspend (Int, Int) -> Unit,
    constructor: (Input, CharSequence) -> Loaded
): Collection<Loaded> {
    val infos = ConcurrentLinkedDeque<Loaded>()

    this?.forEachIndexed { index, input ->
        val label = input.loadLabel(pm).ifBlank { appLabel }

        infos.add(constructor(input, label))
        progress(index, size)
    }

    return infos
}
