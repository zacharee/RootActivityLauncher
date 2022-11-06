package tk.zwander.rootactivitylauncher.adapters.component

import android.content.Context
import android.view.View
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ReceiverInfo
import tk.zwander.rootactivitylauncher.util.launch.launchReceiver

class ReceiverAdapter(isForTasker: Boolean, selectionCallback: (BaseComponentInfo) -> Unit) :
    BaseComponentAdapter<ReceiverInfo, ReceiverAdapter.ReceiverVH>(
        ReceiverInfo::class.java,
        isForTasker,
        selectionCallback
    ) {
    override fun onCreateViewHolder(view: View, viewType: Int): ReceiverAdapter.ReceiverVH {
        return ReceiverVH(view)
    }

    inner class ReceiverVH(view: View) : BaseComponentVH(view) {
        override fun onLaunch(data: ReceiverInfo, context: Context, extras: List<ExtraInfo>) {
            context.launchReceiver(extras, currentComponentKey)
        }
    }
}