package tk.zwander.rootactivitylauncher

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.MainModel
import tk.zwander.rootactivitylauncher.data.component.*
import tk.zwander.rootactivitylauncher.util.*
import tk.zwander.rootactivitylauncher.views.components.MainView
import tk.zwander.rootactivitylauncher.views.components.Theme
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@SuppressLint("RestrictedApi")
open class MainActivity : ComponentActivity(), CoroutineScope by MainScope(), PermissionResultListener {
    protected open val isForTasker = false
    protected open var selectedItem: Pair<ComponentType, ComponentName>? = null

    private val packageUpdateReceiver = object : BroadcastReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
        }

        fun register() {
            registerReceiver(this, filter)
        }

        fun unregister() {
            unregisterReceiver(this)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            launch(Dispatchers.IO) {
                val host = intent?.data?.host

                when (intent?.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        if (host != null) {
                            val loaded = loadApp(getPackageInfo(host), packageManager)

                            launch(Dispatchers.Main) {
                                MainModel.apps.value = MainModel.apps.value + loaded
                            }
                        }
                    }

                    Intent.ACTION_PACKAGE_REMOVED -> {
                        if (host != null) {
                            launch(Dispatchers.Main) {
                                MainModel.apps.value = MainModel.apps.value.toMutableList().apply {
                                    removeAll { it.info.packageName == host }
                                }
                            }
                        }
                    }

                    Intent.ACTION_PACKAGE_REPLACED -> {
                        if (host != null) {
                            launch(Dispatchers.Main) {
                                val old = ArrayList(MainModel.apps.value)

                                old[old.indexOfFirst { it.info.packageName == host }] =
                                    loadApp(getPackageInfo(host), packageManager)

                                MainModel.apps.value = old
                            }
                        }
                    }
                }
            }
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission(), ::onPermissionResult)

    private var currentDataJob: Deferred<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Maybe if we ever get a KNOX license key...
//        val cInfoClass = Class.forName("com.samsung.android.knox.ContextInfo")
//        val cInfo = cInfoClass.getDeclaredConstructor(Int::class.java)
//                .newInstance(myUid())
//
//        val lmClass = Class.forName("com.samsung.android.knox.license.IEnterpriseLicense\$Stub")
//        val lm = lmClass.getMethod("asInterface", IBinder::class.java)
//                .invoke(null, SystemServiceHelper.getSystemService("enterprise_license_policy"))
//
//        lmClass.getMethod("activateLicense", cInfoClass, String::class.java, String::class.java, String::class.java)
//                .invoke(lm, cInfo, "<KPE_LICENSE>", null, null)

        packageUpdateReceiver.register()

        if (Shizuku.pingBinder()) {
            if (!hasShizukuPermission) {
                requestShizukuPermission(::onPermissionResult, permissionLauncher)
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            Theme {
                val darkTheme = isSystemInDarkTheme()
                val variant = MaterialTheme.colorScheme.surfaceColorAtElevation(3.0.dp)

                LaunchedEffect(variant) {
                    window.navigationBarColor = variant.toArgb()
                }

                LaunchedEffect(darkTheme) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        window.decorView.apply {
                            @Suppress("DEPRECATION")
                            systemUiVisibility = if (darkTheme) {
                                systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                            } else {
                                systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            }
                        }
                    }
                }

                MainView(
                    modifier = Modifier.fillMaxSize(),
                    onItemSelected = {
                        selectedItem = it.type() to it.component
                    },
                    isForTasker = isForTasker
                )
            }
        }

        currentDataJob = loadDataAsync()
    }

    override fun onPermissionResult(granted: Boolean) {
        // Currently no-op
    }

    override fun onDestroy() {
        super.onDestroy()

        packageUpdateReceiver.unregister()
        cancel()
    }

    private fun loadDataAsync(silent: Boolean = false): Deferred<*> {
        return async(Dispatchers.Main) {
            if (!silent) {
                MainModel.progress.emit(0f)
            }

            //This mess is because of a bug in Marshmallow and possibly earlier that
            //causes getInstalledPackages() to fail because one of the PackageInfo objects
            //is too large. It'll throw a DeadObjectException internally and then just return
            //as much as it already transferred before the error. The bug seems to be fixed in
            //Nougat and later.
            //If the device is on Marshmallow, retrieve a list of packages without any components
            //attached. Attempt to retrieve all components at once per-app. If that fails, retrieve
            //each set of components individually and combine them.
            //https://twitter.com/Wander1236/status/1412928863798190083?s=20
            val apps = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                packageManager.getInstalledPackagesCompat(
                    PackageManager.GET_ACTIVITIES or
                            PackageManager.GET_SERVICES or
                            PackageManager.GET_RECEIVERS or
                            PackageManager.GET_PERMISSIONS or
                            PackageManager.GET_CONFIGURATIONS or
                            PackageManager.MATCH_DISABLED_COMPONENTS
                )
            } else {
                @Suppress("DEPRECATION")
                val packages =
                    packageManager.getInstalledPackagesCompat(PackageManager.GET_DISABLED_COMPONENTS)
                packages.map { info ->
                    try {
                        //Try to get all the components for this package at once.
                        packageManager.getPackageInfoCompat(
                            info.packageName,
                            PackageManager.GET_ACTIVITIES or
                                    PackageManager.GET_SERVICES or
                                    PackageManager.GET_RECEIVERS or
                                    PackageManager.GET_PERMISSIONS or
                                    PackageManager.GET_CONFIGURATIONS
                        )
                    } catch (e: Exception) {
                        //The resulting PackageInfo was too large. Retrieve each set
                        //separately and combine them.
                        Log.e(
                            "RootActivityLauncher",
                            "Unable to get full info, splitting ${info.packageName}",
                            e
                        )
                        val awaits = listOf(
                            PackageManager.GET_ACTIVITIES,
                            PackageManager.GET_SERVICES,
                            PackageManager.GET_RECEIVERS,
                            PackageManager.GET_PERMISSIONS,
                            PackageManager.GET_CONFIGURATIONS
                        ).map { flag ->
                            async {
                                try {
                                    packageManager.getPackageInfoCompat(info.packageName, flag)
                                } catch (e: Exception) {
                                    Log.e(
                                        "RootActivityLauncher",
                                        "Unable to get split info for ${info.packageName} for flag $flag",
                                        e
                                    )
                                    null
                                }
                            }
                        }.awaitAll()

                        info.apply {
                            awaits.forEach { element ->
                                element?.activities?.let { this.activities = it }
                                element?.services?.let { this.services = it }
                                element?.receivers?.let { this.receivers = it }
                                element?.permissions?.let { this.permissions = it }
                                element?.configPreferences?.let { this.configPreferences = it }
                                element?.reqFeatures?.let { this.reqFeatures = it }
                                element?.featureGroups?.let { this.featureGroups = it }
                            }
                        }
                    }
                }
            }

            val max = apps.size - 1
            val loaded = ConcurrentLinkedQueue<AppInfo>()

            val progressIndex = AtomicInteger(0)
            val lastUpdate = AtomicLong(0L)
            val previousProgress = AtomicInteger(0f.toBits())

            apps.forEachParallel { app ->
                loadApp(app, packageManager).let {
                    loaded.add(it)
                }

                if (!silent) {
                    val p = progressIndex.incrementAndGet().toFloat() / max.toFloat()
                    val time = System.currentTimeMillis()

                    if (time - lastUpdate.get() >= 10) {
                        lastUpdate.set(time)
                        if (Float.fromBits(previousProgress.get()) < p) {
                            previousProgress.set(p.toBits())

                            MainModel.progress.emit(p)
                        }
                    }
                }
            }

            MainModel.apps.emit(loaded.toList())
            MainModel.progress.emit(null)
        }
    }

    private fun getPackageInfo(packageName: String): PackageInfo {
        @Suppress("DEPRECATION")
        return packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_CONFIGURATIONS or
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) PackageManager.MATCH_DISABLED_COMPONENTS
                    else PackageManager.GET_DISABLED_COMPONENTS
        )
    }

    private suspend fun loadApp(app: PackageInfo, pm: PackageManager): AppInfo = coroutineScope {
        val activities = app.activities
        val services = app.services
        val receivers = app.receivers

        val appLabel = app.applicationInfo.loadLabel(pm)

        return@coroutineScope AppInfo(
            pInfo = app,
            label = appLabel,
            activitiesLoader = { progress ->
                activities.loadItems(
                    pm = pm,
                    appLabel = appLabel,
                    progress = progress,
                    constructor = { input, label -> ActivityInfo(input, label) }
                )
            },
            servicesLoader = { progress ->
                services.loadItems(
                    pm = pm,
                    appLabel = appLabel,
                    progress = progress,
                    constructor = { input, label -> ServiceInfo(input, label) }
                )
            },
            receiversLoader = { progress ->
                receivers.loadItems(
                    pm = pm,
                    appLabel = appLabel,
                    progress = progress,
                    constructor = { input, label -> ReceiverInfo(input, label) }
                )
            },
            initialActivitiesSize = activities?.size ?: 0,
            initialServicesSize = services?.size ?: 0,
            initialReceiversSize = receivers?.size ?: 0,
            isForTasker = isForTasker,
            context = this@MainActivity
        )
    }
}