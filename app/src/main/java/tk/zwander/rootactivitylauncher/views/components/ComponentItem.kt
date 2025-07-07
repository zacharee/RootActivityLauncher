package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.util.isActuallyEnabled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentItem(
    forTasker: Boolean,
    component: BaseComponentInfo,
    appEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

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
        contentAlignment = Alignment.CenterStart,
    ) {
        ComponentBar(
            component = component,
            enabled = enabled && appEnabled,
            onEnabledChanged = {
                enabled = it
            },
            showActions = !forTasker,
        )
    }
}
