package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.FilterMode

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FilterDialog(
    showing: Boolean,
    initialEnabledMode: FilterMode.EnabledFilterMode,
    initialExportedMode: FilterMode.ExportedFilterMode,
    initialPermissionMode: FilterMode.PermissionFilterMode,
    onDismissRequest: (
        FilterMode.EnabledFilterMode,
        FilterMode.ExportedFilterMode,
        FilterMode.PermissionFilterMode
    ) -> Unit,
) {
    if (showing) {
        var enabledMode by remember {
            mutableStateOf(initialEnabledMode)
        }
        var exportedMode by remember {
            mutableStateOf(initialExportedMode)
        }
        var permissionMode by remember {
            mutableStateOf(initialPermissionMode)
        }

        AlertDialog(
            onDismissRequest = {
                onDismissRequest(enabledMode, exportedMode, permissionMode)
            },
            title = {
                Text(text = stringResource(id = R.string.filter))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissRequest(enabledMode, exportedMode, permissionMode)
                    }
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismissRequest(
                            initialEnabledMode,
                            initialExportedMode,
                            initialPermissionMode
                        )
                    }
                ) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    FilterGroup(
                        name = stringResource(id = R.string.enabled_filter),
                        modes = arrayOf(
                            FilterMode.EnabledFilterMode.ShowAll,
                            FilterMode.EnabledFilterMode.ShowEnabled,
                            FilterMode.EnabledFilterMode.ShowDisabled
                        ),
                        onModeSelected = {
                            enabledMode = it
                        },
                        selectedMode = enabledMode
                    )

                    FilterGroup(
                        name = stringResource(id = R.string.exported_filter),
                        modes = arrayOf(
                            FilterMode.ExportedFilterMode.ShowAll,
                            FilterMode.ExportedFilterMode.ShowExported,
                            FilterMode.ExportedFilterMode.ShowUnexported
                        ),
                        onModeSelected = {
                            exportedMode = it
                        },
                        selectedMode = exportedMode
                    )

                    FilterGroup(
                        name = stringResource(id = R.string.permission_filter),
                        modes = arrayOf(
                            FilterMode.PermissionFilterMode.ShowAll,
                            FilterMode.PermissionFilterMode.ShowRequiresPermission,
                            FilterMode.PermissionFilterMode.ShowNoPermissionRequired
                        ),
                        onModeSelected = {
                            permissionMode = it
                        },
                        selectedMode = permissionMode
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(0.85f),
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}

@Composable
private fun <T : FilterMode> FilterGroup(
    name: String,
    modes: Array<T>,
    selectedMode: T,
    onModeSelected: (T) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = name,
            fontSize = 18.sp
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            modes.forEach { mode ->
                SelectableCard(
                    selected = mode.id == selectedMode.id,
                    onClick = { onModeSelected(mode) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(id = mode.id))
                    }
                }
            }
        }
    }
}
