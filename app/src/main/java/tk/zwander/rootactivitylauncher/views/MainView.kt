package tk.zwander.rootactivitylauncher.views

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.model.AppModel
import tk.zwander.rootactivitylauncher.util.LocalMainModel
import tk.zwander.rootactivitylauncher.util.extractApk
import tk.zwander.rootactivitylauncher.views.components.AppList
import tk.zwander.rootactivitylauncher.views.components.BottomBar
import tk.zwander.rootactivitylauncher.views.dialogs.FilterDialog
import tk.zwander.rootactivitylauncher.views.dialogs.SortDialog

@OptIn(ExperimentalMaterial3Api::class)
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
    val mainModel = LocalMainModel.current
    val scope = rememberCoroutineScope()

    var showingFilterDialog by remember {
        mutableStateOf(false)
    }
    var showingSortDialog by remember {
        mutableStateOf(false)
    }
    var extractInfo: AppModel? by remember {
        mutableStateOf(null)
    }
    val extractErrors: MutableList<Throwable> = remember {
        mutableStateListOf()
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
                    extractErrors.clear()
                    extractErrors.addAll(context.extractApk(result, it))
                }
                extractInfo = null
            }
        }
    }

    val query by mainModel.query.collectAsState()
    val filteredApps by mainModel.filteredApps.collectAsState(listOf())
    val progress by mainModel.progress.collectAsState()
    val isSearching by mainModel.isSearching.collectAsState()
    val useRegex by mainModel.useRegex.collectAsState()
    val includeComponents by mainModel.includeComponents.collectAsState()
    var refreshing by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(extractInfo) {
        if (extractInfo != null) {
            extractLauncher.launch(null)
        }
    }

    LaunchedEffect(refreshing) {
        refreshing = false
    }


    Surface(
        modifier = modifier,
    ) {
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize(),
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                onRefresh()
            },
        ) {
            Scaffold(
                bottomBar = {
                    BottomBar(
                        modifier = Modifier
                            .fillMaxWidth(),
                        isSearching = isSearching,
                        useRegex = useRegex,
                        includeComponents = includeComponents,
                        query = query,
                        progress = progress,
                        apps = filteredApps,
                        appListState = appListState,
                        onShowFilterDialog = {
                            showingFilterDialog = true
                        },
                        onShowSortDialog = {
                            showingSortDialog = true
                        },
                        onQueryChanged = {
                            mainModel.query.value = it
                        },
                        onIncludeComponentsChanged = {
                            mainModel.includeComponents.value = it
                        },
                        onIsSearchingChanged = {
                            mainModel.isSearching.value = it
                        },
                        onUseRegexChanged = {
                            mainModel.useRegex.value = it
                        }
                    )
                },
                content = { contentPadding ->
                    AppList(
                        modifier = Modifier
                            .fillMaxWidth(),
                        appListState = appListState,
                        isForTasker = isForTasker,
                        onItemSelected = onItemSelected,
                        extractCallback = {
                            extractInfo = it
                        },
                        filteredApps = filteredApps,
                        contentPaddingValues = contentPadding,
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )

            ScrimView(
                modifier = Modifier.fillMaxSize(),
                progress = progress
            )
        }
    }

    FilterDialog(
        showing = showingFilterDialog,
        initialEnabledMode = mainModel.enabledFilterMode.collectAsState().value,
        initialExportedMode = mainModel.exportedFilterMode.collectAsState().value,
        initialPermissionMode = mainModel.permissionFilterMode.collectAsState().value,
        initialComponentMode = mainModel.componentFilterMode.collectAsState().value,
        initialSystemAppsMode = mainModel.systemAppsFilterMode.collectAsState().value,
        onDismissRequest = { enabled, exported, permission, component, systemApps ->
            mainModel.enabledFilterMode.value = enabled
            mainModel.exportedFilterMode.value = exported
            mainModel.permissionFilterMode.value = permission
            mainModel.componentFilterMode.value = component
            mainModel.systemAppsFilterMode.value = systemApps
            showingFilterDialog = false
        }
    )

    SortDialog(
        showing = showingSortDialog,
        initialSortBy = mainModel.sortAppsBy.collectAsState().value,
        initialSortOrder = mainModel.sortOrder.collectAsState().value,
        onDismissRequest = { sortBy, sortOrder ->
            mainModel.sortAppsBy.value = sortBy
            mainModel.sortOrder.value = sortOrder
            showingSortDialog = false
        },
    )

    if (extractErrors.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                extractErrors.clear()
            },
            title = {
                Text(text = stringResource(id = R.string.extraction_failed_title))
            },
            text = {
                Column {
                    Text(
                        text = stringResource(id = R.string.extraction_failed_desc)
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(extractErrors.size, { it }) {
                            val item = extractErrors[it]

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column {
                                        SelectionContainer {
                                            Text(text = item.message ?: "")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { extractErrors.clear() }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}
