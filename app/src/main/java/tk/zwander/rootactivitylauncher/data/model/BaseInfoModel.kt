package tk.zwander.rootactivitylauncher.data.model

import android.content.Context
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tk.zwander.rootactivitylauncher.data.FilterMode
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ReceiverInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.util.AdvancedSearcher
import tk.zwander.rootactivitylauncher.util.isActuallyEnabled
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.regex.PatternSyntaxException

abstract class BaseInfoModel {
    protected abstract val context: Context
    protected abstract val scope: CoroutineScope
    protected abstract val mainModel: MainModel

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

    @Suppress("PropertyName")
    protected var _hasLoadedActivities = false
    @Suppress("PropertyName")
    protected var _hasLoadedServices = false
    @Suppress("PropertyName")
    protected var _hasLoadedReceivers = false

    val activitiesExpanded = MutableStateFlow(false)
    val servicesExpanded = MutableStateFlow(false)
    val receiversExpanded = MutableStateFlow(false)

    val activitiesLoading = MutableStateFlow(false)
    val servicesLoading = MutableStateFlow(false)
    val receiversLoading = MutableStateFlow(false)

    val filteredActivities by lazy {
        MutableStateFlow<List<ActivityInfo>>(ArrayList(initialActivitiesSize.value))
    }
    val filteredServices by lazy {
        MutableStateFlow<List<ServiceInfo>>(ArrayList(initialServicesSize.value))
    }
    val filteredReceivers by lazy {
        MutableStateFlow<List<ReceiverInfo>>(ArrayList(initialReceiversSize.value))
    }

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

    protected fun postInit() {
        scope.launch {
            activitiesExpanded.combine(hasLoadedActivities) { e, l ->
                e && !l
            }.collect {
                activitiesLoading.value = it

                if (it) {
                    loadActivities(true)
                    onFilterChange(true)
                }
            }
        }

        scope.launch {
            servicesExpanded.combine(hasLoadedServices) { e, l ->
                e && !l
            }.collect {
                servicesLoading.value = it

                if (it) {
                    loadServices(true)
                    onFilterChange(true)
                }
            }
        }

        scope.launch {
            receiversExpanded.combine(hasLoadedReceivers) { e, l ->
                e && !l
            }.collect {
                receiversLoading.value = it

                if (it) {
                    loadReceivers(true)
                    onFilterChange(true)
                }
            }
        }
    }

    private val loadActivitiesMutex = Mutex()
    private val loadServicesMutex = Mutex()
    private val loadReceiversMutex = Mutex()

    private suspend fun loadActivities(willBeFiltering: Boolean, progress: (suspend () -> Unit)? = null) {
        loadActivitiesMutex.withLock {
            if (!_hasLoadedActivities && _loadedActivities.isEmpty()) {
                _loadedActivities.clear()
                _loadedActivities.addAll(performActivityLoad(progress))

                _hasLoadedActivities = true

                if (!willBeFiltering) {
                    hasLoadedActivities.value = true
                }
            }
        }
    }

    private suspend fun loadServices(willBeFiltering: Boolean, progress: (suspend () -> Unit)? = null) {
        loadServicesMutex.withLock {
            if (!_hasLoadedServices && _loadedServices.isEmpty()) {
                _loadedServices.clear()
                _loadedServices.addAll(performServiceLoad(progress))

                _hasLoadedServices = true

                if (!willBeFiltering) {
                    hasLoadedServices.value = true
                }
            }
        }
    }

    private suspend fun loadReceivers(willBeFiltering: Boolean, progress: (suspend () -> Unit)? = null) {
        loadReceiversMutex.withLock {
            if (!_hasLoadedReceivers && _loadedReceivers.isEmpty()) {
                _loadedReceivers.clear()
                _loadedReceivers.addAll(performReceiverLoad(progress))

                _hasLoadedReceivers = true

                if (!willBeFiltering) {
                    hasLoadedReceivers.value = true
                }
            }
        }
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
}
