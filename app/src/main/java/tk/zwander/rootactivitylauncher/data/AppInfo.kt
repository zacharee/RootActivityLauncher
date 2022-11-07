package tk.zwander.rootactivitylauncher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

data class AppInfo(
    val pInfo: PackageInfo,
    val info: ApplicationInfo = pInfo.applicationInfo,
    val label: CharSequence,
    private val activitiesLoader: ((Int, Int) -> Unit) -> Collection<ActivityInfo>,
    private val servicesLoader: ((Int, Int) -> Unit) -> Collection<ServiceInfo>,
    private val receiversLoader: ((Int, Int) -> Unit) -> Collection<ReceiverInfo>,
    private val _activitiesSize: Int,
    private val _servicesSize: Int,
    private val _receiversSize: Int,
    private val isForTasker: Boolean,
    private val context: Context,
    private val selectionCallback: (BaseComponentInfo) -> Unit
) {
    val activityAdapter = ActivityAdapter(isForTasker, selectionCallback)
    val serviceAdapter = ServiceAdapter(isForTasker, selectionCallback)
    val receiverAdapter = ReceiverAdapter(isForTasker, selectionCallback)

    val filteredActivities = mutableStateListOf<ActivityInfo>()
    val filteredServices = mutableStateListOf<ServiceInfo>()
    val filteredReceivers = mutableStateListOf<ReceiverInfo>()

    val activitiesSize: Int by derivedStateOf {
        if (!hasLoadedActivities) _activitiesSize else filteredActivities.size
    }
    val servicesSize: Int by derivedStateOf {
        if (!hasLoadedServices) _servicesSize else filteredServices.size
    }
    val receiversSize: Int by derivedStateOf {
        if (!hasLoadedReceivers) _receiversSize else filteredReceivers.size
    }

    val totalUnfilteredSize: Int by derivedStateOf {
        _activitiesSize + _servicesSize + _receiversSize
    }

    private val _loadedActivities = mutableStateListOf<ActivityInfo>()
    private val _loadedServices = mutableStateListOf<ServiceInfo>()
    private val _loadedReceivers = mutableStateListOf<ReceiverInfo>()

    var activitiesExpanded: Boolean by mutableStateOf(false)
    var servicesExpanded: Boolean by mutableStateOf(false)
    var receiversExpanded: Boolean by mutableStateOf(false)

    var hasLoadedActivities by mutableStateOf(false)
        private set
    var hasLoadedServices by mutableStateOf(false)
        private set
    var hasLoadedReceivers by mutableStateOf(false)
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

    fun onFilterChange() {
        val total = _activitiesSize + _servicesSize + _receiversSize
        var current = 0

        val activities = getLoadedActivities { _, _ ->
            MainModel.progress = (current++ / total.toFloat() * 100).toInt()
        }
        val services = getLoadedServices { _, _ ->
            MainModel.progress = (current++ / total.toFloat() * 100).toInt()
        }
        val receivers = getLoadedReceivers { _, _ ->
            MainModel.progress = (current++ / total.toFloat() * 100).toInt()
        }

        filteredActivities.clear()
        filteredActivities.addAll(
            activities.filter { matches(it, context) }
                .sortedBy { it.label.toString().lowercase() }
        )

        filteredServices.clear()
        filteredServices.addAll(
            services.filter { matches(it, context) }
                .sortedBy { it.label.toString().lowercase() }
        )

        filteredReceivers.clear()
        filteredReceivers.addAll(
            receivers.filter { matches(it, context) }
                .sortedBy { it.label.toString().lowercase() }
        )
    }

//    suspend fun onFilterChange(
//        override: Boolean = false,
//        progress: (Int, Int) -> Unit,
//    ) {
//        if (override
//            || currentQuery != query
//            || enabledFilterMode != enabledMode
//            || exportedFilterMode != exportedMode
//            || permissionFilterMode != permissionMode
//            || this.useRegex != useRegex
//            || this.includeComponents != includeComponents
//        ) {
//            currentQuery = query
//            enabledFilterMode = enabledMode
//            exportedFilterMode = exportedMode
//            permissionFilterMode = permissionMode
//            this.useRegex = useRegex
//            this.includeComponents = includeComponents
//        }
//    }

    fun loadActivities(progress: (Int, Int) -> Unit) {
        if (activitiesSize > 0 && _loadedActivities.isEmpty()) {
            _loadedActivities.addAll(activitiesLoader(progress))
            hasLoadedActivities = true
        }
    }

    fun loadServices(progress: (Int, Int) -> Unit) {
        if (servicesSize > 0 && _loadedServices.isEmpty()) {
            _loadedServices.addAll(servicesLoader(progress))
            hasLoadedServices = true
        }
    }

    fun loadReceivers(progress: (Int, Int) -> Unit) {
        if (receiversSize > 0 && _loadedReceivers.isEmpty()) {
            _loadedReceivers.addAll(receiversLoader(progress))
            hasLoadedReceivers = true
        }
    }

    private fun getLoadedActivities(progress: (Int, Int) -> Unit): Collection<ActivityInfo> {
        loadActivities(progress)

        return _loadedActivities
    }

    private fun getLoadedServices(progress: (Int, Int) -> Unit): Collection<ServiceInfo> {
        loadServices(progress)

        return _loadedServices
    }

    private fun getLoadedReceivers(progress: (Int, Int) -> Unit): Collection<ReceiverInfo> {
        loadReceivers(progress)

        return _loadedReceivers
    }

    fun matches(data: BaseComponentInfo, context: Context): Boolean {
        when (MainModel.enabledFilterMode) {
            EnabledFilterMode.SHOW_DISABLED -> if (data.info.isActuallyEnabled(context)) return false
            EnabledFilterMode.SHOW_ENABLED -> if (!data.info.isActuallyEnabled(context)) return false
            else -> {
                //no-op
            }
        }

        when (MainModel.exportedFilterMode) {
            ExportedFilterMode.SHOW_EXPORTED -> if (!data.info.exported) return false
            ExportedFilterMode.SHOW_UNEXPORTED -> if (data.info.exported) return false
            else -> {
                //no-op
            }
        }

        val permission = when (data) {
            is ActivityInfo -> data.info.permission
            is ServiceInfo -> data.info.permission
            else -> null
        }

        when (MainModel.permissionFilterMode) {
            PermissionFilterMode.SHOW_REQUIRES_NO_PERMISSION -> if (!permission.isNullOrBlank()) return false
            PermissionFilterMode.SHOW_REQUIRES_PERMISSION -> if (permission.isNullOrBlank()) return false
            else -> {
                //no-op
            }
        }

        if (MainModel.query.isBlank() || !MainModel.includeComponents) return true

        val advancedMatch = AdvancedSearcher.matchesRequiresPermission(MainModel.query, data.info)

        if (MainModel.useRegex && MainModel.query.isValidRegex()) {
            if (Regex(MainModel.query).run {
                    containsMatchIn(data.info.name)
                            || containsMatchIn(data.label)
                } || advancedMatch) {
                return true
            }
        } else {
            if (data.info.name.contains(MainModel.query, true)
                || (data.label.contains(MainModel.query, true))
                || advancedMatch
            ) {
                return true
            }
        }

        return false
    }
}
