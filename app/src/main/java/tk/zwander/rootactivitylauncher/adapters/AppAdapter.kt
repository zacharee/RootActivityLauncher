package tk.zwander.rootactivitylauncher.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.app_item.view.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.EnabledFilterMode
import tk.zwander.rootactivitylauncher.data.ExportedFilterMode
import tk.zwander.rootactivitylauncher.picasso.AppIconHandler
import tk.zwander.rootactivitylauncher.util.InnerDividerItemDecoration
import java.util.*
import kotlin.collections.ArrayList

class AppAdapter(context: Context, private val picasso: Picasso) : RecyclerView.Adapter<AppAdapter.AppVH>(), FastScrollRecyclerView.SectionedAdapter {
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
    private val activityViewPool = RecyclerView.RecycledViewPool()
    private val serviceViewPool = RecyclerView.RecycledViewPool()
    private val innerDividerItemDecoration = InnerDividerItemDecoration(context, RecyclerView.VERTICAL)

    private var currentQuery: String = ""

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return items[position].info.packageName.hashCode().toLong()
    }

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

        orig.forEach { it.activityAdapter.onQueryTextChange(newText) }
        orig.forEach { it.serviceAdapter.onQueryTextChange(newText) }
        items.replaceAll(filter(currentQuery))
    }

    fun setEnabledFilterMode(filterMode: EnabledFilterMode) {
        orig.forEach { it.activityAdapter.setEnabledFilterMode(filterMode) }
        orig.forEach { it.serviceAdapter.setEnabledFilterMode(filterMode) }
    }

    fun setExportedFilterMode(filterMode: ExportedFilterMode) {
        orig.forEach { it.activityAdapter.setExportedFilterMode(filterMode) }
        orig.forEach { it.serviceAdapter.setExportedFilterMode(filterMode) }
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
        private var prevPos = -1

        init {
            itemView.apply {
                activities.setRecycledViewPool(activityViewPool)
                activities.setItemViewCacheSize(20)
                activities.addItemDecoration(innerDividerItemDecoration)

                services.setRecycledViewPool(serviceViewPool)
                services.setItemViewCacheSize(20)
                services.addItemDecoration(innerDividerItemDecoration)
            }
        }

        fun bind(data: AppInfo) {
            itemView.apply {
                activities.isVisible = data.activitiesExpanded
                services.isVisible = data.servicesExpanded

                activities_arrow.scaleY = if (data.activitiesExpanded) 1f else -1f
                services_arrow.scaleY = if (data.servicesExpanded) 1f else -1f

                if (prevPos != adapterPosition) {
                    prevPos = adapterPosition

                    activities_expansion.isVisible = data.activities.isNotEmpty()
                    services_expansion.isVisible = data.services.isNotEmpty()

                    picasso.load(AppIconHandler.createUri(data.info.packageName))
                        .fit()
                        .centerInside()
                        .into(app_icon)

                    app_name.text = data.label
                    app_pkg.text = data.info.packageName

                    activities.adapter = data.activityAdapter
                    services.adapter = data.serviceAdapter

                    data.activityAdapter.setItems(data.activities)
                    data.serviceAdapter.setItems(data.services)

                    activities_expansion.setOnClickListener {
                        val d = items[adapterPosition]
                        d.activitiesExpanded = !d.activitiesExpanded

                        notifyItemChanged(adapterPosition)
                    }

                    services_expansion.setOnClickListener {
                        val d = items[adapterPosition]
                        d.servicesExpanded = !d.servicesExpanded

                        notifyItemChanged(adapterPosition)
                    }
                }
            }
        }
    }
}