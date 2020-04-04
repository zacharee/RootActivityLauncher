package tk.zwander.rootactivitylauncher.data.component

import android.content.pm.ComponentInfo

open class BaseComponentInfo(
    open val info: ComponentInfo,
    open val label: CharSequence
)