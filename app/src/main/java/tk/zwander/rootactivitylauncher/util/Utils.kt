package tk.zwander.rootactivitylauncher.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.AtomicLong

val Int.hexString: String
    get() = Integer.toHexString(this)

fun Context.launchUrl(url: String) {
    try {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(browserIntent)
    } catch (_: Exception) {}
}

val Context.isTouchWiz: Boolean
    get() = packageManager.hasSystemFeature("com.samsung.feature.samsung_experience_mobile")

fun updateProgress(
    current: AtomicInt,
    lastUpdateTime: AtomicLong,
    total: Int,
    setter: (newProgress: Float) -> Unit
) {
    val oldCurrent = current.value
    val newCurrent = current.incrementAndGet()

    val oldProgress = (oldCurrent / total.toFloat() * 100f).toInt() / 100f
    val newProgress = (newCurrent / total.toFloat() * 100f).toInt() / 100f

    val oldUpdateTime = lastUpdateTime.value
    val newUpdateTime = System.currentTimeMillis()

    if (newProgress > oldProgress && newUpdateTime > oldUpdateTime) {
        lastUpdateTime.value = newUpdateTime

        setter(newProgress)
    }
}
