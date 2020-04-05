package tk.zwander.rootactivitylauncher.activities

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.util.launchActivity
import tk.zwander.rootactivitylauncher.util.launchService

class ShortcutLaunchActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_COMPONENT_KEY = "component_key"
        const val EXTRA_COMPONENT_TYPE = "component_type"
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

        val extras = findExtrasForComponent(componentKey)

        when (componentType) {
            ComponentType.ACTIVITY -> {
                Log.e("RAL", "activity")
                launchActivity(extras, componentKey)
            }

            ComponentType.SERVICE -> {
                launchService(extras, componentKey)
            }
        }

        finish()
    }
}