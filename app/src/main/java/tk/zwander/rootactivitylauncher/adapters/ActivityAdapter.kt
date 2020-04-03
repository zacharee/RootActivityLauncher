package tk.zwander.rootactivitylauncher.adapters

import android.content.ComponentName
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.squareup.picasso.Picasso
import eu.chainfire.libsuperuser.Shell
import kotlinx.android.synthetic.main.activity_item.view.*
import kotlinx.android.synthetic.main.app_item.view.*
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ActivityInfo
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import tk.zwander.rootactivitylauncher.util.constructActivityKey
import tk.zwander.rootactivitylauncher.util.findExtrasForActivity
import tk.zwander.rootactivitylauncher.views.ExtrasDialog
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

class ActivityAdapter(private val picasso: Picasso) : RecyclerView.Adapter<ActivityAdapter.ActivityVH>(), CoroutineScope by MainScope() {
    val items = SortedList(ActivityInfo::class.java, object : SortedList.Callback<ActivityInfo>() {
        override fun areItemsTheSame(item1: ActivityInfo?, item2: ActivityInfo?) =
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

        override fun compare(o1: ActivityInfo, o2: ActivityInfo) =
            o1.label.toString().compareTo(o2.label.toString())

        override fun areContentsTheSame(oldItem: ActivityInfo, newItem: ActivityInfo) =
            oldItem.info.packageName == newItem.info.packageName

    })
    private val orig = object : ArrayList<ActivityInfo>() {
        override fun add(element: ActivityInfo): Boolean {
            if (matches(currentQuery, element)) {
                items.add(element)
            }
            return super.add(element)
        }

        override fun addAll(elements: Collection<ActivityInfo>): Boolean {
            items.addAll(elements.filter { matches(currentQuery, it) })
            return super.addAll(elements)
        }

        override fun remove(element: ActivityInfo): Boolean {
            items.remove(element)
            return super.remove(element)
        }

        override fun clear() {
            items.clear()
            super.clear()
        }
    }

    private var currentQuery: String = ""

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

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        cancel()
    }

    fun setItems(items: List<ActivityInfo>) {
        orig.clear()
        orig.addAll(items)
    }

    fun onQueryTextChange(newText: String?) {
        currentQuery = newText ?: ""

        items.replaceAll(filter(currentQuery))
    }

    private fun filter(query: String): List<ActivityInfo> {
        val lowerCaseQuery = query.toLowerCase(Locale.getDefault())

        val filteredModelList = ArrayList<ActivityInfo>()

        for (i in 0 until orig.size) {
            val item = orig[i]

            if (matches(lowerCaseQuery, item)) filteredModelList.add(item)
        }

        return filteredModelList
    }

    private fun matches(query: String, data: ActivityInfo): Boolean {
        if (query.isBlank()) return true

        if (data.label.contains(query, true)
            || data.info.name.contains(query, true)
        ) return true

        return false
    }

    inner class ActivityVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(data: ActivityInfo) = launch {
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

                if (data.info.enabled) {
                    disable.isVisible = true
                    enable.isVisible = false
                    disable.setOnClickListener {
                        val d = items[adapterPosition]
                        if (Shell.SU.available()) {
                            Shell.Pool.SU.run("pm disable ${constructActivityKey(d.info.packageName, d.info.name)}")
                            data.info.enabled = false
                            notifyItemChanged(adapterPosition)
                        } else {
                            Toast.makeText(context, R.string.requires_root, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    disable.isVisible = false
                    enable.isVisible = true
                    enable.setOnClickListener {
                        val d = items[adapterPosition]
                        if (Shell.SU.available()) {
                            Shell.Pool.SU.run("pm enable ${constructActivityKey(d.info.packageName, d.info.name)}")
                            data.info.enabled = true
                            notifyItemChanged(adapterPosition)
                        } else {
                            Toast.makeText(context, R.string.requires_root, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                setOnClickListener {
                    val d = items[adapterPosition]
                    val extras = context.findExtrasForActivity(constructActivityKey(d.info.packageName, d.info.name))

                    try {
                        val intent = Intent(Intent.ACTION_MAIN)
                        intent.component = ComponentName(d.info.packageName, d.info.name)

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
            }
        }
    }
}