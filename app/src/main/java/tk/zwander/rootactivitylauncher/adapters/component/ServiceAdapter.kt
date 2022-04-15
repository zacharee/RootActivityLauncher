package tk.zwander.rootactivitylauncher.adapters.component

import android.content.Context
import android.net.Uri
import android.view.View
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.picasso.ServiceIconHandler
import tk.zwander.rootactivitylauncher.util.launch.launchService

class ServiceAdapter(isForTasker: Boolean, selectionCallback: (BaseComponentInfo) -> Unit) :
    BaseComponentAdapter<ServiceAdapter, ServiceInfo, ServiceAdapter.ServiceVH>(
        ServiceInfo::class.java,
        isForTasker,
        selectionCallback
    ) {
    override fun onCreateViewHolder(view: View, viewType: Int): ServiceVH {
        return ServiceVH(view)
    }

    inner class ServiceVH(view: View) : BaseComponentVH(view) {
        override val componentType: ComponentType = ComponentType.SERVICE

        override fun getPicassoUri(data: ServiceInfo): Uri? {
            return ServiceIconHandler.createUri(data.info.packageName, data.info.name)
        }

        override fun onLaunch(data: ServiceInfo, context: Context, extras: List<ExtraInfo>) {
            context.launchService(extras, currentComponentKey)
        }
    }
}