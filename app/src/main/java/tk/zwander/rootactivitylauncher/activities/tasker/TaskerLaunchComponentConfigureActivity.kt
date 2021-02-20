package tk.zwander.rootactivitylauncher.activities.tasker

import android.content.ComponentName
import android.content.Context
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import tk.zwander.rootactivitylauncher.MainActivity
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.data.tasker.TaskerLaunchComponentInfo
import tk.zwander.rootactivitylauncher.util.tasker.TaskerLaunchComponentHelper

class TaskerLaunchComponentConfigureActivity : MainActivity(), TaskerPluginConfig<TaskerLaunchComponentInfo> {
    override val context: Context
        get() = this
    override val inputForTasker: TaskerInput<TaskerLaunchComponentInfo>
        get() = TaskerInput(TaskerLaunchComponentInfo(selectedItem?.first?.serialize(), selectedItem?.second?.flattenToString()))

    override val isForTasker = true
    override var selectedItem: Pair<ComponentType, ComponentName>? = null
        set(value) {
            field = value

            if (value != null) {
                TaskerLaunchComponentHelper(this).finishForTasker()
            }
        }

    override fun assignFromInput(input: TaskerInput<TaskerLaunchComponentInfo>) {
        val type = ComponentType.deserialize(input.regular.type)
        val cmp = ComponentName.unflattenFromString(input.regular.component ?: "")

        selectedItem = if (type == null || cmp == null) {
            null
        } else {
            type to cmp
        }
    }
}