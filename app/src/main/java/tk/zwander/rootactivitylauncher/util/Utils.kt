package tk.zwander.rootactivitylauncher.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.activities.ShortcutLaunchActivity
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.PrefManager
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import java.lang.StringBuilder
import kotlin.coroutines.CoroutineContext

val Context.prefs: PrefManager
    get() = PrefManager.getInstance(this)

fun Context.findExtrasForComponent(activityName: String): List<ExtraInfo> {
    val extras = ArrayList<ExtraInfo>()

    prefs.extras[activityName]?.let { extras.addAll(it) }

    return extras
}

fun Context.updateExtrasForComponent(componentName: String, extras: List<ExtraInfo>) {
    val map = prefs.extras

    map[componentName] = extras
    prefs.extras = map
}

fun Context.dpToPx(dp: Number): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
}

fun Context.launchService(extras: List<ExtraInfo>, componentKey: String) {
    try {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.component = ComponentName.unflattenFromString(componentKey)

        if (extras.isNotEmpty()) extras.forEach {
            intent.putExtra(it.key, it.value)
        }

        ContextCompat.startForegroundService(this, intent)
    } catch (e: SecurityException) {
        if (Shell.SU.available()) {
            val command = StringBuilder("am startservice $componentKey")

            if (extras.isNotEmpty()) extras.forEach {
                command.append(" -e \"${it.key}\" \"${it.value}\"")
            }

            Shell.Pool.SU.run(command.toString())
        } else {
            Toast.makeText(this, R.string.requires_root, Toast.LENGTH_SHORT).show()
        }
    }
}

fun Context.launchActivity(extras: List<ExtraInfo>, componentKey: String) {
    try {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.component = ComponentName.unflattenFromString(componentKey)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (extras.isNotEmpty()) extras.forEach {
            intent.putExtra(it.key, it.value)
        }

        startActivity(intent)
    } catch (e: SecurityException) {
        if (Shell.SU.available()) {
            val command = StringBuilder("am start -n $componentKey")

            if (extras.isNotEmpty()) extras.forEach {
                command.append(" -e \"${it.key}\" \"${it.value}\"")
            }

            Shell.Pool.SU.run(command.toString())
        } else {
            Toast.makeText(this, R.string.requires_root, Toast.LENGTH_SHORT).show()
        }
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