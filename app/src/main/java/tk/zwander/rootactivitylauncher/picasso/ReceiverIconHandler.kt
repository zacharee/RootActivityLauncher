package tk.zwander.rootactivitylauncher.picasso

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler

class ReceiverIconHandler(private val context: Context) : RequestHandler() {
    companion object {
        const val SCHEME = "receiver"

        fun createUri(packageName: String, receiverName: String): Uri {
            return Uri.parse("$SCHEME:$packageName/$receiverName")
        }
    }

    override fun canHandleRequest(data: Request): Boolean {
        return data.uri != null && data.uri.scheme == SCHEME
    }

    override fun load(request: Request, networkPolicy: Int): Result? {
        val component = ComponentName.unflattenFromString(request.uri.schemeSpecificPart)!!
        return Result(
                try {
                    context.packageManager.getActivityIcon(component).toBitmap()
                            .run { copy(config, false) }
                } catch (e: Exception) {
                    context.packageManager.getApplicationIcon(component.packageName).toBitmap()
                            .run { copy(config, false) }
                },
                Picasso.LoadedFrom.DISK
        )
    }
}