package tk.zwander.rootactivitylauncher.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import tk.zwander.rootactivitylauncher.adapters.component.ActivityAdapter
import tk.zwander.rootactivitylauncher.adapters.component.ReceiverAdapter
import tk.zwander.rootactivitylauncher.adapters.component.ServiceAdapter
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ReceiverInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.util.isValidRegex
import kotlin.collections.ArrayList

data class AppInfo(
    val pInfo: PackageInfo,
    val info: ApplicationInfo = pInfo.applicationInfo,
    val label: CharSequence,
    val activities: Collection<ActivityInfo>,
    val services: Collection<ServiceInfo>,
    val receivers: Collection<ReceiverInfo>,
    val isForTasker: Boolean,
    val selectionCallback: (BaseComponentInfo) -> Unit
) {
    val activityAdapter = ActivityAdapter(isForTasker, selectionCallback)
    val serviceAdapter = ServiceAdapter(isForTasker, selectionCallback)
    val receiverAdapter = ReceiverAdapter(isForTasker, selectionCallback)

    val filteredActivities = ArrayList<ActivityInfo>(activities.size)
    val filteredServices = ArrayList<ServiceInfo>(services.size)
    val filteredReceivers = ArrayList<ReceiverInfo>(receivers.size)

    var activitiesExpanded: Boolean = false
    var servicesExpanded: Boolean = false
    var receiversExpanded: Boolean = false

    internal var currentQuery: String = ""
    internal var enabledFilterMode = EnabledFilterMode.SHOW_ALL
    internal var exportedFilterMode = ExportedFilterMode.SHOW_ALL
    internal var useRegex: Boolean = false
    internal var includeComponents: Boolean = true

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
        includeComponents: Boolean = this.includeComponents,
        enabledMode: EnabledFilterMode = enabledFilterMode,
        exportedMode: ExportedFilterMode = exportedFilterMode,
        override: Boolean = false
    ) {
        if (override
            || currentQuery != query
            || enabledFilterMode != enabledMode
            || exportedFilterMode != exportedMode
            || this.useRegex != useRegex
            || this.includeComponents != includeComponents
        ) {
            currentQuery = query
            enabledFilterMode = enabledMode
            exportedFilterMode = exportedMode
            this.useRegex = useRegex
            this.includeComponents = includeComponents

            filteredActivities.clear()
            filteredServices.clear()
            filteredReceivers.clear()
            activities.filterTo(filteredActivities) { matches(it) }
            services.filterTo(filteredServices) { matches(it) }
            receivers.filterTo(filteredReceivers) { matches(it) }
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

        if (currentQuery.isBlank() || !includeComponents) return true

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
