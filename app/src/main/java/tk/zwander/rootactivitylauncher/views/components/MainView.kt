package tk.zwander.rootactivitylauncher.views.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.AppInfo
import tk.zwander.rootactivitylauncher.data.MainModel
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import java.io.File

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

    var showingFilterDialog by remember {
        mutableStateOf(false)
    }
    var extractInfo: AppInfo? by remember {
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
        if (extractInfo != null) {
            val dirUri = result ?: return@rememberLauncherForActivityResult
            val dir = DocumentFile.fromTreeUri(context, dirUri)
                ?: return@rememberLauncherForActivityResult

            val actualInfo = extractInfo!!

            val baseDir = File(actualInfo.info.sourceDir)

            val splits = actualInfo.info.splitSourceDirs?.mapIndexed { index, s ->
                val splitApk = File(s)
                val splitName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    actualInfo.info.splitNames[index]
                } else splitApk.nameWithoutExtension

                splitName to s
            }

            val baseFile = dir.createFile(
                "application/vnd.android.package-archive",
                actualInfo.info.packageName
            ) ?: return@rememberLauncherForActivityResult
            context.contentResolver.openOutputStream(baseFile.uri).use { writer ->
                Log.e("RootActivityLauncher", "$baseDir")
                try {
                    baseDir.inputStream().use { reader ->
                        reader.copyTo(writer!!)
                    }
                } catch (e: Exception) {
                    Log.e("RootActivityLauncher", "Extraction failed", e)
                    Toast.makeText(
                        context,
                        context.resources.getString(R.string.extraction_failed, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            splits?.forEach { split ->
                val name = split.first
                val path = File(split.second)

                val file = dir.createFile(
                    "application/vnd.android.package-archive",
                    "${actualInfo.info.packageName}_$name"
                ) ?: return@rememberLauncherForActivityResult
                context.contentResolver.openOutputStream(file.uri).use { writer ->
                    try {
                        path.inputStream().use { reader ->
                            reader.copyTo(writer!!)
                        }
                    } catch (e: Exception) {
                        Log.e("RootActivityLauncher", "Extraction failed", e)
                        Toast.makeText(
                            context,
                            context.resources.getString(R.string.extraction_failed, e.message),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(extractInfo) {
        if (extractInfo != null) {
            extractLauncher.launch(null)
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
                val layoutDirection = LocalLayoutDirection.current

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .captionBarPadding()
                        .imePadding()
                ) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(400.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = WindowInsets.statusBars.asPaddingValues().run {
                            PaddingValues(
                                start = 8.dp + this.calculateStartPadding(layoutDirection),
                                top = 8.dp + this.calculateTopPadding(),
                                end = 8.dp + this.calculateEndPadding(layoutDirection),
                                bottom = 8.dp + this.calculateBottomPadding()
                            )
                        },
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        state = appListState
                    ) {
                        items(items = filteredApps, key = { it.info.packageName }) { app ->
                            AppItem(
                                info = app,
                                isForTasker = isForTasker,
                                selectionCallback = {
                                    onItemSelected(it)

                                },
                                progressCallback = {
                                    MainModel.progress.emit(it)
                                },
                                extractCallback = {
                                    extractInfo = it
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 3.0.dp,
                        tonalElevation = 3.0.dp
                    ) {
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
