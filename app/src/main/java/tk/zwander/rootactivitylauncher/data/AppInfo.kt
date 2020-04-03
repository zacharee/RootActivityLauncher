package tk.zwander.rootactivitylauncher.data

import android.content.pm.ApplicationInfo
import tk.zwander.rootactivitylauncher.adapters.ActivityAdapter

data class AppInfo(
    val info: ApplicationInfo,
    val label: CharSequence,
    val activities: List<ActivityInfo>,
    var adapter: ActivityAdapter,
    var expanded: Boolean = false
)