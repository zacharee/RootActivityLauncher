package tk.zwander.rootactivitylauncher

import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.hmomeni.progresscircula.ProgressCircula
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.adapters.component.ActivityAdapter
import tk.zwander.rootactivitylauncher.adapters.AppAdapter
import tk.zwander.rootactivitylauncher.adapters.component.ServiceAdapter
import tk.zwander.rootactivitylauncher.data.*
import tk.zwander.rootactivitylauncher.data.component.ActivityInfo
import tk.zwander.rootactivitylauncher.data.component.ServiceInfo
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import tk.zwander.rootactivitylauncher.picasso.AppIconHandler
import tk.zwander.rootactivitylauncher.picasso.ServiceIconHandler

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
        AppAdapter(picasso)
    }

    private var progress: MenuItem? = null
    private val progressView: ProgressCircula?
        get() = (progress?.actionView as ProgressCircula?)

    private var search: MenuItem? = null
    private val searchView: SearchView?
        get() = (search?.actionView as SearchView?)

    private var showOnlyDisabled: MenuItem? = null

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
        showOnlyDisabled = menu.findItem(R.id.filter_disabled)

        searchView?.setOnQueryTextListener(this)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filter_disabled -> {
                item.isChecked = true
                appAdapter.setEnabledFilterMode(EnabledFilterMode.SHOW_DISABLED)
                true
            }
            R.id.filter_enabled -> {
                item.isChecked = true
                appAdapter.setEnabledFilterMode(EnabledFilterMode.SHOW_ENABLED)
                true
            }
            R.id.filter_all -> {
                item.isChecked = true
                appAdapter.setEnabledFilterMode(EnabledFilterMode.SHOW_ALL)
                true
            }
            R.id.filter_exported -> {
                item.isChecked = true
                appAdapter.setExportedFilterMode(ExportedFilterMode.SHOW_EXPORTED)
                true
            }
            R.id.filter_unexported -> {
                item.isChecked = true
                appAdapter.setExportedFilterMode(ExportedFilterMode.SHOW_UNEXPORTED)
                true
            }
            R.id.filter_all_ex -> {
                item.isChecked = true
                appAdapter.setExportedFilterMode(ExportedFilterMode.SHOW_ALL)
                true
            }
            else -> return super.onOptionsItemSelected(item)
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

    private fun loadData() = launch {
        val appInfo = ArrayList<AppInfo>()

        withContext(Dispatchers.IO) {
            val apps = packageManager.getInstalledApplications(0)
            val max = apps.size - 1

            progressView?.progress = 0

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

                    activities?.forEach { act ->
                        activityInfos.add(
                            ActivityInfo(
                                act,
                                act.loadLabel(packageManager)
                            )
                        )
                    }

                    services?.forEach { srv ->
                        serviceInfos.add(
                            ServiceInfo(
                                srv,
                                srv.loadLabel(packageManager)
                            )
                        )
                    }

                    val label = app.loadLabel(packageManager)

                    appInfo.add(
                        AppInfo(
                            app,
                            label,
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

                withContext(Dispatchers.Main) {
                    progressView?.progress = (index.toFloat() / max.toFloat() * 100f).toInt()
                }
            }
        }

        progress?.isVisible = false

        appAdapter.setItems(appInfo)
    }
}