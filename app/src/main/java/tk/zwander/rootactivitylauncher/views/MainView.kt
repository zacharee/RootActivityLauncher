package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.model.AppModel
import tk.zwander.rootactivitylauncher.data.model.MainModel
import tk.zwander.rootactivitylauncher.util.extractApk
import tk.zwander.rootactivitylauncher.views.components.AppList
import tk.zwander.rootactivitylauncher.views.components.BottomBar
import tk.zwander.rootactivitylauncher.views.dialogs.FilterDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainView(
    isForTasker: Boolean,
    onItemSelected: (BaseComponentInfo) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appListState = remember {
        LazyStaggeredGridState()
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showingFilterDialog by remember {
        mutableStateOf(false)
    }
    var extractInfo: AppModel? by remember {
        mutableStateOf(null)
    }

    val extractLauncher = rememberLauncherForActivityResult(
        object : ActivityResultContracts.OpenDocumentTree() {
            override fun createIntent(context: Context, input: Uri?): Intent {
                return super.createIntent(context, input).also {
                    it.putExtra(
                        Intent.EXTRA_TITLE,
                        context.resources.getString(R.string.choose_extract_folder_msg)
                    )
                }
            }
        }
    ) { result ->
        if (result != null) {
            scope.launch(Dispatchers.IO) {
                extractInfo?.let {
                    context.extractApk(result, it)
                }
                extractInfo = null
            }
        }
    }

    val enabledFilterMode by MainModel.enabledFilterMode.collectAsState()
    val exportedFilterMode by MainModel.exportedFilterMode.collectAsState()
    val permissionFilterMode by MainModel.permissionFilterMode.collectAsState()
    val query by MainModel.query.collectAsState()
    val apps by MainModel.apps.collectAsState()
    val filteredApps by MainModel.filteredApps.collectAsState()
    val progress by MainModel.progress.collectAsState()
    val isSearching by MainModel.isSearching.collectAsState()
    val useRegex by MainModel.useRegex.collectAsState()
    val includeComponents by MainModel.includeComponents.collectAsState()

    LaunchedEffect(extractInfo) {
        if (extractInfo != null) {
            extractLauncher.launch(null)
        }
    }

    LaunchedEffect(
        isSearching,
        useRegex,
        includeComponents
    ) {
        if (isSearching) {
            MainModel.update()
        }
    }

    LaunchedEffect(
        apps,
        enabledFilterMode,
        exportedFilterMode,
        permissionFilterMode,
        query
    ) {
        MainModel.update()
    }

    Surface(
        modifier = modifier
    ) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(false),
            onRefresh = onRefresh,
            indicator = { state, trigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = trigger,
                    scale = true,
                    backgroundColor = MaterialTheme.colorScheme.primary,
                )
            }
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .captionBarPadding()
                        .imePadding()
                ) {
                    AppList(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        appListState = appListState,
                        isForTasker = isForTasker,
                        onItemSelected = onItemSelected,
                        extractCallback = {
                            extractInfo = it
                        },
                        filteredApps = filteredApps
                    )

                    BottomBar(
                        modifier = Modifier
                            .fillMaxWidth(),
                        isSearching = isSearching,
                        useRegex = useRegex,
                        includeComponents = includeComponents,
                        query = query,
                        progress = progress,
                        apps = apps,
                        appListState = appListState,
                        onShowFilterDialog = {
                            showingFilterDialog = true
                        }
                    )
                }

                ScrimView(
                    modifier = Modifier.fillMaxSize(),
                    progress = progress
                )
            }
        }
    }

    FilterDialog(
        showing = showingFilterDialog,
        initialEnabledMode = MainModel.enabledFilterMode.collectAsState().value,
        initialExportedMode = MainModel.exportedFilterMode.collectAsState().value,
        initialPermissionMode = MainModel.permissionFilterMode.collectAsState().value,
        onDismissRequest = { enabled, exported, permission ->
            MainModel.enabledFilterMode.value = enabled
            MainModel.exportedFilterMode.value = exported
            MainModel.permissionFilterMode.value = permission
            showingFilterDialog = false
        }
    )
}
