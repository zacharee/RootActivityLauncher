package tk.zwander.rootactivitylauncher.adapters.component

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import com.squareup.picasso.Picasso
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import java.lang.StringBuilder

class ActivityAdapter(picasso: Picasso) : BaseComponentAdapter<ActivityAdapter, ActivityInfo, ActivityAdapter.ActivityVH>(picasso, ActivityInfo::class.java) {
    override fun onCreateViewHolder(view: View, viewType: Int): ActivityVH {
        return ActivityVH(view)
    }

    inner class ActivityVH(view: View) : BaseComponentVH(view) {
        override fun getPicassoUri(data: ActivityInfo): Uri? {
            return ActivityIconHandler.createUri(data.info.packageName, data.info.name)
        }

        override fun onLaunch(data: ActivityInfo, context: Context, extras: List<ExtraInfo>): Job = launch {
            try {
                val intent = Intent(Intent.ACTION_MAIN)
                intent.component = ComponentName(data.info.packageName, data.info.name)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                if (extras.isNotEmpty()) extras.forEach {
                    intent.putExtra(it.key, it.value)
                }

                context.startActivity(intent)
            } catch (e: SecurityException) {
                if (Shell.SU.available()) {
                    val command = StringBuilder("am start -n $currentComponentKey")

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