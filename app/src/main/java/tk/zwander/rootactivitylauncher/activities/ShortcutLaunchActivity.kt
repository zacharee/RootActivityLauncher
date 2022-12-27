package tk.zwander.rootactivitylauncher.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.util.determineComponentNamePackage
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.util.launch.launch

class ShortcutLaunchActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_COMPONENT_KEY = "component_key"
        const val EXTRA_COMPONENT_TYPE = "component_type"

        fun createShortcut(
            context: Context,
            label: CharSequence,
            icon: IconCompat,
            componentKey: String,
            componentType: ComponentType
        ) {
            with(context) {
                val shortcut = Intent(this, ShortcutLaunchActivity::class.java)
                shortcut.action = Intent.ACTION_MAIN
                shortcut.putExtra(EXTRA_COMPONENT_KEY, componentKey)
                shortcut.putExtra(EXTRA_COMPONENT_TYPE, componentType.serialize())

                val info = ShortcutInfoCompat.Builder(this, componentKey)
                    .setIcon(icon)
                    .setShortLabel(label)
                    .setLongLabel("$label: $componentKey")
                    .setIntent(shortcut)
                    .build()

                ShortcutManagerCompat.requestPinShortcut(
                    this,
                    info,
                    null
                )
            }
        }
    }

    private val componentKey by lazy { intent.getStringExtra(EXTRA_COMPONENT_KEY) }
    private val componentType by lazy {
        ComponentType.deserialize(
            intent.getStringExtra(
                EXTRA_COMPONENT_TYPE
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (componentKey.isNullOrBlank() || componentType == null) {
            finish()
            return
        }

        val extras = findExtrasForComponent(componentKey!!)
        val globalExtras = findExtrasForComponent(determineComponentNamePackage(componentKey!!))

        runBlocking(Dispatchers.IO) {
            launch(
                componentType!!,
                globalExtras + extras,
                componentKey!!,
                packageManager.getAllIntentFilters(ComponentName.unflattenFromString(componentKey!!).packageName)
            )
        }

        finish()
    }
}