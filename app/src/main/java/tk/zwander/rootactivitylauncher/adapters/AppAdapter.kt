package tk.zwander.rootactivitylauncher.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
import kotlin.collections.ArrayList

class AppAdapter(context: Context, private val picasso: Picasso) :
    RecyclerView.Adapter<AppAdapter.AppVH>(), FastScrollRecyclerView.SectionedAdapter {
    val items = SortedList(AppInfo::class.java, object : SortedList.Callback<AppInfo>() {
        override fun areItemsTheSame(item1: AppInfo, item2: AppInfo) =
            item1.label == item2.label
                    && item1.info.packageName == item2.info.packageName

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
            o1.loadedLabel.toString().compareTo(o2.loadedLabel.toString(), ignoreCase = true)

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) =
            oldItem.activitiesExpanded == newItem.activitiesExpanded
                    && oldItem.servicesExpanded == newItem.servicesExpanded

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
    private val innerDividerItemDecoration =
        InnerDividerItemDecoration(context, RecyclerView.VERTICAL)

    private val arrowUp =
        ContextCompat.getDrawable(context, R.drawable.ic_baseline_keyboard_arrow_up_24)?.mutate()
            ?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
    private val arrowDown =
        ContextCompat.getDrawable(context, R.drawable.ic_baseline_keyboard_arrow_down_24)?.mutate()
            ?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }

    private var currentQuery: String = ""
    var enabledFilterMode = EnabledFilterMode.SHOW_ALL
        set(value) {
            if (field != value) {
                field = value

                orig.forEach { it.activityAdapter.setEnabledFilterMode(value) }
                orig.forEach { it.serviceAdapter.setEnabledFilterMode(value) }
                items.replaceAll(filter(currentQuery))
            }
        }
    var exportedFilterMode = ExportedFilterMode.SHOW_ALL
        set(value) {
            if (field != value) {
                field = value

                orig.forEach { it.activityAdapter.setExportedFilterMode(value) }
                orig.forEach { it.serviceAdapter.setExportedFilterMode(value) }
                items.replaceAll(filter(currentQuery))
            }
        }

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
        return items[position].loadedLabel.substring(0, 1)
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

    private fun filter(query: String): List<AppInfo> {
        val filteredModelList = ArrayList<AppInfo>()

        for (i in 0 until orig.size) {
            val item = orig[i]

            if (matches(query, item)) filteredModelList.add(item)
        }

        return filteredModelList
    }

    private fun matches(query: String, data: AppInfo): Boolean {
        val activityFilterEmpty =
            data.activityAdapter.filter(currentQuery, data.activities).isEmpty()
        val serviceFilterEmpty = data.serviceAdapter.filter(currentQuery, data.services).isEmpty()

        if (activityFilterEmpty && serviceFilterEmpty) return false

        if (query.isBlank()) return true

        if (data.loadedLabel.contains(query, true)
            || data.info.packageName.contains(query, true)
        ) return true

        if (!activityFilterEmpty || !serviceFilterEmpty)
            return true

        return false
    }

    inner class AppVH(view: View) : RecyclerView.ViewHolder(view) {
        private var prevPos = -1

        init {
            itemView.apply {
                activities.addItemDecoration(innerDividerItemDecoration)

                services.addItemDecoration(innerDividerItemDecoration)
            }
        }

        fun bind(data: AppInfo) {
            itemView.apply {
                if (prevPos != adapterPosition) {
                    prevPos = adapterPosition

                    picasso.load(AppIconHandler.createUri(data.info.packageName))
                        .fit()
                        .centerInside()
                        .into(app_icon)

                    app_name.text = data.loadedLabel
                    app_pkg.text = data.info.packageName

                    activities.adapter = data.activityAdapter
                    services.adapter = data.serviceAdapter

                    activities_title.setOnClickListener {
                        val d = items[adapterPosition]
                        d.activitiesExpanded = !d.activitiesExpanded

                        notifyItemChanged(adapterPosition)
                    }

                    services_title.setOnClickListener {
                        val d = items[adapterPosition]
                        d.servicesExpanded = !d.servicesExpanded

                        notifyItemChanged(adapterPosition)
                    }
                }

                activities.isVisible = data.activitiesExpanded
                services.isVisible = data.servicesExpanded

                activities_title.setCompoundDrawablesRelative(
                    null,
                    null,
                    if (data.activitiesExpanded) arrowUp else arrowDown,
                    null
                )
                services_title.setCompoundDrawablesRelative(
                    null,
                    null,
                    if (data.servicesExpanded) arrowUp else arrowDown,
                    null
                )

                if (activities.isVisible) {
                    data.activityAdapter.setItems(data.activities)
                }

                if (services.isVisible) {
                    data.serviceAdapter.setItems(data.services)
                }

                activities_title.isVisible =
                    data.activityAdapter.filter(currentQuery, data.activities).isNotEmpty()
                services_title.isVisible =
                    data.serviceAdapter.filter(currentQuery, data.services).isNotEmpty()
            }
        }
    }
}