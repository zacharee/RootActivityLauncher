package tk.zwander.rootactivitylauncher.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Dimension
import coil.size.Size
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.activities.ShortcutLaunchActivity
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ReceiverInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.data.model.AppModel
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.util.getCoilData
import tk.zwander.rootactivitylauncher.util.launch.launch
import tk.zwander.rootactivitylauncher.util.openAppInfo

sealed class ComponentActionButton<T>(protected val data: T) {
    @Composable
    abstract fun getIcon(): Painter

    @Composable
    abstract fun getLabel(): String

    abstract suspend fun onClick(context: Context)

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
                } catch (e: IllegalArgumentException) {
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