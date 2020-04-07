package tk.zwander.rootactivitylauncher.data.component

import android.content.pm.ComponentInfo
import kotlinx.coroutines.runBlocking

open class BaseComponentInfo(
    open val info: ComponentInfo,
    open val label: CharSequence
)