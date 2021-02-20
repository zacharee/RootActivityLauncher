package tk.zwander.rootactivitylauncher.data.component

import android.content.ComponentName
import android.content.pm.ComponentInfo
import com.google.android.gms.common.internal.Objects
import tk.zwander.rootactivitylauncher.util.constructComponentKey
import java.lang.IllegalStateException

open class BaseComponentInfo(
    open val info: ComponentInfo,
    open val label: CharSequence
) : Comparable<BaseComponentInfo> {
    val component by lazy { ComponentName(info.packageName, info.name) }

    fun type(): ComponentType {
        return when(this) {
            is ActivityInfo -> ComponentType.ACTIVITY
            is ReceiverInfo -> ComponentType.RECEIVER
            is ServiceInfo -> ComponentType.SERVICE
            else -> throw IllegalStateException("Unrecognized instance ${this.javaClass.canonicalName}")
        }
    }

    override fun compareTo(other: BaseComponentInfo): Int {
        return label.toString().compareTo(other.label.toString(), true).run {
            if (this == 0) constructComponentKey(info).compareTo(constructComponentKey(other.info), true)
            else this
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is BaseComponentInfo
                && component == other.component
                && label == other.label
    }

    override fun hashCode(): Int {
        return Objects.hashCode(component, label)
    }
}