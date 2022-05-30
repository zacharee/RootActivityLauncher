package tk.zwander.rootactivitylauncher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.databinding.AdvancedSearchDialogItemBinding

class AdvancedSearchAdapter : RecyclerView.Adapter<AdvancedSearchAdapter.AdvancedSearchItemVH>() {
    private val items = arrayListOf(
        R.string.usage_advanced_search_has_permission to R.string.usage_advanced_search_has_permission_desc,
        R.string.usage_advanced_search_declares_permission to R.string.usage_advanced_search_declares_permission_desc,
        R.string.usage_advanced_search_requires_permission to R.string.usage_advanced_search_requires_permission_desc,
        R.string.usage_advanced_search_requires_feature to R.string.usage_advanced_search_requires_feature_desc
    )

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdvancedSearchItemVH {
        return AdvancedSearchItemVH(
            LayoutInflater.from(parent.context).inflate(R.layout.advanced_search_dialog_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AdvancedSearchItemVH, position: Int) {
        holder.bind(items[position])
    }

    class AdvancedSearchItemVH(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = AdvancedSearchDialogItemBinding.bind(itemView)

        fun bind(data: Pair<Int, Int>) {
            binding.advancedSearchTitle.setText(data.first)
            binding.advancedSearchDesc.setText(data.second)
        }
    }
}