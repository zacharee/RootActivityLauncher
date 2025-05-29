package tk.zwander.rootactivitylauncher.util

import android.content.pm.ActivityInfo
import android.content.pm.ComponentInfo
import android.content.pm.ServiceInfo
import tk.zwander.rootactivitylauncher.data.model.AppModel

object AdvancedSearcher {
    enum class LogicMode {
        AND,
        OR;

        companion object {
            fun fromString(source: String, def: LogicMode): LogicMode {
                return entries.find {
                    it.name.lowercase() == source.lowercase()
                } ?: def
            }
        }
    }

    private const val MODE_DELIMITER = ";"
    private const val ITEM_DELIMITER = ","
    private const val EXP_DELIMITER = " "
    private const val TYPE_DELIMITER = ":"

    private const val HAS_PERMISSION = "has-permission"
    private const val REQUIRES_PERMISSION = "requires-permission"
    private const val DECLARES_PERMISSION = "declares-permission"
    private const val REQUIRES_FEATURE = "requires-feature"

    /**
     * Expects a [query] string in a format like:
     * `<possible earlier text> [type]:[AND;|OR;]item1,item2,item3,... <possible later text>`.
     *
     * If neither AND; nor OR; are found, the default mode is [LogicMode.AND].
     */
    private fun itemMatch(
        query: String,
        type: String,
        checker: (LogicMode, List<String>) -> Boolean
    ): Boolean {
        if (!query.contains("$type$TYPE_DELIMITER")) return false

        val i = query.substringAfter("$type$TYPE_DELIMITER").substringBefore(EXP_DELIMITER)
        val (mode, items) = try {
            if (i.contains(MODE_DELIMITER)) {
                i.split(MODE_DELIMITER).run {
                    LogicMode.fromString(this[0], LogicMode.AND) to
                            this[1].split(ITEM_DELIMITER)
                }
            } else {
                LogicMode.AND to i.split(ITEM_DELIMITER)
            }
        } catch (_: Exception) {
            return false
        }

        return checker(mode, items)
    }

    private fun logicMatch(mode: LogicMode, items: List<String>, availableItems: List<String>): Boolean {
        return if (mode == LogicMode.OR) {
            items.any { availableItems.contains(it) }
        } else {
            items.all { availableItems.contains(it) }
        }
    }

    fun matchesHasPermission(query: String, data: AppModel): Boolean {
        return itemMatch(query, HAS_PERMISSION) { mode, items ->
            val p = data.pInfo.requestedPermissions ?: return@itemMatch false

            logicMatch(mode, items, p.toList())
        }
    }

    fun matchesRequiresPermission(query: String, data: AppModel): Boolean {
        return matchesRequiresPermission(query, data.info?.permission)
    }

    fun matchesRequiresPermission(query: String, componentInfo: ComponentInfo): Boolean {
        val permission = when (componentInfo) {
            is ActivityInfo -> componentInfo.permission
            is ServiceInfo -> componentInfo.permission
            else -> null
        }

        return matchesRequiresPermission(query, permission)
    }

    private fun matchesRequiresPermission(query: String, requiredPermission: String?): Boolean {
        return itemMatch(query, REQUIRES_PERMISSION) { _, items ->
            // Always an OR check, so we don't care about any mode given.
            items.contains(requiredPermission)
        }
    }

    fun matchesDeclaresPermission(query: String, data: AppModel): Boolean {
        return itemMatch(query, DECLARES_PERMISSION) { mode, items ->
            val declaredPermissions = data.pInfo.permissions?.map { it.name } ?: return@itemMatch false

            logicMatch(mode, items, declaredPermissions)
        }
    }

    fun matchesRequiresFeature(query: String, data: AppModel): Boolean {
        return itemMatch(query, REQUIRES_FEATURE) { mode, items ->
            val requiredFeatures = data.pInfo.reqFeatures?.map { it.name } ?: return@itemMatch false

            logicMatch(mode, items, requiredFeatures)
        }
    }
}