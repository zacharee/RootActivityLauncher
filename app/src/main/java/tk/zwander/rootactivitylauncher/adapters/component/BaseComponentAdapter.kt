package tk.zwander.rootactivitylauncher.adapters.component

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.squareup.picasso.Picasso
import eu.chainfire.libsuperuser.Shell
import kotlinx.android.synthetic.main.component_item.view.*
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.EnabledFilterMode
import tk.zwander.rootactivitylauncher.data.ExportedFilterMode
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import tk.zwander.rootactivitylauncher.util.constructComponentKey
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.views.ExtrasDialog
import kotlin.collections.ArrayList

abstract class BaseComponentAdapter<
        Self : BaseComponentAdapter<Self, DataClass, VHClass>,
        DataClass : BaseComponentInfo,
        VHClass : BaseComponentAdapter<Self, DataClass, VHClass>.BaseComponentVH>(
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
                        && oldItem.info.exported == newItem.info.exported

        })
    internal val orig = ArrayList<DataClass>()

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHClass {
        return onCreateViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.component_item, parent, false),
            viewType
        )
    }

    abstract fun onCreateViewHolder(view: View, viewType: Int): VHClass

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
        this.items.replaceAll(filter(currentQuery))
    }

    internal open fun filter(query: String): List<DataClass> {
        val filteredModelList = ArrayList<DataClass>()

        for (i in 0 until orig.size) {
            val item = orig[i]

            if (matches(query, item)) filteredModelList.add(item)
        }

        return filteredModelList
    }

    internal open fun filter(query: String, items: Collection<DataClass>): List<DataClass> {
        return items.filter { matches(query, it) }
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
                    || data.info.name.contains(query, true))
        ) return true

        return false
    }

    abstract inner class BaseComponentVH(view: View) : RecyclerView.ViewHolder(view) {
        internal val currentExtras: List<ExtraInfo>
            get() = itemView.context.findExtrasForComponent(currentComponentKey)
        internal val currentComponentKey: String
            get() = items[adapterPosition].run {
                constructComponentKey(
                    info.packageName,
                    info.name
                )
            }

        internal val componentEnabledListener = object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                val d = items[adapterPosition]
                val l = this

                launch(Dispatchers.Main) {
                    val hasRoot = withContext(Dispatchers.IO) {
                        Shell.SU.available()
                    }

                    if (hasRoot) {
                        val result = withContext(Dispatchers.IO) {
                            Shell.Pool.SU.run("pm ${if (isChecked) "enable" else "disable"} $currentComponentKey") == 0
                        }

                        if (result) {
                            d.info.enabled = isChecked
                            updateLaunchVisibility(d)
                        } else {
                            buttonView.setOnCheckedChangeListener(null)
                            buttonView.isChecked = !isChecked
                            buttonView.setOnCheckedChangeListener(l)
                        }
                    } else {
                        Toast.makeText(itemView.context, R.string.requires_root, Toast.LENGTH_SHORT)
                            .show()
                        buttonView.setOnCheckedChangeListener(null)
                        buttonView.isChecked = !isChecked
                        buttonView.setOnCheckedChangeListener(l)
                    }
                }
            }
        }

        internal var prevPos = -1

        init {
            itemView.apply {
                set_extras.setOnClickListener {
                    ExtrasDialog(context, currentComponentKey)
                        .show()
                }
                launch.setOnClickListener {
                    onLaunch(items[adapterPosition], context, currentExtras)
                }
            }
        }

        fun bind(data: DataClass): Job = launch {
            if (adapterPosition != prevPos) {
                prevPos = adapterPosition

                onNewPosition(data)
            }

            onBind(data)
        }

        open fun onBind(data: DataClass): Job = launch {
            itemView.apply {
                name.text = data.loadedLabel
                cmp.text = data.info.name

                getPicassoUri(data)?.apply {
                    picasso.load(this)
                        .fit()
                        .centerInside()
                        .into(icon)
                }

                enabled.setOnCheckedChangeListener(null)
                if (enabled.isChecked != data.info.enabled) enabled.isChecked = data.info.enabled
                updateLaunchVisibility(data)
                enabled.setOnCheckedChangeListener(componentEnabledListener)
            }
        }

        open fun onNewPosition(data: DataClass): Job = launch {
            itemView.apply {
                name.text = data.loadedLabel
                cmp.text = data.info.name

                picasso.load(ActivityIconHandler.createUri(data.info.packageName, data.info.name))
                    .fit()
                    .centerInside()
                    .into(icon)
            }
        }

        open fun onLaunch(data: DataClass, context: Context, extras: List<ExtraInfo>): Job =
            launch {}

        abstract fun getPicassoUri(data: DataClass): Uri?

        private fun updateLaunchVisibility(data: DataClass) {
            itemView.apply {
                if (launch.isVisible != data.info.enabled) launch.isVisible = data.info.enabled
            }
        }
    }
}