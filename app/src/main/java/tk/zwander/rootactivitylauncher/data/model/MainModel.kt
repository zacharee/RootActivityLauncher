package tk.zwander.rootactivitylauncher.data.model

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tk.zwander.rootactivitylauncher.data.FilterMode
import tk.zwander.rootactivitylauncher.util.AdvancedSearcher
import tk.zwander.rootactivitylauncher.util.forEachParallel
import tk.zwander.rootactivitylauncher.util.updateProgress
import java.util.regex.PatternSyntaxException

class MainModel {
    val apps = MutableStateFlow<List<AppModel>>(listOf())
    val filteredApps = MutableStateFlow<List<AppModel>>(listOf())

    val enabledFilterMode = MutableStateFlow<FilterMode.EnabledFilterMode>(FilterMode.EnabledFilterMode.ShowAll)
    val exportedFilterMode = MutableStateFlow<FilterMode.ExportedFilterMode>(FilterMode.ExportedFilterMode.ShowAll)
    val permissionFilterMode = MutableStateFlow<FilterMode.PermissionFilterMode>(FilterMode.PermissionFilterMode.ShowAll)

    val query = MutableStateFlow("")

    val progress = MutableStateFlow<Float?>(null)

    val useRegex = MutableStateFlow(false)
    val includeComponents = MutableStateFlow(true)

    val isSearching = MutableStateFlow(false)

    private val hasFilters: Boolean
        get() = query.value.isNotBlank() ||
                enabledFilterMode.value != FilterMode.EnabledFilterMode.ShowAll ||
                exportedFilterMode.value != FilterMode.ExportedFilterMode.ShowAll ||
                permissionFilterMode.value != FilterMode.PermissionFilterMode.ShowAll

    suspend fun update() = coroutineScope {
        val apps = apps.value.toList()
        val hasFilters = hasFilters
        val isSearching = isSearching.value

        launch(Dispatchers.IO) {
            if (hasFilters || isSearching) {
                val total = apps.sumOf { it.totalUnfilteredSize }
                val current = atomic(0)
                val lastUpdateTime = atomic(0L)

                apps.forEachParallel {
                    it.loadEverything(true) {
                        updateProgress(current, lastUpdateTime, total) { newProgress ->
                            progress.value = newProgress
                        }
                    }
                    it.onFilterChange(true)
                }
            } else {
                apps.forEachParallel {
                    it.onFilterChange(false)
                }
            }

            val filtered = if (hasFilters) {
                apps.filter { app ->
                    matches(app)
                }
            } else {
                apps
            }

            val sorted = filtered.sortedBy { it.label.toString().lowercase() }

            filteredApps.value = sorted
            progress.value = null
        }
    }

    private fun matches(data: AppModel): Boolean {
        val query = query.value
        val useRegex = useRegex.value
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

        return false
    }
}