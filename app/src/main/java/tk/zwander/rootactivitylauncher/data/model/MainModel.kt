package tk.zwander.rootactivitylauncher.data.model

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import tk.zwander.rootactivitylauncher.data.FilterMode
import tk.zwander.rootactivitylauncher.util.AdvancedSearcher
import tk.zwander.rootactivitylauncher.util.distinctByPackageName
import tk.zwander.rootactivitylauncher.util.forEachParallel
import tk.zwander.rootactivitylauncher.util.updateProgress
import java.util.regex.PatternSyntaxException

class MainModel {
    val apps = MutableStateFlow<List<BaseInfoModel>>(listOf())
    val filteredApps = MutableStateFlow<List<BaseInfoModel>>(listOf())

    val enabledFilterMode = MutableStateFlow<FilterMode.EnabledFilterMode>(FilterMode.EnabledFilterMode.ShowAll)
    val exportedFilterMode = MutableStateFlow<FilterMode.ExportedFilterMode>(FilterMode.ExportedFilterMode.ShowAll)
    val permissionFilterMode = MutableStateFlow<FilterMode.PermissionFilterMode>(FilterMode.PermissionFilterMode.ShowAll)
    val componentFilterMode = MutableStateFlow<FilterMode.HasComponentsFilterMode>(FilterMode.HasComponentsFilterMode.ShowHasComponents)

    val query = MutableStateFlow("")

    val progress = MutableStateFlow<Float?>(null)

    val useRegex = MutableStateFlow(false)
    val includeComponents = MutableStateFlow(false)

    val isSearching = MutableStateFlow(false)

    private val hasLoadedFilters: Boolean
        get() = query.value.isNotBlank() ||
                enabledFilterMode.value != FilterMode.EnabledFilterMode.ShowAll ||
                exportedFilterMode.value != FilterMode.ExportedFilterMode.ShowAll ||
                permissionFilterMode.value != FilterMode.PermissionFilterMode.ShowAll

    suspend fun update() {
        val apps = apps.value
        val hasFilters = hasLoadedFilters
        val isSearching = isSearching.value
        val includeComponents = includeComponents.value
        val componentFilterMode = componentFilterMode.value
        val query = query.value

        withContext(Dispatchers.IO) {
            if ((hasFilters && !isSearching) || (hasFilters && query.isBlank()) || (isSearching && includeComponents)) {
                val total = apps.sumOf {
                    it.totalInitialSize.value
                }
                val current = atomic(0)
                val lastUpdateTime = atomic(0L)

                apps.forEachParallel(context = Dispatchers.IO, scope = this) {
                    it.loadEverything(true) {
                        updateProgress(current, lastUpdateTime, total) { newProgress ->
                            progress.value = newProgress
                        }
                    }
                    it.onFilterChange(true)
                }
            } else {
                apps.forEachParallel(context = Dispatchers.IO, scope = this) {
                    it.onFilterChange(false)
                }
            }

            val filtered = if (hasFilters || (componentFilterMode !is FilterMode.HasComponentsFilterMode.ShowAll)) {
                apps.filter(::matches)
            } else {
                apps
            }

            val sorted = filtered.sortedWith { o1, o2 ->
                when {
                    o1 is FavoriteModel && o2 !is FavoriteModel -> -1
                    o1 !is FavoriteModel && o2 is FavoriteModel -> 1
                    o1 is AppModel && o2 is AppModel -> o1.label.toString().lowercase().compareTo(o2.label.toString().lowercase())
                    else -> 0
                }
            }

            filteredApps.value = sorted.distinctByPackageName()
            progress.value = null
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
}