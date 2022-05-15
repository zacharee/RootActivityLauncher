package tk.zwander.rootactivitylauncher.adapters.component

import android.content.Context
import android.net.Uri
import android.view.View
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import tk.zwander.rootactivitylauncher.util.launch.launchActivity

class ActivityAdapter(isForTasker: Boolean, selectionCallback: (BaseComponentInfo) -> Unit) :
    BaseComponentAdapter<ActivityAdapter, ActivityInfo, ActivityAdapter.ActivityVH>(
        ActivityInfo::class.java,
        isForTasker,
        selectionCallback
    ) {
    override fun onCreateViewHolder(view: View, viewType: Int): ActivityVH {
        return ActivityVH(view)
    }

    inner class ActivityVH(view: View) : BaseComponentVH(view) {
        override val componentType: ComponentType = ComponentType.ACTIVITY

        override fun getPicassoUri(data: ActivityInfo): Uri {
            return ActivityIconHandler.createUri(data.info.packageName, data.info.name)
        }

        override fun onLaunch(data: ActivityInfo, context: Context, extras: List<ExtraInfo>) {
            context.launchActivity(extras, currentComponentKey)
        }
    }
}