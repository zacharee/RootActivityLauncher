package tk.zwander.rootactivitylauncher.data

import android.content.pm.ApplicationInfo
import tk.zwander.rootactivitylauncher.adapters.component.ActivityAdapter
import tk.zwander.rootactivitylauncher.adapters.component.ServiceAdapter
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.util.isValidRegex
import kotlin.collections.ArrayList

data class AppInfo(
    val info: ApplicationInfo,
    val label: CharSequence,
    val activities: List<ActivityInfo>,
    val services: List<ServiceInfo>
) {
    val activityAdapter = ActivityAdapter()
    val serviceAdapter = ServiceAdapter()

    val filteredActivities = ArrayList<ActivityInfo>(activities.size)
    val filteredServices = ArrayList<ServiceInfo>(services.size)

    var activitiesExpanded: Boolean = false
    var servicesExpanded: Boolean = false

    internal var currentQuery: String = ""
    internal var enabledFilterMode = EnabledFilterMode.SHOW_ALL
    internal var exportedFilterMode = ExportedFilterMode.SHOW_ALL
    internal var useRegex: Boolean = false

    init {
        onFilterChange(
            override = true
        )
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
        useRegex: Boolean = this.useRegex,
        enabledMode: EnabledFilterMode = enabledFilterMode,
        exportedMode: ExportedFilterMode = exportedFilterMode,
        override: Boolean = false
    ) {
        if (override
            || currentQuery != query
            || enabledFilterMode != enabledMode
            || exportedFilterMode != exportedMode
            || this.useRegex != useRegex
        ) {
            currentQuery = query
            enabledFilterMode = enabledMode
            exportedFilterMode = exportedMode
            this.useRegex = useRegex

            filteredActivities.clear()
            filteredServices.clear()
            activities.filterTo(filteredActivities) { matches(it) }
            services.filterTo(filteredServices) { matches(it) }
        }
    }

    private fun matches(data: BaseComponentInfo): Boolean {
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

        if (useRegex && currentQuery.isValidRegex()) {
            if (Regex(currentQuery).run {
                    containsMatchIn(data.info.name)
                            || containsMatchIn(data.label)
                }) {
                return true
            }
        } else {
            if (data.info.name.contains(currentQuery, true)
                || (data.label.contains(currentQuery, true))
            ) {
                return true
            }
        }

        return false
    }
}
