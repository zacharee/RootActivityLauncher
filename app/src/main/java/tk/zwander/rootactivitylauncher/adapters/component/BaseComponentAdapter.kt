package tk.zwander.rootactivitylauncher.adapters.component

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.data.EnabledFilterMode
import tk.zwander.rootactivitylauncher.data.ExportedFilterMode
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.util.constructComponentKey
import java.util.*
import kotlin.collections.ArrayList

abstract class BaseComponentAdapter<Self : BaseComponentAdapter<Self, DataClass, VHClass>, DataClass : BaseComponentInfo, VHClass : BaseComponentAdapter<Self, DataClass, VHClass>.BaseComponentVH>(
    internal val picasso: Picasso,
    dataClass: Class<DataClass>
) : RecyclerView.Adapter<VHClass>(), CoroutineScope by MainScope() {
    val items: SortedList<DataClass> =
        SortedList(dataClass, object : SortedList.Callback<DataClass>() {
            override fun areItemsTheSame(item1: DataClass, item2: DataClass) =
                constructComponentKey(item1.info.packageName, item1.info.name) ==
                        constructComponentKey(item2.info.packageName, item2.info.name)

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

            override fun compare(o1: DataClass, o2: DataClass): Int {
                return runBlocking {
                    withContext(Dispatchers.IO) {
                        o1.loadedLabel.toString()
                            .compareTo(o2.loadedLabel.toString(), true)
                    }
                }
            }

            override fun areContentsTheSame(oldItem: DataClass, newItem: DataClass) =
                oldItem.info.packageName == newItem.info.packageName

        })
    internal val orig =
        object : SortedList<DataClass>(dataClass, object : SortedList.Callback<DataClass>() {
            override fun areItemsTheSame(item1: DataClass, item2: DataClass): Boolean {
                return item1 == item2
            }

            override fun compare(o1: DataClass, o2: DataClass): Int {
                return runBlocking {
                    withContext(Dispatchers.IO) {
                        o1.loadedLabel.toString()
                            .compareTo(o2.loadedLabel.toString(), true)
                    }
                }
            }

            override fun areContentsTheSame(oldItem: DataClass, newItem: DataClass): Boolean {
                return oldItem.info.packageName == newItem.info.packageName
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {}
            override fun onChanged(position: Int, count: Int) {}
            override fun onInserted(position: Int, count: Int) {}
            override fun onRemoved(position: Int, count: Int) {}
        }) {
            override fun replaceAll(items: Array<out DataClass>, mayModifyInput: Boolean) {
                this@BaseComponentAdapter.items.replaceAll(items.filter {
                    matches(
                        currentQuery,
                        it
                    )
                })
                super.replaceAll(items, mayModifyInput)
            }

            override fun add(item: DataClass): Int {
                if (matches(currentQuery, item)) {
                    items.add(item)
                }
                return super.add(item)
            }

            override fun addAll(items: Array<out DataClass>, mayModifyInput: Boolean) {
                this@BaseComponentAdapter.items.addAll(items.filter { matches(currentQuery, it) })
                super.addAll(items, mayModifyInput)
            }

            override fun remove(item: DataClass): Boolean {
                items.remove(item)
                return super.remove(item)
            }

            override fun clear() {
                items.clear()
                super.clear()
            }
        }

    internal var currentQuery: String = ""
    internal var enabledFilterMode = EnabledFilterMode.SHOW_ALL
    internal var exportedFilterMode = ExportedFilterMode.SHOW_ALL

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return items[position].run {
            constructComponentKey(info.packageName, info.name).hashCode().toLong()
        }
    }

    override fun getItemCount(): Int {
        return items.size()
    }

    override fun onBindViewHolder(holder: VHClass, position: Int) {
        holder.bind(items[position])
    }

    fun setEnabledFilterMode(filterMode: EnabledFilterMode) {
        this.enabledFilterMode = filterMode
        items.replaceAll(filter(currentQuery))
    }

    fun setExportedFilterMode(filterMode: ExportedFilterMode) {
        this.exportedFilterMode = filterMode
        items.replaceAll(filter(currentQuery))
    }

    fun onQueryTextChange(newText: String?) {
        currentQuery = newText ?: ""
        items.replaceAll(filter(currentQuery))
    }

    fun setItems(items: List<DataClass>) {
        orig.replaceAll(items)
    }

    internal open fun filter(query: String): List<DataClass> {
        val lowerCaseQuery = query.toLowerCase(Locale.getDefault())

        val filteredModelList = ArrayList<DataClass>()

        for (i in 0 until orig.size()) {
            val item = orig[i]

            if (matches(lowerCaseQuery, item)) filteredModelList.add(item)
        }

        return filteredModelList
    }

    internal open fun filter(query: String, items: Collection<DataClass>): List<DataClass> {
        val lowerCaseQuery = query.toLowerCase(Locale.getDefault())

        val filteredModelList = ArrayList<DataClass>()

        for (element in items) {
            if (matches(lowerCaseQuery, element)) filteredModelList.add(element)
        }

        return filteredModelList
    }

    internal open fun matches(query: String, data: DataClass): Boolean {
        when (enabledFilterMode) {
            EnabledFilterMode.SHOW_DISABLED -> if (data.info.enabled) return false
            EnabledFilterMode.SHOW_ENABLED -> if (!data.info.enabled) return false
            else -> {
                //no-op
            }
        }

        when (exportedFilterMode) {
            ExportedFilterMode.SHOW_EXPORTED -> if (!data.info.exported) return false
            ExportedFilterMode.SHOW_UNEXPORTED -> if (data.info.exported) return false
            else -> {
                //no-op
            }
        }

        if (query.isBlank()) return true

        if ((data.loadedLabel.contains(query, true)
                    || data.info.name.contains(query, true))) return true

        return false
    }

    abstract inner class BaseComponentVH(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(data: DataClass): Job
    }
}