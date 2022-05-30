package tk.zwander.rootactivitylauncher.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.EnabledFilterMode
import tk.zwander.rootactivitylauncher.data.ExportedFilterMode
import tk.zwander.rootactivitylauncher.databinding.AppItemBinding
import tk.zwander.rootactivitylauncher.picasso.AppIconHandler
import tk.zwander.rootactivitylauncher.util.*
import tk.zwander.rootactivitylauncher.views.ComponentInfoDialog
import tk.zwander.rootactivitylauncher.views.ExtrasDialog
import kotlin.collections.HashMap

class AppAdapter(
    private val context: Context,
    private val scope: CoroutineScope,
    private val isForTasker: Boolean,
    private val progressCallback: (Int) -> Unit,
    private val extractCallback: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppVH>(), FastScrollRecyclerView.SectionedAdapter {
    init {
        setHasStableIds(true)
    }

    private val async = AsyncListDiffer(this, object : DiffUtil.ItemCallback<AppInfo>() {
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

    var state: State = State()
        private set

    override fun getItemCount(): Int {
        return async.currentList.size
    }

    override fun getItemId(position: Int): Long {
        return async.currentList[position].info.packageName.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppVH {
        return AppVH(LayoutInflater.from(parent.context).inflate(R.layout.app_item, parent, false))
    }

    override fun onBindViewHolder(holder: AppVH, position: Int) {
        holder.bind(async.currentList[position])
    }

    override fun getSectionName(position: Int): String {
        return async.currentList.getOrNull(position)?.label?.substring(0, 1) ?: ""
    }

    suspend fun addItem(item: AppInfo, progress: (Int) -> Unit) {
        orig[item.info.packageName] = item

        onFilterChange(override = true, progress = { current, total -> progress((current / total.toFloat() * 100f).toInt())})
    }

    suspend fun removeItem(packageName: String, progress: (Int) -> Unit) {
        orig.remove(packageName)

        onFilterChange(override = true, progress = { current, total -> progress((current / total.toFloat() * 100f).toInt())})
    }

    fun updateItem(item: AppInfo) {
        updateState { it.copy(hasLoadedItems = false) }
        orig[item.info.packageName] = item

        val index = async.currentList.indexOf(item)
        if (index != -1) {
            notifyItemChanged(index, listOf(Unit))
        }
    }

    fun setItems(items: Collection<AppInfo>) {
        updateState { it.copy(hasLoadedItems = false) }
        orig.clear()
        items.forEachParallelBlocking {
            orig[it.info.packageName] = it
        }

        sortAndSubmitList(orig.values.toList())
    }

    private fun sortAndSubmitList(items: List<AppInfo>) {
        async.submitList(items.sortedWith { o1, o2 ->
            o1.label.toString().compareTo(o2.label.toString(), true)
        })
    }

    private fun updateState(block: (State) -> State) {
        state = block(state)
    }

    suspend fun onFilterChange(
        newState: State = state,
        override: Boolean = false,
        progress: (Int, Int) -> Unit
    ) {
        if (override || newState != state) {
            updateState { newState }

            val total = orig.values.sumOf { it.totalUnfilteredSize }
            var current = 0

            orig.values.forEachParallel {
                it.onFilterChange(
                    context,
                    newState.currentQuery,
                    newState.useRegex,
                    newState.includeComponents,
                    newState.enabledFilterMode,
                    newState.exportedFilterMode,
                    override
                ) { _, _ ->
                    progress(current++, total)
                }
            }
            sortAndSubmitList(filter(newState))
            updateState { newState.copy(hasLoadedItems = true) }
        }
    }

    private fun filter(newState: State): List<AppInfo> {
        return orig.values.filter { matches(it, newState) }
    }

    private fun matches(data: AppInfo, state: State): Boolean {
        val activityFilterEmpty = data.filteredActivities.isEmpty()
        val serviceFilterEmpty = data.filteredServices.isEmpty()
        val receiverFilterEmpty = data.filteredReceivers.isEmpty()

        val advancedMatch = AdvancedSearcher.matchesHasPermission(state.currentQuery, data)
                || AdvancedSearcher.matchesRequiresPermission(state.currentQuery, data)
                || AdvancedSearcher.matchesDeclaresPermission(state.currentQuery, data)
                || AdvancedSearcher.matchesRequiresFeature(state.currentQuery, data)

        if (!advancedMatch && activityFilterEmpty && serviceFilterEmpty && receiverFilterEmpty) return false

        if (state.currentQuery.isBlank()) return true

        if (state.useRegex && state.currentQuery.isValidRegex()) {
            if (Regex(state.currentQuery).run {
                    containsMatchIn(data.info.packageName)
                            || containsMatchIn(data.label)
                } || advancedMatch) {
                return true
            }
        } else {
            if (data.label.contains(state.currentQuery, true)
                || data.info.packageName.contains(state.currentQuery, true)
                || advancedMatch
            ) {
                return true
            }
        }

        if (state.includeComponents /* filters won't be empty by this point (if changing code, make sure they still won't be) */)
            return true

        return false
    }

    inner class AppVH(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = AppItemBinding.bind(itemView)

        private val enabledListener = object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                val d = async.currentList[adapterPosition]

                if (!itemView.context.setPackageEnabled(d.info.packageName, isChecked)) {
                    binding.appEnabled.setOnCheckedChangeListener(null)
                    binding.appEnabled.isChecked = !isChecked
                    binding.appEnabled.setOnCheckedChangeListener(this)
                }
            }
        }

        init {
            binding.activitiesComponent.addItemDecoration(innerDividerItemDecoration)
            binding.servicesComponent.addItemDecoration(innerDividerItemDecoration)
            binding.receiversComponent.addItemDecoration(innerDividerItemDecoration)

            binding.activitiesComponent.setOnTitleClickListener {
                if (adapterPosition == -1) return@setOnTitleClickListener

                val d = async.currentList[adapterPosition]
                d.activitiesExpanded = !d.activitiesExpanded

                notifyItemChanged(adapterPosition, listOf(Unit))
            }

            binding.servicesComponent.setOnTitleClickListener {
                if (adapterPosition == -1) return@setOnTitleClickListener

                val d = async.currentList[adapterPosition]
                d.servicesExpanded = !d.servicesExpanded

                notifyItemChanged(adapterPosition, listOf(Unit))
            }

            binding.receiversComponent.setOnTitleClickListener {
                if (adapterPosition == -1) return@setOnTitleClickListener

                val d = async.currentList[adapterPosition]
                d.receiversExpanded = !d.receiversExpanded

                notifyItemChanged(adapterPosition, listOf(Unit))
            }

            binding.appInfo.setOnClickListener {
                if (adapterPosition == -1) return@setOnClickListener

                val d = async.currentList[adapterPosition]

                context.openAppInfo(d.info.packageName)
            }

            binding.globalExtras.setOnClickListener {
                if (adapterPosition == -1) return@setOnClickListener

                val d = async.currentList[adapterPosition]

                ExtrasDialog(context, d.info.packageName)
                    .show()
            }

            binding.appComponentInfo.setOnClickListener {
                if (adapterPosition == -1) return@setOnClickListener

                val d = async.currentList[adapterPosition]

                ComponentInfoDialog(context, d.pInfo)
                    .show()
            }

            binding.appExtract.setOnClickListener {
                if (adapterPosition == -1) return@setOnClickListener

                val d = async.currentList[adapterPosition]
                extractCallback(d)
            }

            binding.actionWrapper.isVisible = !isForTasker
        }

        fun bind(data: AppInfo) {
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

            binding.activitiesComponent.adapter = data.activityAdapter
            binding.servicesComponent.adapter = data.serviceAdapter
            binding.receiversComponent.adapter = data.receiverAdapter

            binding.activitiesComponent.updateLayoutManager(itemView.width)
            binding.servicesComponent.updateLayoutManager(itemView.width)
            binding.receiversComponent.updateLayoutManager(itemView.width)

            binding.activitiesComponent.count = data.activitiesSize
            binding.servicesComponent.count = data.servicesSize
            binding.receiversComponent.count = data.receiversSize

            binding.activitiesComponent.isVisible = data.activitiesSize > 0
            binding.servicesComponent.isVisible = data.servicesSize > 0
            binding.receiversComponent.isVisible = data.receiversSize > 0

            if (data.activitiesExpanded) {
                scope.async(Dispatchers.Main) {
                    data.loadActivities { current, total ->
                        progressCallback((current / total.toFloat() * 100f).toInt())
                    }

                    binding.activitiesComponent.updateHeight(data.activitiesSize)
                    data.activityAdapter.setItems(data.filteredActivities)
                }
            }
            binding.activitiesComponent.expanded = data.activitiesExpanded

            if (data.servicesExpanded) {
                scope.async(Dispatchers.Main) {
                    data.loadServices { current, total ->
                        progressCallback((current / total.toFloat() * 100f).toInt())
                    }

                    binding.servicesComponent.updateHeight(data.servicesSize)
                    data.serviceAdapter.setItems(data.filteredServices)
                }
            }
            binding.servicesComponent.expanded = data.servicesExpanded

            if (data.receiversExpanded) {
                scope.async(Dispatchers.Main) {
                    data.loadReceivers { current, total ->
                        progressCallback((current / total.toFloat() * 100f).toInt())
                    }

                    binding.receiversComponent.updateHeight(data.receiversSize)
                    data.receiverAdapter.setItems(data.filteredReceivers)
                }
            }
            binding.receiversComponent.expanded = data.receiversExpanded
        }
    }

    data class State(
        val currentQuery: String = "",
        val enabledFilterMode: EnabledFilterMode = EnabledFilterMode.SHOW_ALL,
        val exportedFilterMode: ExportedFilterMode = ExportedFilterMode.SHOW_ALL,
        val includeComponents: Boolean = true,
        val hasLoadedItems: Boolean = false,
        val useRegex: Boolean = false,
    ) {
        val hasFilters: Boolean
            get() = currentQuery.isNotBlank() || enabledFilterMode != EnabledFilterMode.SHOW_ALL || exportedFilterMode != ExportedFilterMode.SHOW_ALL
    }
}