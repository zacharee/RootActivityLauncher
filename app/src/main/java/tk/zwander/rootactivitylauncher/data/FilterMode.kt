package tk.zwander.rootactivitylauncher.data

import tk.zwander.rootactivitylauncher.R

sealed class FilterMode(labelRes: Int) : BaseOption(labelRes) {
    sealed class EnabledFilterMode(id: Int) : FilterMode(id) {
        data object ShowAll : EnabledFilterMode(R.string.filter_all)
        data object ShowEnabled : EnabledFilterMode(R.string.filter_enabled)
        data object ShowDisabled : EnabledFilterMode(R.string.filter_disabled)
    }

    sealed class ExportedFilterMode(id: Int) : FilterMode(id) {
        data object ShowAll : ExportedFilterMode(R.string.filter_all)
        data object ShowExported : ExportedFilterMode(R.string.filter_exported)
        data object ShowUnexported : ExportedFilterMode(R.string.filter_unexported)
    }

    sealed class PermissionFilterMode(id: Int) : FilterMode(id) {
        data object ShowAll : PermissionFilterMode(R.string.filter_all)
        data object ShowRequiresPermission : PermissionFilterMode(R.string.filter_requires_permission)
        data object ShowNoPermissionRequired : PermissionFilterMode(R.string.filter_requires_no_permission)
    }

    sealed class HasComponentsFilterMode(id: Int) : FilterMode(id) {
        data object ShowAll : HasComponentsFilterMode(R.string.filter_all)
        data object ShowHasComponents : HasComponentsFilterMode(R.string.filter_has_components)
        data object ShowHasNoComponents : HasComponentsFilterMode(R.string.filter_has_no_components)
    }

    sealed class SystemAppFilterMode(id: Int) : FilterMode(id) {
        data object ShowAll : SystemAppFilterMode(R.string.filter_all)
        data object ShowSystemApps : SystemAppFilterMode(R.string.filter_system_apps)
        data object ShowNonSystemApps : SystemAppFilterMode(R.string.filter_non_system_apps)
    }
}
