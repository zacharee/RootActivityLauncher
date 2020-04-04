package tk.zwander.rootactivitylauncher.data

import tk.zwander.rootactivitylauncher.R

enum class EnabledFilterMode(val id: Int) {
    SHOW_ENABLED(R.id.filter_enabled),
    SHOW_DISABLED(R.id.filter_disabled),
    SHOW_ALL(R.id.show_all_en)
}