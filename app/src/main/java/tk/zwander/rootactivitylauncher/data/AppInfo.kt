package tk.zwander.rootactivitylauncher.data

import android.content.pm.ApplicationInfo
import tk.zwander.rootactivitylauncher.adapters.component.ActivityAdapter
import tk.zwander.rootactivitylauncher.adapters.component.ServiceAdapter
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo

data class AppInfo(
    val info: ApplicationInfo,
    val label: CharSequence,
    val activities: List<ActivityInfo>,
    val services: List<ServiceInfo>,
    val activityAdapter: ActivityAdapter,
    val serviceAdapter: ServiceAdapter
) {
    //It'd be nice to be able to use lazyDeferred here too, but it causes laggy scrolling
    val loadedLabel: CharSequence
        get() = label

    var activitiesExpanded: Boolean = false
    var servicesExpanded: Boolean = false
    var activiesShown: Boolean = false
    var servicesShown: Boolean = false

    override fun equals(other: Any?): Boolean {
        return other is AppInfo
                && info.packageName == other.info.packageName
    }

    override fun hashCode(): Int {
        return info.packageName.hashCode()
    }
}
