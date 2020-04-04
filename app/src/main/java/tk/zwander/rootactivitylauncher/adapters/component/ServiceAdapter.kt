package tk.zwander.rootactivitylauncher.adapters.component

import android.content.ComponentName
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.SortedList
import com.squareup.picasso.Picasso
import eu.chainfire.libsuperuser.Shell
import kotlinx.android.synthetic.main.activity_item.view.*
import kotlinx.android.synthetic.main.service_item.view.*
import kotlinx.android.synthetic.main.service_item.view.enabled
import kotlinx.android.synthetic.main.service_item.view.launch
import kotlinx.android.synthetic.main.service_item.view.set_extras
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.picasso.ServiceIconHandler
import tk.zwander.rootactivitylauncher.util.constructComponentKey
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.views.ExtrasDialog
import java.lang.StringBuilder

class ServiceAdapter(picasso: Picasso) : BaseComponentAdapter<ServiceAdapter, ServiceInfo, ServiceAdapter.ServiceVH>(picasso, ServiceInfo::class.java) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceVH {
        return ServiceVH(
            LayoutInflater.from(parent.context).inflate(R.layout.service_item, parent, false)
        )
    }

    inner class ServiceVH(view: View) : BaseComponentVH(view) {
        override fun bind(data: ServiceInfo) = launch {
            itemView.apply {
                service_name.text = data.loadedLabel
                service_cmp.text = data.info.name

                picasso.load(ServiceIconHandler.createUri(data.info.packageName, data.info.name))
                    .fit()
                    .centerInside()
                    .into(service_icon)

                set_extras.setOnClickListener {
                    val d = items[adapterPosition]
                    ExtrasDialog(context, constructComponentKey(d.info.packageName, d.info.name))
                        .show()
                }

                enabled.setOnCheckedChangeListener(null)
                enabled.isChecked = data.info.enabled
                enabled.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
                    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                        val d = items[adapterPosition]
                        if (Shell.SU.available()) {
                            if (Shell.Pool.SU.run("pm ${if (isChecked) "enable" else "disable"} ${constructComponentKey(d.info.packageName, d.info.name)}") == 0) {
                                data.info.enabled = isChecked
                                notifyItemChanged(adapterPosition)
                            } else {
                                enabled.setOnCheckedChangeListener(null)
                                enabled.isChecked = !isChecked
                                enabled.setOnCheckedChangeListener(this)
                            }
                        } else {
                            Toast.makeText(context, R.string.requires_root, Toast.LENGTH_SHORT).show()
                            enabled.setOnCheckedChangeListener(null)
                            enabled.isChecked = !isChecked
                            enabled.setOnCheckedChangeListener(this)
                        }
                    }
                })

                launch.isVisible = data.info.enabled
                launch.setOnClickListener {
                    val d = items[adapterPosition]
                    val extras = context.findExtrasForComponent(constructComponentKey(d.info.packageName, d.info.name))

                    try {
                        val intent = Intent(Intent.ACTION_MAIN)
                        intent.component = ComponentName(d.info.packageName, d.info.name)

                        if (extras.isNotEmpty()) extras.forEach {
                            intent.putExtra(it.key, it.value)
                        }

                        ContextCompat.startForegroundService(context, intent)
                    } catch (e: SecurityException) {
                        if (Shell.SU.available()) {
                            val command = StringBuilder("am startservice ${d.info.packageName}/${d.info.name}")

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
    }
}