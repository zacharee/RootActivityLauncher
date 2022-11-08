package tk.zwander.rootactivitylauncher.data

import tk.zwander.rootactivitylauncher.R

sealed class FilterMode(val id: Int) {
    sealed class EnabledFilterMode(id: Int) : FilterMode(id) {
        object ShowAll : EnabledFilterMode(R.string.filter_all)
        object ShowEnabled : EnabledFilterMode(R.string.filter_enabled)
        object ShowDisabled : EnabledFilterMode(R.string.filter_disabled)
    }

    sealed class ExportedFilterMode(id: Int) : FilterMode(id) {
        object ShowAll : ExportedFilterMode(R.string.filter_all)
        object ShowExported : ExportedFilterMode(R.string.filter_exported)
        object ShowUnexported : ExportedFilterMode(R.string.filter_unexported)
    }

    sealed class PermissionFilterMode(id: Int) : FilterMode(id) {
        object ShowAll : PermissionFilterMode(R.string.filter_all)
        object ShowRequiresPermission : PermissionFilterMode(R.string.filter_requires_permission)
        object ShowNoPermissionRequired : PermissionFilterMode(R.string.filter_requires_no_permission)
    }
}
