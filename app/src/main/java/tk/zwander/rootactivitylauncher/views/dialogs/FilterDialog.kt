package tk.zwander.rootactivitylauncher.views.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.FilterMode
import tk.zwander.rootactivitylauncher.views.components.OptionGroup

@Composable
fun FilterDialog(
    showing: Boolean,
    initialEnabledMode: FilterMode.EnabledFilterMode,
    initialExportedMode: FilterMode.ExportedFilterMode,
    initialPermissionMode: FilterMode.PermissionFilterMode,
    initialComponentMode: FilterMode.HasComponentsFilterMode,
    onDismissRequest: (
        FilterMode.EnabledFilterMode,
        FilterMode.ExportedFilterMode,
        FilterMode.PermissionFilterMode,
        FilterMode.HasComponentsFilterMode,
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
        var componentMode by remember {
            mutableStateOf(initialComponentMode)
        }

        BaseAlertDialog(
            onDismissRequest = {
                onDismissRequest(
                    initialEnabledMode,
                    initialExportedMode,
                    initialPermissionMode,
                    initialComponentMode,
                )
            },
            title = {
                Text(text = stringResource(id = R.string.filter))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissRequest(enabledMode, exportedMode, permissionMode, componentMode)
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
                            initialPermissionMode,
                            initialComponentMode,
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
                    OptionGroup(
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

                    OptionGroup(
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

                    OptionGroup(
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

                    OptionGroup(
                        name = stringResource(id = R.string.component_filter),
                        modes = arrayOf(
                            FilterMode.HasComponentsFilterMode.ShowAll,
                            FilterMode.HasComponentsFilterMode.ShowHasComponents,
                            FilterMode.HasComponentsFilterMode.ShowHasNoComponents
                        ),
                        onModeSelected = {
                            componentMode = it
                        },
                        selectedMode = componentMode
                    )
                }
            },
        )
    }
}
