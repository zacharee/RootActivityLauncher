package tk.zwander.rootactivitylauncher.util

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.PrefManager

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

fun RecyclerView.removeAllItemDecorations() {
    for (i in itemDecorationCount downTo 1) {
        removeItemDecorationAt(0)
    }
}

fun constructComponentKey(packageName: String, componentName: String): String {
    return "$packageName/$componentName"
}