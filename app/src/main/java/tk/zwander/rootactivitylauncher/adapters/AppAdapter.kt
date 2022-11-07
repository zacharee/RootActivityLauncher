package tk.zwander.rootactivitylauncher.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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
import tk.zwander.rootactivitylauncher.data.PermissionFilterMode
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.databinding.AppItemBinding
import tk.zwander.rootactivitylauncher.picasso.AppIconHandler
import tk.zwander.rootactivitylauncher.util.*
import tk.zwander.rootactivitylauncher.views.ComponentInfoDialog
import tk.zwander.rootactivitylauncher.views.ExtrasDialog
import tk.zwander.rootactivitylauncher.views.components.ComponentGroup
import kotlin.collections.HashMap

class AppAdapter(
    private val context: Context,
    private val scope: CoroutineScope,
    private val isForTasker: Boolean,
    private val progressCallback: (Int) -> Unit,
    private val extractCallback: (AppInfo) -> Unit,
    private val selectionCallback: (BaseComponentInfo) -> Unit
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

    fun updateState(block: (State) -> State) {
        state = block(state)
    }

    suspend fun onFilterChange(
        newState: State = state,
        override: Boolean = false,
        progress: ((Int, Int) -> Unit)? = null
    ) {
//        if (override || newState != state) {
//            updateState { newState }
//
//            val total = orig.values.sumOf { it.totalUnfilteredSize }
//            var current = 0
//
//            orig.values.forEachParallel {
//                it.onFilterChange(
//                    newState.currentQuery,
//                    newState.useRegex,
//                    newState.includeComponents,
//                    newState.enabledFilterMode,
//                    newState.exportedFilterMode,
//                    newState.permissionFilterMode,
//                    override
//                ) { _, _ ->
//                    progress?.invoke(current++, total)
//                }
//            }
//            sortAndSubmitList(filter(newState))
//            updateState { newState.copy(hasLoadedItems = true) }
//        }
    }

//    private fun filter(newState: State): List<AppInfo> {
//        return orig.values.filter { matches(it, newState) }
//    }

    inner class AppVH(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = AppItemBinding.bind(itemView)

        private val enabledListener = object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                val d = async.currentList[bindingAdapterPosition]

                if (!itemView.context.setPackageEnabled(d.info.packageName, isChecked)) {
                    binding.appEnabled.setOnCheckedChangeListener(null)
                    binding.appEnabled.isChecked = !isChecked
                    binding.appEnabled.setOnCheckedChangeListener(this)
                }
            }
        }

        init {
            binding.appInfo.setOnClickListener {
                if (bindingAdapterPosition == -1) return@setOnClickListener

                val d = async.currentList[bindingAdapterPosition]

                context.openAppInfo(d.info.packageName)
            }

            binding.globalExtras.setOnClickListener {
                if (bindingAdapterPosition == -1) return@setOnClickListener

                val d = async.currentList[bindingAdapterPosition]

                ExtrasDialog(context, d.info.packageName)
                    .show()
            }

            binding.appComponentInfo.setOnClickListener {
                if (bindingAdapterPosition == -1) return@setOnClickListener

                val d = async.currentList[bindingAdapterPosition]

                ComponentInfoDialog(context, d.pInfo)
                    .show()
            }

            binding.appExtract.setOnClickListener {
                if (bindingAdapterPosition == -1) return@setOnClickListener

                val d = async.currentList[bindingAdapterPosition]
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

            if (data.info.isActuallyEnabled(context) != binding.appEnabled.isChecked) {
                binding.appEnabled.setOnCheckedChangeListener(null)
                binding.appEnabled.isChecked = data.info.isActuallyEnabled(context)
                binding.appEnabled.setOnCheckedChangeListener(enabledListener)
            } else {
                binding.appEnabled.setOnCheckedChangeListener(enabledListener)
            }

            binding.activitiesComponent.setContent {

            }

            binding.servicesComponent.setContent {
                ComponentGroup(
                    titleRes = R.string.services,
                    items = data.filteredServices,
                    expanded = data.servicesExpanded,
                    onExpandChange = {
                        data.servicesExpanded = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    forTasker = isForTasker,
                    onItemSelected = selectionCallback,
                    count = data.servicesSize
                )

                LaunchedEffect(key1 = data.servicesExpanded) {
                    if (data.servicesExpanded) {
                        data.loadServices { current, total ->
                            progressCallback((current / total.toFloat() * 100f).toInt())
                        }
                    }
                }
            }

            binding.receiversComponent.setContent {
                ComponentGroup(
                    titleRes = R.string.receivers,
                    items = data.filteredReceivers,
                    expanded = data.receiversExpanded,
                    onExpandChange = {
                        data.receiversExpanded = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    forTasker = isForTasker,
                    onItemSelected = selectionCallback,
                    count = data.receiversSize
                )

                LaunchedEffect(key1 = data.receiversExpanded) {
                    if (data.receiversExpanded) {
                        data.loadReceivers { current, total ->
                            progressCallback((current / total.toFloat() * 100f).toInt())
                        }
                    }
                }
            }
        }
    }

    data class State(
        val currentQuery: String = "",
        val enabledFilterMode: EnabledFilterMode = EnabledFilterMode.SHOW_ALL,
        val exportedFilterMode: ExportedFilterMode = ExportedFilterMode.SHOW_ALL,
        val permissionFilterMode: PermissionFilterMode = PermissionFilterMode.SHOW_ALL,
        val includeComponents: Boolean = true,
        val hasLoadedItems: Boolean = false,
        val useRegex: Boolean = false,
    ) {
        val hasFilters: Boolean
            get() = currentQuery.isNotBlank() ||
                    enabledFilterMode != EnabledFilterMode.SHOW_ALL ||
                    exportedFilterMode != ExportedFilterMode.SHOW_ALL ||
                    permissionFilterMode != PermissionFilterMode.SHOW_ALL
    }
}