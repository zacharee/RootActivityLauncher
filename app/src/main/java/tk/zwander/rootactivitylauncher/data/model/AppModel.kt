package tk.zwander.rootactivitylauncher.data.model

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import com.google.android.gms.common.internal.Objects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import java.util.regex.PatternSyntaxException

data class AppModel(
    val pInfo: PackageInfo,
    val info: ApplicationInfo = pInfo.applicationInfo,
    val label: CharSequence,
    private val mainModel: MainModel,
    private val scope: CoroutineScope,
    private val context: Context,
) {
    private val initialActivitiesSize: Int = pInfo.activities?.size ?: 0
    private val initialServicesSize: Int = pInfo.services?.size ?: 0
    private val initialReceiversSize: Int = pInfo.receivers?.size ?: 0

    private val hasLoadedActivities = MutableStateFlow(false)
    private val hasLoadedServices = MutableStateFlow(false)
    private val hasLoadedReceivers = MutableStateFlow(false)

    private val _loadedActivities = ConcurrentLinkedDeque<ActivityInfo>()
    private val _loadedServices = ConcurrentLinkedDeque<ServiceInfo>()
    private val _loadedReceivers = ConcurrentLinkedDeque<ReceiverInfo>()

    private var _hasLoadedActivities = false
    private var _hasLoadedServices = false
    private var _hasLoadedReceivers = false

    val filteredActivities = MutableStateFlow<List<ActivityInfo>>(ArrayList(initialActivitiesSize))
    val filteredServices = MutableStateFlow<List<ServiceInfo>>(ArrayList(initialServicesSize))
    val filteredReceivers = MutableStateFlow<List<ReceiverInfo>>(ArrayList(initialReceiversSize))

    val activitiesSize = MutableStateFlow(initialActivitiesSize)
    val servicesSize = MutableStateFlow(initialServicesSize)
    val receiversSize = MutableStateFlow(initialReceiversSize)

    val totalUnfilteredSize: Int = initialActivitiesSize + initialServicesSize + initialReceiversSize

    val activitiesExpanded = MutableStateFlow(false)
    val servicesExpanded = MutableStateFlow(false)
    val receiversExpanded = MutableStateFlow(false)

    val activitiesLoading = MutableStateFlow(false)
    val servicesLoading = MutableStateFlow(false)
    val receiversLoading = MutableStateFlow(false)

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

        scope.launch(Dispatchers.IO) {
            hasLoadedActivities.collect {
                activitiesSize.value = if (it) filteredActivities.value.size else initialActivitiesSize
            }
        }

        scope.launch(Dispatchers.IO) {
            hasLoadedServices.collect {
                servicesSize.value = if (it) filteredServices.value.size else initialServicesSize
            }
        }

        scope.launch(Dispatchers.IO) {
            hasLoadedReceivers.collect {
                receiversSize.value = if (it) filteredReceivers.value.size else initialReceiversSize
            }
        }

        scope.launch(Dispatchers.IO) {
            activitiesExpanded.collect {
                activitiesLoading.value = it && !hasLoadedActivities.value
            }
        }

        scope.launch(Dispatchers.IO) {
            servicesExpanded.collect {
                servicesLoading.value = it && !hasLoadedServices.value
            }
        }

        scope.launch(Dispatchers.IO) {
            receiversExpanded.collect {
                receiversLoading.value = it && !hasLoadedReceivers.value
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

    suspend fun loadEverything(willBeFiltering: Boolean, progressCallback: suspend () -> Unit) = coroutineScope {
        val act = launch {
            loadActivities(willBeFiltering, progressCallback)
        }

        val ser = launch {
            loadServices(willBeFiltering, progressCallback)
        }

        val rec = launch {
            loadReceivers(willBeFiltering, progressCallback)
        }

        act.join()
        ser.join()
        rec.join()
    }

    private val filterChangeMutex = Mutex()

    suspend fun onFilterChange(
        afterLoading: Boolean,

    ) {
        filterChangeMutex.withLock {
            val query = mainModel.query.value
            val enabledFilterMode = mainModel.enabledFilterMode.value
            val exportedFilterMode = mainModel.exportedFilterMode.value
            val permissionFilterMode = mainModel.permissionFilterMode.value
            val useRegex = mainModel.useRegex.value
            val includeComponents = mainModel.includeComponents.value

            val validRegex = if (useRegex) {
                try {
                    Regex(query)
                    true
                } catch (e: PatternSyntaxException) {
                    false
                }
            } else {
                false
            }

            filteredActivities.value = _loadedActivities.filter {
                matches(it, query, useRegex, validRegex, includeComponents,
                    enabledFilterMode, exportedFilterMode, permissionFilterMode)
            }
            filteredServices.value = _loadedServices.filter {
                matches(it, query, useRegex, validRegex, includeComponents,
                    enabledFilterMode, exportedFilterMode, permissionFilterMode)
            }
            filteredReceivers.value = _loadedReceivers.filter {
                matches(it, query, useRegex, validRegex, includeComponents,
                    enabledFilterMode, exportedFilterMode, permissionFilterMode)
            }

            if (afterLoading) {
                hasLoadedActivities.value = _hasLoadedActivities
                hasLoadedServices.value = _hasLoadedServices
                hasLoadedReceivers.value = _hasLoadedReceivers
            }
        }
    }

    private val loadActivitiesMutex = Mutex()

    private suspend fun loadActivities(willBeFiltering: Boolean, progress: (suspend () -> Unit)? = null) = coroutineScope {
        loadActivitiesMutex.withLock {
            if (!_hasLoadedActivities && activitiesSize.value > 0 && _loadedActivities.isEmpty()) {
                _loadedActivities.clear()
                _loadedActivities.addAll(
                    pInfo.activities.loadItems(
                        pm = context.packageManager,
                        progress = progress
                    ) { input, label -> ActivityInfo(input, label) }
                        .toSortedSet()
                )

                _hasLoadedActivities = true

                if (!willBeFiltering) {
                    hasLoadedActivities.value = true
                }
            }
        }
    }

    private val loadServicesMutex = Mutex()

    private suspend fun loadServices(willBeFiltering: Boolean, progress: (suspend () -> Unit)? = null) = coroutineScope {
        loadServicesMutex.withLock {
            if (!_hasLoadedServices && servicesSize.value > 0 && _loadedServices.isEmpty()) {
                _loadedServices.clear()
                _loadedServices.addAll(
                    pInfo.services.loadItems(
                        pm = context.packageManager,
                        progress = progress
                    ) { input, label -> ServiceInfo(input, label) }
                        .toSortedSet()
                )

                _hasLoadedServices = true

                if (!willBeFiltering) {
                    hasLoadedServices.value = true
                }
            }
        }
    }

    private val loadReceiversMutex = Mutex()

    private suspend fun loadReceivers(willBeFiltering: Boolean, progress: (suspend () -> Unit)? = null) = coroutineScope {
        loadReceiversMutex.withLock {
            if (!_hasLoadedReceivers && receiversSize.value > 0 && _loadedReceivers.isEmpty()) {
                _loadedReceivers.clear()
                _loadedReceivers.addAll(
                    pInfo.receivers.loadItems(
                        pm = context.packageManager,
                        progress = progress
                    ) { input, label -> ReceiverInfo(input, label) }
                        .toSortedSet()
                )

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
        includeComponents: Boolean,
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

        if (query.isBlank() || !includeComponents) return true

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

    private suspend fun <Loaded : BaseComponentInfo, Input : PackageItemInfo> Array<Input>?.loadItems(
        pm: PackageManager,
        progress: (suspend () -> Unit)?,
        constructor: (Input, CharSequence) -> Loaded
    ): Collection<Loaded> {
        val infos = ConcurrentLinkedDeque<Loaded>()

        this?.forEach { input ->
            val label = input.loadLabel(pm).ifBlank { label }

            infos.add(constructor(input, label))
            progress?.invoke()
        }

        return infos
    }
}
