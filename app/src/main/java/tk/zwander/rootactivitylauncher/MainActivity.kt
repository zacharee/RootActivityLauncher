package tk.zwander.rootactivitylauncher

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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
import tk.zwander.rootactivitylauncher.util.lazyDeferred
import tk.zwander.rootactivitylauncher.views.FilterDialog

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(),
    SearchView.OnQueryTextListener {
    private val picasso by lazy {
        Picasso.Builder(this)
            .addRequestHandler(AppIconHandler(this))
            .addRequestHandler(ActivityIconHandler(this))
            .addRequestHandler(ServiceIconHandler(this))
            .build()
    }
    private val appAdapter by lazy {
        AppAdapter(this, picasso)
    }

    private var progress: MenuItem? = null
    private val progressView: ProgressCircula?
        get() = (progress?.actionView as ProgressCircula?)

    private var search: MenuItem? = null
    private val searchView: SearchView?
        get() = (search?.actionView as SearchView?)

    private var filter: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(bottom_bar)

        app_list.adapter = appAdapter
        loadData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.search, menu)

        progress = menu.findItem(R.id.progress)
        search = menu.findItem(R.id.action_search)
        filter = menu.findItem(R.id.action_filter)

        searchView?.setOnQueryTextListener(this)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                FilterDialog(
                    this,
                    appAdapter.enabledFilterMode,
                    appAdapter.exportedFilterMode
                ) { enabledMode, exportedMode ->
                    appAdapter.enabledFilterMode = enabledMode
                    appAdapter.exportedFilterMode = exportedMode
                }.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        appAdapter.onQueryTextChange(newText)
        app_list.scrollToPosition(0)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
        picasso.shutdown()
    }

    private fun loadData() = launch(Dispatchers.IO) {
        val appInfo = ArrayList<AppInfo>()

        val apps = packageManager.getInstalledApplications(0)
        val max = apps.size - 1

        launch(Dispatchers.Main) {
            progressView?.progress = 0
        }

        apps.forEachIndexed { index, app ->
            val i = packageManager.getPackageInfo(
                app.packageName,
                PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) PackageManager.MATCH_DISABLED_COMPONENTS
                        else PackageManager.GET_DISABLED_COMPONENTS
            )
            val activities = i.activities
            val services = i.services

            if ((activities != null && activities.isNotEmpty()) || (services != null && services.isNotEmpty())) {
                val activityInfos = ArrayList<ActivityInfo>()
                val serviceInfos = ArrayList<ServiceInfo>()

                val appLabel = app.loadLabel(packageManager)

                activities?.forEach { act ->
                    val label by lazyDeferred {
                        act.loadLabel(packageManager).run { if (isBlank()) appLabel else this }
                    }
                    activityInfos.add(
                        ActivityInfo(
                            act,
                            label
                        )
                    )
                }

                services?.forEach { srv ->
                    val label by lazyDeferred {
                        srv.loadLabel(packageManager).run { if (isBlank()) appLabel else this }
                    }
                    serviceInfos.add(
                        ServiceInfo(
                            srv,
                            label
                        )
                    )
                }

                appInfo.add(
                    AppInfo(
                        app,
                        appLabel,
                        activityInfos,
                        serviceInfos,
                        ActivityAdapter(
                            picasso
                        ),
                        ServiceAdapter(
                            picasso
                        )
                    )
                )
            }

            launch(Dispatchers.Main) {
                progressView?.progress = (index.toFloat() / max.toFloat() * 100f).toInt()
            }
        }

        launch(Dispatchers.Main) {
            progress?.isVisible = false
            search?.isVisible = true
            filter?.isVisible = true

            appAdapter.setItems(appInfo)
        }
    }
}