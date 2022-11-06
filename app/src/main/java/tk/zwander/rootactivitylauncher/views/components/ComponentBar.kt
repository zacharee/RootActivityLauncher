package tk.zwander.rootactivitylauncher.views.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.util.createShortcut
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.util.isActuallyEnabled
import tk.zwander.rootactivitylauncher.util.launch.launchActivity
import tk.zwander.rootactivitylauncher.util.launch.launchReceiver
import tk.zwander.rootactivitylauncher.util.launch.launchService
import tk.zwander.rootactivitylauncher.util.openAppInfo
import tk.zwander.rootactivitylauncher.util.setComponentEnabled
import tk.zwander.rootactivitylauncher.util.setPackageEnabled

sealed class Button<T>(protected val data: T) {
    abstract val iconRes: Int
    abstract val labelRes: Int

    abstract fun onClick(context: Context)

    class ComponentInfoButton(data: Any, private val onClick: () -> Unit) : Button<Any>(data) {
        override val iconRes = R.drawable.ic_baseline_help_outline_24
        override val labelRes = R.string.component_info

        override fun onClick(context: Context) {
            onClick()
        }
    }

    class IntentDialogButton(data: String, private val onClick: () -> Unit) : Button<String>(data) {
        override val iconRes = R.drawable.tune
        override val labelRes = R.string.intent

        override fun onClick(context: Context) {
            onClick()
        }
    }

    class AppInfoButton(data: String) : Button<String>(data) {
        override val iconRes = R.drawable.about_outline
        override val labelRes = R.string.app_info

        override fun onClick(context: Context) {
            context.openAppInfo(data)
        }
    }

    class SaveApkButton(data: AppInfo, private val onClick: () -> Unit) : Button<AppInfo>(data) {
        override val iconRes = R.drawable.save
        override val labelRes = R.string.extract_apk

        override fun onClick(context: Context) {
            onClick()
        }
    }

    class CreateShortcutButton(data: BaseComponentInfo) : Button<BaseComponentInfo>(data) {
        override val iconRes = R.drawable.ic_baseline_link_24
        override val labelRes = R.string.create_shortcut

        override fun onClick(context: Context) {
            context.createShortcut(
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

    class LaunchButton(data: BaseComponentInfo) : Button<BaseComponentInfo>(data) {
        override val iconRes = R.drawable.ic_baseline_open_in_new_24
        override val labelRes = R.string.launch

        override fun onClick(context: Context) {
            val componentKey = data.component.flattenToString()

            val extras = context.findExtrasForComponent(data.component.packageName) +
                    context.findExtrasForComponent(componentKey)

            when (data.type()) {
                ComponentType.ACTIVITY -> context.launchActivity(extras, componentKey)
                ComponentType.SERVICE -> context.launchService(extras, componentKey)
                ComponentType.RECEIVER -> context.launchReceiver(extras, componentKey)
            }
        }
    }
}

@Composable
fun AppBar(
    icon: Painter,
    name: String,
    app: AppInfo,
    whichButtons: List<Button<*>>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BarGuts(
        icon = icon,
        name = name,
        enabled = app.info.isActuallyEnabled(context),
        onEnabledChanged = {
            context.setPackageEnabled(app.info.packageName, it)
        },
        whichButtons = whichButtons,
        modifier = modifier
    )
}

@Composable
fun ComponentBar(
    icon: Painter,
    name: String,
    component: BaseComponentInfo,
    whichButtons: List<Button<*>>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BarGuts(
        icon = icon,
        name = name,
        enabled = component.info.isActuallyEnabled(context),
        onEnabledChanged = {
            context.setComponentEnabled(component, it)
        },
        whichButtons = whichButtons,
        modifier = modifier
    )
}

@Composable
private fun BarGuts(
    icon: Painter,
    name: String,
    enabled: Boolean,
    onEnabledChanged: suspend (Boolean) -> Boolean,
    whichButtons: List<Button<*>>,
    modifier: Modifier = Modifier
) {
    val actualButtons = whichButtons.filter { enabled || it !is Button.LaunchButton }


}
