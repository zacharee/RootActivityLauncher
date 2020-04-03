package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class EditableRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    override fun onCheckIsTextEditor(): Boolean {
        return true
    }
}