package tk.zwander.rootactivitylauncher.data.component

import android.content.pm.ComponentInfo
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import tk.zwander.rootactivitylauncher.util.getOrAwaitResult

open class BaseComponentInfo(
    open val info: ComponentInfo,
    open val label: Deferred<CharSequence>
) {
    val loadedLabel: CharSequence
        get() = runBlocking {
            label.getOrAwaitResult()
        }
}