package tk.zwander.rootactivitylauncher.data

import tk.zwander.rootactivitylauncher.R

enum class ExportedFilterMode(val id: Int) {
    SHOW_EXPORTED(R.id.filter_exported),
    SHOW_UNEXPORTED(R.id.filter_unexported),
    SHOW_ALL(R.id.show_all_ex)
}