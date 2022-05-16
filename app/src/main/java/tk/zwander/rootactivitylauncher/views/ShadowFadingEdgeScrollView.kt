package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import tk.zwander.rootactivitylauncher.R

class ShadowFadingEdgeScrollView(context: Context, attrs: AttributeSet) : ScrollView(context, attrs) {
    private var color: Int = ContextCompat.getColor(context, R.color.colorFadingEdge)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ShadowFadingEdgeScrollView)

        color = a.getColor(R.styleable.ShadowFadingEdgeScrollView_fadingEdgeColor, color)

        a.recycle()
    }

    override fun getSolidColor(): Int {
        return color
    }

    fun setSolidColor(color: Int) {
        this.color = color
        invalidate()
    }
}