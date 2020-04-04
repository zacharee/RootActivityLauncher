package tk.zwander.rootactivitylauncher.adapters.component

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import tk.zwander.rootactivitylauncher.data.EnabledFilterMode
import tk.zwander.rootactivitylauncher.data.ExportedFilterMode
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.util.constructComponentKey
import java.util.*
import kotlin.collections.ArrayList

abstract class BaseComponentAdapter<Self : BaseComponentAdapter<Self, DataClass, VHClass>, DataClass : BaseComponentInfo, VHClass : BaseComponentAdapter<Self, DataClass, VHClass>.BaseComponentVH>(
    internal val picasso: Picasso
) : RecyclerView.Adapter<VHClass>(), CoroutineScope by MainScope() {
    internal abstract val items: SortedList<DataClass>

    internal val orig = object : ArrayList<DataClass>() {
        override fun add(element: DataClass): Boolean {
            if (matches(currentQuery, element)) {
                items.add(element)
            }
            return super.add(element)
        }

        override fun addAll(elements: Collection<DataClass>): Boolean {
            items.addAll(elements.filter { matches(currentQuery, it) })
            return super.addAll(elements)
        }

        override fun remove(element: DataClass): Boolean {
            items.remove(element)
            return super.remove(element)
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
        return items[position].run { constructComponentKey(info.packageName, info.name).hashCode().toLong() }
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
        orig.clear()
        orig.addAll(items)
    }

    internal open fun filter(query: String): List<DataClass> {
        val lowerCaseQuery = query.toLowerCase(Locale.getDefault())

        val filteredModelList = ArrayList<DataClass>()

        for (i in 0 until orig.size) {
            val item = orig[i]

            if (matches(lowerCaseQuery, item)) filteredModelList.add(item)
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

        if (data.label.contains(query, true)
            || data.info.name.contains(query, true)
        ) return true

        return false
    }

    abstract inner class BaseComponentVH(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(data: DataClass): Job
    }
}