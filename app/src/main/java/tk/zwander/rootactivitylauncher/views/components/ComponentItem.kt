package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tk.zwander.rootactivitylauncher.data.ComponentActionButton
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.util.getCoilData
import tk.zwander.rootactivitylauncher.util.isActuallyEnabled
import tk.zwander.rootactivitylauncher.views.dialogs.ComponentInfoDialog
import tk.zwander.rootactivitylauncher.views.dialogs.ExtrasDialog

@Composable
fun ComponentItem(
    forTasker: Boolean,
    component: BaseComponentInfo,
    appEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showingIntentOptions by rememberSaveable {
        mutableStateOf(false)
    }
    var showingComponentInfo by rememberSaveable {
        mutableStateOf(false)
    }
    var enabled by rememberSaveable {
        mutableStateOf(appEnabled)
    }

    LaunchedEffect(component.info.packageName, appEnabled) {
        enabled = withContext(Dispatchers.IO) {
            component.info.isActuallyEnabled(context)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        ComponentBar(
            icon = rememberSaveable {
                component.getCoilData()
            },
            name = component.label.toString(),
            component = component,
            whichButtons = remember(component.info.packageName) {
                arrayListOf(
                    ComponentActionButton.ComponentInfoButton(component.info) {
                        showingComponentInfo = true
                    },
                    ComponentActionButton.IntentDialogButton(component.component.flattenToString()) {
                        showingIntentOptions = true
                    },
                    ComponentActionButton.CreateShortcutButton(component),
                    ComponentActionButton.LaunchButton(component)
                )
            },
            enabled = enabled && appEnabled,
            onEnabledChanged = {
                enabled = it
            },
            showActions = !forTasker
        )
    }

    ExtrasDialog(
        showing = showingIntentOptions,
        componentKey = component.component.flattenToString()
    ) { showingIntentOptions = false }

    ComponentInfoDialog(
        info = component.info,
        showing = showingComponentInfo
    ) { showingComponentInfo = false }
}
