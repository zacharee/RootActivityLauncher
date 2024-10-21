package tk.zwander.rootactivitylauncher.data.model

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import tk.zwander.rootactivitylauncher.data.FilterMode
import tk.zwander.rootactivitylauncher.data.SortMode
import tk.zwander.rootactivitylauncher.data.SortOrder
import tk.zwander.rootactivitylauncher.util.AdvancedSearcher
import tk.zwander.rootactivitylauncher.util.distinctByPackageName
import java.util.regex.PatternSyntaxException

class MainModel(
    @Suppress("CanBeParameter", "RedundantSuppression")
    private val scope: CoroutineScope,
) {
    val apps = MutableStateFlow<List<BaseInfoModel>>(listOf())
    private val totalInitialSize = apps.map { apps ->
        combine(apps.map { it.totalInitialSize }) { sizes ->
            sizes.sum()
        }.fold(0) { accumulator, value -> accumulator + value }
    }.stateIn(scope, SharingStarted.Eagerly, 0)

    private val currentProgress = atomic(0)
    private val lastUpdateTime = atomic(0L)

    val enabledFilterMode = MutableStateFlow<FilterMode.EnabledFilterMode>(FilterMode.EnabledFilterMode.ShowAll)
    val exportedFilterMode = MutableStateFlow<FilterMode.ExportedFilterMode>(FilterMode.ExportedFilterMode.ShowAll)
    val permissionFilterMode = MutableStateFlow<FilterMode.PermissionFilterMode>(FilterMode.PermissionFilterMode.ShowAll)
    val componentFilterMode = MutableStateFlow<FilterMode.HasComponentsFilterMode>(FilterMode.HasComponentsFilterMode.ShowHasComponents)

    val sortAppsBy = MutableStateFlow<SortMode>(SortMode.SortByName)
    val sortOrder = MutableStateFlow<SortOrder>(SortOrder.Ascending)

    val query = MutableStateFlow("")

    val progress = MutableStateFlow<Float?>(null)

    val useRegex = MutableStateFlow(false)
    val includeComponents = MutableStateFlow(false)

    val isSearching = MutableStateFlow(false)

    val filteredApps = combine(
        isSearching, useRegex, includeComponents,
        apps, enabledFilterMode, exportedFilterMode,
        permissionFilterMode, componentFilterMode, query,
        sortAppsBy, sortOrder, totalInitialSize,
    ) { flowValues ->
        @Suppress("UNCHECKED_CAST")
        val apps = flowValues[3] as List<BaseInfoModel>
        val enabledFilterMode = flowValues[4] as FilterMode.EnabledFilterMode
        val exportedFilterMode = flowValues[5] as FilterMode.ExportedFilterMode
        val permissionFilterMode = flowValues[6] as FilterMode.PermissionFilterMode
        val componentFilterMode = flowValues[7] as FilterMode.HasComponentsFilterMode
        val query = flowValues[8] as String
        val sortAppsBy = flowValues[9] as SortMode
        val sortOrder = flowValues[10] as SortOrder

        val hasFilters = query.isNotBlank() ||
                enabledFilterMode != FilterMode.EnabledFilterMode.ShowAll ||
                exportedFilterMode != FilterMode.ExportedFilterMode.ShowAll ||
                permissionFilterMode != FilterMode.PermissionFilterMode.ShowAll

        withContext(Dispatchers.IO) {
            val filtered = if (hasFilters || (componentFilterMode !is FilterMode.HasComponentsFilterMode.ShowAll)) {
                apps.filter(::matches)
            } else {
                apps
            }

            val sorted = filtered.sortedWith { o1, o2 ->
                when {
                    o1 is FavoriteModel && o2 !is FavoriteModel -> -1
                    o1 !is FavoriteModel && o2 is FavoriteModel -> 1
                    o1 is AppModel && o2 is AppModel -> {
                        sortOrder.applyOrder(
                            when (sortAppsBy) {
                                SortMode.SortByName -> o1.label.toString().lowercase().compareTo(o2.label.toString().lowercase())
                                SortMode.SortByInstalledDate -> o1.pInfo.firstInstallTime.compareTo(o2.pInfo.firstInstallTime)
                                SortMode.SortByUpdatedDate -> o1.pInfo.lastUpdateTime.compareTo(o2.pInfo.lastUpdateTime)
                            }
                        )
                    }
                    else -> 0
                }
            }

            sorted.distinctByPackageName().also {
                progress.value = null
            }
        }
    }

    private fun matches(data: BaseInfoModel): Boolean {
        val query = query.value
        val useRegex = useRegex.value
        val componentFilterMode = componentFilterMode.value
        val componentSize = data.totalInitialSize.value

        if (data is AppModel) {
            when (componentFilterMode) {
                FilterMode.HasComponentsFilterMode.ShowHasNoComponents -> {
                    if (componentSize > 0) {
                        return false
                    }
                }
                FilterMode.HasComponentsFilterMode.ShowHasComponents -> {
                    if (componentSize == 0) {
                        return false
                    }
                }
                else -> {}
            }
        }

        val isValidRegex = if (useRegex) {
            try {
                Regex(query)
                true
            } catch (e: PatternSyntaxException) {
                false
            }
        } else {
            false
        }

        if (query.isBlank()) return true

        val activityFilterEmpty = data.filteredActivities.value.isEmpty()
        val serviceFilterEmpty = data.filteredServices.value.isEmpty()
        val receiverFilterEmpty = data.filteredReceivers.value.isEmpty()

        if (includeComponents.value && (!activityFilterEmpty || !serviceFilterEmpty || !receiverFilterEmpty)) return true

        if (data is AppModel) {
            val advancedMatch = AdvancedSearcher.matchesHasPermission(query, data)
                    || AdvancedSearcher.matchesRequiresPermission(query, data)
                    || AdvancedSearcher.matchesDeclaresPermission(query, data)
                    || AdvancedSearcher.matchesRequiresFeature(query, data)

            if (advancedMatch) return true

            if (useRegex && isValidRegex) {
                if (Regex(query).run {
                        containsMatchIn(data.info.packageName)
                                || containsMatchIn(data.label)
                    }) {
                    return true
                }
            } else {
                if (data.label.contains(query, true)
                    || data.info.packageName.contains(query, true)) {
                    return true
                }
            }
        }

        return false
    }

    fun updateProgress(checkProgress: Boolean = false) {
        if (checkProgress) {
            if (progress.value.run { this != null && this >= 100 }) {
                resetProgress()
            }
        } else {
            tk.zwander.rootactivitylauncher.util.updateProgress(
                currentProgress,
                lastUpdateTime,
                totalInitialSize.value
            ) { newProgress ->
                progress.value = newProgress
            }
        }
    }

    fun resetProgress() {
        progress.value = null
        currentProgress.value = 0
        lastUpdateTime.value = 0
    }
}