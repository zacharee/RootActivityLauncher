package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.databinding.ComponentGroupBinding
import tk.zwander.rootactivitylauncher.util.*
import tk.zwander.rootactivitylauncher.views.components.ComponentItem

sealed class ComponentGroup(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private val binding by lazy { ComponentGroupBinding.inflate(LayoutInflater.from(context), this) }

    protected abstract val formatRes: Int

    var count: Int = 0
        set(value) {
            field = value

            title = context.resources.getString(formatRes, count)
        }

    var expanded: Boolean = false
        set(value) {
            if (field != value) {
                binding.arrow.animate().rotation(if (value) 180f else 0f).start()
                binding.list.isVisibleAnimated = value
            }

            field = value
        }

    var title: CharSequence
        get() = binding.title.text
        set(value) {
            binding.title.text = value
        }

    var items: List<BaseComponentInfo>? = null

    var forTasker by mutableStateOf(false)

//    var layoutManager: LayoutManager?
//        get() = binding.recycler.layoutManager
//        set(value) {
//            binding.recycler.layoutManager = value
//        }
//
//    var adapter: RecyclerView.Adapter<*>?
//        get() = binding.recycler.adapter
//        set(value) {
//            binding.recycler.adapter = value
//        }

    init {
        orientation = VERTICAL
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

//        binding.recycler.itemAnimator = null

        binding.list.setContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                contentPadding = PaddingValues(start = 32.dp)
            ) {
                items?.let { items ->
                    items(items = items, key = { it.component }) {
                        ComponentItem(
                            forTasker = forTasker,
                            component = it,
                            onClick = {

                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

//    fun updateHeight(size: Int) {
//        binding.recycler.setHeightParams(size)
//    }
//
//    fun setOnTitleClickListener(listener: OnClickListener) {
//        binding.clicker.setOnClickListener(listener)
//    }
//
//    fun addItemDecoration(decoration: ItemDecoration) {
//        binding.recycler.addItemDecoration(decoration)
//    }

//    fun updateLayoutManager(width: Int) {
//        layoutManager = context.getAppropriateLayoutManager(context.pxAsDp(width).toInt(), layoutManager)
//    }
}

class ActivityGroup(context: Context, attrs: AttributeSet) : ComponentGroup(context, attrs) {
    override val formatRes = R.string.activities
}

class ServicesGroup(context: Context, attrs: AttributeSet) : ComponentGroup(context, attrs) {
    override val formatRes = R.string.services
}

class ReceiversGroup(context: Context, attrs: AttributeSet) : ComponentGroup(context, attrs) {
    override val formatRes = R.string.receivers
}