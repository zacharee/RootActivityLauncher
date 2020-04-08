package tk.zwander.rootactivitylauncher.adapters.component

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.*
import eu.chainfire.libsuperuser.Shell
import kotlinx.android.synthetic.main.component_item.view.*
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import tk.zwander.rootactivitylauncher.util.constructComponentKey
import tk.zwander.rootactivitylauncher.util.createShortcut
import tk.zwander.rootactivitylauncher.util.findExtrasForComponent
import tk.zwander.rootactivitylauncher.util.picasso
import tk.zwander.rootactivitylauncher.views.ExtrasDialog

abstract class BaseComponentAdapter<
        Self : BaseComponentAdapter<Self, DataClass, VHClass>,
        DataClass : BaseComponentInfo,
        VHClass : BaseComponentAdapter<Self, DataClass, VHClass>.BaseComponentVH>(
    dataClass: Class<DataClass>
) :
    RecyclerView.Adapter<VHClass>(), CoroutineScope by MainScope() {
    val currentList = SortedList(dataClass, object : SortedListAdapterCallback<DataClass>(this) {
        override fun areItemsTheSame(item1: DataClass, item2: DataClass): Boolean {
            return constructComponentKey(item1.info) == constructComponentKey(item2.info)
        }

        override fun compare(o1: DataClass, o2: DataClass): Int {
            return o1.compareTo(o2)
        }

        override fun areContentsTheSame(oldItem: DataClass, newItem: DataClass): Boolean {
            return false
        }

    })

    override fun getItemCount(): Int {
        return currentList.size()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHClass {
        return onCreateViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.component_item, parent, false),
            viewType
        )
    }

    abstract fun onCreateViewHolder(view: View, viewType: Int): VHClass

    override fun onBindViewHolder(holder: VHClass, position: Int) {
        holder.bind(currentList[position])
    }

    fun setItems(items: Collection<DataClass>) {
        currentList.replaceAll(items)
    }

    abstract inner class BaseComponentVH(view: View) : RecyclerView.ViewHolder(view) {
        internal abstract val componentType: ComponentType

        internal val currentExtras: List<ExtraInfo>
            get() = itemView.context.findExtrasForComponent(currentComponentKey)
        internal val currentComponentKey: String
            get() = currentList[adapterPosition].run {
                constructComponentKey(
                    info.packageName,
                    info.name
                )
            }

        internal val componentEnabledListener = object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                val d = currentList[adapterPosition]
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
                    onLaunch(currentList[adapterPosition], context, currentExtras)
                }
                shortcut.setOnClickListener {
                    val d = currentList[adapterPosition]
                    context.createShortcut(
                        d.label,
                        IconCompat.createWithBitmap(
                            icon.drawable.toBitmap()
                        ),
                        currentComponentKey,
                        componentType
                    )
                }
            }
        }

        fun bind(data: DataClass) {
            onBind(data)
        }

        open fun onBind(data: DataClass) {
            itemView.apply {
                picasso.load(ActivityIconHandler.createUri(data.info.packageName, data.info.name))
                    .fit()
                    .centerInside()
                    .into(icon)

                name.text = data.label
                cmp.text = data.info.name

                val info = data.info
                val requiresPermission = (info is ActivityInfo && info.permission != null) || (info is ServiceInfo && info.permission != null)

                launch_status_indicator.setColorFilter(
                    ContextCompat.getColor(context,
                        when {
                            !data.info.exported -> R.color.colorUnexported
                            requiresPermission -> R.color.colorNeedsPermission
                            else -> R.color.colorExported
                        }
                    )
                )

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

        open fun onLaunch(data: DataClass, context: Context, extras: List<ExtraInfo>) {}

        abstract fun getPicassoUri(data: DataClass): Uri?

        private fun updateLaunchVisibility(data: DataClass) {
            itemView.apply {
                if (launch.isVisible != data.info.enabled) launch.isVisible = data.info.enabled
            }
        }
    }
}