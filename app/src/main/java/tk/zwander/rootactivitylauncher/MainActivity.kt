package tk.zwander.rootactivitylauncher

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.documentfile.provider.DocumentFile
import com.hmomeni.progresscircula.ProgressCircula
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.MainModel
import tk.zwander.rootactivitylauncher.data.component.*
import tk.zwander.rootactivitylauncher.util.*
import tk.zwander.rootactivitylauncher.views.FilterDialog
import tk.zwander.rootactivitylauncher.views.components.AppItem
import tk.zwander.rootactivitylauncher.views.components.SearchComponent
import tk.zwander.rootactivitylauncher.views.components.SelectableCard
import tk.zwander.rootactivitylauncher.views.components.Theme
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

@SuppressLint("RestrictedApi")
open class MainActivity : ComponentActivity(), CoroutineScope by MainScope(), PermissionResultListener {
    protected open val isForTasker = false
    protected open var selectedItem: Pair<ComponentType, ComponentName>? = null

    @Volatile
    private var extractInfo: AppInfo? = null

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
                                MainModel.apps.add(loaded)
                            }
                        }
                    }

                    Intent.ACTION_PACKAGE_REMOVED -> {
                        if (host != null) {
                            launch(Dispatchers.Main) {
                                MainModel.apps.removeAll { it.info.packageName == host }
                            }
                        }
                    }

                    Intent.ACTION_PACKAGE_REPLACED -> {
                        if (host != null) {
                            launch(Dispatchers.Main) {
                                MainModel.apps[MainModel.apps.indexOfFirst { it.info.packageName == host }] =
                                    loadApp(getPackageInfo(host), packageManager)
                            }
                        }
                    }
                }
            }
        }
    }

    private val extractLauncher = registerForActivityResult(
        object : ActivityResultContracts.OpenDocumentTree() {
            override fun createIntent(context: Context, input: Uri?): Intent {
                return super.createIntent(context, input).also {
                    it.putExtra(Intent.EXTRA_TITLE, getString(R.string.choose_extract_folder_msg))
                }
            }
        }
    ) { result ->
        if (extractInfo != null) {
            val dirUri = result ?: return@registerForActivityResult
            val dir = DocumentFile.fromTreeUri(this, dirUri) ?: return@registerForActivityResult

            val extractInfo = extractInfo!!
            this.extractInfo = null

            val baseDir = File(extractInfo.info.sourceDir)

            val splits = extractInfo.info.splitSourceDirs?.mapIndexed { index, s ->
                val splitApk = File(s)
                val splitName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    extractInfo.info.splitNames[index]
                } else splitApk.nameWithoutExtension

                splitName to s
            }

            val baseFile = dir.createFile(
                "application/vnd.android.package-archive",
                extractInfo.info.packageName
            ) ?: return@registerForActivityResult
            contentResolver.openOutputStream(baseFile.uri).use { writer ->
                Log.e("RootActivityLauncher", "$baseDir")
                try {
                    baseDir.inputStream().use { reader ->
                        reader.copyTo(writer!!)
                    }
                } catch (e: Exception) {
                    Log.e("RootActivityLauncher", "Extraction failed", e)
                    Toast.makeText(
                        this,
                        resources.getString(R.string.extraction_failed, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            splits?.forEach { split ->
                val name = split.first
                val path = File(split.second)

                val file = dir.createFile(
                    "application/vnd.android.package-archive",
                    "${extractInfo.info.packageName}_$name"
                )
                    ?: return@registerForActivityResult
                contentResolver.openOutputStream(file.uri).use { writer ->
                    try {
                        path.inputStream().use { reader ->
                            reader.copyTo(writer!!)
                        }
                    } catch (e: Exception) {
                        Log.e("RootActivityLauncher", "Extraction failed", e)
                        Toast.makeText(
                            this,
                            resources.getString(R.string.extraction_failed, e.message),
                            Toast.LENGTH_SHORT
                        ).show()
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

        setContent {
            val scope = rememberCoroutineScope()
            val appListState = remember {
                LazyListState()
            }

            LaunchedEffect(
                MainModel.isSearching,
                MainModel.useRegex,
                MainModel.includeComponents
            ) {
                if (MainModel.isSearching) {
                    MainModel.update()
                }
            }

            LaunchedEffect(
                MainModel.apps.toList(),
                MainModel.hasFilters,
                MainModel.query
            ) {
                MainModel.update()
            }

            Theme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .systemBarsPadding()
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                state = appListState
                            ) {
                                items(items = MainModel.filteredApps, key = { it.info.packageName }) {
                                    AppItem(
                                        info = it,
                                        isForTasker = isForTasker,
                                        selectionCallback = {
                                            selectedItem = it.type() to it.component
                                        },
                                        progressCallback = {
                                            scope.launch {
                                                MainModel.progress = it
                                            }
                                        },
                                        extractCallback = {
                                            extractInfo = it
                                            extractLauncher.launch(null)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .heightIn(min = 52.dp),
                            ) {
                                AnimatedVisibility(
                                    visible = MainModel.isSearching,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SelectableCard(
                                            modifier = Modifier.weight(1f),
                                            selected = MainModel.useRegex,
                                            onClick = { MainModel.useRegex = !MainModel.useRegex }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = stringResource(id = R.string.regex))
                                            }
                                        }

                                        SelectableCard(
                                            modifier = Modifier.weight(1f),
                                            selected = MainModel.includeComponents,
                                            onClick = { MainModel.includeComponents = !MainModel.includeComponents }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = stringResource(id = R.string.include_components))
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SearchComponent(
                                        expanded = MainModel.isSearching,
                                        query = MainModel.query,
                                        onExpandChange = { MainModel.isSearching = it },
                                        onQueryChange = { MainModel.query = it },
                                        modifier = Modifier.weight(1f)
                                    )

                                    AnimatedVisibility(visible = MainModel.progress == null) {
                                        IconButton(
                                            onClick = {
                                                FilterDialog(
                                                    this@MainActivity,
                                                    MainModel.enabledFilterMode,
                                                    MainModel.exportedFilterMode,
                                                    MainModel.permissionFilterMode
                                                ) { enabledMode, exportedMode, permissionMode ->
                                                    MainModel.enabledFilterMode = enabledMode
                                                    MainModel.exportedFilterMode = exportedMode
                                                    MainModel.permissionFilterMode = permissionMode
                                                }.show()
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_baseline_filter_list_24),
                                                contentDescription = stringResource(id = R.string.filter)
                                            )
                                        }
                                    }

                                    val firstIndex by remember {
                                        derivedStateOf { appListState.firstVisibleItemIndex }
                                    }
                                    val lastIndex by remember {
                                        derivedStateOf {
                                            firstIndex + appListState.layoutInfo.visibleItemsInfo.size - 1
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = MainModel.progress == null &&
                                                firstIndex > 0
                                    ) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    if (firstIndex > 20) {
                                                        appListState.scrollToItem(0)
                                                    } else {
                                                        appListState.animateScrollToItem(0)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.scroll_to_top),
                                                contentDescription = stringResource(id = R.string.scroll_top)
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = MainModel.progress == null &&
                                                lastIndex < MainModel.apps.size - 1
                                    ) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    if (MainModel.apps.size - 1 - lastIndex > 20) {
                                                        appListState.scrollToItem(MainModel.apps.size - 1)
                                                    } else {
                                                        appListState.animateScrollToItem(MainModel.apps.size - 1)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.scroll_to_bottom),
                                                contentDescription = stringResource(id = R.string.scroll_bottom)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = MainModel.progress != null,
                            modifier = Modifier.fillMaxSize(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable(
                                        interactionSource = remember {
                                            MutableInteractionSource()
                                        },
                                        indication = null,
                                    ) {},
                                contentAlignment = Alignment.Center
                            ) {
//                                CircularProgressIndicator(
//                                    progress = (MainModel.progress ?: 0f),
//                                    modifier = Modifier.size(128.dp)
//                                )
//
//                                if (MainModel.progress != null) {
//                                    Text(
//                                        text = "${((MainModel.progress ?: 0f) * 100).toInt()}%"
//                                    )
//                                }

                                val accentColor = MaterialTheme.colorScheme.primary
                                val textColor = Color.White
                                val rimWidth = with(LocalDensity.current) {
                                    8.dp.toPx()
                                }

                                AndroidView(
                                    factory = {
                                        ProgressCircula(context = it).apply {
                                            indeterminate = false
                                            rimColor = accentColor.toArgb()
                                            showProgress = true
                                            speed = 0.5f
                                            this.rimWidth = rimWidth
                                            this.textColor = textColor.toArgb()
                                        }
                                    },
                                    modifier = Modifier.size(200.dp),
                                    update = {
                                        it.progress = ((MainModel.progress ?: 0f) * 100).roundToInt()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        currentDataJob = loadDataAsync()
    }

    override fun onPermissionResult(granted: Boolean) {
        // Currently no-op
    }

//    override fun onRefresh() {
//        if (currentDataJob?.isActive == true) currentDataJob?.cancel()
//        currentDataJob = loadDataAsync()
//    }

    override fun onDestroy() {
        super.onDestroy()

        packageUpdateReceiver.unregister()
        cancel()
    }

    private fun loadDataAsync(silent: Boolean = false): Deferred<*> {
        return async(Dispatchers.Main) {
            if (!silent) {
                MainModel.progress = 0f
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

                            launch(Dispatchers.Main) {
                                MainModel.progress = p
                            }
                        }
                    }
                }
            }

            MainModel.apps.clear()
            MainModel.apps.addAll(loaded)

            MainModel.progress = null
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
            _activitiesSize = activities?.size ?: 0,
            _servicesSize = services?.size ?: 0,
            _receiversSize = receivers?.size ?: 0,
            isForTasker = isForTasker,
            context = this@MainActivity
        )
    }
}