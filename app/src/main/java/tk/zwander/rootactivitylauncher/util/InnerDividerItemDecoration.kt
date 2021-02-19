package tk.zwander.rootactivitylauncher.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class InnerDividerItemDecoration(context: Context, orientation: Int) : DividerItemDecoration(context, orientation) {
    private val mOrientation: Int
        get() = DividerItemDecoration::class.java
            .getDeclaredField("mOrientation")
            .apply { isAccessible = true }
            .getInt(this)

    private val mDivider: Drawable?
        get() = DividerItemDecoration::class.java
            .getDeclaredField("mDivider")
            .apply { isAccessible = true }
            .get(this) as Drawable?

    private val mBounds: Rect
        get() = DividerItemDecoration::class.java
            .getDeclaredField("mBounds")
            .apply { isAccessible = true }
            .get(this) as Rect

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.layoutManager == null || mDivider == null) {
            return
        }
        if (mOrientation == VERTICAL) {
            drawVertical(c, parent)
        } else {
            drawHorizontal(c, parent)
        }
    }

    private fun drawVertical(
        canvas: Canvas,
        parent: RecyclerView
    ) {
        canvas.save()
        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            parent.getDecoratedBoundsWithMargins(child, mBounds)
            val bottom = mBounds.bottom + child.translationY.roundToInt()
            val top = bottom - mDivider!!.intrinsicHeight
            val left = child.left + parent.context.dpToPx(8)
            val right = child.right - parent.context.dpToPx(8)

            mDivider!!.setBounds(left, top, right, bottom)
            mDivider!!.draw(canvas)
        }
        canvas.restore()
    }

    private fun drawHorizontal(
        canvas: Canvas,
        parent: RecyclerView
    ) {
        canvas.save()
        val childCount = parent.childCount
        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            parent.layoutManager!!.getDecoratedBoundsWithMargins(child, mBounds)
            val right = mBounds.right + child.translationX.roundToInt()
            val left = right - mDivider!!.intrinsicWidth
            val top = child.top + parent.context.dpToPx(8)
            val bottom = child.bottom - parent.context.dpToPx(8)

            mDivider!!.setBounds(left, top, right, bottom)
            mDivider!!.draw(canvas)
        }
        canvas.restore()
    }
}