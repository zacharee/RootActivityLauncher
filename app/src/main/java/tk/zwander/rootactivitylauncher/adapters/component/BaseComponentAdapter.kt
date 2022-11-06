package tk.zwander.rootactivitylauncher.adapters.component

import android.content.Context
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.recyclerview.widget.*
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.databinding.ComponentItemBinding
import tk.zwander.rootactivitylauncher.util.*
import tk.zwander.rootactivitylauncher.views.ComponentInfoDialog
import tk.zwander.rootactivitylauncher.views.components.Button
import tk.zwander.rootactivitylauncher.views.components.ComponentBar
import tk.zwander.rootactivitylauncher.views.components.ExtrasDialog

abstract class BaseComponentAdapter<
        Self : BaseComponentAdapter<Self, DataClass, VHClass>,
        DataClass : BaseComponentInfo,
        VHClass : BaseComponentAdapter<Self, DataClass, VHClass>.BaseComponentVH>(
    dataClass: Class<DataClass>,
    private val isForTasker: Boolean,
    private val selectionCallback: (BaseComponentInfo) -> Unit
) : RecyclerView.Adapter<VHClass>(), CoroutineScope by MainScope() {
    init {
        @Suppress("LeakingThis")
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    val currentList by lazy {
        SortedList(dataClass, object : SortedListAdapterCallback<DataClass>(this) {
            override fun areItemsTheSame(item1: DataClass, item2: DataClass): Boolean {
                return constructComponentKey(item1.info) == constructComponentKey(item2.info)
            }

            override fun compare(o1: DataClass, o2: DataClass): Int {
                return o1.compareTo(o2)
            }

            override fun areContentsTheSame(oldItem: DataClass, newItem: DataClass): Boolean {
                return true
            }
        })
    }

    override fun getItemCount(): Int {
        return currentList.size()
    }

    override fun getItemId(position: Int): Long {
        return currentList[position].component.hashCode().toLong()
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
        private val binding = ComponentItemBinding.bind(itemView)

        internal abstract val componentType: ComponentType

        internal val currentComponentKey: String
            get() = currentList[bindingAdapterPosition].run {
                constructComponentKey(
                    info.packageName,
                    info.name
                )
            }

        init {
            itemView.apply {
                if (isForTasker) {
                    binding.root.isClickable = true
                    binding.root.isFocusable = true

                    val outValue = TypedValue()
                    context.theme.resolveAttribute(
                        android.R.attr.selectableItemBackground,
                        outValue,
                        true
                    )
                    binding.root.setBackgroundResource(outValue.resourceId)
                    binding.root.setOnClickListener {
                        selectionCallback(currentList[bindingAdapterPosition])
                    }
                }
            }
        }

        fun bind(data: DataClass) {
            onBind(data)
        }

        open fun onBind(data: DataClass) {
            itemView.apply {
                binding.composeView.setContent {
                    var showingIntentOptions by remember {
                        mutableStateOf(false)
                    }

                    ComponentBar(
                        icon = rememberAsyncImagePainter(model = getCoilData(data)),
                        name = data.label.toString(),
                        component = data,
                        whichButtons = listOf(
                            Button.ComponentInfoButton(data.info) {
                                ComponentInfoDialog(
                                    context,
                                    data.info
                                ).show()
                            },
                            Button.IntentDialogButton(data.component.flattenToString()) {
                                showingIntentOptions = true
                            },
                            Button.CreateShortcutButton(data),
                            Button.LaunchButton(data)
                        )
                    )

                    if (showingIntentOptions) {
                        ExtrasDialog(
                            componentKey = data.component.flattenToString(),
                            onDismissRequest = { showingIntentOptions = false }
                        )
                    }
                }
            }
        }

        open fun onLaunch(data: DataClass, context: Context, extras: List<ExtraInfo>) {}

        private fun getCoilData(data: DataClass): Any? {
            val res = data.info.iconResource.run {
                if (this == 0) data.info.applicationInfo.iconRes.run {
                    if (this == 0) data.info.applicationInfo.roundIconRes
                    else this
                }
                else this
            }

            return if (res != 0) {
                Uri.parse("android.resource://${data.info.packageName}/$res")
            } else {
                data.info.applicationInfo.loadIcon(itemView.context.packageManager)
            }
        }
    }
}