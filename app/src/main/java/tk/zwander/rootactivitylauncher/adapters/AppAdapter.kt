package tk.zwander.rootactivitylauncher.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import kotlinx.android.synthetic.main.app_item.view.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.EnabledFilterMode
import tk.zwander.rootactivitylauncher.data.ExportedFilterMode
import tk.zwander.rootactivitylauncher.picasso.AppIconHandler
import tk.zwander.rootactivitylauncher.util.InnerDividerItemDecoration
import tk.zwander.rootactivitylauncher.util.forEachParallel
import tk.zwander.rootactivitylauncher.util.picasso
import kotlin.Comparator
import kotlin.collections.HashMap

class AppAdapter(context: Context) : RecyclerView.Adapter<AppAdapter.AppVH>(),
    FastScrollRecyclerView.SectionedAdapter {
    val async = AsyncListDiffer(this, object : DiffUtil.ItemCallback<AppInfo>() {
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return false
        }

        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.info.packageName == newItem.info.packageName
        }
    })

    private val orig = HashMap<String, AppInfo>()
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
        private set
    var exportedFilterMode = ExportedFilterMode.SHOW_ALL
        private set

    override fun getItemCount(): Int {
        return async.currentList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppVH {
        return AppVH(LayoutInflater.from(parent.context).inflate(R.layout.app_item, parent, false))
    }

    override fun onBindViewHolder(holder: AppVH, position: Int) {
        holder.bind(async.currentList[position])
    }

    override fun getSectionName(position: Int): String {
        return async.currentList[position].label.substring(0, 1)
    }

    fun setItems(items: List<AppInfo>) {
        orig.clear()
        items.forEachParallel {
            orig[it.info.packageName] = it
        }

        onFilterChange(override = true)
        sortAndSubmitList(items)
    }

    private fun sortAndSubmitList(items: List<AppInfo>) {
        async.submitList(items.sortedWith(Comparator { o1, o2 ->
            o1.label.toString().compareTo(o2.label.toString())
        }))
    }

    fun onFilterChange(
        query: String = currentQuery,
        enabledMode: EnabledFilterMode = enabledFilterMode,
        exportedMode: ExportedFilterMode = exportedFilterMode,
        override: Boolean = false
    ) {
        if (override || currentQuery != query || enabledFilterMode != enabledMode || exportedFilterMode != exportedMode) {
            currentQuery = query
            enabledFilterMode = enabledMode
            exportedFilterMode = exportedMode

            orig.values.forEachParallel {
                it.onFilterChange(
                    currentQuery,
                    enabledFilterMode,
                    exportedFilterMode
                )
            }
            sortAndSubmitList(filter())
        }
    }

    private fun filter(): List<AppInfo> {
        return orig.values.filter { matches(it) }
    }

    private fun matches(data: AppInfo): Boolean {
        val activityFilterEmpty = data.filteredActivities.isEmpty()
        val serviceFilterEmpty = data.filteredServices.isEmpty()

        if (activityFilterEmpty && serviceFilterEmpty) return false

        if (currentQuery.isBlank()) return true

        if (data.label.contains(currentQuery, true)
            || data.info.packageName.contains(currentQuery, true)
        ) return true

        if (!activityFilterEmpty || !serviceFilterEmpty)
            return true

        return false
    }

    inner class AppVH(view: View) : RecyclerView.ViewHolder(view) {
        init {
            itemView.apply {
                activities.addItemDecoration(innerDividerItemDecoration)
                services.addItemDecoration(innerDividerItemDecoration)

                activities_title.setOnClickListener {
                    val d = async.currentList[adapterPosition]
                    d.activitiesExpanded = !d.activitiesExpanded

                    notifyItemChanged(adapterPosition)
                }

                services_title.setOnClickListener {
                    val d = async.currentList[adapterPosition]
                    d.servicesExpanded = !d.servicesExpanded

                    notifyItemChanged(adapterPosition)
                }
            }
        }

        fun bind(data: AppInfo) {
            itemView.apply {
                picasso.load(AppIconHandler.createUri(data.info.packageName))
                    .fit()
                    .centerInside()
                    .into(app_icon)

                app_name.text = data.label
                app_pkg.text = data.info.packageName

                activities.adapter = data.activityAdapter
                services.adapter = data.serviceAdapter

                if (activities.isVisible != data.activitiesExpanded) {
                    activities.isVisible = data.activitiesExpanded
                }
                if (services.isVisible != data.servicesExpanded) {
                    services.isVisible = data.servicesExpanded
                }

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
                    data.activityAdapter.setItems(data.filteredActivities)
                }
                if (services.isVisible) {
                    data.serviceAdapter.setItems(data.filteredServices)
                }

                activities_title.isVisible = data.filteredActivities.isNotEmpty()
                services_title.isVisible = data.filteredServices.isNotEmpty()
            }
        }
    }
}