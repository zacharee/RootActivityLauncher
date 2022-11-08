package tk.zwander.rootactivitylauncher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.common.internal.Objects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ReceiverInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.util.AdvancedSearcher
import tk.zwander.rootactivitylauncher.util.isActuallyEnabled
import tk.zwander.rootactivitylauncher.util.isValidRegex
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

data class AppInfo(
    val pInfo: PackageInfo,
    val info: ApplicationInfo = pInfo.applicationInfo,
    val label: CharSequence,
    private val activitiesLoader: suspend (progress: (Int, Int) -> Unit) -> Collection<ActivityInfo>,
    private val servicesLoader: suspend (progress: (Int, Int) -> Unit) -> Collection<ServiceInfo>,
    private val receiversLoader: suspend (progress: (Int, Int) -> Unit) -> Collection<ReceiverInfo>,
    val _activitiesSize: Int,
    val _servicesSize: Int,
    val _receiversSize: Int,
    private val isForTasker: Boolean,
    private val context: Context
) {
    var filteredActivities by mutableStateOf<List<ActivityInfo>>(listOf())
    var filteredServices by mutableStateOf<List<ServiceInfo>>(listOf())
    var filteredReceivers by mutableStateOf<List<ReceiverInfo>>(listOf())

    val safeFilteredActivities by derivedStateOf {
        if (activitiesExpanded) filteredActivities else listOf()
    }
    val safeFilteredServices by derivedStateOf {
        if (servicesExpanded) filteredServices else listOf()
    }
    val safeFilteredReceivers by derivedStateOf {
        if (receiversExpanded) filteredReceivers else listOf()
    }

    val activitiesSize: Int
        get() = if (!MainModel.hasFilters || !hasLoadedActivities) _activitiesSize else filteredActivities.size
    val servicesSize: Int
        get() = if (!MainModel.hasFilters || !hasLoadedServices) _servicesSize else filteredServices.size
    val receiversSize: Int
        get() = if (!MainModel.hasFilters || !hasLoadedReceivers) _receiversSize else filteredReceivers.size

    val totalUnfilteredSize: Int = _activitiesSize + _servicesSize + _receiversSize

    val _loadedActivities = ConcurrentLinkedDeque<ActivityInfo>()
    val _loadedServices = ConcurrentLinkedDeque<ServiceInfo>()
    val _loadedReceivers = ConcurrentLinkedDeque<ReceiverInfo>()

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

    suspend fun loadEverything(progressCallback: (Int, Int) -> Unit) = coroutineScope {
        val total = totalUnfilteredSize
        val current = AtomicInteger(0)

        val act = async {
            loadActivities { _, _ ->
                progressCallback(current.incrementAndGet(), total)
            }
        }

        val ser = async {
            loadServices { _, _ ->
                progressCallback(current.incrementAndGet(), total)
            }
        }

        val rec = async {
            loadReceivers { _, _ ->
                progressCallback(current.incrementAndGet(), total)
            }
        }

        act.await()
        ser.await()
        rec.await()
    }

    private val filterChangeMutex = Mutex()

    suspend fun onFilterChange() {
        filterChangeMutex.withLock {
            val _filteredActivities = _loadedActivities.filter { matches(it, context) }
            val _filteredServices = _loadedServices.filter { matches(it, context) }
            val _filteredReceivers = _loadedReceivers.filter { matches(it, context) }

            withContext(Dispatchers.Main) {
                filteredActivities = _filteredActivities
                filteredServices = _filteredServices
                filteredReceivers = _filteredReceivers
            }
        }
    }

    private val loadActivitiesMutex = Mutex()

    suspend fun loadActivities(progress: (Int, Int) -> Unit) {
        loadActivitiesMutex.withLock {
            if (!hasLoadedActivities && activitiesSize > 0 && _loadedActivities.isEmpty()) {
                _loadedActivities.clear()
                _loadedActivities.addAll(activitiesLoader(progress).toSortedSet())
                hasLoadedActivities = true
            }
        }
    }

    private val loadServicesMutex = Mutex()

    suspend fun loadServices(progress: (Int, Int) -> Unit) {
        loadServicesMutex.withLock {
            if (!hasLoadedServices && servicesSize > 0 && _loadedServices.isEmpty()) {
                _loadedServices.clear()
                _loadedServices.addAll(servicesLoader(progress).toSortedSet())
                hasLoadedServices = true
            }
        }
    }

    private val loadReceiversMutex = Mutex()

    suspend fun loadReceivers(progress: (Int, Int) -> Unit) {
        loadReceiversMutex.withLock {
            if (!hasLoadedReceivers && receiversSize > 0 && _loadedReceivers.isEmpty()) {
                _loadedReceivers.clear()
                _loadedReceivers.addAll(receiversLoader(progress).toSortedSet())
                hasLoadedReceivers = true
            }
        }
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
