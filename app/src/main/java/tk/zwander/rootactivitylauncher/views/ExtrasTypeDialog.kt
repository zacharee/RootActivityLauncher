package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.ExtraType
import tk.zwander.rootactivitylauncher.databinding.ExtraTypeDialogBinding
import tk.zwander.rootactivitylauncher.databinding.ExtraTypeItemBinding
import tk.zwander.rootactivitylauncher.util.MarginItemDecoration
import tk.zwander.rootactivitylauncher.util.dpToPx

class ExtrasTypeDialog(
    private val context: Context,
    private val initial: ExtraType,
    private val selectionListener: (ExtraType) -> Unit
) {
    private val view = ExtraTypeDialogBinding.inflate(LayoutInflater.from(context))

    private val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(R.string.type)
        .setView(view.root)
        .setPositiveButton(android.R.string.cancel, null)
        .create()

    private val sortedTypes = ExtraType.values().sortedBy {
        context.resources.getString(it.nameRes)
    }

    init {
        val typeRecycler = view.extraTypeList
        val adapter = Adapter()

        adapter.selectedItem = initial

        typeRecycler.adapter = adapter
        typeRecycler.addItemDecoration(MarginItemDecoration(context.dpToPx(8)))
        typeRecycler.scrollToPosition(sortedTypes.indexOf(initial))
    }

    fun show() {
        dialog.show()
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {
        var selectedItem: ExtraType = ExtraType.STRING
            set(value) {
                val previous = field
                field = value
                notifyItemChanged(sortedTypes.indexOf(value))
                notifyItemChanged(sortedTypes.indexOf(previous))
            }

        override fun getItemCount(): Int {
            return sortedTypes.size
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.onBind(sortedTypes[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.extra_type_item, parent, false))
        }

        private inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            fun onBind(info: ExtraType) {
                val binding = ExtraTypeItemBinding.bind(itemView)

                fun onClick() {
                    selectedItem = info
                    selectionListener(info)
                    dialog.dismiss()
                }

                binding.selectionWrapper.setOnClickListener {
                    onClick()
                }
                binding.selectionWrapper.isChecked = selectedItem == info
                binding.selectionText.apply {
                    setText(info.nameRes)
                }
            }
        }
    }
}