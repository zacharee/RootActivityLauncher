package tk.zwander.rootactivitylauncher.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import androidx.core.content.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.prefs: PrefManager
    get() = PrefManager.getInstance(this)

class PrefManager private constructor(context: Context) : ContextWrapper(context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: PrefManager? = null

        fun getInstance(context: Context): PrefManager {
            return instance ?: PrefManager(context.applicationContext).also {
                instance = it
            }
        }

        const val KEY_EXTRAS = "ACTIVITY_EXTRAS"
        const val KEY_ACTIONS = "COMPONENT_ACTIONS"
        const val KEY_DATAS = "COMPONENT_DATAS"
        const val KEY_CATEGORIES = "COMPONENT_CATEGORIES"

        val KEY_FAVORITE_ACTIVITIES = stringSetPreferencesKey("FAVORITE_ACTIVITIES")
        val KEY_FAVORITE_SERVICES = stringSetPreferencesKey("FAVORITE_SERVICES")
        val KEY_FAVORITE_RECEIVERS = stringSetPreferencesKey("FAVORITE_RECEIVERS")
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    private val Context.store by preferencesDataStore("compose_prefs")
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
    var actions: HashMap<String, String?>
        get() = gson.fromJson(
            prefs.getString(KEY_ACTIONS, ""),
            object : TypeToken<HashMap<String, String?>>() {}.type
        ) ?: HashMap()
        set(value) {
            prefs.edit {
                putString(
                    KEY_ACTIONS,
                    gson.toJson(value)
                )
            }
        }
    var datas: HashMap<String, String?>
        get() = gson.fromJson(
            prefs.getString(KEY_DATAS, ""),
            object : TypeToken<HashMap<String, String?>>() {}.type
        ) ?: HashMap()
        set(value) {
            prefs.edit {
                putString(
                    KEY_DATAS,
                    gson.toJson(value)
                )
            }
        }
    var categories: HashMap<String, ArrayList<String?>>
        get() = gson.fromJson(
            prefs.getString(KEY_CATEGORIES, ""),
            object : TypeToken<HashMap<String, ArrayList<String?>>>() {}.type
        ) ?: HashMap()
        set(value) {
            prefs.edit {
                putString(
                    KEY_CATEGORIES,
                    gson.toJson(value)
                )
            }
        }

    val favoriteActivities: Flow<List<String>>
        get() = store.data.map { ArrayList(it[KEY_FAVORITE_ACTIVITIES] ?: setOf()) }
    val favoriteReceivers: Flow<List<String>>
        get() = store.data.map { ArrayList(it[KEY_FAVORITE_RECEIVERS] ?: setOf()) }
    val favoriteServices: Flow<List<String>>
        get() = store.data.map { ArrayList(it[KEY_FAVORITE_SERVICES] ?: setOf()) }

    suspend fun updateFavoriteActivities(activities: List<String>) {
        updateFavorites(KEY_FAVORITE_ACTIVITIES, activities)
    }

    suspend fun updateFavoriteReceivers(receivers: List<String>) {
        updateFavorites(KEY_FAVORITE_RECEIVERS, receivers)
    }

    suspend fun updateFavoriteServices(services: List<String>) {
        updateFavorites(KEY_FAVORITE_SERVICES, services)
    }

    private suspend fun updateFavorites(key: Preferences.Key<Set<String>>, items: List<String>) {
        store.edit {
            it[key] = items.toSet()
        }
    }
}