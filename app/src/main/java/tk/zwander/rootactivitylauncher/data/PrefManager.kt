package tk.zwander.rootactivitylauncher.data

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageItemInfo
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import tk.zwander.rootactivitylauncher.util.constructComponentKey

class PrefManager private constructor(context: Context) : ContextWrapper(context) {
    companion object {
        private var instance: PrefManager? = null

        fun getInstance(context: Context): PrefManager {
            return instance ?: PrefManager(context.applicationContext).also {
                instance = it
            }
        }

        const val KEY_EXTRAS = "ACTIVITY_EXTRAS"
        const val KEY_TEMPLATE_COMPONENT_LABEL = "COMPONENT_LABEL_"
        const val KEY_TEMPLATE_APP_VERSION = "APP_VERSION_"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    private val gson = GsonBuilder()
        .create()

    var extras: HashMap<String, List<ExtraInfo>>
        get() = gson.fromJson(
            prefs.getString(KEY_EXTRAS, ""),
            object : TypeToken<HashMap<String, List<ExtraInfo>>>() {}.type
        ) ?: HashMap()
        set(value) {
            prefs.edit {
                putString(
                    KEY_EXTRAS,
                    gson.toJson(value)
                )
            }
        }

    fun getOrLoadComponentLabel(appInfo: PackageInfo, component: PackageItemInfo): CharSequence {
        val currentVersion = PackageInfoCompat.getLongVersionCode(appInfo)
        val storedVersion = prefs.getLong("$KEY_TEMPLATE_APP_VERSION${appInfo.packageName}", 0)

        return if (currentVersion == storedVersion) {
            prefs.getString(
                "$KEY_TEMPLATE_COMPONENT_LABEL${
                constructComponentKey(component)
                }", null
            ) ?: component.loadLabel(packageManager)
        } else {
            component.loadLabel(packageManager).also { label ->
                prefs.edit {
                    putString(
                        "$KEY_TEMPLATE_COMPONENT_LABEL${constructComponentKey(component)}",
                        label.toString()
                    )
                    putLong("$KEY_TEMPLATE_APP_VERSION${appInfo.packageName}", currentVersion)
                }
            }
        }
    }
}