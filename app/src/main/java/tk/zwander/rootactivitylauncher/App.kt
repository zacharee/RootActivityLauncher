package tk.zwander.rootactivitylauncher

import android.app.Application
import com.squareup.picasso.Picasso
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import tk.zwander.rootactivitylauncher.picasso.AppIconHandler
import tk.zwander.rootactivitylauncher.picasso.ServiceIconHandler

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Picasso.setSingletonInstance(
            Picasso.Builder(this)
                .addRequestHandler(AppIconHandler(this))
                .addRequestHandler(ActivityIconHandler(this))
                .addRequestHandler(ServiceIconHandler(this))
                .build()
        )
    }
}