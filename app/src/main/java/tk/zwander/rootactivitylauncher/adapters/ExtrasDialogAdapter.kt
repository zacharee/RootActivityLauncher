package tk.zwander.rootactivitylauncher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo
import tk.zwander.rootactivitylauncher.databinding.ExtraItemBinding

class ExtrasDialogAdapter : RecyclerView.Adapter<ExtrasDialogAdapter.BaseVH<out Any>>() {
    companion object {
        const val VIEWTYPE_ADD = 0
        const val VIEWTYPE_EXTRA = 1
    }

    private val items = SortedList(
        ExtraInfo::class.java,
        object : SortedList.Callback<ExtraInfo>() {
            override fun compare(o1: ExtraInfo, o2: ExtraInfo): Int {
                return when {
                    o1.key.isBlank() && o1.value.isBlank() -> 1
                    o2.key.isBlank() && o2.value.isBlank() -> -1
                    else -> 0
                }
            }

            override fun onInserted(position: Int, count: Int) {
                notifyItemRangeInserted(position + 1, count)
            }

            override fun onRemoved(position: Int, count: Int) {
                notifyItemRangeRemoved(position + 1, count)
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                notifyItemMoved(fromPosition + 1, toPosition + 1)
            }

            override fun onChanged(position: Int, count: Int) {
                notifyItemRangeChanged(position + 1, count)
            }

            override fun areContentsTheSame(oldItem: ExtraInfo, newItem: ExtraInfo): Boolean {
                return oldItem == newItem
            }

            override fun areItemsTheSame(item1: ExtraInfo, item2: ExtraInfo): Boolean {
                return item1 == item2
            }
        }
    )

    override fun getItemCount(): Int {
        return items.size() + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEWTYPE_ADD
        else VIEWTYPE_EXTRA
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseVH<out Any> {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEWTYPE_ADD -> AddVH(
                inflater.inflate(R.layout.extra_add_item, parent, false)
            )
            VIEWTYPE_EXTRA -> ExtraVH(
                inflater.inflate(R.layout.extra_item, parent, false)
            )
            else -> throw IllegalArgumentException("invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseVH<out Any>, position: Int) {
        when (holder) {
            is AddVH -> holder.bind(Unit)
            is ExtraVH -> holder.bind(items[position - 1])
        }
    }

    fun setItems(items: List<ExtraInfo>) {
        this.items.clear()
        this.items.addAll(items)
    }

    fun currentData(): List<ExtraInfo> {
        return ArrayList<ExtraInfo>().apply {
            for (i in 0 until items.size()) {
                add(items.get(i))
            }
        }
    }

    abstract inner class BaseVH<T: Any>(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(data: T)
    }

    inner class AddVH(view: View) : BaseVH<Unit>(view) {
        override fun bind(data: Unit) {
            itemView.setOnClickListener {
                items.add(ExtraInfo("", ""))
                notifyItemInserted(itemCount)
            }
        }
    }

    inner class ExtraVH(view: View) : BaseVH<ExtraInfo>(view) {
        private val binding = ExtraItemBinding.bind(itemView)

        override fun bind(data: ExtraInfo) {
            binding.apply {
                keyField.setText(data.key, TextView.BufferType.EDITABLE)
                valueField.setText(data.value, TextView.BufferType.EDITABLE)
                keyField.doOnTextChanged { text, _, _, _ ->
                    items[bindingAdapterPosition - 1].key = text.toString()
                }
                valueField.doOnTextChanged { text, _, _, _ ->
                    items[bindingAdapterPosition - 1].value = text.toString()
                }
                remove.setOnClickListener {
                    try {
                        items.removeItemAt(bindingAdapterPosition - 1)
                    } catch (_: ArrayIndexOutOfBoundsException) {}
                }
            }
        }
    }
}