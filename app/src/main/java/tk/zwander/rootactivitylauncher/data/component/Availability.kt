package tk.zwander.rootactivitylauncher.data.component

import tk.zwander.rootactivitylauncher.R

enum class Availability(val labelRes: Int, val tintRes: Int) {
    EXPORTED(R.string.exported, R.color.colorExported),
    PERMISSION_REQUIRED(R.string.permission_required, R.color.colorNeedsPermission),
    UNEXPORTED(R.string.unexported, R.color.colorUnexported),
    NA(0, 0)
}