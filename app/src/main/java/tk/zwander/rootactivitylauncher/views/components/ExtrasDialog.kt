package tk.zwander.rootactivitylauncher.views.components

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.MutableStateFlow
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.ExtraType
import tk.zwander.rootactivitylauncher.util.findActionForComponent
import tk.zwander.rootactivitylauncher.util.findCategoriesForComponent
import tk.zwander.rootactivitylauncher.util.findDataForComponent
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.util.updateActionForComponent
import tk.zwander.rootactivitylauncher.util.updateCategoriesForComponent
import tk.zwander.rootactivitylauncher.util.updateDataForComponent
import tk.zwander.rootactivitylauncher.util.updateExtrasForComponent
import java.util.UUID

class ExtrasDialogModel(private val context: Context, private val componentKey: String) {
    val extras = MutableStateFlow<List<Pair<UUID, ExtraInfo>>>(
        ArrayList<Pair<UUID, ExtraInfo>>().apply {
            addAll(context.findExtrasForComponent(componentKey).map {
                UUID.randomUUID() to it
            })

            add(UUID.randomUUID() to ExtraInfo("", ""))
        }
    )

    val categories = MutableStateFlow<List<Pair<UUID, String?>>>(
        ArrayList<Pair<UUID, String?>>().apply {
            addAll(context.findCategoriesForComponent(componentKey).map {
                UUID.randomUUID() to it
            })

            add(UUID.randomUUID() to "")
        }
    )

    val action = MutableStateFlow(context.findActionForComponent(componentKey))
    val data = MutableStateFlow(context.findDataForComponent(componentKey))
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ExtrasDialog(
    componentKey: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val model = remember {
        ExtrasDialogModel(context, componentKey)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    context.updateExtrasForComponent(
                        componentKey,
                        model.extras.value.mapNotNull { if (it.second.key.isBlank()) null else it.second }
                    )
                    context.updateActionForComponent(componentKey, model.action.value)
                    context.updateDataForComponent(componentKey, model.data.value)
                    context.updateCategoriesForComponent(
                        componentKey,
                        model.categories.value.mapNotNull { if (it.second.isNullOrBlank()) null else it.second }
                    )

                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        },
        modifier = modifier.fillMaxWidth(0.85f),
        title = {
            Text(text = stringResource(id = R.string.intent))
        },
        text = {
            ExtrasDialogContents(
                model = model,
                modifier = Modifier.fillMaxWidth()
                    .animateContentSize()
            )
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtrasDialogContents(
    model: ExtrasDialogModel,
    modifier: Modifier = Modifier
) {
    val action by model.action.collectAsState()
    val data by model.data.collectAsState()
    val categories by model.categories.collectAsState()
    val extras by model.extras.collectAsState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedTextField(
            value = action,
            onValueChange = {
                model.action.value = it
            },
            label = {
                Text(text = stringResource(id = R.string.action))
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = data ?: "",
            onValueChange = {
                model.data.value = it.ifBlank { null }
            },
            label = {
                Text(text = stringResource(id = R.string.data))
            },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(items = categories, key = { _, item -> item.first }) { index, cat ->
                CategoryField(
                    value = cat.second ?: "",
                    onValueChange = { newValue ->
                        val isLast = categories.lastIndex == index
                        val id = cat.first

                        val copy = ArrayList(categories)

                        when {
                            newValue.isBlank() -> {
                                if (!isLast) {
                                    val next = categories[index + 1]

                                    if (next.second.isNullOrBlank()) {
                                        copy[index] = id to newValue
                                        copy.removeAll { it.first == next.first }
                                    } else {
                                        copy.removeAt(index)
                                    }
                                } else {
                                    copy[index] = id to newValue
                                }
                            }

                            else -> {
                                copy[index] = id to newValue

                                if (isLast) {
                                    copy.add(UUID.randomUUID() to "")
                                }
                            }
                        }

                        model.categories.value = copy
                    }
                )
            }
        }

        Spacer(Modifier.size(8.dp))
        Text(text = stringResource(id = R.string.extras))

        LazyColumn(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items = extras, key = { _, item -> item.first }) { index, item ->
                ExtraItem(
                    extraInfo = item.second
                ) { newKey, newType, newValue ->
                    val id = item.first
                    val copy = ArrayList(extras)
                    val isLast = extras.lastIndex == index

                    fun replace() {
                        val old = copy[index]
                        copy[index] = id to ExtraInfo(
                            key = newKey ?: old.second.key,
                            value = newValue ?: old.second.value,
                            type = newType ?: old.second.type
                        )
                    }

                    when {
                        (newKey.isNullOrBlank() && newValue.isNullOrBlank() && newType == null) -> {
                            if (!isLast) {
                                val next = copy[index + 1]

                                if (next.second.run { key.isBlank() && value.isBlank() }) {
                                    replace()
                                    copy.removeAll { it.first == next.first }
                                } else {
                                    copy.removeAt(index)
                                }
                            } else {
                                replace()
                            }
                        }

                        else -> {
                            replace()

                            if (isLast && !newKey.isNullOrBlank()) {
                                copy.add(UUID.randomUUID() to ExtraInfo("", ""))
                            }
                        }
                    }

                    model.extras.value = copy
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(text = stringResource(id = R.string.category))
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun ExtraItem(
    extraInfo: ExtraInfo,
    onUpdate: (key: String?, type: ExtraType?, value: String?) -> Unit
) {
    var showingTypeDialog by remember {
        mutableStateOf(false)
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = TextFieldDefaults.outlinedShape
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                TextField(
                    value = extraInfo.key,
                    onValueChange = {
                        onUpdate(it, null, null)
                    },
                    label = {
                        Text(text = stringResource(id = R.string.hint_key))
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent
                    )
                )

                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(IntrinsicSize.Min)
                        .width(IntrinsicSize.Min)
                ) {
                    TextField(
                        value = extraInfo.safeType.nameRes.let { stringResource(id = it) },
                        onValueChange = {},
                        label = {
                            Text(text = stringResource(id = R.string.type))
                        },
                        readOnly = true,
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = Color.Transparent
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember {
                                    MutableInteractionSource()
                                },
                                indication = rememberRipple()
                            ) {
                                showingTypeDialog = true
                            }
                    )
                }
            }

            TextField(
                value = extraInfo.value,
                onValueChange = {
                    onUpdate(null, null, it)
                },
                label = {
                    Text(text = stringResource(id = R.string.hint_value))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent
                )
            )
        }
    }

    if (showingTypeDialog) {
        AlertDialog(
            onDismissRequest = {
                showingTypeDialog = false
            },
            title = {
                Text(text = stringResource(id = R.string.type))
            },
            text = {
                ExtrasTypeDialogContents(
                    initial = extraInfo.safeType,
                    onTypeSelected = {
                        onUpdate(null, it, null)
                        showingTypeDialog = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showingTypeDialog = false }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.85f)
        )
    }
}
