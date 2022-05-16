package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import tk.zwander.rootactivitylauncher.R

class ShadowFadingEdgeRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    override fun getSolidColor(): Int {
        return ContextCompat.getColor(context, R.color.colorFadingEdge)
    }
}