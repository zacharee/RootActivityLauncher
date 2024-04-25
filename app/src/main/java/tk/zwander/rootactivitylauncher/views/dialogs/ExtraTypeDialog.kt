package tk.zwander.rootactivitylauncher.views.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.ExtraType
import tk.zwander.rootactivitylauncher.views.components.SelectableCard

@Composable
fun ExtrasTypeDialog(
    showing: Boolean,
    extraInfo: ExtraInfo,
    onDismissRequest: () -> Unit,
    onTypeSelected: (ExtraType) -> Unit,
) {
    if (showing) {
        BaseAlertDialog(
            onDismissRequest = {
                onDismissRequest()
            },
            title = {
                Text(text = stringResource(id = R.string.type))
            },
            text = {
                ExtrasTypeDialogContents(
                    initial = extraInfo.safeType,
                    onTypeSelected = {
                        onTypeSelected(it)
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { onDismissRequest() }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun ExtrasTypeDialogContents(
    initial: ExtraType,
    onTypeSelected: (ExtraType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sortedTypes = remember {
        ExtraType.entries.sortedBy {
            context.resources.getString(it.nameRes)
        }
    }
    val state = rememberLazyListState()

    LaunchedEffect(initial) {
        state.scrollToItem(sortedTypes.indexOf(initial))
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = state
    ) {
        items(items = sortedTypes, key = { it.value }) { type ->
            SelectableCard(
                modifier = Modifier.fillMaxWidth(),
                selected = initial == type,
                onClick = {
                    onTypeSelected(type)
                },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(id = type.nameRes))
                }
            }
        }
    }
}
