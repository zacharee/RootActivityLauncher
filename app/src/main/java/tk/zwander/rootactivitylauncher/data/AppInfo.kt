package tk.zwander.rootactivitylauncher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.google.android.gms.common.internal.Objects
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
import java.util.concurrent.ConcurrentLinkedDeque

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

    val filteredActivities = ConcurrentLinkedDeque<ActivityInfo>()
    val filteredServices = ConcurrentLinkedDeque<ServiceInfo>()
    val filteredReceivers = ConcurrentLinkedDeque<ReceiverInfo>()

    val activitiesSize: Int
        get() = if (!hasLoadedActivities) _activitiesSize else filteredActivities.size
    val servicesSize: Int
        get() = if (!hasLoadedServices) _servicesSize else filteredServices.size
    val receiversSize: Int
        get() = if (!hasLoadedReceivers) _receiversSize else filteredReceivers.size

    private val _loadedActivities = ConcurrentLinkedDeque<ActivityInfo>()
    private val _loadedServices = ConcurrentLinkedDeque<ServiceInfo>()
    private val _loadedReceivers = ConcurrentLinkedDeque<ReceiverInfo>()

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

    override fun equals(other: Any?): Boolean {
        return other is AppInfo
                && info.packageName == other.info.packageName
                && super.equals(other)
                && activitiesSize == other.activitiesSize
                && servicesSize == other.servicesSize
                && receiversSize == other._receiversSize
                && filteredActivities == other.filteredActivities
                && filteredServices == other.filteredServices
                && filteredReceivers == other.filteredReceivers
    }

    override fun hashCode(): Int {
        return info.packageName.hashCode() +
                31 * super.hashCode() +
                Objects.hashCode(
                    activitiesSize,
                    servicesSize,
                    receiversSize,
                    filteredActivities,
                    filteredServices,
                    filteredReceivers
                )
    }

    suspend fun onFilterChange(
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

            getLoadedActivities().apply {
                filteredActivities.clear()
                filterTo(filteredActivities) { matches(it, context) }
            }
            getLoadedServices().apply {
                filteredServices.clear()
                filterTo(filteredServices) { matches(it, context) }
            }
            getLoadedReceivers().apply {
                filteredReceivers.clear()
                filterTo(filteredReceivers) { matches(it, context) }
            }
        }
    }

    // Keep these as suspend functions
    suspend fun loadActivities() {
        if (activitiesSize > 0 && _loadedActivities.isEmpty()) {
            _loadedActivities.addAll(activitiesLoader())
            filteredActivities.addAll(_loadedActivities)
            hasLoadedActivities = true
        }
    }

    suspend fun loadServices() {
        if (servicesSize > 0 && _loadedServices.isEmpty()) {
            _loadedServices.addAll(servicesLoader())
            filteredServices.addAll(_loadedServices)
            hasLoadedServices = true
        }
    }

    suspend fun loadReceivers() {
        if (receiversSize > 0 && _loadedReceivers.isEmpty()) {
            _loadedReceivers.addAll(receiversLoader())
            filteredReceivers.addAll(_loadedReceivers)
            hasLoadedReceivers = true
        }
    }

    private suspend fun getLoadedActivities(): Collection<ActivityInfo> {
        loadActivities()

        return _loadedActivities
    }

    private suspend fun getLoadedServices(): Collection<ServiceInfo> {
        loadServices()

        return _loadedServices
    }

    private suspend fun getLoadedReceivers(): Collection<ReceiverInfo> {
        loadReceivers()

        return _loadedReceivers
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
