package tk.zwander.rootactivitylauncher.util

import android.app.Activity
import android.app.IActivityManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.provider.Settings
import android.util.TypedValue
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.squareup.picasso.Picasso
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.SystemServiceHelper
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.activities.ShortcutLaunchActivity
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.PrefManager
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import java.util.regex.PatternSyntaxException
import kotlin.coroutines.CoroutineContext


val Context.prefs: PrefManager
    get() = PrefManager.getInstance(this)

val picasso: Picasso
    get() = Picasso.get()

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
    return prefs.actions[componentName] ?: Intent.ACTION_MAIN
}

fun Context.updateActionForComponent(componentName: String, action: String?) {
    val map = prefs.actions

    map[componentName] = if (action.isNullOrBlank()) null else action
    prefs.actions = map
}

fun Context.dpToPx(dp: Number): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
}

fun Context.launchService(extras: List<ExtraInfo>, componentKey: String) {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.component = ComponentName.unflattenFromString(componentKey)

    if (extras.isNotEmpty()) extras.forEach {
        intent.putExtra(it.key, it.value)
    }

    fun onException() {
        if (Shell.SU.available() || (Shizuku.pingBinder() && hasShizukuPermission)) {
            if (Shell.SU.available()) {
                val command = StringBuilder("am startservice $componentKey")

                if (extras.isNotEmpty()) extras.forEach {
                    command.append(" -e \"${it.key}\" \"${it.value}\"")
                }

                Shell.Pool.SU.run(command.toString())
            } else {
                try {
                    val iam = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(
                            SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))

                    iam.startService(
                            null, intent, null, false, "com.android.shell",
                            null, UserHandle.USER_CURRENT
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.requires_root, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, R.string.requires_root, Toast.LENGTH_SHORT).show()
        }
    }

    try {
        ContextCompat.startForegroundService(this, intent)
    } catch (e: SecurityException) {
        onException()
    }
}

fun Context.launchActivity(extras: List<ExtraInfo>, componentKey: String) {
    val intent = Intent(Intent.ACTION_MAIN)
    intent.component = ComponentName.unflattenFromString(componentKey)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    if (extras.isNotEmpty()) extras.forEach {
        intent.putExtra(it.key, it.value)
    }

    fun onException() {
        if (Shell.SU.available() || (Shizuku.pingBinder() && hasShizukuPermission)) {
            if (Shell.SU.available()) {
                val command = StringBuilder("am start -n $componentKey")

                if (extras.isNotEmpty()) extras.forEach {
                    command.append(" -e \"${it.key}\" \"${it.value}\"")
                }

                Shell.Pool.SU.run(command.toString())
            } else {
                try {
                    val iam = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(
                            SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))

                    iam.startActivity(
                            null, "com.android.shell", intent,
                            null, null, null, 0, 0,
                            null, null
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.requires_root, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, R.string.requires_root, Toast.LENGTH_SHORT).show()
        }
    }

    try {
        startActivity(intent)
    } catch (e: SecurityException) {
        onException()
    } catch (e: ActivityNotFoundException) {
        onException()
    }
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

    startActivity(intent)
}

fun Context.launchUrl(url: String) {
    try {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    } catch (e: Exception) {}
}

fun Context.launchEmail(to: String, subject: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.type = "text/plain"
        intent.data = Uri.parse("mailto:${Uri.encode(to)}?subject=${Uri.encode(subject)}")

        startActivity(intent)
    } catch (e: Exception) {}
}

fun constructComponentKey(component: PackageItemInfo): String {
    return constructComponentKey(component.packageName, component.name)
}

fun constructComponentKey(packageName: String, componentName: String): String {
    return "$packageName/$componentName"
}

fun <T> CoroutineScope.lazyDeferred(
    context: CoroutineContext = Dispatchers.IO,
    block: suspend CoroutineScope.() -> T
): Lazy<Deferred<T>> {
    return lazy {
        async(context = context, start = CoroutineStart.LAZY) {
            block(this)
        }
    }
}

@ExperimentalCoroutinesApi
suspend fun <T> Deferred<T>.getOrAwaitResult() = if (isCompleted) getCompleted() else await()

fun <T> Collection<T>.forEachParallelBlocking(context: CoroutineContext = Dispatchers.IO, block: suspend CoroutineScope.(T) -> Unit) = runBlocking {
    forEachParallel(context, block)
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

suspend fun <T> Array<T>.forEachParallel(context: CoroutineContext = Dispatchers.IO, block: suspend CoroutineScope.(T) -> Unit) = coroutineScope {
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }

        return if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            checkCallingOrSelfPermission(ShizukuProvider.PERMISSION) == PackageManager.PERMISSION_GRANTED
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

fun Activity.requestShizukuPermission(code: Int) {
    if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            requestPermissions(arrayOf(ShizukuProvider.PERMISSION), code)
        }
    } else {
        Shizuku.requestPermission(code)
    }
}