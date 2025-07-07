package tk.zwander.rootactivitylauncher.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Dimension
import coil.size.Size
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

    class ComponentInfoButton(data: Any, private val onClick: (info: Any) -> Unit) :
        ComponentActionButton<Any>(data) {
        @Composable
        override fun getIcon() = painterResource(R.drawable.ic_baseline_help_outline_24)

        @Composable
        override fun getLabel() = stringResource(R.string.component_info)

        override suspend fun onClick(context: Context) {
            onClick(data)
        }
    }

    class IntentDialogButton(data: String, private val onClick: () -> Unit) :
        ComponentActionButton<String>(data) {
        @Composable
        override fun getIcon() = painterResource(R.drawable.tune)

        @Composable
        override fun getLabel() = stringResource(R.string.intent)

        override suspend fun onClick(context: Context) {
            onClick()
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

    class CreateShortcutButton(data: BaseComponentInfo) :
        ComponentActionButton<BaseComponentInfo>(data) {
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
                componentType = data.type()
            )
        }
    }

    class LaunchButton(
        data: BaseComponentInfo,
        private val errorCallback: (error: List<Pair<String, Throwable>>) -> Unit
    ) : ComponentActionButton<BaseComponentInfo>(data) {
        private val showingDialogState = MutableStateFlow(false)

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

            errorCallback(result)
        }

        override suspend fun onLongClick(context: Context): Boolean {
            showingDialogState.value = true

            return true
        }

        @Composable
        override fun RenderExtraContent() {
            val context = LocalContext.current
            val showingDialog by showingDialogState.collectAsState()

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
                                            errorCallback(
                                                context.launch(
                                                    type = type,
                                                    extras = extras,
                                                    componentKey = componentKey,
                                                    strategy = strategy.first.first,
                                                    launchArgs = launchArgs,
                                                )
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
        }
    }

    class FavoriteButton(
        data: BaseComponentInfo
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