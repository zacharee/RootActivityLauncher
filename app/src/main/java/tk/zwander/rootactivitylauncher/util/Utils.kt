package tk.zwander.rootactivitylauncher.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import tk.zwander.rootactivitylauncher.R

val Int.hexString: String
    get() = Integer.toHexString(this)

fun Context.launchUrl(url: String) {
    try {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    } catch (e: Exception) {
        Toast.makeText(this, resources.getString(R.string.unable_to_launch, e.localizedMessage), Toast.LENGTH_SHORT).show()
    }
}

val Context.isTouchWiz: Boolean
    get() = packageManager.hasSystemFeature("com.samsung.feature.samsung_experience_mobile")
