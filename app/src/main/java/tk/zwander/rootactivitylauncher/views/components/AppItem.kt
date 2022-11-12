package tk.zwander.rootactivitylauncher.views.components

import android.content.pm.ApplicationInfo
import android.net.Uri
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ComponentActionButton
import tk.zwander.rootactivitylauncher.data.model.AppModel
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.util.isActuallyEnabled
import tk.zwander.rootactivitylauncher.views.dialogs.ComponentInfoDialog
import tk.zwander.rootactivitylauncher.views.dialogs.ExtrasDialog

@Composable
fun AppItem(
    info: AppModel,
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
    var enabled by rememberSaveable(info.info.packageName) {
        mutableStateOf(true)
    }

    val context = LocalContext.current

    val filteredActivities by info.filteredActivities.collectAsState()
    val filteredServices by info.filteredServices.collectAsState()
    val filteredReceivers by info.filteredReceivers.collectAsState()

    val activityCount by info.aSize.collectAsState(info.activitiesSize)
    val servicesCount by info.sSize.collectAsState(info.servicesSize)
    val receiversCount by info.rSize.collectAsState(info.receiversSize)

    val activitiesExpanded by info.activitiesExpanded.collectAsState()
    val servicesExpanded by info.servicesExpanded.collectAsState()
    val receiversExpanded by info.receiversExpanded.collectAsState()

    var activitiesLoading by remember {
        mutableStateOf(false)
    }
    var servicesLoading by remember {
        mutableStateOf(false)
    }
    var receiversLoading by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = activitiesExpanded) {
        if (activitiesExpanded) {
            activitiesLoading = !info.hasLoadedActivities.value
            withContext(Dispatchers.IO) {
                info.loadActivities(true)
                info.onFilterChange(true)
            }
            activitiesLoading = false
        }
    }

    LaunchedEffect(key1 = servicesExpanded) {
        if (servicesExpanded) {
            servicesLoading = !info.hasLoadedServices.value
            withContext(Dispatchers.IO) {
                info.loadServices(true)
                info.onFilterChange(true)
            }
            servicesLoading = false
        }
    }

    LaunchedEffect(key1 = receiversExpanded) {
        if (receiversExpanded) {
            receiversLoading = !info.hasLoadedReceivers.value
            withContext(Dispatchers.IO) {
                info.loadReceivers(true)
                info.onFilterChange(true)
            }
            receiversLoading = false
        }
    }

    LaunchedEffect(info.info.packageName) {
        enabled = withContext(Dispatchers.IO) {
            info.info.isActuallyEnabled(context)
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
                icon = remember {
                    getCoilData(info.info)
                },
                name = info.label.toString(),
                showActions = !isForTasker,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                app = info,
                whichButtons = remember {
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
                },
                enabled = enabled,
                onEnabledChanged = {
                    enabled = it
                }
            )

            ComponentGroup(
                titleRes = R.string.activities,
                items = filteredActivities,
                expanded = activitiesExpanded,
                onExpandChange = {
                    info.activitiesExpanded.value = it
                },
                modifier = Modifier.fillMaxWidth(),
                forTasker = isForTasker,
                onItemSelected = selectionCallback,
                count = activityCount,
                appEnabled = enabled,
                loading = activitiesLoading
            )

            ComponentGroup(
                titleRes = R.string.services,
                items = filteredServices,
                expanded = servicesExpanded,
                onExpandChange = {
                    info.servicesExpanded.value = it
                },
                modifier = Modifier.fillMaxWidth(),
                forTasker = isForTasker,
                onItemSelected = selectionCallback,
                count = servicesCount,
                appEnabled = enabled,
                loading = servicesLoading
            )

            ComponentGroup(
                titleRes = R.string.receivers,
                items = filteredReceivers,
                expanded = receiversExpanded,
                onExpandChange = {
                    info.receiversExpanded.value = it
                },
                modifier = Modifier.fillMaxWidth(),
                forTasker = isForTasker,
                onItemSelected = selectionCallback,
                count = receiversCount,
                appEnabled = enabled,
                loading = receiversLoading
            )
        }
    }

    ExtrasDialog(
        showing = showingIntentDialog,
        componentKey = info.info.packageName,
        onDismissRequest = { showingIntentDialog = false }
    )

    ComponentInfoDialog(
        info = info.pInfo,
        showing = showingComponentInfo,
        onDismissRequest = { showingComponentInfo = false }
    )
}

private fun getCoilData(info: ApplicationInfo): Any {
    val res = info.icon

    return if (res != 0) {
        Uri.parse("android.resource://${info.packageName}/$res")
    } else {
        Uri.parse("android.resource://android/${com.android.internal.R.drawable.sym_def_app_icon}")
    }
}
