package tk.zwander.rootactivitylauncher.data.model

import android.content.Context
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tk.zwander.rootactivitylauncher.data.FilterMode
import tk.zwander.rootactivitylauncher.data.Query
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ReceiverInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.util.AdvancedSearcher
import tk.zwander.rootactivitylauncher.util.isActuallyEnabled
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.regex.PatternSyntaxException

abstract class BaseInfoModel(
    protected open val context: Context,
    protected open val scope: CoroutineScope,
    protected open val mainModel: MainModel,
) {
    abstract val initialActivitiesSize: StateFlow<Int>
    abstract val initialServicesSize: StateFlow<Int>
    abstract val initialReceiversSize: StateFlow<Int>

    protected val packageManager: PackageManager
        get() = context.packageManager

    val totalInitialSize: StateFlow<Int> by lazy {
        combine(initialActivitiesSize, initialServicesSize, initialReceiversSize) { (a, s, r) -> a + s + r }
            .stateIn(
                scope,
                SharingStarted.Eagerly,
                initialActivitiesSize.value + initialServicesSize.value + initialReceiversSize.value
            )
    }

    protected val hasLoadedActivities = MutableStateFlow(false)
    protected val hasLoadedServices = MutableStateFlow(false)
    protected val hasLoadedReceivers = MutableStateFlow(false)

    @Suppress("PropertyName")
    protected val _loadedActivities = ConcurrentLinkedDeque<ActivityInfo>()
    @Suppress("PropertyName")
    protected val _loadedServices = ConcurrentLinkedDeque<ServiceInfo>()
    @Suppress("PropertyName")
    protected val _loadedReceivers = ConcurrentLinkedDeque<ReceiverInfo>()

    val activitiesExpanded = MutableStateFlow(false)
    val servicesExpanded = MutableStateFlow(false)
    val receiversExpanded = MutableStateFlow(false)

    val activitiesLoading = MutableStateFlow(false)
    val servicesLoading = MutableStateFlow(false)
    val receiversLoading = MutableStateFlow(false)

    val activitiesSize by lazy {
        combine(filteredActivities, hasLoadedActivities, initialActivitiesSize) { f, l, i ->
            if (l) f.size else i
        }.stateIn(scope, SharingStarted.Eagerly, initialActivitiesSize.value)
    }
    val servicesSize by lazy {
        combine(filteredServices, hasLoadedServices, initialServicesSize) { f, l, i ->
            if (l) f.size else i
        }.stateIn(scope, SharingStarted.Eagerly, initialServicesSize.value)
    }
    val receiversSize by lazy {
        combine(filteredReceivers, hasLoadedReceivers, initialReceiversSize) { f, l, i ->
            if (l) f.size else i
        }.stateIn(scope, SharingStarted.Eagerly, initialReceiversSize.value)
    }

    private val loadActivitiesMutex = Mutex()
    private val loadServicesMutex = Mutex()
    private val loadReceiversMutex = Mutex()

    private suspend fun loadActivities() {
        loadActivitiesMutex.withLock {
            if (!hasLoadedActivities.value && _loadedActivities.isEmpty()) {
                _loadedActivities.clear()
                _loadedActivities.addAll(performActivityLoad(mainModel::updateProgress))

                hasLoadedActivities.value = true
                mainModel.updateProgress(true)
            }
        }
    }

    private suspend fun loadServices() {
        loadServicesMutex.withLock {
            if (!hasLoadedServices.value && _loadedServices.isEmpty()) {
                _loadedServices.clear()
                _loadedServices.addAll(performServiceLoad(mainModel::updateProgress))

                hasLoadedServices.value = true
                mainModel.updateProgress(true)
            }
        }
    }

    private suspend fun loadReceivers() {
        loadReceiversMutex.withLock {
            if (!hasLoadedReceivers.value && _loadedReceivers.isEmpty()) {
                _loadedReceivers.clear()
                _loadedReceivers.addAll(performReceiverLoad(mainModel::updateProgress))

                hasLoadedReceivers.value = true
                mainModel.updateProgress(true)
            }
        }
    }

    private val realQuery by lazy {
        mainModel.query.combine(mainModel.useRegex) { query, useRegex ->
            if (useRegex) {
                try {
                    Query.RegexQuery(Regex(query))
                } catch (e: PatternSyntaxException) {
                    Query.StringQuery(query)
                }
            } else {
                Query.StringQuery(query)
            }
        }
    }

    val filteredActivities by lazy {
        combine(
            realQuery,
            mainModel.enabledFilterMode,
            mainModel.exportedFilterMode,
            mainModel.permissionFilterMode,
            mainModel.includeComponents,
            activitiesExpanded,
            hasLoadedActivities,
            mainModel.isSearching,
        ) { values ->
            val query = values[0] as Query
            val enabledFilterMode = values[1] as FilterMode.EnabledFilterMode
            val exportedFilterMode = values[2] as FilterMode.ExportedFilterMode
            val permissionFilterMode = values[3] as FilterMode.PermissionFilterMode
            val includeComponents = values[4] as Boolean
            val activitiesExpanded = values[5] as Boolean
            val hasLoadedActivities = values[6] as Boolean
            val isSearching = values[7] as Boolean

            val hasFilters = query.raw.isNotBlank() ||
                    enabledFilterMode != FilterMode.EnabledFilterMode.ShowAll ||
                    exportedFilterMode != FilterMode.ExportedFilterMode.ShowAll ||
                    permissionFilterMode != FilterMode.PermissionFilterMode.ShowAll

            if ((activitiesExpanded && !hasLoadedActivities) ||
                ((hasFilters && !isSearching) || (hasFilters && query.raw.isBlank()) || (isSearching && includeComponents))) {
                loadActivities()
            }

            _loadedActivities.filter {
                matches(
                    it, query, includeComponents,
                    enabledFilterMode, exportedFilterMode,
                    permissionFilterMode,
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, listOf())
    }

    val filteredServices by lazy {
        combine(
            realQuery,
            mainModel.enabledFilterMode,
            mainModel.exportedFilterMode,
            mainModel.permissionFilterMode,
            mainModel.includeComponents,
            servicesExpanded,
            hasLoadedServices,
            mainModel.isSearching,
        ) { values ->
            val query = values[0] as Query
            val enabledFilterMode = values[1] as FilterMode.EnabledFilterMode
            val exportedFilterMode = values[2] as FilterMode.ExportedFilterMode
            val permissionFilterMode = values[3] as FilterMode.PermissionFilterMode
            val includeComponents = values[4] as Boolean
            val servicesExpanded = values[5] as Boolean
            val hasLoadedServices = values[6] as Boolean
            val isSearching = values[7] as Boolean

            val hasFilters = query.raw.isNotBlank() ||
                    enabledFilterMode != FilterMode.EnabledFilterMode.ShowAll ||
                    exportedFilterMode != FilterMode.ExportedFilterMode.ShowAll ||
                    permissionFilterMode != FilterMode.PermissionFilterMode.ShowAll

            if ((servicesExpanded && !hasLoadedServices) ||
                ((hasFilters && !isSearching) || (hasFilters && query.raw.isBlank()) || (isSearching && includeComponents))) {
                loadServices()
            }

            _loadedServices.filter {
                matches(it, query, includeComponents, enabledFilterMode, exportedFilterMode, permissionFilterMode)
            }
        }.stateIn(scope, SharingStarted.Eagerly, listOf())
    }

    val filteredReceivers by lazy {
        combine(
            realQuery,
            mainModel.enabledFilterMode,
            mainModel.exportedFilterMode,
            mainModel.permissionFilterMode,
            mainModel.includeComponents,
            receiversExpanded,
            hasLoadedReceivers,
            mainModel.isSearching,
        ) { values ->
            val query = values[0] as Query
            val enabledFilterMode = values[1] as FilterMode.EnabledFilterMode
            val exportedFilterMode = values[2] as FilterMode.ExportedFilterMode
            val permissionFilterMode = values[3] as FilterMode.PermissionFilterMode
            val includeComponents = values[4] as Boolean
            val receiversExpanded = values[5] as Boolean
            val hasLoadedReceivers = values[6] as Boolean
            val isSearching = values[7] as Boolean

            val hasFilters = query.raw.isNotBlank() ||
                    enabledFilterMode != FilterMode.EnabledFilterMode.ShowAll ||
                    exportedFilterMode != FilterMode.ExportedFilterMode.ShowAll ||
                    permissionFilterMode != FilterMode.PermissionFilterMode.ShowAll

            if ((receiversExpanded && !hasLoadedReceivers) ||
                ((hasFilters && !isSearching) || (hasFilters && query.raw.isBlank()) || (isSearching && includeComponents))) {
                loadReceivers()
            }

            _loadedReceivers.filter {
                matches(it, query, includeComponents, enabledFilterMode, exportedFilterMode, permissionFilterMode)
            }
        }.stateIn(scope, SharingStarted.Eagerly, listOf())
    }

    protected abstract suspend fun performActivityLoad(progress: (suspend () -> Unit)? = null): Collection<ActivityInfo>
    protected abstract suspend fun performServiceLoad(progress: (suspend () -> Unit)? = null): Collection<ServiceInfo>
    protected abstract suspend fun performReceiverLoad(progress: (suspend () -> Unit)? = null): Collection<ReceiverInfo>

    protected suspend fun <Loaded : BaseComponentInfo, Input : PackageItemInfo> Array<Input>?.loadItems(
        progress: (suspend () -> Unit)?,
        constructor: (Input, CharSequence) -> Loaded
    ): Collection<Loaded> {
        val infos = ConcurrentLinkedDeque<Loaded>()
        val packageManager = packageManager

        this?.forEach { input ->
            try {
                infos.add(constructor(input, input.loadLabel(packageManager)))
            } catch (_: SecurityException) {}
            progress?.invoke()
        }

        return infos
    }

    private fun matches(
        data: BaseComponentInfo,
        query: Query,
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

        if (query.isBlank || !includeComponents) return true

        val advancedMatch = AdvancedSearcher.matchesRequiresPermission(query.raw, data.info)

        @Suppress("RedundantIf", "RedundantSuppression")
        if (query.checkMatch(context, data, advancedMatch)) {
            return true
        }

        return false
    }
}
