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
import tk.zwander.rootactivitylauncher.util.*
import tk.zwander.rootactivitylauncher.views.ComponentInfoDialog
import tk.zwander.rootactivitylauncher.views.ExtrasDialog
import kotlin.Comparator
import kotlin.collections.HashMap

class AppAdapter(context: Context, private val isForTasker: Boolean, private val extractCallback: (AppInfo) -> Unit) : RecyclerView.Adapter<AppAdapter.AppVH>(),
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
    var useRegex: Boolean = false
        private set
    var includeComponents: Boolean = true
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

    fun addItem(item: AppInfo) {
        orig[item.info.packageName] = item

        onFilterChange(override = true)
    }

    fun removeItem(packageName: String) {
        orig.remove(packageName)

        onFilterChange(override = true)
    }

    fun removeItem(item: AppInfo) {
        removeItem(item.info.packageName)
    }

    fun updateItem(item: AppInfo) {
        orig[item.info.packageName] = item

        val index = async.currentList.indexOf(item)
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    fun setItems(items: Collection<AppInfo>) {
        orig.clear()
        items.forEachParallelBlocking {
            orig[it.info.packageName] = it
        }

        onFilterChange(override = true)
    }

    private fun sortAndSubmitList(items: List<AppInfo>) {
        async.submitList(items.sortedWith(Comparator { o1, o2 ->
            o1.label.toString().compareTo(o2.label.toString(), true)
        }))
    }

    fun onFilterChange(
        query: String = currentQuery,
        useRegex: Boolean = this.useRegex,
        includeComponents: Boolean = this.includeComponents,
        enabledMode: EnabledFilterMode = enabledFilterMode,
        exportedMode: ExportedFilterMode = exportedFilterMode,
        override: Boolean = false
    ) {
        if (override
            || currentQuery != query
            || enabledFilterMode != enabledMode
            || exportedFilterMode != exportedMode
            || this.includeComponents != includeComponents
            || this.useRegex != useRegex
        ) {
            currentQuery = query
            enabledFilterMode = enabledMode
            exportedFilterMode = exportedMode
            this.useRegex = useRegex
            this.includeComponents = includeComponents

            orig.values.forEachParallelBlocking {
                it.onFilterChange(
                    currentQuery,
                    useRegex,
                    includeComponents,
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
        val receiverFilterEmpty = data.filteredReceivers.isEmpty()

        if (activityFilterEmpty && serviceFilterEmpty && receiverFilterEmpty) return false

        if (currentQuery.isBlank()) return true

        if (useRegex && currentQuery.isValidRegex()) {
            if (Regex(currentQuery).run {
                    containsMatchIn(data.info.packageName)
                            || containsMatchIn(data.label)
                }) {
                return true
            }
        } else {
            if (data.label.contains(currentQuery, true)
                || data.info.packageName.contains(currentQuery, true)
            ) {
                return true
            }
        }

        if (includeComponents && (!activityFilterEmpty || !serviceFilterEmpty || !receiverFilterEmpty))
            return true

        return false
    }

    inner class AppVH(view: View) : RecyclerView.ViewHolder(view) {
        init {
            itemView.apply {
                activities.addItemDecoration(innerDividerItemDecoration)
                services.addItemDecoration(innerDividerItemDecoration)
                receivers.addItemDecoration(innerDividerItemDecoration)

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

                receivers_title.setOnClickListener {
                    val d = async.currentList[adapterPosition]
                    d.receiversExpanded = !d.receiversExpanded

                    notifyItemChanged(adapterPosition)
                }

                app_info.setOnClickListener {
                    val d = async.currentList[adapterPosition]

                    context.openAppInfo(d.info.packageName)
                }

                global_extras.setOnClickListener {
                    val d = async.currentList[adapterPosition]

                    ExtrasDialog(context, d.info.packageName)
                        .show()
                }

                app_component_info.setOnClickListener {
                    val d = async.currentList[adapterPosition]

                    ComponentInfoDialog(context, d.pInfo)
                            .show()
                }

                app_extract.setOnClickListener {
                    val d = async.currentList[adapterPosition]
                    extractCallback(d)
                }

                action_wrapper.isVisible = !isForTasker
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
                receivers.adapter = data.receiverAdapter

                activities.layoutManager = context.getAppropriateLayoutManager(context.pxAsDp(width).toInt())
                services.layoutManager = context.getAppropriateLayoutManager(context.pxAsDp(width).toInt())
                receivers.layoutManager = context.getAppropriateLayoutManager(context.pxAsDp(width).toInt())

                if (activities.isVisible != data.activitiesExpanded) {
                    activities.isVisible = data.activitiesExpanded
                }
                if (services.isVisible != data.servicesExpanded) {
                    services.isVisible = data.servicesExpanded
                }
                if (receivers.isVisible != data.receiversExpanded) {
                    receivers.isVisible = data.receiversExpanded
                }

                activities_title.text =
                    resources.getString(R.string.activities, data.filteredActivities.size)
                services_title.text =
                    resources.getString(R.string.services, data.filteredServices.size)
                receivers_title.text =
                    resources.getString(R.string.receivers, data.filteredReceivers.size)

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
                receivers_title.setCompoundDrawablesRelative(
                    null,
                    null,
                    if (data.receiversExpanded) arrowUp else arrowDown,
                    null
                )

                if (activities.isVisible) {
                    data.activityAdapter.setItems(data.filteredActivities)
                }
                if (services.isVisible) {
                    data.serviceAdapter.setItems(data.filteredServices)
                }
                if (receivers.isVisible) {
                    data.receiverAdapter.setItems(data.filteredReceivers)
                }

                activities_title.isVisible = data.filteredActivities.isNotEmpty()
                services_title.isVisible = data.filteredServices.isNotEmpty()
                receivers_title.isVisible = data.filteredReceivers.isNotEmpty()
            }
        }
    }
}