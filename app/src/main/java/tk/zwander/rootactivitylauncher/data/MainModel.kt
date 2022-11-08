package tk.zwander.rootactivitylauncher.data

import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tk.zwander.rootactivitylauncher.util.AdvancedSearcher
import tk.zwander.rootactivitylauncher.util.forEachParallel
import tk.zwander.rootactivitylauncher.util.isValidRegex
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object MainModel {
    val apps = mutableStateListOf<AppInfo>()
    val filteredApps = mutableStateListOf<AppInfo>()

    var enabledFilterMode by mutableStateOf(EnabledFilterMode.SHOW_ALL)
    var exportedFilterMode by mutableStateOf(ExportedFilterMode.SHOW_ALL)
    var permissionFilterMode by mutableStateOf(PermissionFilterMode.SHOW_ALL)

    var query by mutableStateOf("")

    var progress by mutableStateOf<Float?>(null)

    var useRegex by mutableStateOf(false)
    var includeComponents by mutableStateOf(true)

    var isSearching by mutableStateOf(false)

    val hasFilters by derivedStateOf {
        query.isNotBlank() ||
                enabledFilterMode != EnabledFilterMode.SHOW_ALL ||
                exportedFilterMode != ExportedFilterMode.SHOW_ALL ||
                permissionFilterMode != PermissionFilterMode.SHOW_ALL
    }

    suspend fun update() = coroutineScope {
        val apps = apps.toList()
        val hasFilters = hasFilters
        val isSearching = isSearching

        launch(Dispatchers.IO) {
            if (hasFilters || isSearching) {
                val total = apps.sumOf { it.totalUnfilteredSize }
                val current = AtomicInteger(0)
                val lastUpdateTime = AtomicLong(0L)

                apps.forEachParallel {
                    it.loadEverything { _, _ ->
                        val oldCurrent = current.get()
                        val newCurrent = current.incrementAndGet()

                        val oldProgress = (oldCurrent / total.toFloat() * 100f).toInt() / 100f
                        val newProgress = (newCurrent / total.toFloat() * 100f).toInt() / 100f

                        val newUpdateTime = System.currentTimeMillis()

                        if (newProgress > oldProgress && newUpdateTime - 10 > lastUpdateTime.get()) {
                            lastUpdateTime.set(newUpdateTime)

                            async(Dispatchers.Main) {
                                progress = newProgress
                            }
                        }
                    }
                    it.onFilterChange()
                }
            }

            val filteredApps = if (hasFilters) {
                apps.filter { app ->
                    matches(app)
                }
            } else {
                apps
            }

            val sorted = filteredApps.sortedBy { it.label.toString().lowercase() }

            launch(Dispatchers.Main) {
                MainModel.filteredApps.clear()
                MainModel.filteredApps.addAll(sorted)

                progress = null
            }
        }
    }

    fun matches(data: AppInfo): Boolean {
        if (query.isBlank()) return true

        val activityFilterEmpty = data.filteredActivities.isEmpty()
        val serviceFilterEmpty = data.filteredServices.isEmpty()
        val receiverFilterEmpty = data.filteredReceivers.isEmpty()

        if (includeComponents && (!activityFilterEmpty || !serviceFilterEmpty || !receiverFilterEmpty)) return true

        val advancedMatch = AdvancedSearcher.matchesHasPermission(query, data)
                || AdvancedSearcher.matchesRequiresPermission(query, data)
                || AdvancedSearcher.matchesDeclaresPermission(query, data)
                || AdvancedSearcher.matchesRequiresFeature(query, data)

        if (advancedMatch) return true

        if (useRegex && query.isValidRegex()) {
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