package tk.zwander.rootactivitylauncher.views.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.views.ComponentInfoDialog

@Composable
fun ComponentItem(
    forTasker: Boolean,
    component: BaseComponentInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showingIntentOptions by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier
            .then(if (forTasker) {
                Modifier.clickable {
                    onClick()
                }
            } else Modifier)
    ) {
        ComponentBar(
            icon = rememberAsyncImagePainter(model = context.getCoilData(component)),
            name = component.label.toString(),
            component = component,
            whichButtons = listOf(
                Button.ComponentInfoButton(component.info) {
                    ComponentInfoDialog(
                        context,
                        it
                    ).show()
                },
                Button.IntentDialogButton(component.component.flattenToString()) {
                    showingIntentOptions = true
                },
                Button.CreateShortcutButton(component),
                Button.LaunchButton(component)
            )
        )
    }

    if (showingIntentOptions) {
        ExtrasDialog(
            componentKey = component.component.flattenToString(),
            onDismissRequest = { showingIntentOptions = false }
        )
    }
}

private fun Context.getCoilData(data: BaseComponentInfo): Any? {
    val res = data.info.iconResource.run {
        if (this == 0) data.info.applicationInfo.iconRes.run {
            if (this == 0) data.info.applicationInfo.roundIconRes
            else this
        }
        else this
    }

    return if (res != 0) {
        Uri.parse("android.resource://${data.info.packageName}/$res")
    } else {
        Uri.parse("android.resource://android/${com.android.internal.R.drawable.sym_def_app_icon}")
    }
}
