package tk.zwander.rootactivitylauncher.util

import android.content.Context
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.PrefManager
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