package tk.zwander.rootactivitylauncher.data.model

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.google.android.gms.common.internal.Objects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.ReceiverInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo

data class AppModel(
    val pInfo: PackageInfo,
    val info: ApplicationInfo = pInfo.applicationInfo,
    val label: CharSequence,
    override val mainModel: MainModel,
    override val scope: CoroutineScope,
    override val context: Context,
) : BaseInfoModel() {
    override val initialActivitiesSize = MutableStateFlow(pInfo.activities?.size ?: 0)
    override val initialServicesSize = MutableStateFlow(pInfo.services?.size ?: 0)
    override val initialReceiversSize = MutableStateFlow(pInfo.receivers?.size ?: 0)

    init {
        postInit()
    }

    override fun equals(other: Any?): Boolean {
        return other is AppModel
                && info.packageName == other.info.packageName
                && super.equals(other)
                && activitiesSize.value == other.activitiesSize.value
                && servicesSize.value == other.servicesSize.value
                && receiversSize.value == other.receiversSize.value
                && filteredActivities.value == other.filteredActivities.value
                && filteredServices.value == other.filteredServices.value
                && filteredReceivers.value == other.filteredReceivers.value
    }

    override fun hashCode(): Int {
        return info.packageName.hashCode() +
                31 * super.hashCode() +
                Objects.hashCode(
                    activitiesSize.value,
                    servicesSize.value,
                    receiversSize.value,
                    filteredActivities.value,
                    filteredServices.value,
                    filteredReceivers.value
                )
    }

    override suspend fun performActivityLoad(progress: (suspend () -> Unit)?): Collection<ActivityInfo> {
        return pInfo.activities.loadItems(
            progress = progress
        ) { input, label -> ActivityInfo(input, label.ifBlank { this.label }) }
            .toSortedSet()
    }

    override suspend fun performServiceLoad(progress: (suspend () -> Unit)?): Collection<ServiceInfo> {
        return pInfo.services.loadItems(
            progress = progress
        ) { input, label -> ServiceInfo(input, label.ifBlank { this.label }) }
            .toSortedSet()
    }

    override suspend fun performReceiverLoad(progress: (suspend () -> Unit)?): Collection<ReceiverInfo> {
        return pInfo.receivers.loadItems(
            progress = progress
        ) { input, label -> ReceiverInfo(input, label.ifBlank { this.label }) }
            .toSortedSet()
    }
}
