package tk.zwander.rootactivitylauncher.data.component

import android.content.pm.ServiceInfo
import kotlinx.coroutines.Deferred

data class ServiceInfo(
    override val info: ServiceInfo,
    override val label: CharSequence
) : BaseComponentInfo(info, label)