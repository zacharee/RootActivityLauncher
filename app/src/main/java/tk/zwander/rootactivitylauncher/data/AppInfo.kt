package tk.zwander.rootactivitylauncher.data

import android.content.pm.ApplicationInfo
import tk.zwander.rootactivitylauncher.adapters.ActivityAdapter
import tk.zwander.rootactivitylauncher.adapters.ServiceAdapter

data class AppInfo(
    val info: ApplicationInfo,
    val label: CharSequence,
    val activities: List<ActivityInfo>,
    val services: List<ServiceInfo>,
    val activityAdapter: ActivityAdapter,
    val serviceAdapter: ServiceAdapter,
    var activitiesExpanded: Boolean = false,
    var servicesExpanded: Boolean = false
)