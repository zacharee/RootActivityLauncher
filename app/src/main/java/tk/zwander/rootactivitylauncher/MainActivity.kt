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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import tk.zwander.rootactivitylauncher.data.component.*
import tk.zwander.rootactivitylauncher.data.model.AppModel
import tk.zwander.rootactivitylauncher.data.model.BaseInfoModel
import tk.zwander.rootactivitylauncher.data.model.FavoriteModel
import tk.zwander.rootactivitylauncher.data.model.MainModel
import tk.zwander.rootactivitylauncher.data.prefs
import tk.zwander.rootactivitylauncher.util.*
import tk.zwander.rootactivitylauncher.views.MainView
import tk.zwander.rootactivitylauncher.views.theme.Theme
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@SuppressLint("RestrictedApi")
open class MainActivity : ComponentActivity(), CoroutineScope by MainScope(), PermissionResultListener {
    protected open val isForTasker = false
    protected open var selectedItem: Pair<ComponentType, ComponentName>? = null

    protected val model = MainModel()

    private val packageUpdateReceiver = object : BroadcastReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        fun register() {
            registerReceiver(this, filter)
        }

        fun unregister() {
            unregisterReceiver(this)
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            launch(Dispatchers.IO) {
                val pkg = intent?.data?.schemeSpecificPart

                when (intent?.action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        if (pkg != null) {
                            try {
                                val loaded = loadApp(getPackageInfo(pkg), packageManager)

                                model.apps.value = model.apps.value + loaded
                            } catch (e: PackageManager.NameNotFoundException) {
                                Log.e("RootActivityLauncher", "Error parsing package info for newly-added app.", e)
                            }
                        }
                    }

                    Intent.ACTION_PACKAGE_REMOVED -> {
                        if (pkg != null) {
                            val new = model.apps.value.toMutableList().apply {
                                removeAll { it is AppModel && it.info.packageName == pkg }
                            }

                            model.apps.value = new
                        }
                    }

                    Intent.ACTION_PACKAGE_CHANGED, Intent.ACTION_PACKAGE_REPLACED -> {
                        if (pkg != null) {
                            val old = ArrayList(model.apps.value)
                            val oldIndex = old.indexOfFirst { it is AppModel && it.info.packageName == pkg }
                                .takeIf { it != -1 } ?: return@launch

                            old[oldIndex] = loadApp(getPackageInfo(pkg), packageManager)

                            model.apps.value = old
                        }
                    }
                }
            }
        }
    }

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
                requestShizukuPermission(::onPermissionResult)
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
                    isForTasker = isForTasker,
                    onRefresh = {
                        currentDataJob?.cancel()
                        currentDataJob = loadDataAsync()
                    },
                    mainModel = model
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
        return async(Dispatchers.IO) {
            if (!silent) {
                model.progress.value = 0f
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
            val loaded = ConcurrentLinkedQueue<BaseInfoModel>()

            loaded.add(
                FavoriteModel(
                    activityKeys = prefs.favoriteActivities,
                    serviceKeys = prefs.favoriteServices,
                    receiverKeys = prefs.favoriteReceivers,
                    context = this@MainActivity,
                    scope = this@MainActivity,
                    mainModel = model
                )
            )

            val progressIndex = atomic(0)
            val lastUpdate = atomic(0L)

            apps.forEachParallel(context = Dispatchers.IO, scope = this) { app ->
                loadApp(app, packageManager).let {
                    loaded.add(it)
                }

                if (!silent) {
                    updateProgress(progressIndex, lastUpdate, max) {
                        model.progress.value = it
                    }
                }
            }

            model.apps.value = loaded.toList()
            model.progress.value = null
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

    private suspend fun loadApp(app: PackageInfo, pm: PackageManager): AppModel = coroutineScope {
        val appLabel = app.applicationInfo.loadLabel(pm)

        return@coroutineScope AppModel(
            pInfo = app,
            label = appLabel,
            context = this@MainActivity,
            scope = this@MainActivity,
            mainModel = model,
        )
    }
}