package tk.zwander.rootactivitylauncher.data.model

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageItemInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import kotlinx.coroutines.CoroutineScope
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
) : BaseInfoModel(context, scope, mainModel) {
    override val initialActivitiesSize = MutableStateFlow(0)
    override val initialServicesSize = MutableStateFlow(0)
    override val initialReceiversSize = MutableStateFlow(0)

    init {
        scope.launch {
            activityKeys.collect {
                _loadedActivities.clear()
                initialActivitiesSize.value = it.size
                hasLoadedActivities.value = false
            }
        }

        scope.launch {
            serviceKeys.collect {
                _loadedServices.clear()
                initialServicesSize.value = it.size
                hasLoadedServices.value = false
            }
        }

        scope.launch {
            receiverKeys.collect {
                _loadedReceivers.clear()
                initialReceiversSize.value = it.size
                hasLoadedReceivers.value = false
            }
        }
    }

    @SuppressLint("InlinedApi")
    override suspend fun performActivityLoad(progress: (suspend () -> Unit)?): Collection<ActivityInfo> {
        val activityInfos = activityKeys.first().mapNotNull { key ->
            context.getInfoFromKey(key) {
                packageManager.getActivityInfoCompat(it, PackageManager.MATCH_DISABLED_COMPONENTS)
            }
        }.toTypedArray()

        return activityInfos.loadItems(progress) { input, label ->
            ActivityInfo(input, label.ifBlank {
                packageManager.getApplicationLabel(input.applicationInfo)
            })
        }.sortedBy { it.label.toString().lowercase() }
    }

    @SuppressLint("InlinedApi")
    override suspend fun performServiceLoad(progress: (suspend () -> Unit)?): Collection<ServiceInfo> {
        val activityInfos = serviceKeys.first().mapNotNull { key ->
            context.getInfoFromKey(key) {
                packageManager.getServiceInfoCompat(it, PackageManager.MATCH_DISABLED_COMPONENTS)
            }
        }.toTypedArray()

        return activityInfos.loadItems(progress) { input, label ->
            ServiceInfo(input, label.ifBlank {
                packageManager.getApplicationLabel(input.applicationInfo)
            })
        }.sortedBy { it.label.toString().lowercase() }
    }

    @SuppressLint("InlinedApi")
    override suspend fun performReceiverLoad(progress: (suspend () -> Unit)?): Collection<ReceiverInfo> {
        val activityInfos = receiverKeys.first().mapNotNull { key ->
            context.getInfoFromKey(key) {
                packageManager.getReceiverInfoCompat(it, PackageManager.MATCH_DISABLED_COMPONENTS)
            }
        }.toTypedArray()

        return activityInfos.loadItems(progress) { input, label ->
            ReceiverInfo(input, label.ifBlank {
                packageManager.getApplicationLabel(input.applicationInfo)
            })
        }.sortedBy { it.label.toString().lowercase() }
    }

    private fun <T : PackageItemInfo> Context.getInfoFromKey(key: String, getter: Context.(ComponentName) -> T): T? {
        val componentName = ComponentName.unflattenFromString(key)!!

        return try {
            getter(componentName)
        } catch (e: NameNotFoundException) {
            if (componentName.className.contains(".")) {
                val lastIndex = key.lastIndexOf(".")
                getInfoFromKey(key.replaceRange(lastIndex..lastIndex, "$"), getter)
            } else {
                null
            }
        }
    }
}