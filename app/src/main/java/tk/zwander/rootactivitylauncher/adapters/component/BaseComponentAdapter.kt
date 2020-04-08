package tk.zwander.rootactivitylauncher.adapters.component

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
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
        VHClass : BaseComponentAdapter<Self, DataClass, VHClass>.BaseComponentVH> :
    RecyclerView.Adapter<VHClass>(), CoroutineScope by MainScope() {
    val async = AsyncListDiffer(this, object : DiffUtil.ItemCallback<DataClass>() {
        override fun areContentsTheSame(oldItem: DataClass, newItem: DataClass): Boolean {
            return false
        }

        override fun areItemsTheSame(oldItem: DataClass, newItem: DataClass): Boolean {
            return constructComponentKey(oldItem.info) ==
                    constructComponentKey(newItem.info)
        }
    })

    override fun getItemCount(): Int {
        return async.currentList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHClass {
        return onCreateViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.component_item, parent, false),
            viewType
        )
    }

    abstract fun onCreateViewHolder(view: View, viewType: Int): VHClass

    override fun onBindViewHolder(holder: VHClass, position: Int) {
        holder.bind(async.currentList[position])
    }

    fun setItems(items: Collection<DataClass>) {
        async.submitList(items.toList())
    }

    abstract inner class BaseComponentVH(view: View) : RecyclerView.ViewHolder(view) {
        internal abstract val componentType: ComponentType

        internal val currentExtras: List<ExtraInfo>
            get() = itemView.context.findExtrasForComponent(currentComponentKey)
        internal val currentComponentKey: String
            get() = async.currentList[adapterPosition].run {
                constructComponentKey(
                    info.packageName,
                    info.name
                )
            }

        internal val componentEnabledListener = object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                val d = async.currentList[adapterPosition]
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
                    onLaunch(async.currentList[adapterPosition], context, currentExtras)
                }
                shortcut.setOnClickListener {
                    val d = async.currentList[adapterPosition]
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

        fun bind(data: DataClass): Job = launch {
            if (adapterPosition != prevPos) {
                prevPos = adapterPosition

                onNewPosition(data)
            }

            onBind(data)
        }

        open fun onBind(data: DataClass): Job = launch {
            itemView.apply {
                name.text = data.label
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
                name.text = data.label
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