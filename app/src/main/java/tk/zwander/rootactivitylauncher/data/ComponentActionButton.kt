package tk.zwander.rootactivitylauncher.data

import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.activities.ShortcutLaunchActivity
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.model.AppModel
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.util.launch.launch
import tk.zwander.rootactivitylauncher.util.openAppInfo

sealed class ComponentActionButton<T>(protected val data: T) {
    abstract val iconRes: Int
    abstract val labelRes: Int

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
        override val iconRes = R.drawable.ic_baseline_help_outline_24
        override val labelRes = R.string.component_info

        override suspend fun onClick(context: Context) {
            onClick(data)
        }
    }

    class IntentDialogButton(data: String, private val onClick: () -> Unit) : ComponentActionButton<String>(data) {
        override val iconRes = R.drawable.tune
        override val labelRes = R.string.intent

        override suspend fun onClick(context: Context) {
            onClick()
        }
    }

    class AppInfoButton(data: String) : ComponentActionButton<String>(data) {
        override val iconRes = R.drawable.about_outline
        override val labelRes = R.string.app_info

        override suspend fun onClick(context: Context) {
            context.openAppInfo(data)
        }
    }

    class SaveApkButton(data: AppModel, private val onClick: (AppModel) -> Unit) :
        ComponentActionButton<AppModel>(data) {
        override val iconRes = R.drawable.save
        override val labelRes = R.string.extract_apk

        override suspend fun onClick(context: Context) {
            onClick(data)
        }
    }

    class CreateShortcutButton(data: BaseComponentInfo) : ComponentActionButton<BaseComponentInfo>(data) {
        override val iconRes = R.drawable.ic_baseline_link_24
        override val labelRes = R.string.create_shortcut

        override suspend fun onClick(context: Context) {
            ShortcutLaunchActivity.createShortcut(
                context = context,
                label = data.label,
                icon = IconCompat.createWithBitmap(
                    (data.info.loadIcon(context.packageManager) ?: ContextCompat.getDrawable(
                        context,
                        R.mipmap.ic_launcher
                    ))!!.toBitmap()
                ),
                componentKey = data.component.flattenToString(),
                componentType = data.type()
            )
        }
    }

    class LaunchButton(data: BaseComponentInfo, private val filters: List<IntentFilter>, private val errorCallback: (error: Throwable?) -> Unit) : ComponentActionButton<BaseComponentInfo>(data) {
        override val iconRes = R.drawable.ic_baseline_open_in_new_24
        override val labelRes = R.string.launch

        override suspend fun onClick(context: Context) {
            val componentKey = data.component.flattenToString()

            val extras = context.findExtrasForComponent(data.component.packageName) +
                    context.findExtrasForComponent(componentKey)

            val result = context.launch(data.type(), extras, componentKey, filters)

            errorCallback(result)
        }
    }
}