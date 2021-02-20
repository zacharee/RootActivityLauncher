package tk.zwander.rootactivitylauncher.data.tasker

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import tk.zwander.rootactivitylauncher.data.component.ComponentType

@TaskerInputRoot
class TaskerLaunchComponentInfo @JvmOverloads constructor(
    @field:TaskerInputField("type") var type: String? = null,
    @field:TaskerInputField("component") var component: String? = null
)