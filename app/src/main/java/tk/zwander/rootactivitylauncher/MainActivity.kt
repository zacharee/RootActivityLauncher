package tk.zwander.rootactivitylauncher

import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hmomeni.progresscircula.ProgressCircula
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.adapters.AppAdapter
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.util.dpToPx
import tk.zwander.rootactivitylauncher.util.forEachParallel
import tk.zwander.rootactivitylauncher.util.launchEmail
import tk.zwander.rootactivitylauncher.util.launchUrl
import tk.zwander.rootactivitylauncher.views.FilterDialog
import kotlin.collections.ArrayList


@SuppressLint("RestrictedApi")
class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(),
    SearchView.OnQueryTextListener, SwipeRefreshLayout.OnRefreshListener {
    private val appAdapter by lazy {
        AppAdapter(this)
    }
    private val appListLayoutManager: LinearLayoutManager
        get() = app_list.layoutManager as LinearLayoutManager
    private val menu by lazy { action_menu_view.menu as MenuBuilder }

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

    private var currentDataJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        packageUpdateReceiver.register()

        setSupportActionBar(bottom_bar)

        search_options_wrapper.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                setSearchWrapperState(false)
                search_options_wrapper.isVisible = false
                search_options_wrapper.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        open_close.setOnClickListener {
            setSearchWrapperState(search_options_wrapper.translationX != 0f)
        }
        use_regex.setOnCheckedChangeListener { _, isChecked ->
            appAdapter.onFilterChange(useRegex = isChecked)
            app_list.scrollToPosition(0)
        }
        include_components.setOnCheckedChangeListener { _, isChecked ->
            appAdapter.onFilterChange(includeComponents = isChecked)
            app_list.scrollToPosition(0)
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
                            appAdapter.onFilterChange(enabledMode = enabledMode, exportedMode = exportedMode)
                        }.show()
                        true
                    }
                    R.id.scroll_top -> {
                        val vis = (app_list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                        if (vis > 20) {
                            app_list.scrollToPosition(0)
                        } else {
                            app_list.smoothScrollToPosition(0)
                        }
                        true
                    }
                    R.id.scroll_bottom -> {
                        val vis = (app_list.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                        if (appAdapter.itemCount - vis > 20) {
                            app_list.scrollToPosition(appAdapter.itemCount - 1)
                        } else {
                            app_list.smoothScrollToPosition(appAdapter.itemCount - 1)
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
                    else -> false
                }
            }

            override fun onMenuModeChange(menu: MenuBuilder) {}
        })

        updateScrollButtonState()

        searchView?.setOnQueryTextListener(this)
        searchView?.setOnSearchClickListener {
            search_options_wrapper.isVisible = true
        }
        searchView?.setOnCloseListener {
            setSearchWrapperState(false)
            search_options_wrapper.isVisible = false
            false
        }

        app_list.adapter = appAdapter
        app_list.setItemViewCacheSize(20)
        app_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateScrollButtonState()
            }
        })
        refresh.setOnRefreshListener(this)
        currentDataJob = loadData()
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        appAdapter.onFilterChange(query = newText ?: "")
        app_list.scrollToPosition(0)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onRefresh() {
        refresh.isRefreshing = true
        if (currentDataJob?.isActive == true) currentDataJob?.cancel()
        currentDataJob = loadData()
        refresh.isRefreshing = false
    }

    override fun onDestroy() {
        super.onDestroy()

        packageUpdateReceiver.unregister()
        cancel()
    }

    private fun updateProgress(progress: Int) {
        progressView?.progress = progress
        scrim_progress.progress = progress
    }

    private fun updateScrollButtonState() {
        val isActive = currentDataJob?.isActive == true
        val newTopVis = app_list.computeVerticalScrollOffset() > 0 && !isActive
        val newBotVis = appListLayoutManager.findLastCompletelyVisibleItemPosition() < appAdapter.itemCount - 1 && !isActive

        if (scrollToTop?.isVisible != newTopVis) {
            scrollToTop?.isVisible = newTopVis
        }
        if (scrollToBottom?.isVisible != newBotVis) {
            scrollToBottom?.isVisible = newBotVis
        }
    }

    private fun setSearchWrapperState(open: Boolean) {
        search_options_wrapper.apply {
            animate()
                .translationX(if (open) 0f else run {
                    width - dpToPx(28).toFloat()
                })
                .apply {
                    duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

                    //Casts are required here for some reason
                    interpolator = if (open) DecelerateInterpolator() as TimeInterpolator else AccelerateInterpolator() as TimeInterpolator
                }
        }
        open_close.animate()
            .scaleX(if (open) -1f else 1f)
    }

    private fun loadData(silent: Boolean = false): Job {
        return launch(Dispatchers.IO) {
            launch(Dispatchers.Main) {
                if (!silent) {
                    updateProgress(0)
                    scrim.isVisible = true
                    progress.isVisible = true
                    search.isVisible = false
                    filter.isVisible = false
                    scrollToTop.isVisible = false
                    scrollToBottom.isVisible = false
                    setSearchWrapperState(false)
                }
            }

            val apps = packageManager.getInstalledPackages(
                PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) PackageManager.MATCH_DISABLED_COMPONENTS
                        else PackageManager.GET_DISABLED_COMPONENTS)
            val max = apps.size - 1

            var progressIndex = 0

            val loaded = ArrayList<AppInfo>(apps.size)

            apps.forEachParallel { app ->
                loadApp(app)?.let { loaded.add(it) }

                if (!silent) {
                    launch(Dispatchers.Main) {
                        val p = (progressIndex++.toFloat() / max.toFloat() * 100f).toInt()
                        updateProgress(p)
                    }
                }
            }

            loaded.trimToSize()

            launch(Dispatchers.Main) {
                appAdapter.setItems(loaded)
                if (!silent) {
                    progress.isVisible = false
                    scrim.isVisible = false

                    refresh.postDelayed({
                        updateScrollButtonState()
                        search.isVisible = true
                        filter.isVisible = true
                    }, 10)
                } else {
                    refresh.postDelayed({
                        updateScrollButtonState()
                    }, 10)
                }
            }
        }
    }

    private fun getPackageInfo(packageName: String): PackageInfo {
        return packageManager.getPackageInfo(packageName,
            PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) PackageManager.MATCH_DISABLED_COMPONENTS
                    else PackageManager.GET_DISABLED_COMPONENTS)
    }

    private fun loadApp(app: PackageInfo) : AppInfo? {
        val activities = app.activities
        val services = app.services

        if (((activities != null && activities.isNotEmpty()) || (services != null && services.isNotEmpty()))) {
            val activityInfos = ArrayList<ActivityInfo>(activities?.size ?: 0)
            val serviceInfos = ArrayList<ServiceInfo>(services?.size ?: 0)

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

            return AppInfo(
                app.applicationInfo,
                appLabel,
                activityInfos,
                serviceInfos
            )
        }

        return null
    }
}