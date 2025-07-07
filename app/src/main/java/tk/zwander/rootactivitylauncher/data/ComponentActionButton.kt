package tk.zwander.rootactivitylauncher.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Dimension
import coil.size.Size
import com.github.skgmn.composetooltip.AnchorEdge
import com.github.skgmn.composetooltip.Tooltip
import com.github.skgmn.composetooltip.rememberTooltipStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.activities.ShortcutLaunchActivity
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ReceiverInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.data.model.AppModel
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.util.getCoilData
import tk.zwander.rootactivitylauncher.util.launch.LaunchStrategy
import tk.zwander.rootactivitylauncher.util.launch.createLaunchArgs
import tk.zwander.rootactivitylauncher.util.launch.launch
import tk.zwander.rootactivitylauncher.util.launch.launchStrategiesMap
import tk.zwander.rootactivitylauncher.util.openAppInfo
import tk.zwander.rootactivitylauncher.views.dialogs.BaseAlertDialog
import tk.zwander.rootactivitylauncher.views.dialogs.ComponentInfoDialog
import tk.zwander.rootactivitylauncher.views.dialogs.ExtrasDialog
import kotlin.reflect.KClass

sealed class ComponentActionButton<T>(protected val data: T) {
    @Composable
    abstract fun getIcon(): Painter

    @Composable
    abstract fun getLabel(): String

    abstract suspend fun onClick(context: Context)

    open suspend fun onLongClick(context: Context): Boolean {
        return false
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Render(
        enabled: Boolean,
        modifier: Modifier = Modifier,
    ) {
        val context = LocalContext.current
        val animatedAlpha by animateFloatAsState(
            targetValue = if (!enabled) 0.5f else 1.0f,
            label = "ComponentButtonAnimation_${getLabel()}",
        )
        val scope = rememberCoroutineScope()

        var showingTooltip by remember {
            mutableStateOf(false)
        }

        Box(
            modifier = modifier.clip(CircleShape),
            contentAlignment = Alignment.Center,
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
                            scope.launch(Dispatchers.Main) {
                                if (!enabled || !onLongClick(context)) {
                                    showingTooltip = true
                                }
                            }
                        },
                        onClick = {
                            if (enabled) {
                                scope.launch(Dispatchers.Main) {
                                    onClick(context)
                                }
                            }
                        },
                    ),
            ) {
                Icon(
                    painter = getIcon(),
                    contentDescription = getLabel(),
                    tint = LocalContentColor.current.copy(alpha = animatedAlpha),
                )

                if (showingTooltip) {
                    Tooltip(
                        anchorEdge = AnchorEdge.Top,
                        onDismissRequest = { showingTooltip = false },
                        tooltipStyle = rememberTooltipStyle(
                            color = MaterialTheme.colorScheme.surface,
                            tipHeight = 0.dp,
                            tipWidth = 0.dp,
                        ),
                    ) {
                        Text(text = getLabel())
                    }
                }
            }

            RenderExtraContent()
        }
    }

    @Composable
    open fun RenderExtraContent() {}

    override fun equals(other: Any?): Boolean {
        return other != null &&
                other::class == this::class &&
                data == (other as ComponentActionButton<*>).data
    }

    override fun hashCode(): Int {
        return this::class.qualifiedName!!.hashCode() * 31 +
                data.hashCode()
    }

    class ComponentInfoButton(data: Any) : ComponentActionButton<Any>(data) {
        private val showingComponentInfo = MutableStateFlow(false)

        @Composable
        override fun getIcon() = painterResource(R.drawable.ic_baseline_help_outline_24)

        @Composable
        override fun getLabel() = stringResource(R.string.component_info)

        override suspend fun onClick(context: Context) {
            showingComponentInfo.value = true
        }

        @Composable
        override fun RenderExtraContent() {
            val showing by showingComponentInfo.collectAsState()

            ComponentInfoDialog(
                info = data,
                showing = showing,
            ) { showingComponentInfo.value = false }
        }
    }

    class IntentDialogButton(data: String) : ComponentActionButton<String>(data) {
        private val showingIntentDialog = MutableStateFlow(false)

        @Composable
        override fun getIcon() = painterResource(R.drawable.tune)

        @Composable
        override fun getLabel() = stringResource(R.string.intent)

        override suspend fun onClick(context: Context) {
            showingIntentDialog.value = true
        }

        @Composable
        override fun RenderExtraContent() {
            val showing by showingIntentDialog.collectAsState()

            ExtrasDialog(
                showing = showing,
                componentKey = data,
            ) { showingIntentDialog.value = false }
        }
    }

    class AppInfoButton(data: String) : ComponentActionButton<String>(data) {
        @Composable
        override fun getIcon() = rememberVectorPainter(Icons.Outlined.Info)

        @Composable
        override fun getLabel() = stringResource(R.string.app_info)

        override suspend fun onClick(context: Context) {
            context.openAppInfo(data)
        }
    }

    class SaveApkButton(data: AppModel, private val onClick: (AppModel) -> Unit) :
        ComponentActionButton<AppModel>(data) {
        @Composable
        override fun getIcon() = painterResource(R.drawable.save)

        @Composable
        override fun getLabel() = stringResource(R.string.extract_apk)

        override suspend fun onClick(context: Context) {
            onClick(data)
        }
    }

    class CreateShortcutButton(data: BaseComponentInfo) : ComponentActionButton<BaseComponentInfo>(data) {
        @Composable
        override fun getIcon() = painterResource(R.drawable.ic_baseline_link_24)

        @Composable
        override fun getLabel() = stringResource(R.string.create_shortcut)

        @SuppressLint("RestrictedApi")
        override suspend fun onClick(context: Context) {
            ShortcutLaunchActivity.createShortcut(
                context = context,
                label = data.label,
                icon = try {
                    context.imageLoader.execute(
                        ImageRequest.Builder(context)
                            .data(data.getCoilData())
                            .size(Size(Dimension(256), Dimension.Undefined))
                            .build()
                    ).drawable?.toBitmap()?.let { IconCompat.createWithBitmap(it) }
                } catch (_: IllegalArgumentException) {
                    null
                },
                componentKey = data.component.flattenToString(),
                componentType = data.type(),
            )
        }
    }

    class LaunchButton(
        data: BaseComponentInfo,
    ) : ComponentActionButton<BaseComponentInfo>(data) {
        private val showingDialogState = MutableStateFlow(false)
        private val errors = MutableStateFlow<List<Pair<String, Throwable>>>(listOf())

        @Composable
        override fun getIcon() = painterResource(R.drawable.ic_baseline_open_in_new_24)

        @Composable
        override fun getLabel() = stringResource(R.string.launch)

        override suspend fun onClick(context: Context) {
            val componentKey = data.component.flattenToString()

            val extras = context.findExtrasForComponent(data.component.packageName) +
                    context.findExtrasForComponent(componentKey)

            val result = context.launch(data.type(), extras, componentKey)

            Log.e("RootActivityLauncher", "$result")

            errors.value = result
        }

        override suspend fun onLongClick(context: Context): Boolean {
            showingDialogState.value = true

            return true
        }

        @Composable
        override fun RenderExtraContent() {
            val context = LocalContext.current
            val showingDialog by showingDialogState.collectAsState()
            val errorsState by errors.collectAsState()

            if (showingDialog) {
                val componentKey = remember {
                    data.component.flattenToString()
                }
                val type = remember {
                    data.type()
                }
                val extras = remember {
                    context.findExtrasForComponent(data.component.packageName) +
                            context.findExtrasForComponent(componentKey)
                }
                val launchArgs = remember {
                    context.createLaunchArgs(extras, componentKey)
                }
                var strategies by remember {
                    mutableStateOf<List<Pair<Pair<LaunchStrategy, KClass<out LaunchStrategy>>, Boolean>>>(listOf())
                }

                LaunchedEffect(null) {
                    strategies = launchStrategiesMap[type]?.map {
                        it to with (it.first) {
                            context.canRun(launchArgs)
                        }
                    }?.sortedBy {
                        context.resources.getString(it.first.first.labelRes)
                    } ?: listOf()
                }

                BaseAlertDialog(
                    onDismissRequest = {
                        showingDialogState.value = false
                    },
                    title = {
                        Text(text = stringResource(R.string.launch))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showingDialogState.value = false
                            },
                        ) {
                            Text(text = stringResource(android.R.string.cancel))
                        }
                    },
                    text = {
                        val scope = rememberCoroutineScope()

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(items = strategies, key = { it.first.first.labelRes }) { strategy ->
                                Card(
                                    onClick = {
                                        scope.launch {
                                            errors.value = context.launch(
                                                type = type,
                                                extras = extras,
                                                componentKey = componentKey,
                                                strategy = strategy.first.first,
                                                launchArgs = launchArgs,
                                            )
                                        }
                                    },
                                    enabled = strategy.second,
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .padding(8.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.Start,
                                        ) {
                                            Text(
                                                text = stringResource(strategy.first.first.labelRes),
                                                style = MaterialTheme.typography.titleMedium,
                                            )

                                            Text(text = stringResource(strategy.first.first.descRes))
                                        }
                                    }
                                }
                            }
                        }
                    },
                )
            }

            if (errorsState.isNotEmpty()) {
                BaseAlertDialog(
                    onDismissRequest = {
                        errors.value = listOf()
                    },
                    title = {
                        Text(text = stringResource(id = R.string.launch_error))
                    },
                    text = {
                        val expanded = remember {
                            mutableStateMapOf<Int, Boolean>()
                        }

                        Column {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                item {
                                    Text(
                                        text = stringResource(id = R.string.unable_to_launch_template),
                                    )
                                }

                                items(errorsState.size, { it }) {
                                    val item = errorsState[it]
                                    val rotation by animateFloatAsState(targetValue = if (expanded[it] == true) 180f else 0f)

                                    Card(
                                        onClick = { expanded[it] = expanded[it] != true },
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

                                                AnimatedVisibility(visible = expanded[it] == true) {
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
                        TextButton(onClick = { errors.value = listOf() }) {
                            Text(text = stringResource(id = android.R.string.ok))
                        }
                    },
                    properties = DialogProperties(
                        dismissOnClickOutside = false,
                        dismissOnBackPress = false
                    ),
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
        }
    }

    class FavoriteButton(
        data: BaseComponentInfo,
    ) : ComponentActionButton<BaseComponentInfo>(data) {
        @Composable
        override fun getIcon(): Painter {
            val context = LocalContext.current
            val flow = context.flowForType()
            val state by flow.collectAsState(initial = listOf())

            return rememberVectorPainter(
                if (state.contains(data.component.flattenToString())) {
                    Icons.Default.Favorite
                } else {
                    Icons.Default.FavoriteBorder
                },
            )
        }

        @Composable
        override fun getLabel(): String {
            val context = LocalContext.current
            val flow = context.flowForType()
            val state by flow.collectAsState(initial = listOf())

            return stringResource(
                if (state.contains(data.component.flattenToString())) {
                    R.string.unfavorite
                } else {
                    R.string.favorite
                },
            )
        }

        override suspend fun onClick(context: Context) {
            val flow = context.flowForType()
            val current = flow.first().toMutableList()
            val key = data.component.flattenToString()
            val contains = current.contains(key)

            if (contains) {
                current.remove(key)
            } else {
                current.add(key)
            }

            context.updateForType(current)
        }

        private fun Context.flowForType(): Flow<List<String>> {
            return when (data) {
                is ActivityInfo -> prefs.favoriteActivities
                is ServiceInfo -> prefs.favoriteServices
                is ReceiverInfo -> prefs.favoriteReceivers
            }
        }

        private suspend fun Context.updateForType(info: List<String>) {
            when (data) {
                is ActivityInfo -> prefs.updateFavoriteActivities(info)
                is ServiceInfo -> prefs.updateFavoriteServices(info)
                is ReceiverInfo -> prefs.updateFavoriteReceivers(info)
            }
        }
    }
}