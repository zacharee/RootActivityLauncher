package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView


class RecycledImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {
    override fun onDraw(canvas: Canvas?) {
        if (drawable is BitmapDrawable) {
            if ((drawable as BitmapDrawable).bitmap.isRecycled) {
                return
            }
        }
        super.onDraw(canvas)
    }
}