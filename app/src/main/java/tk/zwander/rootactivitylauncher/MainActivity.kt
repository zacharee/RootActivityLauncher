package tk.zwander.rootactivitylauncher

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.hmomeni.progresscircula.ProgressCircula
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import tk.zwander.rootactivitylauncher.adapters.ActivityAdapter
import tk.zwander.rootactivitylauncher.adapters.AppAdapter
import tk.zwander.rootactivitylauncher.data.ActivityInfo
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.picasso.ActivityIconHandler
import tk.zwander.rootactivitylauncher.picasso.AppIconHandler

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope(), SearchView.OnQueryTextListener {
    private val picasso by lazy {
        Picasso.Builder(this)
            .addRequestHandler(AppIconHandler(this))
            .addRequestHandler(ActivityIconHandler(this))
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

        searchView?.setOnQueryTextListener(this)

        return super.onCreateOptionsMenu(menu)
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
                val activities = packageManager.getPackageInfo(app.packageName, PackageManager.GET_ACTIVITIES).activities
                val infos = ArrayList<ActivityInfo>()

                if (activities != null && activities.isNotEmpty()) {
                    activities.forEach { act ->
                        infos.add(ActivityInfo(act, act.loadLabel(packageManager)))
                    }

                    val label = app.loadLabel(packageManager)

                    appInfo.add(AppInfo(
                        app,
                        label,
                        infos,
                        ActivityAdapter(picasso)
                    ))
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