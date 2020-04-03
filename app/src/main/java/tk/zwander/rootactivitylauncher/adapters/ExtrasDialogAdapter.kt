package tk.zwander.rootactivitylauncher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.extra_item.view.*
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraInfo

class ExtrasDialogAdapter : RecyclerView.Adapter<ExtrasDialogAdapter.BaseVH<out Any>>() {
    companion object {
        const val VIEWTYPE_ADD = 0
        const val VIEWTYPE_EXTRA = 1
    }

    private val items = ArrayList<ExtraInfo>()

    override fun getItemCount(): Int {
        return items.size + 1
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

        notifyDataSetChanged()
    }

    fun currentData(): List<ExtraInfo> {
        return ArrayList(items)
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
        override fun bind(data: ExtraInfo) {
            itemView.apply {
                key_field.setText(data.key, TextView.BufferType.EDITABLE)
                value_field.setText(data.value, TextView.BufferType.EDITABLE)
                key_field.doOnTextChanged { text, _, _, _ ->
                    items[adapterPosition - 1].key = text.toString()
                }
                value_field.doOnTextChanged { text, _, _, _ ->
                    items[adapterPosition - 1].value = text.toString()
                }
                remove.setOnClickListener {
                    try {
                        items.removeAt(adapterPosition - 1)
                        notifyItemRemoved(adapterPosition)
                    } catch (e: ArrayIndexOutOfBoundsException) {}
                }
            }
        }
    }
}