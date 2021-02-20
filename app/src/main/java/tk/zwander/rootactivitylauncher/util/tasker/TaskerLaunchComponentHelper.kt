package tk.zwander.rootactivitylauncher.util.tasker

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutput
import tk.zwander.rootactivitylauncher.data.tasker.TaskerLaunchComponentInfo

class TaskerLaunchComponentHelper(config: TaskerPluginConfig<TaskerLaunchComponentInfo>) : TaskerPluginConfigHelperNoOutput<TaskerLaunchComponentInfo, TaskerLaunchComponentRunner>(config) {
    override val runnerClass = TaskerLaunchComponentRunner::class.java
    override val inputClass = TaskerLaunchComponentInfo::class.java
}