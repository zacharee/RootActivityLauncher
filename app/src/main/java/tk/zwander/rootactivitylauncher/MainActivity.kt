package tk.zwander.rootactivitylauncher

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hmomeni.progresscircula.ProgressCircula
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.adapters.AppAdapter
import tk.zwander.rootactivitylauncher.adapters.component.ActivityAdapter
import tk.zwander.rootactivitylauncher.adapters.component.ServiceAdapter
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import tk.zwander.rootactivitylauncher.picasso.AppIconHandler
import tk.zwander.rootactivitylauncher.picasso.ServiceIconHandler
import tk.zwander.rootactivitylauncher.util.picasso
import tk.zwander.rootactivitylauncher.util.prefs
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

    private var currentDataJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(bottom_bar)

        menuInflater.inflate(R.menu.search, menu)
        menu.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder?, item: MenuItem?): Boolean {
                return when (item?.itemId) {
                    R.id.action_filter -> {
                        FilterDialog(
                            this@MainActivity,
                            appAdapter.enabledFilterMode,
                            appAdapter.exportedFilterMode
                        ) { enabledMode, exportedMode ->
                            appAdapter.enabledFilterMode = enabledMode
                            appAdapter.exportedFilterMode = exportedMode
                        }.show()
                        true
                    }
                    R.id.scroll_top -> {
                        val vis = (app_list.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                        if (vis > 20) {
                            app_list.scrollToPosition(0)
                        } else {
                            app_list.smoothScrollToPosition(0)
                        }
                        true
                    }
                    R.id.scroll_bottom -> {
                        val vis = (app_list.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                        if (appAdapter.itemCount - vis > 20) {
                            app_list.scrollToPosition(appAdapter.itemCount - 1)
                        } else {
                            app_list.smoothScrollToPosition(appAdapter.itemCount - 1)
                        }
                        true
                    }
                    else -> false
                }
            }

            override fun onMenuModeChange(menu: MenuBuilder?) {}
        })

        updateScrollButtonState()

        searchView?.setOnQueryTextListener(this)

        app_list.adapter = appAdapter
        app_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateScrollButtonState()
            }
        })
        refresh.setOnRefreshListener(this)
        currentDataJob = loadData()
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        appAdapter.onQueryTextChange(newText)
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

        cancel()
    }

    private fun updateProgress(progress: Int) {
        progressView?.progress = progress
        scrim_progress.progress = progress
    }

    private fun updateScrollButtonState() {
        val isActive = currentDataJob?.isActive == true
        scrollToTop?.isVisible = appListLayoutManager.findFirstCompletelyVisibleItemPosition() > 0 && !isActive
        scrollToBottom?.isVisible = appListLayoutManager.findLastCompletelyVisibleItemPosition() < appAdapter.itemCount - 1 && !isActive
    }

    private fun loadData(): Job {
        return launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                updateProgress(0)
                appAdapter.clearItems()
                scrim.isVisible = true
                progress.isVisible = true
                search.isVisible = false
                filter.isVisible = false
                scrollToTop.isVisible = false
                scrollToBottom.isVisible = false
            }

            val apps = packageManager.getInstalledPackages(
                PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) PackageManager.MATCH_DISABLED_COMPONENTS
                        else PackageManager.GET_DISABLED_COMPONENTS)
            val max = apps.size - 1

            val jobs = ArrayList<Deferred<*>>(apps.size)
            var progressIndex = 0

            apps.forEachIndexed { index, app ->
                jobs.add(
                    async {
                        val activities = app.activities
                        val services = app.services

                        if (((activities != null && activities.isNotEmpty()) || (services != null && services.isNotEmpty()))) {
                            val activityInfos = ArrayList<ActivityInfo>(activities?.size ?: 0)
                            val serviceInfos = ArrayList<ServiceInfo>(services?.size ?: 0)

                            val appLabel = prefs.getOrLoadAppLabel(app)

                            activities?.forEachIndexed { index, act ->
                                val label = prefs.getOrLoadComponentLabel(app, act).run { if (isBlank()) appLabel else this }
                                activityInfos.add(
                                    ActivityInfo(
                                        act,
                                        label
                                    )
                                )
                            }

                            services?.forEachIndexed { index, srv ->
                                val label = prefs.getOrLoadComponentLabel(app, srv).run { if (isBlank()) appLabel else this }
                                serviceInfos.add(
                                    ServiceInfo(
                                        srv,
                                        label
                                    )
                                )
                            }

                            launch(Dispatchers.Main) {
                                appAdapter.addItem(AppInfo(
                                    app.applicationInfo,
                                    appLabel,
                                    activityInfos,
                                    serviceInfos
                                ))
                                app_list.scrollToPosition(0)
                            }
                        }

                        launch(Dispatchers.Main) {
                            val p = (progressIndex++.toFloat() / max.toFloat() * 100f).toInt()
                            updateProgress(p)
                        }
                    }
                )
            }

            jobs.awaitAll()

            launch(Dispatchers.Main) {
                progress.isVisible = false
                scrim.isVisible = false
                search.isVisible = true
                filter.isVisible = true

                scrim.postDelayed({
                    updateScrollButtonState()
                }, 10)
            }
        }
    }
}