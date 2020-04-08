package tk.zwander.rootactivitylauncher.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageItemInfo
import tk.zwander.rootactivitylauncher.adapters.component.ActivityAdapter
import tk.zwander.rootactivitylauncher.adapters.component.ServiceAdapter
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class AppInfo(
    val info: ApplicationInfo,
    val label: CharSequence,
    val activities: List<ActivityInfo>,
    val services: List<ServiceInfo>
) {
    val activityAdapter = ActivityAdapter()
    val serviceAdapter = ServiceAdapter()

    val filteredActivities = LinkedList<ActivityInfo>()
    val filteredServices = LinkedList<ServiceInfo>()

    var activitiesExpanded: Boolean = false
    var servicesExpanded: Boolean = false
    var activiesShown: Boolean = false
    var servicesShown: Boolean = false

    internal var currentQuery: String = ""
    internal var enabledFilterMode = EnabledFilterMode.SHOW_ALL
    internal var exportedFilterMode = ExportedFilterMode.SHOW_ALL

    init {
        onFilterChange(currentQuery, enabledFilterMode, exportedFilterMode, true)
    }

    override fun equals(other: Any?): Boolean {
        return other is AppInfo
                && info.packageName == other.info.packageName
    }

    override fun hashCode(): Int {
        return info.packageName.hashCode()
    }

    fun onFilterChange(
        query: String = currentQuery,
        enabledMode: EnabledFilterMode = enabledFilterMode,
        exportedMode: ExportedFilterMode = exportedFilterMode,
        override: Boolean = false
    ) {
        if (override || currentQuery != query || enabledFilterMode != enabledMode || exportedFilterMode != exportedMode) {
            currentQuery = query
            enabledFilterMode = enabledMode
            exportedFilterMode = exportedMode

            filteredActivities.clear()
            filteredServices.clear()
            activities.filterTo(filteredActivities) { matches(it) }
            services.filterTo(filteredServices) { matches(it) }
        }
    }

    fun matches(data: BaseComponentInfo): Boolean {
        when (enabledFilterMode) {
            EnabledFilterMode.SHOW_DISABLED -> if (data.info.enabled) return false
            EnabledFilterMode.SHOW_ENABLED -> if (!data.info.enabled) return false
            else -> {
                //no-op
            }
        }

        when (exportedFilterMode) {
            ExportedFilterMode.SHOW_EXPORTED -> if (!data.info.exported) return false
            ExportedFilterMode.SHOW_UNEXPORTED -> if (data.info.exported) return false
            else -> {
                //no-op
            }
        }

        if (currentQuery.isBlank()) return true

        if (data.info.name.contains(currentQuery, true)
            || (data.label.contains(currentQuery, true))
        )
            return true

        return false
    }
}
