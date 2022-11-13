package tk.zwander.rootactivitylauncher.util.tasker

import android.content.ComponentName
import android.content.Context
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlinx.coroutines.runBlocking
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.data.tasker.TaskerLaunchComponentInfo
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.util.launch.launch

class TaskerLaunchComponentRunner : TaskerPluginRunnerActionNoOutput<TaskerLaunchComponentInfo>() {
    override fun run(
        context: Context,
        input: TaskerInput<TaskerLaunchComponentInfo>
    ): TaskerPluginResult<Unit> {
        val type = input.regular.type
        val component = input.regular.component

        if (type == null) {
            return TaskerPluginResultError(10, "The component type cannot be null!")
        }

        if (component == null) {
            return TaskerPluginResultError(11, "The component cannot be null!")
        }

        val componentObj = ComponentName.unflattenFromString(component)
        val extras =
            context.findExtrasForComponent(componentObj.packageName) + context.findExtrasForComponent(
                component
            )
        var result: TaskerPluginResult<Unit> = TaskerPluginResultError(
            -1,
            "Unable to launch component: $type, $component. You may need root or Shizuku."
        )

        runBlocking {
            val componentType = try {
                ComponentType.valueOf(type)
            } catch (e: Exception) {
                result = TaskerPluginResultError(12, "Invalid component type $type")
                null
            }

            if (context.launch(
                    componentType ?: return@runBlocking,
                    extras,
                    component
                )
            ) {
                result = TaskerPluginResultSucess()
            }
        }

        return result
    }
}