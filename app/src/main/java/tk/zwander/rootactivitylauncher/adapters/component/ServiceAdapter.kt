package tk.zwander.rootactivitylauncher.adapters.component

import android.content.ComponentName
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.SortedList
import com.squareup.picasso.Picasso
import eu.chainfire.libsuperuser.Shell
import kotlinx.android.synthetic.main.service_item.view.*
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import tk.zwander.rootactivitylauncher.util.constructComponentKey
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.views.ExtrasDialog
import java.lang.StringBuilder

class ServiceAdapter(picasso: Picasso) : BaseComponentAdapter<ServiceAdapter, ServiceInfo, ServiceAdapter.ServiceVH>(picasso) {
    override val items = SortedList(ServiceInfo::class.java, object : SortedList.Callback<ServiceInfo>() {
        override fun areItemsTheSame(item1: ServiceInfo?, item2: ServiceInfo?) =
            item1 == item2

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int) {
            notifyItemRangeChanged(position, count)
        }

        override fun onInserted(position: Int, count: Int) {
            notifyItemRangeInserted(position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            notifyItemRangeRemoved(position, count)
        }

        override fun compare(o1: ServiceInfo, o2: ServiceInfo) =
            o1.label.toString().compareTo(o2.label.toString())

        override fun areContentsTheSame(oldItem: ServiceInfo, newItem: ServiceInfo) =
            oldItem.info.packageName == newItem.info.packageName

    })

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceVH {
        return ServiceVH(
            LayoutInflater.from(parent.context).inflate(R.layout.service_item, parent, false)
        )
    }

    inner class ServiceVH(view: View) : BaseComponentVH(view) {
        override fun bind(data: ServiceInfo) = launch {
            itemView.apply {
                service_name.text = data.label
                service_cmp.text = data.info.name

                picasso.load(ActivityIconHandler.createUri(data.info.packageName, data.info.name))
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

                setOnClickListener {
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