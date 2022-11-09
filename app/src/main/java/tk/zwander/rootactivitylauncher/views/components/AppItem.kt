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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo

@Composable
fun AppItem(
    info: AppInfo,
    isForTasker: Boolean,
    selectionCallback: (BaseComponentInfo) -> Unit,
    progressCallback: (Float?) -> Unit,
    extractCallback: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var showingIntentDialog by remember {
        mutableStateOf(false)
    }
    var showingComponentInfo by remember {
        mutableStateOf(false)
    }

    val filteredActivities by info.filteredActivities.observeAsState()
    val filteredServices by info.filteredServices.observeAsState()
    val filteredReceivers by info.filteredReceivers.observeAsState()

    val hasLoadedActivities by info.hasLoadedActivities.observeAsState()
    val hasLoadedServices by info.hasLoadedServices.observeAsState()
    val hasLoadedReceivers by info.hasLoadedReceivers.observeAsState()

    val activityCount = if (hasLoadedActivities!!) filteredActivities!!.size else info._activitiesSize
    val servicesCount = if (hasLoadedServices!!) filteredServices!!.size else info._servicesSize
    val receiversCount = if (hasLoadedReceivers!!) filteredReceivers!!.size else info._receiversSize

    val activitiesExpanded by info.activitiesExpanded.observeAsState()
    val servicesExpanded by info.servicesExpanded.observeAsState()
    val receiversExpanded by info.receiversExpanded.observeAsState()

    LaunchedEffect(key1 = activitiesExpanded) {
        if (activitiesExpanded == true) {
            info.loadActivities { current, total ->
                progressCallback(current / total.toFloat())
            }
            info.onFilterChange()
            progressCallback(null)
        }
    }

    LaunchedEffect(key1 = servicesExpanded) {
        if (servicesExpanded == true) {
            info.loadServices { current, total ->
                progressCallback(current / total.toFloat())
            }
            info.onFilterChange()
            progressCallback(null)
        }
    }

    LaunchedEffect(key1 = receiversExpanded) {
        if (receiversExpanded == true) {
            info.loadReceivers { current, total ->
                progressCallback(current / total.toFloat())
            }
            info.onFilterChange()
            progressCallback(null)
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
                        Button.ComponentInfoButton(info.pInfo) {
                            showingComponentInfo = true
                        },
                        Button.IntentDialogButton(info.info.packageName) {
                            showingIntentDialog = true
                        },
                        Button.AppInfoButton(info.info.packageName),
                        Button.SaveApkButton(info, extractCallback)
                    )
                }
            )

            ComponentGroup(
                titleRes = R.string.activities,
                items = filteredActivities ?: listOf(),
                expanded = activitiesExpanded == true,
                onExpandChange = {
                    info.activitiesExpanded.value = it
                },
                modifier = Modifier.fillMaxWidth(),
                forTasker = isForTasker,
                onItemSelected = selectionCallback,
                count = activityCount
            )

            ComponentGroup(
                titleRes = R.string.services,
                items = filteredServices ?: listOf(),
                expanded = servicesExpanded == true,
                onExpandChange = {
                    info.servicesExpanded.value = it
                },
                modifier = Modifier.fillMaxWidth(),
                forTasker = isForTasker,
                onItemSelected = selectionCallback,
                count = servicesCount
            )

            ComponentGroup(
                titleRes = R.string.receivers,
                items = filteredReceivers ?: listOf(),
                expanded = receiversExpanded == true,
                onExpandChange = {
                    info.receiversExpanded.value = it
                },
                modifier = Modifier.fillMaxWidth(),
                forTasker = isForTasker,
                onItemSelected = selectionCallback,
                count = receiversCount
            )
        }
    }
    
    if (showingIntentDialog) {
        ExtrasDialog(
            componentKey = info.info.packageName, 
            onDismissRequest = { showingIntentDialog = false }
        )
    }

    ComponentInfoDialog(
        info = info.pInfo,
        showing = showingComponentInfo,
        onDismissRequest = { showingComponentInfo = false }
    )
}

private fun getCoilData(info: ApplicationInfo): Any? {
    val res = info.icon

    return if (res != 0) {
        Uri.parse("android.resource://${info.packageName}/$res")
    } else {
        Uri.parse("android.resource://android/${com.android.internal.R.drawable.sym_def_app_icon}")
    }
}
