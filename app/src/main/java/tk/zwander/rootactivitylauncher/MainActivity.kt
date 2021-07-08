package tk.zwander.rootactivitylauncher

import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hmomeni.progresscircula.ProgressCircula
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import tk.zwander.patreonsupportersretrieval.view.SupporterView
import tk.zwander.rootactivitylauncher.adapters.AppAdapter
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.component.*
import tk.zwander.rootactivitylauncher.databinding.ActivityMainBinding
import tk.zwander.rootactivitylauncher.util.*
import tk.zwander.rootactivitylauncher.views.AdvancedUsageDialog
import tk.zwander.rootactivitylauncher.views.FilterDialog
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue


@SuppressLint("RestrictedApi")
open class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(),
    SearchView.OnQueryTextListener, SwipeRefreshLayout.OnRefreshListener, Shizuku.OnRequestPermissionResultListener {
    companion object {
        const val REQ_SHIZUKU = 10001
        const val REQ_EXTRACT = 1001
    }

    protected open val isForTasker = false
    protected open var selectedItem: Pair<ComponentType, ComponentName>? = null
    protected val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    @Volatile
    private var extractInfo: AppInfo? = null

    private val appAdapter by lazy {
        fun onExtract(d: AppInfo) {
            extractInfo = d

            val extractIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            extractIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.choose_extract_folder_msg))

            startActivityForResult(extractIntent, REQ_EXTRACT)
        }

        AppAdapter(this, isForTasker, ::onExtract)
    }
    private val appListLayoutManager: RecyclerView.LayoutManager
        get() = binding.appList.layoutManager as RecyclerView.LayoutManager
    private val menu by lazy { binding.actionMenuView.menu as MenuBuilder }

    private val progress by lazy { menu.findItem(R.id.progress) }
    private val progressView: ProgressCircula?
        get() = (progress?.actionView as ProgressCircula?)

    private val search by lazy { menu.findItem(R.id.action_search) }
    private val searchView: SearchView?
        get() = (search?.actionView as SearchView?)

    private val filter by lazy { menu.findItem(R.id.action_filter) }
    private val scrollToTop by lazy { menu.findItem(R.id.scroll_top) }
    private val scrollToBottom by lazy { menu.findItem(R.id.scroll_bottom) }

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
                            val loaded = loadApp(getPackageInfo(host))

                            appAdapter.addItem(loaded ?: return@launch)
                        }
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        if (host != null) {
                            appAdapter.removeItem(host)
                        }
                    }
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        if (host != null) {
                            appAdapter.updateItem(loadApp(getPackageInfo(host)) ?: return@launch)
                        }
                    }
                }
            }
        }
    }

    private var currentDataJob: Deferred<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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

        setSupportActionBar(binding.bottomBar)

        binding.searchOptionsWrapper.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                setSearchWrapperState(false)
                binding.searchOptionsWrapper.isVisible = false
                binding.searchOptionsWrapper.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        binding.openClose.setOnClickListener {
            setSearchWrapperState(binding.searchOptionsWrapper.translationX != 0f)
        }
        binding.useRegex.setOnCheckedChangeListener { _, isChecked ->
            appAdapter.onFilterChange(useRegex = isChecked)
            binding.appList.scrollToPosition(0)
        }
        binding.includeComponents.setOnCheckedChangeListener { _, isChecked ->
            appAdapter.onFilterChange(includeComponents = isChecked)
            binding.appList.scrollToPosition(0)
        }

        if (Shizuku.pingBinder()) {
            if (!hasShizukuPermission) {
                Shizuku.addRequestPermissionResultListener(this)
                requestShizukuPermission(REQ_SHIZUKU)
            }
        }

        menuInflater.inflate(R.menu.search, menu)
        menu.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.action_filter -> {
                        FilterDialog(
                            this@MainActivity,
                            appAdapter.enabledFilterMode,
                            appAdapter.exportedFilterMode
                        ) { enabledMode, exportedMode ->
                            appAdapter.onFilterChange(
                                enabledMode = enabledMode,
                                exportedMode = exportedMode
                            )
                        }.show()
                        true
                    }
                    R.id.scroll_top -> {
                        val vis =
                            (appListLayoutManager).findFirstVisibleItemPosition()
                        if (vis > 20) {
                            binding.appList.scrollToPosition(0)
                        } else {
                            binding.appList.smoothScrollToPosition(0)
                        }
                        true
                    }
                    R.id.scroll_bottom -> {
                        val vis = (appListLayoutManager).findLastVisibleItemPosition()
                        if (appAdapter.itemCount - vis > 20) {
                            binding.appList.scrollToPosition(appAdapter.itemCount - 1)
                        } else {
                            binding.appList.smoothScrollToPosition(appAdapter.itemCount - 1)
                        }
                        true
                    }
                    R.id.action_twitter -> {
                        launchUrl("https://twitter.com/Wander1236")
                        true
                    }
                    R.id.action_web -> {
                        launchUrl("https://zwander.dev")
                        true
                    }
                    R.id.action_github -> {
                        launchUrl("https://github.com/zacharee")
                        true
                    }
                    R.id.action_email -> {
                        launchEmail("zachary@zwander.dev", resources.getString(R.string.app_name))
                        true
                    }
                    R.id.action_telegram -> {
                        launchUrl("https://bit.ly/ZachareeTG")
                        true
                    }
                    R.id.action_discord -> {
                        launchUrl("https://bit.ly/zwanderDiscord")
                        true
                    }
                    R.id.action_patreon -> {
                        launchUrl("https://bit.ly/zwanderPatreon")
                        true
                    }
                    R.id.action_supporters -> {
                        MaterialAlertDialogBuilder(this@MainActivity)
                            .setTitle(R.string.supporters)
                            .setMessage(R.string.supporters_desc)
                            .setView(SupporterView(this@MainActivity))
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                        true
                    }
                    R.id.action_advanced_search -> {
                        AdvancedUsageDialog(this@MainActivity)
                            .show()
                        true
                    }
                    else -> false
                }
            }

            override fun onMenuModeChange(menu: MenuBuilder) {}
        })

        updateScrollButtonState()

        searchView?.setOnQueryTextListener(this)
        searchView?.setOnSearchClickListener {
            binding.searchOptionsWrapper.isVisible = true
        }
        searchView?.setOnCloseListener {
            setSearchWrapperState(false)
            binding.searchOptionsWrapper.isVisible = false
            false
        }

        binding.appList.adapter = appAdapter
        binding.appList.layoutManager = getAppropriateLayoutManager(resources.configuration.screenWidthDp)
        binding.appList.setItemViewCacheSize(20)
        binding.appList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateScrollButtonState()
            }
        })
        binding.refresh.setOnRefreshListener(this)
        currentDataJob = loadDataAsync()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        binding.appList.layoutManager = getAppropriateLayoutManager(newConfig.screenWidthDp)
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == REQ_SHIZUKU && grantResult == PackageManager.PERMISSION_GRANTED) {
            //Nothing yet
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        onRequestPermissionResult(requestCode, grantResults[0])
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_EXTRACT && resultCode == Activity.RESULT_OK
            && extractInfo != null) {
            val dirUri = data?.data ?: return
            val dir = DocumentFile.fromTreeUri(this, dirUri) ?: return

            val extractInfo = extractInfo!!
            val baseDir = File(extractInfo.info.sourceDir)

            val splits = extractInfo.info.splitSourceDirs?.mapIndexed { index, s ->
                val splitApk = File(s)
                val splitName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    extractInfo.info.splitNames[index]
                } else splitApk.nameWithoutExtension

                splitName to s
            }

            val baseFile = dir.createFile("application/vnd.android.package-archive", extractInfo.info.packageName) ?: return
            contentResolver.openOutputStream(baseFile.uri).use { writer ->
                Log.e("RootActivityLauncher", "$baseDir")
                baseDir.inputStream().use { reader ->
                    reader.copyTo(writer!!)
                }
            }

            splits?.forEach { split ->
                val name = split.first
                val path = File(split.second)

                val file = dir.createFile("application/vnd.android.package-archive", "${extractInfo.info.packageName}_$name") ?: return
                contentResolver.openOutputStream(file.uri).use { writer ->
                    path.inputStream().use { reader ->
                        reader.copyTo(writer!!)
                    }
                }
            }
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        appAdapter.onFilterChange(query = newText ?: "")
        binding.appList.scrollToPosition(0)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onRefresh() {
        binding.refresh.isRefreshing = true
        if (currentDataJob?.isActive == true) currentDataJob?.cancel()
        currentDataJob = loadDataAsync()
        binding.refresh.isRefreshing = false
    }

    override fun onDestroy() {
        super.onDestroy()

        currentDataJob?.cancel()
        packageUpdateReceiver.unregister()
        cancel()
    }

    private fun updateProgress(progress: Int) {
        progressView?.progress = progress
        binding.scrimProgress.progress = progress
    }

    private fun updateScrollButtonState() {
        val isActive = currentDataJob?.isActive == true
        val newTopVis = binding.appList.computeVerticalScrollOffset() > 0 && !isActive
        val newBotVis = run {
            val pos = when (val manager = appListLayoutManager) {
                is StaggeredGridLayoutManager -> {
                    IntArray(50).apply { manager.findLastCompletelyVisibleItemPositions(this) }
                        .maxOrNull() ?: 0
                }
                is LinearLayoutManager -> {
                    manager.findLastCompletelyVisibleItemPosition()
                }
                else -> {
                    throw IllegalStateException("Invalid layout manager $manager")
                }
            }
            pos < appAdapter.itemCount - 1 && !isActive
        }

        if (scrollToTop?.isVisible != newTopVis) {
            scrollToTop?.isVisible = newTopVis
        }
        if (scrollToBottom?.isVisible != newBotVis) {
            scrollToBottom?.isVisible = newBotVis
        }
    }

    private fun setSearchWrapperState(open: Boolean) {
        binding.searchOptionsWrapper.apply {
            animate()
                .translationX(if (open) 0f else run {
                    width - dpToPx(28).toFloat()
                })
                .apply {
                    duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

                    //Casts are required here for some reason
                    interpolator =
                        if (open) DecelerateInterpolator() as TimeInterpolator else AccelerateInterpolator() as TimeInterpolator
                }
                .start()
        }
        binding.openClose.animate()
            .scaleX(if (open) -1f else 1f)
            .start()
    }

    private fun loadDataAsync(silent: Boolean = false): Deferred<*> {
        return async(Dispatchers.Main) {
            if (!silent) {
                updateProgress(0)
                binding.scrim.isVisible = true
                progress.isVisible = true
                search.isVisible = false
                filter.isVisible = false
                scrollToTop.isVisible = false
                scrollToBottom.isVisible = false
                setSearchWrapperState(false)
            }

            //This mess is because of a bug in Marshmallow and possibly earlier that
            //causes getInstalledPackages() to fail because one of the PackageInfo objects
            //is to large. It'll throw a DeadObjectException internally and then just return
            //as much as it already transferred before the error. The bug seems to be fixed in
            //Nougat and later.
            //If the device is on Marshmallow, retrieve a list of packages without any components
            //attached. Attempt to retrieve all components at once per-app. If that fails, retrieve
            //each set of components individually and combine them.
            //https://twitter.com/Wander1236/status/1412928863798190083?s=20
            val apps = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                packageManager.getInstalledPackages(
                    PackageManager.GET_ACTIVITIES or
                            PackageManager.GET_SERVICES or
                            PackageManager.GET_RECEIVERS or
                            PackageManager.GET_PERMISSIONS or
                            PackageManager.GET_CONFIGURATIONS or
                            PackageManager.MATCH_DISABLED_COMPONENTS
                )
            } else {
                val packages = packageManager.getInstalledPackages(PackageManager.GET_DISABLED_COMPONENTS)
                packages.map {
                    try {
                        //Try to get all the components for this package at once.
                        packageManager.getPackageInfo(
                            it.packageName,
                            PackageManager.GET_ACTIVITIES or
                                    PackageManager.GET_SERVICES or
                                    PackageManager.GET_RECEIVERS or
                                    PackageManager.GET_PERMISSIONS or
                                    PackageManager.GET_CONFIGURATIONS
                        )
                    } catch (e: Exception) {
                        //The resulting PackageInfo was too large. Retrieve each set
                        //separately and combine them.
                        Log.e("RootActivityLauncher", "Unable to get full info, splitting ${it.packageName}", e)
                        val awaits = listOf(
                            PackageManager.GET_ACTIVITIES,
                            PackageManager.GET_SERVICES,
                            PackageManager.GET_RECEIVERS,
                            PackageManager.GET_PERMISSIONS,
                            PackageManager.GET_CONFIGURATIONS
                        ).map { flag ->
                            async {
                                packageManager.getPackageInfo(it.packageName, flag)
                            }
                        }.awaitAll()

                        it.apply {
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

            var progressIndex = 0

            apps.forEachParallel { app ->
                loadApp(app)?.let {
                    loaded.add(it)
                }

                if (!silent) {
                    val p = (progressIndex++.toFloat() / max.toFloat() * 100f).toInt()
                    updateProgress(p)
                }
            }

            appAdapter.setItems(loaded)

            if (!silent) {
                progress.isVisible = false
                binding.scrim.isVisible = false

                binding.refresh.postDelayed({
                    updateScrollButtonState()
                    search.isVisible = true
                    filter.isVisible = true
                }, 10)
            } else {
                binding.refresh.postDelayed({
                    updateScrollButtonState()
                }, 10)
            }
        }
    }

    private fun getPackageInfo(packageName: String): PackageInfo {
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

    private suspend fun loadApp(app: PackageInfo): AppInfo? = coroutineScope {
        val activities = app.activities
        val services = app.services
        val receivers = app.receivers

        if ((activities != null && activities.isNotEmpty())
                || (services != null && services.isNotEmpty())
                || (receivers != null && receivers.isNotEmpty())
        ) {
            val activityInfos = LinkedList<ActivityInfo>()
            val serviceInfos = LinkedList<ServiceInfo>()
            val receiverInfos = LinkedList<ReceiverInfo>()

            val appLabel = app.applicationInfo.loadLabel(packageManager)

            activities?.forEach { act ->
                val label = act.loadLabel(packageManager).run { if (isBlank()) appLabel else this }
                activityInfos.add(
                    ActivityInfo(
                        act,
                        label
                    )
                )
            }

            services?.forEach { srv ->
                val label = srv.loadLabel(packageManager).run { if (isBlank()) appLabel else this }
                serviceInfos.add(
                    ServiceInfo(
                        srv,
                        label
                    )
                )
            }

            receivers?.forEach { rec ->
                val label = rec.loadLabel(packageManager).run { if (isBlank()) appLabel else this }
                receiverInfos.add(
                        ReceiverInfo(
                            rec,
                            label
                        )
                )
            }

            return@coroutineScope AppInfo(
                pInfo = app,
                label = appLabel,
                activities = activityInfos,
                services = serviceInfos,
                receivers = receiverInfos,
                isForTasker = isForTasker,
                selectionCallback = { info ->
                    selectedItem = info.type() to info.component
                }
            )
        }

        return@coroutineScope null
    }
}