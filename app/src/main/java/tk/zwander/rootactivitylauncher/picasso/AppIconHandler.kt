package tk.zwander.rootactivitylauncher.picasso

import android.content.Context
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler

class AppIconHandler(private val context: Context) : RequestHandler() {
    companion object {
        const val SCHEME = "package"

        fun createUri(packageName: String): Uri {
            return Uri.parse("$SCHEME:$packageName")
        }
    }

    override fun canHandleRequest(data: Request): Boolean {
        return data.uri != null && data.uri.scheme == SCHEME
    }

    override fun load(request: Request, networkPolicy: Int): Result {
        return Result(
            context.packageManager.getApplicationIcon(request.uri.schemeSpecificPart)
                .toBitmap().run { copy(config, false) },
            Picasso.LoadedFrom.DISK
        )
    }
}