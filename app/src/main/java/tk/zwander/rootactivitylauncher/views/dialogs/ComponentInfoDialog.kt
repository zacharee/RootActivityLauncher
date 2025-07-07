package tk.zwander.rootactivitylauncher.views.dialogs

import android.content.ClipData
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.util.applyQuery
import tk.zwander.rootactivitylauncher.util.processActivityInfo
import tk.zwander.rootactivitylauncher.util.processPackageInfo
import tk.zwander.rootactivitylauncher.util.processServiceInfo

@Composable
fun <T : Any> ComponentInfoDialog(
    info: T,
    showing: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (showing) {
        val clipboardManager = LocalClipboard.current
        val highlightColor = MaterialTheme.colorScheme.primary
        val scope = rememberCoroutineScope()

        var query by remember {
            mutableStateOf("")
        }

        val dump = remember {
            mutableStateListOf<AnnotatedString>()
        }

        LaunchedEffect(query) {
            val d = if (dump.isEmpty()) {
                withContext(Dispatchers.IO) {
                    when (info) {
                        is ActivityInfo -> processActivityInfo(info)
                        is ServiceInfo -> processServiceInfo(info)
                        is PackageInfo -> processPackageInfo(info)
                        else -> listOf()
                    }
                }
            } else dump

            val q = withContext(Dispatchers.IO) {
                applyQuery(highlightColor, d, query, dump.isEmpty())
            }

            dump.clear()
            dump.addAll(q)
        }

        BaseAlertDialog(
            title = {
                Text(text = stringResource(id = R.string.component_info))
            },
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            clipboardManager.setClipEntry(
                                ClipEntry(ClipData.newPlainText(null, dump.joinToString("\n")))
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(id = tk.zwander.patreonsupportersretrieval.R.string.copy))
                }
            },
            text = {
                ComponentInfoContents(
                    query = query,
                    onQueryChanged = { query = it },
                    content = dump,
                    modifier = Modifier,
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComponentInfoContents(
    query: String,
    onQueryChanged: (String) -> Unit,
    content: List<AnnotatedString>,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = content.isEmpty(),
        modifier = modifier,
    ) {
        if (it) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = stringResource(id = R.string.search))
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(autoCorrectEnabled = false),
                )

                SelectionContainer {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        itemsIndexed(items = content, key = { index, item -> item.toString() + index }) { _, item ->
                            Text(text = item)
                        }
                    }
                }
            }
        }
    }
}
