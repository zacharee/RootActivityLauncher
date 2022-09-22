package tk.zwander.rootactivitylauncher.data

import tk.zwander.rootactivitylauncher.R

enum class PermissionFilterMode(val id: Int) {
    SHOW_REQUIRES_PERMISSION(R.id.filter_requires_permission),
    SHOW_REQUIRES_NO_PERMISSION(R.id.filter_requires_no_permission),
    SHOW_ALL(R.id.show_all_perm)
}