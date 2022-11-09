package tk.zwander.rootactivitylauncher.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.internal.Objects
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
    val filteredActivities = MutableLiveData<List<ActivityInfo>>(ArrayList(_activitiesSize))
    val filteredServices = MutableLiveData<List<ServiceInfo>>(ArrayList(_servicesSize))
    val filteredReceivers = MutableLiveData<List<ReceiverInfo>>(ArrayList(_receiversSize))

    val activitiesSize: Int
        get() = if (!MainModel.hasFilters || !hasLoadedActivities.value!!) _activitiesSize else filteredActivities.value!!.size
    val servicesSize: Int
        get() = if (!MainModel.hasFilters || !hasLoadedServices.value!!) _servicesSize else filteredServices.value!!.size
    val receiversSize: Int
        get() = if (!MainModel.hasFilters || !hasLoadedReceivers.value!!) _receiversSize else filteredReceivers.value!!.size

    val totalUnfilteredSize: Int = _activitiesSize + _servicesSize + _receiversSize

    val _loadedActivities = ConcurrentLinkedDeque<ActivityInfo>()
    val _loadedServices = ConcurrentLinkedDeque<ServiceInfo>()
    val _loadedReceivers = ConcurrentLinkedDeque<ReceiverInfo>()

    val activitiesExpanded = MutableLiveData(false)
    val servicesExpanded = MutableLiveData(false)
    val receiversExpanded = MutableLiveData(false)

    val hasLoadedActivities = MutableLiveData(false)
    val hasLoadedServices = MutableLiveData(false)
    val hasLoadedReceivers = MutableLiveData(false)

    private var _hasLoadedActivities = false
    private var _hasLoadedServices = false
    private var _hasLoadedReceivers = false

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

    suspend fun loadEverything(willBeFiltering: Boolean, progressCallback: (Int, Int) -> Unit) = coroutineScope {
        val total = totalUnfilteredSize
        val current = AtomicInteger(0)

        val act = async {
            loadActivities(willBeFiltering) { _, _ ->
                progressCallback(current.incrementAndGet(), total)
            }
        }

        val ser = async {
            loadServices(willBeFiltering) { _, _ ->
                progressCallback(current.incrementAndGet(), total)
            }
        }

        val rec = async {
            loadReceivers(willBeFiltering) { _, _ ->
                progressCallback(current.incrementAndGet(), total)
            }
        }

        act.await()
        ser.await()
        rec.await()
    }

    private val filterChangeMutex = Mutex()

    suspend fun onFilterChange(afterLoading: Boolean) {
        filterChangeMutex.withLock {
            val query = withContext(Dispatchers.Main) {
                MainModel.query.value!!
            }
            val enabledFilterMode = withContext(Dispatchers.Main) {
                MainModel.enabledFilterMode.value!!
            }
            val exportedFilterMode = withContext(Dispatchers.Main) {
                MainModel.exportedFilterMode.value!!
            }
            val permissionFilterMode = withContext(Dispatchers.Main) {
                MainModel.permissionFilterMode.value!!
            }

            val _filteredActivities = _loadedActivities.filter {
                matches(it, query, enabledFilterMode, exportedFilterMode, permissionFilterMode)
            }
            val _filteredServices = _loadedServices.filter {
                matches(it, query, enabledFilterMode, exportedFilterMode, permissionFilterMode)
            }
            val _filteredReceivers = _loadedReceivers.filter {
                matches(it, query, enabledFilterMode, exportedFilterMode, permissionFilterMode)
            }

            filteredActivities.postValue(_filteredActivities)
            filteredServices.postValue(_filteredServices)
            filteredReceivers.postValue(_filteredReceivers)

            if (afterLoading) {
                hasLoadedActivities.postValue(_hasLoadedActivities)
                hasLoadedServices.postValue(_hasLoadedServices)
                hasLoadedReceivers.postValue(_hasLoadedReceivers)
            }
        }
    }

    private val loadActivitiesMutex = Mutex()

    suspend fun loadActivities(willBeFiltering: Boolean, progress: (Int, Int) -> Unit) = coroutineScope {
        loadActivitiesMutex.withLock {
            if (!_hasLoadedActivities && activitiesSize > 0 && _loadedActivities.isEmpty()) {
                _loadedActivities.clear()
                _loadedActivities.addAll(activitiesLoader(progress).toSortedSet())

                _hasLoadedActivities = true

                if (!willBeFiltering) {
                    hasLoadedActivities.postValue(true)
                }
            }
        }
    }

    private val loadServicesMutex = Mutex()

    suspend fun loadServices(willBeFiltering: Boolean, progress: (Int, Int) -> Unit) = coroutineScope {
        loadServicesMutex.withLock {
            if (!_hasLoadedServices && servicesSize > 0 && _loadedServices.isEmpty()) {
                _loadedServices.clear()
                _loadedServices.addAll(servicesLoader(progress).toSortedSet())

                _hasLoadedServices = true

                if (!willBeFiltering) {
                    hasLoadedServices.postValue(true)
                }
            }
        }
    }

    private val loadReceiversMutex = Mutex()

    suspend fun loadReceivers(willBeFiltering: Boolean, progress: (Int, Int) -> Unit) = coroutineScope {
        loadReceiversMutex.withLock {
            if (!_hasLoadedReceivers && receiversSize > 0 && _loadedReceivers.isEmpty()) {
                _loadedReceivers.clear()
                _loadedReceivers.addAll(receiversLoader(progress).toSortedSet())

                _hasLoadedReceivers = true

                if (!willBeFiltering) {
                    hasLoadedReceivers.postValue(true)
                }
            }
        }
    }

    private fun matches(
        data: BaseComponentInfo,
        query: String,
        enabledFilterMode: FilterMode.EnabledFilterMode,
        exportedFilterMode: FilterMode.ExportedFilterMode,
        permissionFilterMode: FilterMode.PermissionFilterMode
    ): Boolean {
        when (enabledFilterMode) {
            FilterMode.EnabledFilterMode.ShowDisabled -> if (data.info.isActuallyEnabled(context)) return false
            FilterMode.EnabledFilterMode.ShowEnabled -> if (!data.info.isActuallyEnabled(context)) return false
            else -> {
                //no-op
            }
        }

        when (exportedFilterMode) {
            FilterMode.ExportedFilterMode.ShowExported -> if (!data.info.exported) return false
            FilterMode.ExportedFilterMode.ShowUnexported -> if (data.info.exported) return false
            else -> {
                //no-op
            }
        }

        val permission = when (data) {
            is ActivityInfo -> data.info.permission
            is ServiceInfo -> data.info.permission
            else -> null
        }

        when (permissionFilterMode) {
            FilterMode.PermissionFilterMode.ShowNoPermissionRequired -> if (!permission.isNullOrBlank()) return false
            FilterMode.PermissionFilterMode.ShowRequiresPermission -> if (permission.isNullOrBlank()) return false
            else -> {
                //no-op
            }
        }

        if (query.isBlank() || !MainModel.includeComponents.value!!) return true

        val advancedMatch = AdvancedSearcher.matchesRequiresPermission(query, data.info)

        if (MainModel.useRegex.value!! && query.isValidRegex()) {
            if (Regex(query).run {
                    containsMatchIn(data.info.name)
                            || containsMatchIn(data.label)
                } || advancedMatch) {
                return true
            }
        } else {
            if (data.info.name.contains(query, true)
                || (data.label.contains(query, true))
                || advancedMatch
            ) {
                return true
            }
        }

        return false
    }
}
