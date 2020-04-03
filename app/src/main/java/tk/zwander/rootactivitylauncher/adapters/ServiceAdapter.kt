package tk.zwander.rootactivitylauncher.adapters

import android.content.ComponentName
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.squareup.picasso.Picasso
import eu.chainfire.libsuperuser.Shell
import kotlinx.android.synthetic.main.activity_item.view.*
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ServiceInfo
import tk.zwander.rootactivitylauncher.data.EnabledFilterMode
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import tk.zwander.rootactivitylauncher.util.constructActivityKey
import tk.zwander.rootactivitylauncher.util.findExtrasForActivity
import tk.zwander.rootactivitylauncher.views.ExtrasDialog
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

class ServiceAdapter(private val picasso: Picasso) : RecyclerView.Adapter<ServiceAdapter.ActivityVH>(), CoroutineScope by MainScope() {
    val items = SortedList(ServiceInfo::class.java, object : SortedList.Callback<ServiceInfo>() {
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
    private val orig = object : ArrayList<ServiceInfo>() {
        override fun add(element: ServiceInfo): Boolean {
            if (matches(currentQuery, element)) {
                items.add(element)
            }
            return super.add(element)
        }

        override fun addAll(elements: Collection<ServiceInfo>): Boolean {
            items.addAll(elements.filter { matches(currentQuery, it) })
            return super.addAll(elements)
        }

        override fun remove(element: ServiceInfo): Boolean {
            items.remove(element)
            return super.remove(element)
        }

        override fun clear() {
            items.clear()
            super.clear()
        }
    }

    private var currentQuery: String = ""
    private var filterMode = EnabledFilterMode.SHOW_ALL

    override fun getItemCount(): Int {
        return items.size()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityVH {
        return ActivityVH(
            LayoutInflater.from(parent.context).inflate(R.layout.activity_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ActivityVH, position: Int) {
        holder.bind(items[position])
    }

    fun setItems(items: List<ServiceInfo>) {
        orig.clear()
        orig.addAll(items)
    }

    fun onQueryTextChange(newText: String?) {
        currentQuery = newText ?: ""

        items.replaceAll(filter(currentQuery))
    }

    fun setEnabledFilterMode(filterMode: EnabledFilterMode) {
        this.filterMode = filterMode
        items.replaceAll(filter(currentQuery))
    }

    private fun filter(query: String): List<ServiceInfo> {
        val lowerCaseQuery = query.toLowerCase(Locale.getDefault())

        val filteredModelList = ArrayList<ServiceInfo>()

        for (i in 0 until orig.size) {
            val item = orig[i]

            if (matches(lowerCaseQuery, item)) filteredModelList.add(item)
        }

        return filteredModelList
    }

    private fun matches(query: String, data: ServiceInfo): Boolean {
        when (filterMode) {
            EnabledFilterMode.SHOW_DISABLED -> if (data.info.enabled) return false
            EnabledFilterMode.SHOW_ENABLED -> if (!data.info.enabled) return false
            else -> {
                //no-op
            }
        }

        if (query.isBlank()) return true

        if (data.label.contains(query, true)
            || data.info.name.contains(query, true)
        ) return true

        return false
    }

    inner class ActivityVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(data: ServiceInfo) = launch {
            itemView.apply {
                activity_name.text = data.label
                activity_cmp.text = data.info.name

                picasso.load(ActivityIconHandler.createUri(data.info.packageName, data.info.name))
                    .fit()
                    .centerInside()
                    .into(activity_icon)

                set_extras.setOnClickListener {
                    val d = items[adapterPosition]
                    ExtrasDialog(context, constructActivityKey(d.info.packageName, d.info.name))
                        .show()
                }

                enabled.setOnCheckedChangeListener(null)
                enabled.isChecked = data.info.enabled
                enabled.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
                    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                        val d = items[adapterPosition]
                        if (Shell.SU.available()) {
                            if (Shell.Pool.SU.run("pm ${if (isChecked) "enable" else "disable"} ${constructActivityKey(d.info.packageName, d.info.name)}") == 0) {
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
                    val extras = context.findExtrasForActivity(constructActivityKey(d.info.packageName, d.info.name))

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