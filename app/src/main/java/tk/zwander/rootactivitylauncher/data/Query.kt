package tk.zwander.rootactivitylauncher.data

import android.content.Context
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo

sealed class Query(val raw: String) {
    val isBlank: Boolean
        get() = raw.isBlank()

    abstract fun checkMatch(context: Context, data: BaseComponentInfo, advancedMatch: Boolean): Boolean

    data class StringQuery(val query: String) : Query(query) {
        override fun checkMatch(context: Context, data: BaseComponentInfo, advancedMatch: Boolean): Boolean {
            return data.info.name.contains(query, true)
                    || (data.label.contains(query, true))
                    || advancedMatch
        }
    }

    data class RegexQuery(val query: Regex) : Query(query.pattern) {
        override fun checkMatch(
            context: Context,
            data: BaseComponentInfo,
            advancedMatch: Boolean
        ): Boolean {
            return query.run {
                containsMatchIn(data.info.name)
                        || containsMatchIn(data.label)
            } || advancedMatch
        }
    }
}
