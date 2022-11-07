package tk.zwander.rootactivitylauncher.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import tk.zwander.rootactivitylauncher.util.AdvancedSearcher
import tk.zwander.rootactivitylauncher.util.isValidRegex

object MainModel {
    val apps = mutableStateListOf<AppInfo>()
    val filteredApps = mutableStateListOf<AppInfo>()

    var enabledFilterMode by mutableStateOf(EnabledFilterMode.SHOW_ALL)
    var exportedFilterMode by mutableStateOf(ExportedFilterMode.SHOW_ALL)
    var permissionFilterMode by mutableStateOf(PermissionFilterMode.SHOW_ALL)

    var hasLoadedItems by mutableStateOf(false)
    var query by mutableStateOf("")

    var progress by mutableStateOf<Int?>(null)

    var useRegex by mutableStateOf(false)
    var includeComponents by mutableStateOf(true)

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