package tk.zwander.rootactivitylauncher.util

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.PrefManager

val Context.prefs: PrefManager
    get() = PrefManager.getInstance(this)

fun Context.findExtrasForActivity(activityName: String): List<ExtraInfo> {
    val extras = ArrayList<ExtraInfo>()

    prefs.extras[activityName]?.let { extras.addAll(it) }

    return extras
}

fun Context.updateExtrasForActivity(activityName: String, extras: List<ExtraInfo>) {
    val map = prefs.extras

    map[activityName] = extras

    prefs.extras = map
}

fun RecyclerView.removeAllItemDecorations() {
    for (i in itemDecorationCount downTo 1) {
        removeItemDecorationAt(0)
    }
}

fun constructActivityKey(packageName: String, activityName: String): String {
    return "$packageName/$activityName"
}