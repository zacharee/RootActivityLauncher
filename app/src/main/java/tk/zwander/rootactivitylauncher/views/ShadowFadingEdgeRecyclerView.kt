package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import tk.zwander.rootactivitylauncher.R

open class ShadowFadingEdgeRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    private var color: Int = ContextCompat.getColor(context, R.color.colorFadingEdge)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ShadowFadingEdgeRecyclerView)

        color = a.getColor(R.styleable.ShadowFadingEdgeRecyclerView_fadingEdgeColor, color)

        a.recycle()
    }

//    override fun getSolidColor(): Int {
//        return color
//    }

    fun setSolidColor(color: Int) {
        this.color = color
        invalidate()
    }
}