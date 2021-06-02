package tk.zwander.rootactivitylauncher.util

import android.content.pm.ActivityInfo
import android.content.pm.ComponentInfo
import android.content.pm.ServiceInfo
import tk.zwander.rootactivitylauncher.data.AppInfo

object AdvancedSearcher {
    enum class LogicMode {
        AND,
        OR;

        companion object {
            fun fromString(source: String, def: LogicMode): LogicMode {
                return when (source.lowercase()) {
                    "or" -> OR
                    "and" -> AND
                    else -> def
                }
            }
        }
    }

    fun matchesHasPermission(query: String, data: AppInfo): Boolean {
        if (!query.contains("has-permission:")) return false

        val permission = query.substringAfter("has-permission:").substringBefore(" ")
        val (mode, permissions) = try {
            if (permission.contains(";")) {
                permission.split(";").run {
                    LogicMode.fromString(this[0], LogicMode.AND) to
                            this[1].split(",") }
            } else {
                LogicMode.AND to permission.split(",")
            }
        } catch (e: Exception) {
            return false
        }

        val requestedPermissions = data.pInfo.requestedPermissions ?: return false

        return if (mode == LogicMode.OR) {
            permissions.any { requestedPermissions.contains(it) }
        } else {
            permissions.all { requestedPermissions.contains(it) }
        }
    }

    fun matchesRequiresPermission(query: String, data: AppInfo): Boolean {
        return matchesRequiresPermission(query, data.info.permission)
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
        if (!query.contains("requires-permission:")) return false
        if (requiredPermission == null) return false

        val permission = query.substringAfter("requires-permission:").substringBefore(" ")
        val (mode, permissions) = try {
            if (permission.contains(";")) {
                permission.split(";").run {
                    LogicMode.fromString(this[0], LogicMode.AND) to
                            this[1].split(",") }
            } else {
                LogicMode.AND to permission.split(",")
            }
        } catch (e: Exception) {
            return false
        }

        //Always OR mode here
        return permissions.contains(requiredPermission)
    }

    fun matchesDeclaresPermission(query: String, data: AppInfo): Boolean {
        if (!query.contains("declares-permission:")) return false

        val permission = query.substringAfter("declares-permission:").substringBefore(" ")
        val (mode, permissions) = try {
            if (permission.contains(";")) {
                permission.split(";").run {
                    LogicMode.fromString(this[0], LogicMode.AND) to
                            this[1].split(",") }
            } else {
                LogicMode.AND to permission.split(",")
            }
        } catch (e: Exception) {
            return false
        }

        val declaredPermissions = data.pInfo.permissions?.map { it.name } ?: return false

        return if (mode == LogicMode.AND) {
            permissions.any { declaredPermissions.contains(it) }
        } else {
            permissions.all { declaredPermissions.contains(it) }
        }
    }
}