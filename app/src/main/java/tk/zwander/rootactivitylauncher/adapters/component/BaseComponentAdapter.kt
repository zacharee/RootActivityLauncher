package tk.zwander.rootactivitylauncher.adapters.component

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.UserHandle
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
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.component.ComponentType
import tk.zwander.rootactivitylauncher.util.*
import tk.zwander.rootactivitylauncher.views.ComponentInfoDialog
import tk.zwander.rootactivitylauncher.views.ExtrasDialog
import android.util.TypedValue
import tk.zwander.rootactivitylauncher.databinding.ComponentItemBinding


abstract class BaseComponentAdapter<
        Self : BaseComponentAdapter<Self, DataClass, VHClass>,
        DataClass : BaseComponentInfo,
        VHClass : BaseComponentAdapter<Self, DataClass, VHClass>.BaseComponentVH>(
    dataClass: Class<DataClass>,
    private val isForTasker: Boolean,
    private val selectionCallback: (BaseComponentInfo) -> Unit
) : RecyclerView.Adapter<VHClass>(), CoroutineScope by MainScope() {
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
        protected val binding = ComponentItemBinding.bind(itemView)

        internal abstract val componentType: ComponentType

        internal val currentExtras: List<ExtraInfo>
            get() = itemView.context.findExtrasForComponent(currentComponentKey)
        internal val currentGlobalExtras: List<ExtraInfo>
            get() = itemView.context.findExtrasForComponent(currentPackageName)
        internal val currentPackageName: String
            get() = currentList[adapterPosition].info.packageName
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

                    if (hasRoot || (Shizuku.pingBinder() && itemView.context.hasShizukuPermission)) {
                        val result = withContext(Dispatchers.IO) {
                            if (hasRoot) {
                                try {
                                    Shell.Pool.SU.run("pm ${if (isChecked) "enable" else "disable"} $currentComponentKey") == 0
                                } catch (e: Exception) {
                                    false
                                }
                            } else {
                                val ipm = IPackageManager.Stub.asInterface(
                                    ShizukuBinderWrapper(
                                        SystemServiceHelper.getSystemService("package")
                                    )
                                )

                                try {
                                    ipm.setComponentEnabledSetting(
                                        ComponentName.unflattenFromString(currentComponentKey),
                                        if (isChecked) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                        0,
                                        UserHandle.USER_SYSTEM
                                    )

                                    true
                                } catch (e: Exception) {
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            itemView.context,
                                            R.string.requires_root,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    false
                                }
                            }
                        }

                        if (result) {
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
                binding.actionWrapper.isVisible = !isForTasker
                binding.enabled.isVisible = !isForTasker

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
                        selectionCallback(currentList[adapterPosition])
                    }
                }

                binding.setExtras.setOnClickListener {
                    ExtrasDialog(context, currentComponentKey)
                        .show()
                }
                binding.launch.setOnClickListener {
                    onLaunch(
                        currentList[adapterPosition],
                        context,
                        currentGlobalExtras + currentExtras
                    )
                }
                binding.shortcut.setOnClickListener {
                    val d = currentList[adapterPosition]
                    context.createShortcut(
                        d.label.run { if (!isNullOrBlank()) this else d.info.applicationInfo.loadLabel(context.packageManager) },
                        IconCompat.createWithBitmap(
                            (binding.icon.drawable ?: ContextCompat.getDrawable(
                                context,
                                R.mipmap.ic_launcher
                            ))!!.toBitmap()
                        ),
                        currentComponentKey,
                        componentType
                    )
                }
                binding.info.setOnClickListener {
                    val d = currentList[adapterPosition]
                    ComponentInfoDialog(context, d.info)
                        .show()
                }
            }
        }

        fun bind(data: DataClass) {
            onBind(data)
        }

        open fun onBind(data: DataClass) {
            itemView.apply {
                binding.name.text = data.label
                binding.cmp.text = data.info.name

                val info = data.info
                val requiresPermission = (info is ActivityInfo && info.permission != null) || (info is ServiceInfo && info.permission != null)

                binding.launchStatusIndicator.setColorFilter(
                    ContextCompat.getColor(
                        context,
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
                        .into(binding.icon)
                } ?: binding.icon.setImageDrawable(null)

                binding.enabled.setOnCheckedChangeListener(null)
                if (binding.enabled.isChecked != data.info.isActuallyEnabled(context)) binding.enabled.isChecked = data.info.isActuallyEnabled(context)
                updateLaunchVisibility(data)
                binding.enabled.setOnCheckedChangeListener(componentEnabledListener)
            }
        }

        open fun onLaunch(data: DataClass, context: Context, extras: List<ExtraInfo>) {}

        abstract fun getPicassoUri(data: DataClass): Uri?

        private fun updateLaunchVisibility(data: DataClass) {
            itemView.apply {
                if (binding.launch.isVisible != data.info.isActuallyEnabled(context)) binding.launch.isVisible = data.info.isActuallyEnabled(context)
            }
        }
    }
}