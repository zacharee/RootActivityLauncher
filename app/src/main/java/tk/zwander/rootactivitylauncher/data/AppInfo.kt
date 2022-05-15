package tk.zwander.rootactivitylauncher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import tk.zwander.rootactivitylauncher.adapters.component.ActivityAdapter
import tk.zwander.rootactivitylauncher.adapters.component.ReceiverAdapter
import tk.zwander.rootactivitylauncher.adapters.component.ServiceAdapter
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ReceiverInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.util.AdvancedSearcher
import tk.zwander.rootactivitylauncher.util.isActuallyEnabled
import tk.zwander.rootactivitylauncher.util.isValidRegex
import kotlin.collections.ArrayList

data class AppInfo(
    val pInfo: PackageInfo,
    val info: ApplicationInfo = pInfo.applicationInfo,
    val label: CharSequence,
    private val activitiesLoader: () -> Collection<ActivityInfo>,
    private val servicesLoader: () -> Collection<ServiceInfo>,
    private val receiversLoader: () -> Collection<ReceiverInfo>,
    private val _activitiesSize: Int,
    private val _servicesSize: Int,
    private val _receiversSize: Int,
    val isForTasker: Boolean,
    val selectionCallback: (BaseComponentInfo) -> Unit
) {
    val activityAdapter = ActivityAdapter(isForTasker, selectionCallback)
    val serviceAdapter = ServiceAdapter(isForTasker, selectionCallback)
    val receiverAdapter = ReceiverAdapter(isForTasker, selectionCallback)

    val filteredActivities = ArrayList<ActivityInfo>(_activitiesSize)
    val filteredServices = ArrayList<ServiceInfo>(_servicesSize)
    val filteredReceivers = ArrayList<ReceiverInfo>(_receiversSize)

    val activitiesSize: Int
        get() = if (!hasLoadedActivities) _activitiesSize else filteredActivities.size
    val servicesSize: Int
        get() = if (!hasLoadedServices) _servicesSize else filteredServices.size
    val receiversSize: Int
        get() = if (!hasLoadedReceivers) _receiversSize else filteredReceivers.size

    val activities: Collection<ActivityInfo>
        get() {
            if (_activitiesSize > 0 && _loadedActivities.isEmpty()) {
                _loadedActivities.addAll(activitiesLoader())
                filteredActivities.addAll(_loadedActivities)
                hasLoadedActivities = true
            }

            return _loadedActivities
        }
    val services: Collection<ServiceInfo>
        get() {
            if (_servicesSize > 0 && _loadedServices.isEmpty()) {
                _loadedServices.addAll(servicesLoader())
                filteredServices.addAll(_loadedServices)
                hasLoadedServices = true
            }

            return _loadedServices
        }
    val receivers: Collection<ReceiverInfo>
        get() {
            if (_receiversSize > 0 && _loadedReceivers.isEmpty()) {
                _loadedReceivers.addAll(receiversLoader())
                filteredReceivers.addAll(_loadedReceivers)
                hasLoadedReceivers = true
            }

            return _loadedReceivers
        }

    private val _loadedActivities = arrayListOf<ActivityInfo>()
    private val _loadedServices = arrayListOf<ServiceInfo>()
    private val _loadedReceivers = arrayListOf<ReceiverInfo>()

    var activitiesExpanded: Boolean = false
    var servicesExpanded: Boolean = false
    var receiversExpanded: Boolean = false

    internal var currentQuery: String = ""
    internal var enabledFilterMode = EnabledFilterMode.SHOW_ALL
    internal var exportedFilterMode = ExportedFilterMode.SHOW_ALL
    internal var useRegex: Boolean = false
    internal var includeComponents: Boolean = true

    private var hasLoadedActivities = false
    private var hasLoadedServices = false
    private var hasLoadedReceivers = false

    init {
//        filteredActivities.addAll(activities)
//        filteredServices.addAll(services)
//        filteredReceivers.addAll(receivers)
    }

    override fun equals(other: Any?): Boolean {
        return other is AppInfo
                && info.packageName == other.info.packageName
    }

    override fun hashCode(): Int {
        return info.packageName.hashCode()
    }

    fun onFilterChange(
        context: Context,
        query: String = currentQuery,
        useRegex: Boolean = this.useRegex,
        includeComponents: Boolean = this.includeComponents,
        enabledMode: EnabledFilterMode = enabledFilterMode,
        exportedMode: ExportedFilterMode = exportedFilterMode,
        override: Boolean = false,
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
            activities.filterTo(filteredActivities) { matches(it, context) }
            services.filterTo(filteredServices) { matches(it, context) }
            receivers.filterTo(filteredReceivers) { matches(it, context) }
        }
    }

    private fun matches(data: BaseComponentInfo, context: Context): Boolean {
        when (enabledFilterMode) {
            EnabledFilterMode.SHOW_DISABLED -> if (data.info.isActuallyEnabled(context)) return false
            EnabledFilterMode.SHOW_ENABLED -> if (!data.info.isActuallyEnabled(context)) return false
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

        val advancedMatch = AdvancedSearcher.matchesRequiresPermission(currentQuery, data.info)

        if (useRegex && currentQuery.isValidRegex()) {
            if (Regex(currentQuery).run {
                    containsMatchIn(data.info.name)
                            || containsMatchIn(data.label)
                } || advancedMatch) {
                return true
            }
        } else {
            if (data.info.name.contains(currentQuery, true)
                || (data.label.contains(currentQuery, true))
                || advancedMatch
            ) {
                return true
            }
        }

        return false
    }
}
