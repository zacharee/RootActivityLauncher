package tk.zwander.rootactivitylauncher.views.components

import android.content.pm.ActivityInfo
import android.content.pm.ServiceInfo
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.github.skgmn.composetooltip.AnchorEdge
import com.github.skgmn.composetooltip.Tooltip
import com.github.skgmn.composetooltip.rememberTooltipStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ComponentActionButton
import tk.zwander.rootactivitylauncher.data.component.Availability
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.model.AppModel
import tk.zwander.rootactivitylauncher.data.model.BaseInfoModel
import tk.zwander.rootactivitylauncher.util.setComponentEnabled
import tk.zwander.rootactivitylauncher.util.setPackageEnabled

@Composable
fun AppBar(
    icon: Any?,
    name: String,
    app: BaseInfoModel,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    whichButtons: List<ComponentActionButton<*>>,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
) {
    val context = LocalContext.current

    var appStateError by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    BarGuts(
        icon = icon,
        name = name,
        subLabel = if (app is AppModel) app.info.packageName else null,
        enabled = enabled,
        availability = Availability.NA,
        onEnabledChanged = {
            if (app is AppModel) {
                appStateError = context.setPackageEnabled(app.info, it)?.message
                if (appStateError == null) {
                    onEnabledChanged(it)
                }
            }
        },
        whichButtons = whichButtons,
        modifier = modifier,
        showActions = showActions
    )

    ErrorDialog(error = appStateError) {
        appStateError = null
    }
}

@Composable
fun ComponentBar(
    icon: Any?,
    name: String,
    component: BaseComponentInfo,
    whichButtons: List<ComponentActionButton<*>>,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
) {
    val context = LocalContext.current

    var componentStateError by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    BarGuts(
        icon = icon,
        name = name,
        subLabel = component.component.flattenToString(),
        enabled = enabled,
        onEnabledChanged = {
            componentStateError = context.setComponentEnabled(component, it)?.message
            if (componentStateError == null) {
                onEnabledChanged(it)
            }
        },
        availability = rememberSaveable(component.component) {
            val requiresPermission = (component.info as? ActivityInfo)?.permission != null ||
                    (component.info as? ServiceInfo)?.permission != null

            when {
                !component.info.exported -> Availability.UNEXPORTED
                requiresPermission -> Availability.PERMISSION_REQUIRED
                else -> Availability.EXPORTED
            }
        },
        whichButtons = whichButtons,
        modifier = modifier,
        showActions = showActions
    )

    ErrorDialog(error = componentStateError) {
        componentStateError = null
    }
}

@Composable
private fun ErrorDialog(error: String?, onDismissRequest: () -> Unit) {
    if (error != null) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(text = stringResource(id = R.string.state_change_error))
            },
            text = {
                Text(text = stringResource(id = R.string.unable_to_change_component_state, error))
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BarGuts(
    icon: Any?,
    name: String,
    subLabel: String?,
    enabled: Boolean,
    onEnabledChanged: suspend (Boolean) -> Unit,
    availability: Availability,
    whichButtons: List<ComponentActionButton<*>>,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(
                    if (availability == Availability.NA) 48.dp else 36.dp
                )
            ) {
                var showingTooltip by remember {
                    mutableStateOf(false)
                }

                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .crossfade(true)
                        .data(icon)
                        .memoryCacheKey(subLabel)
                        .build(),
                    contentDescription = name,
                    modifier = Modifier
                        .size(if (availability == Availability.NA) 48.dp else 32.dp)
                        .align(Alignment.Center)
                        .then(
                            if (availability != Availability.NA) {
                                Modifier.combinedClickable(
                                    interactionSource = remember {
                                        MutableInteractionSource()
                                    },
                                    indication = null,
                                    onClick = {},
                                    onLongClick = {
                                        showingTooltip = true
                                    }
                                )
                            } else {
                                Modifier
                            }
                        )
                )

                if (availability != Availability.NA) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .align(Alignment.BottomEnd)
                            .background(colorResource(id = availability.tintRes))
                    )
                }

                if (showingTooltip) {
                    Tooltip(
                        anchorEdge = AnchorEdge.Top,
                        onDismissRequest = { showingTooltip = false },
                        tooltipStyle = rememberTooltipStyle(
                            color = MaterialTheme.colorScheme.surface,
                            tipHeight = 0.dp,
                            tipWidth = 0.dp
                        )
                    ) {
                        Text(text = stringResource(id = availability.labelRes))
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )

                subLabel?.let {
                    SelectionContainer {
                        Text(
                            text = subLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 12.sp
                        )
                    }
                }
            }

            if (showActions) {
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        scope.launch(Dispatchers.IO) {
                            onEnabledChanged(it)
                        }
                    }
                )
            }
        }

        if (showActions) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                whichButtons.forEach { button ->
                    var showingTooltip by remember {
                        mutableStateOf(false)
                    }
                    val buttonEnabled = button !is ComponentActionButton.LaunchButton || enabled

                    ComponentButton(
                        button = button,
                        showingTooltip = showingTooltip,
                        onShowingTooltipChanged = { showingTooltip = it },
                        enabled = buttonEnabled,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComponentButton(
    button: ComponentActionButton<*>,
    showingTooltip: Boolean,
    onShowingTooltipChanged: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animatedAlpha by animateFloatAsState(
        if (!enabled) 0.5f
        else 1.0f
    )
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier.clip(CircleShape)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                .combinedClickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = if (enabled) {
                        ripple(bounded = false)
                    } else {
                        null
                    },
                    enabled = true,
                    onLongClick = {
                        onShowingTooltipChanged(true)
                    },
                    onClick = {
                        if (enabled) {
                            scope.launch(Dispatchers.Main) {
                                button.onClick(context)
                            }
                        }
                    }
                )
        ) {
            Icon(
                painter = painterResource(id = button.getIconRes()),
                contentDescription = stringResource(id = button.getLabelRes()),
                tint = LocalContentColor.current.copy(alpha = animatedAlpha)
            )

            if (showingTooltip) {
                Tooltip(
                    anchorEdge = AnchorEdge.Top,
                    onDismissRequest = { onShowingTooltipChanged(false) },
                    tooltipStyle = rememberTooltipStyle(
                        color = MaterialTheme.colorScheme.surface,
                        tipHeight = 0.dp,
                        tipWidth = 0.dp,
                    ),
                ) {
                    Text(text = stringResource(id = button.getLabelRes()))
                }
            }
        }
    }
}
