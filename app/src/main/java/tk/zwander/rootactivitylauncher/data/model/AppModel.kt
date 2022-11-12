package tk.zwander.rootactivitylauncher.data.model

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.ensody.reactivestate.derived
import com.ensody.reactivestate.get
import com.google.android.gms.common.internal.Objects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tk.zwander.rootactivitylauncher.data.FilterMode
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ReceiverInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.util.AdvancedSearcher
import tk.zwander.rootactivitylauncher.util.isActuallyEnabled
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

data class AppModel(
    val pInfo: PackageInfo,
    val info: ApplicationInfo = pInfo.applicationInfo,
    val label: CharSequence,
    private val initialActivitiesSize: Int,
    private val initialServicesSize: Int,
    private val initialReceiversSize: Int,
    private val scope: CoroutineScope,
    private val context: Context,
    private val activitiesLoader: suspend (progress: (suspend (Int, Int) -> Unit)?) -> Collection<ActivityInfo>,
    private val servicesLoader: suspend (progress: (suspend (Int, Int) -> Unit)?) -> Collection<ServiceInfo>,
    private val receiversLoader: suspend (progress: (suspend (Int, Int) -> Unit)?) -> Collection<ReceiverInfo>,
) {
    val filteredActivities = MutableStateFlow<List<ActivityInfo>>(ArrayList(initialActivitiesSize))
    val filteredServices = MutableStateFlow<List<ServiceInfo>>(ArrayList(initialServicesSize))
    val filteredReceivers = MutableStateFlow<List<ReceiverInfo>>(ArrayList(initialReceiversSize))

    private val hasLoadedActivities = MutableStateFlow(false)
    private val hasLoadedServices = MutableStateFlow(false)
    private val hasLoadedReceivers = MutableStateFlow(false)

    val activitiesSize = derived {
        if (!get(hasLoadedActivities)) initialActivitiesSize else get(filteredActivities).size
    }
    val servicesSize = derived {
        if (!get(hasLoadedServices)) initialServicesSize else get(filteredServices).size
    }
    val receiversSize = derived {
        if (!get(hasLoadedReceivers)) initialReceiversSize else get(filteredReceivers).size
}

    val totalUnfilteredSize: Int = initialActivitiesSize + initialServicesSize + initialReceiversSize

    private val _loadedActivities = ConcurrentLinkedDeque<ActivityInfo>()
    private val _loadedServices = ConcurrentLinkedDeque<ServiceInfo>()
    private val _loadedReceivers = ConcurrentLinkedDeque<ReceiverInfo>()

    val activitiesExpanded = MutableStateFlow(false)
    val servicesExpanded = MutableStateFlow(false)
    val receiversExpanded = MutableStateFlow(false)

    val activitiesLoading = derived {
        get(activitiesExpanded) && !get(hasLoadedActivities)
    }
    val servicesLoading = derived {
        get(servicesExpanded) && !get(hasLoadedServices)
    }
    val receiversLoading = derived {
        get(receiversExpanded) && !get(hasLoadedReceivers)
    }

    private var _hasLoadedActivities = false
    private var _hasLoadedServices = false
    private var _hasLoadedReceivers = false

    init {
        scope.launch(Dispatchers.IO) {
            activitiesExpanded.collect {
                if (it && !hasLoadedActivities.value) {
                    loadActivities(true)
                    onFilterChange(true)
                }
            }
        }

        scope.launch(Dispatchers.IO) {
            servicesExpanded.collect {
                if (it && !hasLoadedServices.value) {
                    loadServices(true)
                    onFilterChange(true)
                }
            }
        }

        scope.launch(Dispatchers.IO) {
            receiversExpanded.collect {
                if (it && !hasLoadedReceivers.value) {
                    withContext(Dispatchers.IO) {
                        loadReceivers(true)
                        onFilterChange(true)
                    }
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is AppModel
                && info.packageName == other.info.packageName
                && super.equals(other)
                && activitiesSize.value == other.activitiesSize.value
                && servicesSize.value == other.servicesSize.value
                && receiversSize.value == other.receiversSize.value
                && filteredActivities.value == other.filteredActivities.value
                && filteredServices.value == other.filteredServices.value
                && filteredReceivers.value == other.filteredReceivers.value
    }

    override fun hashCode(): Int {
        return info.packageName.hashCode() +
                31 * super.hashCode() +
                Objects.hashCode(
                    activitiesSize.value,
                    servicesSize.value,
                    receiversSize.value,
                    filteredActivities.value,
                    filteredServices.value,
                    filteredReceivers.value
                )
    }

    suspend fun loadEverything(willBeFiltering: Boolean, progressCallback: suspend (Int, Int) -> Unit) = coroutineScope {
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
            val query = MainModel.query.value
            val enabledFilterMode = MainModel.enabledFilterMode.value
            val exportedFilterMode = MainModel.exportedFilterMode.value
            val permissionFilterMode = MainModel.permissionFilterMode.value
            val useRegex = MainModel.useRegex.value
            val validRegex = MainModel.isQueryValidRegex.value

            filteredActivities.value = _loadedActivities.filter {
                matches(it, query, useRegex, validRegex, enabledFilterMode, exportedFilterMode, permissionFilterMode)
            }
            filteredServices.value = _loadedServices.filter {
                matches(it, query, useRegex, validRegex, enabledFilterMode, exportedFilterMode, permissionFilterMode)
            }
            filteredReceivers.value = _loadedReceivers.filter {
                matches(it, query, useRegex, validRegex, enabledFilterMode, exportedFilterMode, permissionFilterMode)
            }

            if (afterLoading) {
                hasLoadedActivities.value = _hasLoadedActivities
                hasLoadedServices.value = _hasLoadedServices
                hasLoadedReceivers.value = _hasLoadedReceivers
            }
        }
    }

    private val loadActivitiesMutex = Mutex()

    suspend fun loadActivities(willBeFiltering: Boolean, progress: (suspend (Int, Int) -> Unit)? = null) = coroutineScope {
        loadActivitiesMutex.withLock {
            if (!_hasLoadedActivities && activitiesSize.value > 0 && _loadedActivities.isEmpty()) {
                _loadedActivities.clear()
                _loadedActivities.addAll(activitiesLoader(progress).toSortedSet())

                _hasLoadedActivities = true

                if (!willBeFiltering) {
                    hasLoadedActivities.value = true
                }
            }
        }
    }

    private val loadServicesMutex = Mutex()

    suspend fun loadServices(willBeFiltering: Boolean, progress: (suspend (Int, Int) -> Unit)? = null) = coroutineScope {
        loadServicesMutex.withLock {
            if (!_hasLoadedServices && servicesSize.value > 0 && _loadedServices.isEmpty()) {
                _loadedServices.clear()
                _loadedServices.addAll(servicesLoader(progress).toSortedSet())

                _hasLoadedServices = true

                if (!willBeFiltering) {
                    hasLoadedServices.value = true
                }
            }
        }
    }

    private val loadReceiversMutex = Mutex()

    suspend fun loadReceivers(willBeFiltering: Boolean, progress: (suspend (Int, Int) -> Unit)? = null) = coroutineScope {
        loadReceiversMutex.withLock {
            if (!_hasLoadedReceivers && receiversSize.value > 0 && _loadedReceivers.isEmpty()) {
                _loadedReceivers.clear()
                _loadedReceivers.addAll(receiversLoader(progress).toSortedSet())

                _hasLoadedReceivers = true

                if (!willBeFiltering) {
                    hasLoadedReceivers.value = true
                }
            }
        }
    }

    private fun matches(
        data: BaseComponentInfo,
        query: String,
        useRegex: Boolean,
        validRegex: Boolean,
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

        if (query.isBlank() || !MainModel.includeComponents.value) return true

        val advancedMatch = AdvancedSearcher.matchesRequiresPermission(query, data.info)

        if (useRegex && validRegex) {
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
