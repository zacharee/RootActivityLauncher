package tk.zwander.rootactivitylauncher.views.components

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ComponentActionButton
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.model.AppModel
import tk.zwander.rootactivitylauncher.data.model.BaseInfoModel
import tk.zwander.rootactivitylauncher.util.getCoilData
import tk.zwander.rootactivitylauncher.util.isActuallyEnabled
import tk.zwander.rootactivitylauncher.views.dialogs.ComponentInfoDialog
import tk.zwander.rootactivitylauncher.views.dialogs.ExtrasDialog

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun AppItem(
    info: BaseInfoModel,
    isForTasker: Boolean,
    selectionCallback: (BaseComponentInfo) -> Unit,
    extractCallback: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var showingIntentDialog by remember {
        mutableStateOf(false)
    }
    var showingComponentInfo by remember {
        mutableStateOf(false)
    }
    var enabled by rememberSaveable(if (info is AppModel) info.info.packageName else null) {
        mutableStateOf(true)
    }

    val context = LocalContext.current

    val filteredActivities by info.filteredActivities.collectAsState()
    val filteredServices by info.filteredServices.collectAsState()
    val filteredReceivers by info.filteredReceivers.collectAsState()

    val activityCount by info.activitiesSize.collectAsState(info.initialActivitiesSize.value)
    val servicesCount by info.servicesSize.collectAsState(info.initialServicesSize.value)
    val receiversCount by info.receiversSize.collectAsState(info.initialReceiversSize.value)

    val activitiesExpanded by info.activitiesExpanded.collectAsState()
    val servicesExpanded by info.servicesExpanded.collectAsState()
    val receiversExpanded by info.receiversExpanded.collectAsState()

    val activitiesLoading by info.activitiesLoading.collectAsState()
    val servicesLoading by info.servicesLoading.collectAsState()
    val receiversLoading by info.receiversLoading.collectAsState()

    if (info is AppModel) {
        LaunchedEffect(info.info.packageName) {
            enabled = withContext(Dispatchers.IO) {
                info.info.isActuallyEnabled(context)
            }
        }
    }
    
    ElevatedCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppBar(
                icon = remember(info is AppModel) {
                    if (info is AppModel) info.info.getCoilData() else tk.zwander.patreonsupportersretrieval.R.drawable.ic_baseline_heart_24
                },
                name = if (info is AppModel) info.label.toString() else stringResource(id = R.string.favorites),
                showActions = !isForTasker && info is AppModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                app = info,
                whichButtons = remember(info is AppModel) {
                    if (info is AppModel) {
                        listOf(
                            ComponentActionButton.ComponentInfoButton(info.pInfo) {
                                showingComponentInfo = true
                            },
                            ComponentActionButton.IntentDialogButton(info.info.packageName) {
                                showingIntentDialog = true
                            },
                            ComponentActionButton.AppInfoButton(info.info.packageName),
                            ComponentActionButton.SaveApkButton(info, extractCallback)
                        )
                    } else {
                        listOf()
                    }
                },
                enabled = enabled,
                onEnabledChanged = {
                    enabled = it
                }
            )

            ComponentGroup(
                titleRes = R.string.activities,
                items = filteredActivities,
                forTasker = isForTasker,
                expanded = activitiesExpanded,
                appEnabled = enabled,
                loading = activitiesLoading,
                onExpandChange = {
                    info.activitiesExpanded.value = it
                },
                onItemSelected = selectionCallback,
                modifier = Modifier.fillMaxWidth(),
                count = activityCount
            )

            ComponentGroup(
                titleRes = R.string.services,
                items = filteredServices,
                forTasker = isForTasker,
                expanded = servicesExpanded,
                appEnabled = enabled,
                loading = servicesLoading,
                onExpandChange = {
                    info.servicesExpanded.value = it
                },
                onItemSelected = selectionCallback,
                modifier = Modifier.fillMaxWidth(),
                count = servicesCount
            )

            ComponentGroup(
                titleRes = R.string.receivers,
                items = filteredReceivers,
                forTasker = isForTasker,
                expanded = receiversExpanded,
                appEnabled = enabled,
                loading = receiversLoading,
                onExpandChange = {
                    info.receiversExpanded.value = it
                },
                onItemSelected = selectionCallback,
                modifier = Modifier.fillMaxWidth(),
                count = receiversCount
            )
        }
    }

    if (info is AppModel) {
        ExtrasDialog(
            showing = showingIntentDialog,
            componentKey = info.info.packageName
        ) { showingIntentDialog = false }

        ComponentInfoDialog(
            info = info.pInfo,
            showing = showingComponentInfo
        ) { showingComponentInfo = false }
    }
}
