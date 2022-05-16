package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import tk.zwander.rootactivitylauncher.databinding.ComponentGroupBinding
import tk.zwander.rootactivitylauncher.util.isVisibleAnimated
import tk.zwander.rootactivitylauncher.util.setHeightParams

class ComponentGroup(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private val binding = ComponentGroupBinding.inflate(LayoutInflater.from(context), this)

    var expanded: Boolean = false
        set(value) {
            field = value

            binding.arrow.animate().rotation(if (value) 180f else 0f).start()
            binding.recycler.isVisibleAnimated = value
        }

    var title: CharSequence
        get() = binding.title.text
        set(value) {
            binding.title.text = value
        }

    var layoutManager: LayoutManager?
        get() = binding.recycler.layoutManager
        set(value) {
            binding.recycler.layoutManager = value
        }

    var adapter: RecyclerView.Adapter<*>?
        get() = binding.recycler.adapter
        set(value) {
            binding.recycler.adapter = value
        }

    init {
        orientation = VERTICAL
    }

    fun updateHeight(size: Int) {
        binding.recycler.setHeightParams(size)
    }

    fun setOnTitleClickListener(listener: OnClickListener) {
        binding.clicker.setOnClickListener(listener)
    }

    fun addItemDecoration(decoration: ItemDecoration) {
        binding.recycler.addItemDecoration(decoration)
    }
}