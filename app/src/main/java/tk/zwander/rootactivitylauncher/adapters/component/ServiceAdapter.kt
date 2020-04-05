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
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.picasso.ServiceIconHandler
import java.lang.StringBuilder

class ServiceAdapter(picasso: Picasso) : BaseComponentAdapter<ServiceAdapter, ServiceInfo, ServiceAdapter.ServiceVH>(picasso, ServiceInfo::class.java) {
    override fun onCreateViewHolder(view: View, viewType: Int): ServiceVH {
        return ServiceVH(view)
    }

    inner class ServiceVH(view: View) : BaseComponentVH(view) {
        override fun getPicassoUri(data: ServiceInfo): Uri? {
            return ServiceIconHandler.createUri(data.info.packageName, data.info.name)
        }

        override fun onLaunch(data: ServiceInfo, context: Context, extras: List<ExtraInfo>): Job = launch {
            try {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.component = ComponentName(data.info.packageName, data.info.name)

                if (extras.isNotEmpty()) extras.forEach {
                    intent.putExtra(it.key, it.value)
                }

                ContextCompat.startForegroundService(context, intent)
            } catch (e: SecurityException) {
                if (Shell.SU.available()) {
                    val command = StringBuilder("am startservice $currentComponentKey")

                    if (extras.isNotEmpty()) extras.forEach {
                        command.append(" -e \"${it.key}\" \"${it.value}\"")
                    }

                    Shell.Pool.SU.run(command.toString())
                } else {
                    Toast.makeText(context, R.string.requires_root, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}