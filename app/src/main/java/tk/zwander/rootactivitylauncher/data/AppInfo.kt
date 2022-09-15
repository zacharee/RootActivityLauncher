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
    private val activitiesLoader: suspend (suspend (Int, Int) -> Unit) -> Collection<ActivityInfo>,
    private val servicesLoader: suspend (suspend (Int, Int) -> Unit) -> Collection<ServiceInfo>,
    private val receiversLoader: suspend (suspend (Int, Int) -> Unit) -> Collection<ReceiverInfo>,
    private val _activitiesSize: Int,
    private val _servicesSize: Int,
    private val _receiversSize: Int,
    private val isForTasker: Boolean,
    private val selectionCallback: (BaseComponentInfo) -> Unit
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

    val totalUnfilteredSize: Int
        get() = _activitiesSize + _servicesSize + _receiversSize

    private val _loadedActivities = ConcurrentLinkedDeque<ActivityInfo>()
    private val _loadedServices = ConcurrentLinkedDeque<ServiceInfo>()
    private val _loadedReceivers = ConcurrentLinkedDeque<ReceiverInfo>()

    var activitiesExpanded: Boolean = false
    var servicesExpanded: Boolean = false
    var receiversExpanded: Boolean = false

    private var currentQuery: String = ""
    private var enabledFilterMode = EnabledFilterMode.SHOW_ALL
    private var exportedFilterMode = ExportedFilterMode.SHOW_ALL
    private var useRegex: Boolean = false
    private var includeComponents: Boolean = true

    var hasLoadedActivities = false
        private set
    var hasLoadedServices = false
        private set
    var hasLoadedReceivers = false
        private set

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
        progress: (Int, Int) -> Unit,
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

            val total = _activitiesSize + _servicesSize + _receiversSize
            var current = 0

            getLoadedActivities { _, _ ->
                progress(current++, total)
            }.apply {
                filteredActivities.clear()
                filterTo(filteredActivities) { matches(it, context) }
            }
            getLoadedServices { _, _ ->
                progress(current++, total)
            }.apply {
                filteredServices.clear()
                filterTo(filteredServices) { matches(it, context) }
            }
            getLoadedReceivers { _, _ ->
                progress(current++, total)
            }.apply {
                filteredReceivers.clear()
                filterTo(filteredReceivers) { matches(it, context) }
            }
        }
    }

    // Keep these as suspend functions
    suspend fun loadActivities(progress: suspend (Int, Int) -> Unit) {
        if (activitiesSize > 0 && _loadedActivities.isEmpty()) {
            _loadedActivities.addAll(activitiesLoader(progress))
            filteredActivities.addAll(_loadedActivities)
            hasLoadedActivities = true
        }
    }

    suspend fun loadServices(progress: suspend (Int, Int) -> Unit) {
        if (servicesSize > 0 && _loadedServices.isEmpty()) {
            _loadedServices.addAll(servicesLoader(progress))
            filteredServices.addAll(_loadedServices)
            hasLoadedServices = true
        }
    }

    suspend fun loadReceivers(progress: suspend (Int, Int) -> Unit) {
        if (receiversSize > 0 && _loadedReceivers.isEmpty()) {
            _loadedReceivers.addAll(receiversLoader(progress))
            filteredReceivers.addAll(_loadedReceivers)
            hasLoadedReceivers = true
        }
    }

    private suspend fun getLoadedActivities(progress: (Int, Int) -> Unit): Collection<ActivityInfo> {
        loadActivities(progress)

        return _loadedActivities
    }

    private suspend fun getLoadedServices(progress: (Int, Int) -> Unit): Collection<ServiceInfo> {
        loadServices(progress)

        return _loadedServices
    }

    private suspend fun getLoadedReceivers(progress: (Int, Int) -> Unit): Collection<ReceiverInfo> {
        loadReceivers(progress)

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
