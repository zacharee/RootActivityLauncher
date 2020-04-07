package tk.zwander.rootactivitylauncher.adapters.component

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.squareup.picasso.Picasso
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.picasso.ServiceIconHandler
import tk.zwander.rootactivitylauncher.util.launchService
import java.lang.StringBuilder

class ServiceAdapter : BaseComponentAdapter<ServiceAdapter, ServiceInfo, ServiceAdapter.ServiceVH>(ServiceInfo::class.java) {
    override fun onCreateViewHolder(view: View, viewType: Int): ServiceVH {
        return ServiceVH(view)
    }

    inner class ServiceVH(view: View) : BaseComponentVH(view) {
        override val componentType: ComponentType = ComponentType.SERVICE

        override fun getPicassoUri(data: ServiceInfo): Uri? {
            return ServiceIconHandler.createUri(data.info.packageName, data.info.name)
        }

        override fun onLaunch(data: ServiceInfo, context: Context, extras: List<ExtraInfo>): Job = launch {
            context.launchService(extras, currentComponentKey)
        }
    }
}