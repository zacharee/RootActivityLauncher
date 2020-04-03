package tk.zwander.rootactivitylauncher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.app_item.view.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.picasso.AppIconHandler
import java.util.*
import kotlin.collections.ArrayList

class AppAdapter(private val picasso: Picasso) : RecyclerView.Adapter<AppAdapter.AppVH>(), FastScrollRecyclerView.SectionedAdapter {
    val items = SortedList(AppInfo::class.java, object : SortedList.Callback<AppInfo>() {
        override fun areItemsTheSame(item1: AppInfo?, item2: AppInfo?) =
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

        override fun compare(o1: AppInfo, o2: AppInfo) =
            o1.label.toString().compareTo(o2.label.toString(), ignoreCase = true)

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) =
            oldItem.info.packageName == newItem.info.packageName

    })
    private val orig = object : ArrayList<AppInfo>() {
        override fun add(element: AppInfo): Boolean {
            if (matches(currentQuery, element)) {
                items.add(element)
            }
            return super.add(element)
        }

        override fun addAll(elements: Collection<AppInfo>): Boolean {
            items.addAll(elements.filter { matches(currentQuery, it) })
            return super.addAll(elements)
        }

        override fun remove(element: AppInfo): Boolean {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppVH {
        return AppVH(LayoutInflater.from(parent.context).inflate(R.layout.app_item, parent, false))
    }

    override fun onBindViewHolder(holder: AppVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getSectionName(position: Int): String {
        return items[position].label.substring(0, 1)
    }

    fun setItems(items: List<AppInfo>) {
        orig.clear()
        orig.addAll(items)
    }

    fun onQueryTextChange(newText: String?) {
        currentQuery = newText ?: ""

        orig.forEach { it.adapter.onQueryTextChange(newText) }
        items.replaceAll(filter(currentQuery))
    }

    private fun filter(query: String): List<AppInfo> {
        val lowerCaseQuery = query.toLowerCase(Locale.getDefault())

        val filteredModelList = ArrayList<AppInfo>()

        for (i in 0 until orig.size) {
            val item = orig[i]

            if (matches(lowerCaseQuery, item)) filteredModelList.add(item)
        }

        return filteredModelList
    }

    private fun matches(query: String, data: AppInfo): Boolean {
        if (query.isBlank()) return true

        if (data.label.contains(query, true)
            || data.info.packageName.contains(query, true)
        ) return true

        if (data.activities.any {
                it.label.contains(query, true) || it.info.name.contains(
                    query,
                    true
                )
            })
            return true

        return false
    }

    inner class AppVH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(data: AppInfo) {
            itemView.apply {
                data.adapter = ActivityAdapter(picasso)

                activities.isVisible = data.expanded
                activities.adapter = data.adapter
                arrow.scaleY = if (data.expanded) 1f else -1f
                app_name.text = data.label
                app_pkg.text = data.info.packageName

                picasso.load(AppIconHandler.createUri(data.info.packageName))
                    .fit()
                    .centerInside()
                    .into(app_icon)

                if (data.expanded) {
                    data.adapter.setItems(data.activities)
                }

                setOnClickListener {
                    val d = items[adapterPosition]
                    d.expanded = !d.expanded

                    post {
                        notifyItemChanged(adapterPosition)
                    }
                }
            }
        }
    }
}