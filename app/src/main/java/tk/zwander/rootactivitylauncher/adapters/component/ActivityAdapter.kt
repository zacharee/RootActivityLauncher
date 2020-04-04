package tk.zwander.rootactivitylauncher.adapters.component

import android.content.ComponentName
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.view.isVisible
import com.squareup.picasso.Picasso
import eu.chainfire.libsuperuser.Shell
import kotlinx.android.synthetic.main.activity_item.view.*
import kotlinx.android.synthetic.main.activity_item.view.enabled
import kotlinx.android.synthetic.main.activity_item.view.launch
import kotlinx.android.synthetic.main.activity_item.view.set_extras
import kotlinx.android.synthetic.main.service_item.view.*
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import tk.zwander.rootactivitylauncher.util.constructComponentKey
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.views.ExtrasDialog
import java.lang.StringBuilder

class ActivityAdapter(picasso: Picasso) : BaseComponentAdapter<ActivityAdapter, ActivityInfo, ActivityAdapter.ActivityVH>(picasso, ActivityInfo::class.java) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityVH {
        return ActivityVH(
            LayoutInflater.from(parent.context).inflate(R.layout.activity_item, parent, false)
        )
    }

    inner class ActivityVH(view: View) : BaseComponentVH(view) {
        private val componentEnabledListener = object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                val d = items[adapterPosition]
                if (Shell.SU.available()) {
                    if (Shell.Pool.SU.run("pm ${if (isChecked) "enable" else "disable"} ${constructComponentKey(d.info.packageName, d.info.name)}") == 0) {
                        d.info.enabled = isChecked
                        notifyItemChanged(adapterPosition)
                    } else {
                        buttonView.setOnCheckedChangeListener(null)
                        buttonView.isChecked = !isChecked
                        buttonView.setOnCheckedChangeListener(this)
                    }
                } else {
                    Toast.makeText(itemView.context, R.string.requires_root, Toast.LENGTH_SHORT).show()
                    buttonView.setOnCheckedChangeListener(null)
                    buttonView.isChecked = !isChecked
                    buttonView.setOnCheckedChangeListener(this)
                }
            }
        }

        private var prevPos = -1
        
        init {
            itemView.apply {
                launch.setOnClickListener {
                    val d = items[adapterPosition]
                    val extras = context.findExtrasForComponent(constructComponentKey(d.info.packageName, d.info.name))

                    try {
                        val intent = Intent(Intent.ACTION_MAIN)
                        intent.component = ComponentName(d.info.packageName, d.info.name)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        if (extras.isNotEmpty()) extras.forEach {
                            intent.putExtra(it.key, it.value)
                        }

                        context.startActivity(intent)
                    } catch (e: SecurityException) {
                        if (Shell.SU.available()) {
                            val command = StringBuilder("am start -n ${d.info.packageName}/${d.info.name}")

                            if (extras.isNotEmpty()) extras.forEach {
                                command.append(" -e \"${it.key}\" \"${it.value}\"")
                            }

                            Shell.Pool.SU.run(command.toString())
                        } else {
                            Toast.makeText(context, R.string.requires_root, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                set_extras.setOnClickListener {
                    val d = items[adapterPosition]
                    ExtrasDialog(context, constructComponentKey(d.info.packageName, d.info.name))
                        .show()
                }
            }
        }
        
        override fun bind(data: ActivityInfo) = launch {
            itemView.apply {
                if (adapterPosition != prevPos) {
                    prevPos = adapterPosition

                    activity_name.text = data.loadedLabel
                    activity_cmp.text = data.info.name

                    picasso.load(ActivityIconHandler.createUri(data.info.packageName, data.info.name))
                        .fit()
                        .centerInside()
                        .into(activity_icon)
                }

                enabled.setOnCheckedChangeListener(null)

                if (enabled.isChecked != data.info.enabled) enabled.isChecked = data.info.enabled
                if (launch.isVisible != data.info.enabled) launch.isVisible = data.info.enabled

                enabled.setOnCheckedChangeListener(componentEnabledListener)
            }
        }
    }
}