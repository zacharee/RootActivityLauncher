package tk.zwander.rootactivitylauncher.adapters.component

import android.content.Context
import android.net.Uri
import android.view.View
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.data.component.ReceiverInfo
import tk.zwander.rootactivitylauncher.picasso.ReceiverIconHandler
import tk.zwander.rootactivitylauncher.util.launchReceiver

class ReceiverAdapter(isForTasker: Boolean, selectionCallback: (BaseComponentInfo) -> Unit) :
    BaseComponentAdapter<ReceiverAdapter, ReceiverInfo, ReceiverAdapter.ReceiverVH>(
        ReceiverInfo::class.java,
        isForTasker,
        selectionCallback
    ) {
    override fun onCreateViewHolder(view: View, viewType: Int): ReceiverAdapter.ReceiverVH {
        return ReceiverVH(view)
    }

    inner class ReceiverVH(view: View) : BaseComponentVH(view) {
        override val componentType = ComponentType.RECEIVER

        override fun getPicassoUri(data: ReceiverInfo): Uri? {
            return ReceiverIconHandler.createUri(data.info.packageName, data.info.name)
        }

        override fun onLaunch(data: ReceiverInfo, context: Context, extras: List<ExtraInfo>) {
            context.launchReceiver(extras, currentComponentKey)
        }
    }
}