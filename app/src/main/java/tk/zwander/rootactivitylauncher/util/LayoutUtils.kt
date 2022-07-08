package tk.zwander.rootactivitylauncher.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import tk.zwander.rootactivitylauncher.R
import kotlin.math.floor

private fun Context.linearLayoutManager(): LinearLayoutManager {
    return LinearLayoutManager(this)
}

private fun gridLayoutManager(spans: Int): StaggeredGridLayoutManager {
    return StaggeredGridLayoutManager(spans, StaggeredGridLayoutManager.VERTICAL)
}

fun Context.getAppropriateLayoutManager(widthDp: Int, currentLayoutManager: LayoutManager? = null): LayoutManager {
    val spans = floor(widthDp / 400f).toInt()

    fun createLayoutManager(): LayoutManager {
        return when {
            widthDp >= 800 -> gridLayoutManager(spans)
            else -> linearLayoutManager()
        }
    }

    return when (currentLayoutManager) {
        is StaggeredGridLayoutManager -> {
            if (currentLayoutManager.spanCount == spans) {
                return currentLayoutManager
            } else {
                createLayoutManager()
            }
        }
        is LinearLayoutManager -> {
            return currentLayoutManager
        }
        else -> {
            createLayoutManager()
        }
    }
}

fun LayoutManager.findFirstVisibleItemPosition(): Int {
    if (this is LinearLayoutManager) {
        return findFirstVisibleItemPosition()
    } else if (this is StaggeredGridLayoutManager) {
        return findFirstVisibleItemPositions(null)[0]
    }

    return -1
}

fun LayoutManager.findLastVisibleItemPosition(): Int {
    if (this is LinearLayoutManager) {
        return findLastVisibleItemPosition()
    } else if (this is StaggeredGridLayoutManager) {
        return findLastVisibleItemPositions(null).last()
    }

    return -1
}

fun RecyclerView.setHeightParams(size: Int) {
    updateLayoutParams<ViewGroup.LayoutParams> {
        height = if (size <= 4) ViewGroup.LayoutParams.WRAP_CONTENT else
            context.resources.getDimensionPixelSize(R.dimen.item_recycler_max_height)
    }
}

var View.isVisibleAnimated: Boolean
    get() = isVisible
    set(value) {
        pivotY = 0f

        if (value) {
            alpha = 0f
            scaleY = 0f
            isVisible = value
            animate().alpha(1f).scaleY(1f).start()
        } else {
            alpha = 1f
            scaleY = 1f
            animate().alpha(0f).scaleY(0f).withEndAction {
                isVisible = value
            }.start()
        }
    }