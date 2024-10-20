package tk.zwander.rootactivitylauncher.data

import tk.zwander.rootactivitylauncher.R

sealed class SortOrder(labelRes: Int) : BaseOption(labelRes) {
    abstract fun applyOrder(input: Int): Int

    data object Ascending : SortOrder(R.string.sort_ascending) {
        override fun applyOrder(input: Int): Int {
            return input
        }
    }
    data object Descending : SortOrder(R.string.sort_descending) {
        override fun applyOrder(input: Int): Int {
            return -input
        }
    }
}
