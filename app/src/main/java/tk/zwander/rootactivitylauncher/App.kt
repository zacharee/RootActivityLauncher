package tk.zwander.rootactivitylauncher

import android.app.Application
import android.os.Build
import com.squareup.picasso.Picasso
import org.lsposed.hiddenapibypass.HiddenApiBypass
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import tk.zwander.rootactivitylauncher.picasso.AppIconHandler
import tk.zwander.rootactivitylauncher.picasso.ReceiverIconHandler
import tk.zwander.rootactivitylauncher.picasso.ServiceIconHandler

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.setHiddenApiExemptions("L")
        }

        Picasso.setSingletonInstance(
            Picasso.Builder(this)
                .addRequestHandler(AppIconHandler(this))
                .addRequestHandler(ActivityIconHandler(this))
                .addRequestHandler(ServiceIconHandler(this))
                .addRequestHandler(ReceiverIconHandler(this))
                .build()
        )
    }
}