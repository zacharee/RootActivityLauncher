package tk.zwander.rootactivitylauncher.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.EnabledFilterMode
import tk.zwander.rootactivitylauncher.data.ExportedFilterMode
import tk.zwander.rootactivitylauncher.databinding.AppItemBinding
import tk.zwander.rootactivitylauncher.picasso.AppIconHandler
import tk.zwander.rootactivitylauncher.util.*
import tk.zwander.rootactivitylauncher.views.ComponentInfoDialog
import tk.zwander.rootactivitylauncher.views.ExtrasDialog
import kotlin.Comparator
import kotlin.collections.HashMap

class AppAdapter(
    context: Context,
    private val isForTasker: Boolean,
    private val extractCallback: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppVH>(),
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

        val advancedMatch = AdvancedSearcher.matchesHasPermission(currentQuery, data)
                || AdvancedSearcher.matchesRequiresPermission(currentQuery, data)
                || AdvancedSearcher.matchesDeclaresPermission(currentQuery, data)

        if (!advancedMatch && activityFilterEmpty && serviceFilterEmpty && receiverFilterEmpty) return false

        if (currentQuery.isBlank()) return true

        if (useRegex && currentQuery.isValidRegex()) {
            if (Regex(currentQuery).run {
                    containsMatchIn(data.info.packageName)
                            || containsMatchIn(data.label)
                } || advancedMatch) {
                return true
            }
        } else {
            if (data.label.contains(currentQuery, true)
                || data.info.packageName.contains(currentQuery, true)
                || advancedMatch
            ) {
                return true
            }
        }

        if (includeComponents && (!activityFilterEmpty || !serviceFilterEmpty || !receiverFilterEmpty))
            return true

        return false
    }

    inner class AppVH(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = AppItemBinding.bind(itemView)

        private val enabledListener = object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                val d = async.currentList[adapterPosition]

                if (!itemView.context.setPackageEnabled(
                        d.info.packageName, isChecked
                    )
                ) {
                    binding.appEnabled.setOnCheckedChangeListener(null)
                    binding.appEnabled.isChecked = !isChecked
                    binding.appEnabled.setOnCheckedChangeListener(this)
                }
            }
        }

        init {
            itemView.apply {
                binding.activities.addItemDecoration(innerDividerItemDecoration)
                binding.services.addItemDecoration(innerDividerItemDecoration)
                binding.receivers.addItemDecoration(innerDividerItemDecoration)

                binding.activitiesTitle.setOnClickListener {
                    val d = async.currentList[adapterPosition]
                    d.activitiesExpanded = !d.activitiesExpanded

                    notifyItemChanged(adapterPosition)
                }

                binding.servicesTitle.setOnClickListener {
                    val d = async.currentList[adapterPosition]
                    d.servicesExpanded = !d.servicesExpanded

                    notifyItemChanged(adapterPosition)
                }

                binding.receiversTitle.setOnClickListener {
                    val d = async.currentList[adapterPosition]
                    d.receiversExpanded = !d.receiversExpanded

                    notifyItemChanged(adapterPosition)
                }

                binding.appInfo.setOnClickListener {
                    val d = async.currentList[adapterPosition]

                    context.openAppInfo(d.info.packageName)
                }

                binding.globalExtras.setOnClickListener {
                    val d = async.currentList[adapterPosition]

                    ExtrasDialog(context, d.info.packageName)
                        .show()
                }

                binding.appComponentInfo.setOnClickListener {
                    val d = async.currentList[adapterPosition]

                    ComponentInfoDialog(context, d.pInfo)
                        .show()
                }

                binding.appExtract.setOnClickListener {
                    val d = async.currentList[adapterPosition]
                    extractCallback(d)
                }

                binding.actionWrapper.isVisible = !isForTasker
            }
        }

        fun bind(data: AppInfo) {
            itemView.apply {
                picasso.load(AppIconHandler.createUri(data.info.packageName))
                    .fit()
                    .centerInside()
                    .into(binding.appIcon)

                binding.appName.text = data.label
                binding.appPkg.text = data.info.packageName
                if (data.info.enabled != binding.appEnabled.isChecked) {
                    binding.appEnabled.setOnCheckedChangeListener(null)
                    binding.appEnabled.isChecked = data.info.enabled
                    binding.appEnabled.setOnCheckedChangeListener(enabledListener)
                }

                binding.activities.adapter = data.activityAdapter
                binding.services.adapter = data.serviceAdapter
                binding.receivers.adapter = data.receiverAdapter

                binding.activities.layoutManager =
                    context.getAppropriateLayoutManager(context.pxAsDp(width).toInt())
                binding.services.layoutManager =
                    context.getAppropriateLayoutManager(context.pxAsDp(width).toInt())
                binding.receivers.layoutManager =
                    context.getAppropriateLayoutManager(context.pxAsDp(width).toInt())

                if (binding.activities.isVisible != data.activitiesExpanded) {
                    binding.activities.isVisible = data.activitiesExpanded
                }
                if (binding.services.isVisible != data.servicesExpanded) {
                    binding.services.isVisible = data.servicesExpanded
                }
                if (binding.receivers.isVisible != data.receiversExpanded) {
                    binding.receivers.isVisible = data.receiversExpanded
                }

                binding.activitiesTitle.text =
                    resources.getString(R.string.activities, data.filteredActivities.size)
                binding.servicesTitle.text =
                    resources.getString(R.string.services, data.filteredServices.size)
                binding.receiversTitle.text =
                    resources.getString(R.string.receivers, data.filteredReceivers.size)

                binding.activitiesTitle.setCompoundDrawablesRelative(
                    null,
                    null,
                    if (data.activitiesExpanded) arrowUp else arrowDown,
                    null
                )
                binding.servicesTitle.setCompoundDrawablesRelative(
                    null,
                    null,
                    if (data.servicesExpanded) arrowUp else arrowDown,
                    null
                )
                binding.receiversTitle.setCompoundDrawablesRelative(
                    null,
                    null,
                    if (data.receiversExpanded) arrowUp else arrowDown,
                    null
                )

                if (binding.activities.isVisible) {
                    data.activityAdapter.setItems(data.filteredActivities)
                }
                if (binding.services.isVisible) {
                    data.serviceAdapter.setItems(data.filteredServices)
                }
                if (binding.receivers.isVisible) {
                    data.receiverAdapter.setItems(data.filteredReceivers)
                }

                binding.activitiesTitle.isVisible = data.filteredActivities.isNotEmpty()
                binding.servicesTitle.isVisible = data.filteredServices.isNotEmpty()
                binding.receiversTitle.isVisible = data.filteredReceivers.isNotEmpty()
            }
        }
    }
}