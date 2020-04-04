package tk.zwander.rootactivitylauncher.data.component

import android.content.pm.ComponentInfo

open class BaseComponentInfo(
    open val info: ComponentInfo,
    open val label: CharSequence
) : Comparable<BaseComponentInfo> {
    override fun compareTo(other: BaseComponentInfo): Int {
        return label.toString().compareTo(other.label.toString(), ignoreCase = true)
    }
}