package tk.zwander.rootactivitylauncher.data

import tk.zwander.rootactivitylauncher.R

sealed class SortMode(labelRes: Int) : BaseOption(labelRes) {
    data object SortByName : SortMode(R.string.sort_by_name)
    data object SortByInstalledDate : SortMode(R.string.sort_by_installed)
    data object SortByUpdatedDate : SortMode(R.string.sort_by_updated)
}