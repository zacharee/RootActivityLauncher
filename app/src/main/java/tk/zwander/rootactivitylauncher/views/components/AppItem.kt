package tk.zwander.rootactivitylauncher.views.components

import android.content.Context
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.views.ComponentInfoDialog

@Composable
fun AppItem(
    info: AppInfo,
    isForTasker: Boolean,
    selectionCallback: (BaseComponentInfo) -> Unit,
    progressCallback: (Int) -> Unit,
    extractCallback: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    var showingIntentDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = info.activitiesExpanded) {
        if (info.activitiesExpanded) {
            info.loadActivities { current, total ->
                progressCallback((current / total.toFloat() * 100f).toInt())
            }
        }
    }

    LaunchedEffect(key1 = info.servicesExpanded) {
        if (info.receiversExpanded) {
            info.loadServices { current, total ->
                progressCallback((current / total.toFloat() * 100f).toInt())
            }
        }
    }

    LaunchedEffect(key1 = info.receiversExpanded) {
        if (info.receiversExpanded) {
            info.loadActivities { current, total ->
                progressCallback((current / total.toFloat() * 100f).toInt())
            }
        }
    }
    
    ElevatedCard(
        modifier = modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppBar(
                icon = rememberAsyncImagePainter(model = context.getCoilData(info.info)),
                name = info.label.toString(),
                showActions = !isForTasker,
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                app = info,
                whichButtons = listOf(
                    Button.ComponentInfoButton(info.pInfo) {
                        ComponentInfoDialog(context, it).show()
                    },
                    Button.IntentDialogButton(info.info.packageName) {
                        showingIntentDialog = true
                    },
                    Button.AppInfoButton(info.info.packageName),
                    Button.SaveApkButton(info, extractCallback)
                )
            )

            ComponentGroup(
                titleRes = R.string.activities,
                items = info.filteredActivities,
                expanded = info.activitiesExpanded,
                onExpandChange = {
                    info.activitiesExpanded = it
                },
                modifier = Modifier.fillMaxWidth(),
                forTasker = isForTasker,
                onItemSelected = selectionCallback,
                count = info.activitiesSize
            )

            ComponentGroup(
                titleRes = R.string.services,
                items = info.filteredServices,
                expanded = info.servicesExpanded,
                onExpandChange = {
                    info.servicesExpanded = it
                },
                modifier = Modifier.fillMaxWidth(),
                forTasker = isForTasker,
                onItemSelected = selectionCallback,
                count = info.servicesSize
            )

            ComponentGroup(
                titleRes = R.string.receivers,
                items = info.filteredReceivers,
                expanded = info.receiversExpanded,
                onExpandChange = {
                    info.receiversExpanded = it
                },
                modifier = Modifier.fillMaxWidth(),
                forTasker = isForTasker,
                onItemSelected = selectionCallback,
                count = info.receiversSize
            )
        }
    }
    
    if (showingIntentDialog) {
        ExtrasDialog(
            componentKey = info.info.packageName, 
            onDismissRequest = { showingIntentDialog = false }
        )
    }
}

private fun Context.getCoilData(info: ApplicationInfo): Any? {
    val res = info.iconRes.run {
        if (this == 0) info.roundIconRes
        else this
    }

    return if (res != 0) {
        Uri.parse("android.resource://${info.packageName}/$res")
    } else {
        info.loadIcon(packageManager)
    }
}
