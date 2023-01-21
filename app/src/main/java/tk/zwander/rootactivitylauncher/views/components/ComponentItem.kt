package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ComponentActionButton
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.util.getCoilData
import tk.zwander.rootactivitylauncher.util.isActuallyEnabled
import tk.zwander.rootactivitylauncher.views.dialogs.ComponentInfoDialog
import tk.zwander.rootactivitylauncher.views.dialogs.ExtrasDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
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
    val launchErrors = remember {
        mutableStateListOf<Pair<String, Throwable>>()
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
            whichButtons = remember(component.component.flattenToString()) {
                arrayListOf(
                    ComponentActionButton.FavoriteButton(component),
                    ComponentActionButton.ComponentInfoButton(component.info) {
                        showingComponentInfo = true
                    },
                    ComponentActionButton.IntentDialogButton(component.component.flattenToString()) {
                        showingIntentOptions = true
                    },
                    ComponentActionButton.CreateShortcutButton(component),
                    ComponentActionButton.LaunchButton(component) {
                        launchErrors.clear()
                        launchErrors.addAll(it)
                    }
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

    if (launchErrors.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                launchErrors.clear()
            },
            title = {
                Text(text = stringResource(id = R.string.launch_error))
            },
            text = {
                val expanded = remember {
                    mutableStateMapOf<Int, Boolean>()
                }

                Column {
                    Text(
                        text = stringResource(id = R.string.unable_to_launch_template)
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(launchErrors.size, { it }) {
                            val item = launchErrors[it]
                            val rotation by animateFloatAsState(targetValue = if (expanded[it] == true) 180f else 0f)

                            Card(
                                onClick = { expanded[it] = !(expanded[it] ?: false) },
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = item.first,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Spacer(Modifier.weight(1f))

                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                modifier = Modifier.rotate(rotation)
                                            )
                                        }

                                        AnimatedVisibility(visible = expanded[it] ?: false) {
                                            Column {
                                                Spacer(Modifier.size(8.dp))

                                                SelectionContainer {
                                                    Text(text = item.second.message ?: "")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { launchErrors.clear() }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}
