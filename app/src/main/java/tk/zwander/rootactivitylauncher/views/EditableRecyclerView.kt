package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.util.AttributeSet

class EditableRecyclerView(context: Context, attrs: AttributeSet) : ShadowFadingEdgeRecyclerView(context, attrs) {
    override fun onCheckIsTextEditor(): Boolean {
        return true
    }
}