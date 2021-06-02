package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.skydoves.balloon.createBalloon
import tk.zwander.rootactivitylauncher.R

class TooltippedClickableImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {
    init {
        isClickable = true
        isFocusable = true
        setOnLongClickListener {
            if (contentDescription.isNotBlank()) {
                createBalloon(context) {
                    isVisibleArrow = false
                    text = contentDescription.toString()
                    setPadding(8)
                    setCornerRadius(2f)
                    autoDismissDuration = 2500L
                    setBackgroundColorResource(R.color.colorPrimary)
                }.apply {
                    showAlignTop(this@TooltippedClickableImageView)
                    setOnBalloonOutsideTouchListener { _, _ ->
                        dismiss()
                    }
                }
                true
            } else {
                false
            }
        }
    }
}