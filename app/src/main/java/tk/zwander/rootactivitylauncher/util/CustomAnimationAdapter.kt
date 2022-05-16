package tk.zwander.rootactivitylauncher.util

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import jp.wasabeef.recyclerview.adapters.AnimationAdapter

class CustomAnimationAdapter(wrapped: RecyclerView.Adapter<out ViewHolder>) : AnimationAdapter(wrapped) {
    init {
        setFirstOnly(false)
    }

    override fun getAnimators(view: View): Array<Animator> {
        return arrayOf(
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f),
            ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            adapter.onBindViewHolder(holder, position, payloads)
        }
    }
}