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
import tk.zwander.rootactivitylauncher.data.SortMode
import tk.zwander.rootactivitylauncher.data.SortOrder
import tk.zwander.rootactivitylauncher.views.components.OptionGroup

@Composable
fun SortDialog(
    showing: Boolean,
    initialSortBy: SortMode,
    initialSortOrder: SortOrder,
    onDismissRequest: (SortMode, SortOrder) -> Unit,
) {
    if (showing) {
        var sortBy by remember {
            mutableStateOf(initialSortBy)
        }
        var sortOrder by remember {
            mutableStateOf(initialSortOrder)
        }

        BaseAlertDialog(
            onDismissRequest = {
                onDismissRequest(initialSortBy, initialSortOrder)
            },
            title = {
                Text(text = stringResource(id = R.string.sort_apps))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissRequest(sortBy, sortOrder)
                    }
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismissRequest(initialSortBy, initialSortOrder)
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
                        name = stringResource(id = R.string.sort_by),
                        modes = arrayOf(
                            SortMode.SortByName,
                            SortMode.SortByUpdatedDate,
                            SortMode.SortByInstalledDate,
                        ),
                        onModeSelected = {
                            sortBy = it
                        },
                        selectedMode = sortBy,
                    )

                    OptionGroup(
                        name = stringResource(id = R.string.sort_order),
                        modes = arrayOf(
                            SortOrder.Ascending,
                            SortOrder.Descending,
                        ),
                        onModeSelected = {
                            sortOrder = it
                        },
                        selectedMode = sortOrder,
                    )
                }
            },
        )
    }
}
