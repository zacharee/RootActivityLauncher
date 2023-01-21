package tk.zwander.rootactivitylauncher.data.model

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.ReceiverInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.util.getActivityInfoCompat
import tk.zwander.rootactivitylauncher.util.getReceiverInfoCompat
import tk.zwander.rootactivitylauncher.util.getServiceInfoCompat

data class FavoriteModel(
    val activityKeys: Flow<List<String>>,
    val serviceKeys: Flow<List<String>>,
    val receiverKeys: Flow<List<String>>,
    override val context: Context,
    override val scope: CoroutineScope,
    override val mainModel: MainModel
) : BaseInfoModel() {
    override val initialActivitiesSize = MutableStateFlow(0)
    override val initialServicesSize = MutableStateFlow(0)
    override val initialReceiversSize = MutableStateFlow(0)

    init {
        scope.launch(Dispatchers.IO) {
            activityKeys.collect {
                _hasLoadedActivities = false
                _loadedActivities.clear()
                initialActivitiesSize.value = it.size
                hasLoadedActivities.value = false
            }
        }

        scope.launch(Dispatchers.IO) {
            serviceKeys.collect {
                _hasLoadedServices = false
                _loadedServices.clear()
                initialServicesSize.value = it.size
                hasLoadedServices.value = false
            }
        }

        scope.launch(Dispatchers.IO) {
            receiverKeys.collect {
                _hasLoadedReceivers = false
                _loadedReceivers.clear()
                initialReceiversSize.value = it.size
                hasLoadedReceivers.value = false
            }
        }

        postInit()
    }

    @SuppressLint("InlinedApi")
    override suspend fun performActivityLoad(progress: (suspend () -> Unit)?): Collection<ActivityInfo> {
        val activityInfos = activityKeys.first().mapNotNull { key ->
            context.getInfoFromKey(key) {
                context.packageManager.getActivityInfoCompat(it, PackageManager.MATCH_DISABLED_COMPONENTS)
            }
        }.sortedBy {
            it.loadLabel(context.packageManager).toString().lowercase()
        }.toTypedArray()

        return activityInfos.loadItems(context.packageManager, progress) { input, label -> ActivityInfo(input, label) }
    }

    @SuppressLint("InlinedApi")
    override suspend fun performServiceLoad(progress: (suspend () -> Unit)?): Collection<ServiceInfo> {
        val activityInfos = serviceKeys.first().mapNotNull { key ->
            context.getInfoFromKey(key) {
                context.packageManager.getServiceInfoCompat(it, PackageManager.MATCH_DISABLED_COMPONENTS)
            }
        }.sortedBy {
            it.loadLabel(context.packageManager).toString().lowercase()
        }.toTypedArray()

        return activityInfos.loadItems(context.packageManager, progress) { input, label -> ServiceInfo(input, label) }
    }

    @SuppressLint("InlinedApi")
    override suspend fun performReceiverLoad(progress: (suspend () -> Unit)?): Collection<ReceiverInfo> {
        val activityInfos = receiverKeys.first().mapNotNull { key ->
            context.getInfoFromKey(key) {
                context.packageManager.getReceiverInfoCompat(it, PackageManager.MATCH_DISABLED_COMPONENTS)
            }
        }.sortedBy {
            it.loadLabel(context.packageManager).toString().lowercase()
        }.toTypedArray()

        return activityInfos.loadItems(context.packageManager, progress) { input, label -> ReceiverInfo(input, label) }
    }

    private fun <T : PackageItemInfo> Context.getInfoFromKey(key: String, getter: Context.(ComponentName) -> T): T? {
        return try {
            getter(ComponentName.unflattenFromString(key))
        } catch (e: NameNotFoundException) {
            val comp = key.split("/")[1]
            if (comp.contains(".")) {
                val lastIndex = key.lastIndexOf(".")
                getInfoFromKey(key.replaceRange(lastIndex..lastIndex, "$"), getter)
            } else {
                null
            }
        }
    }
}