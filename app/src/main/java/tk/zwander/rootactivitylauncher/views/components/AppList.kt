package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import tk.zwander.rootactivitylauncher.data.component.BaseComponentInfo
import tk.zwander.rootactivitylauncher.data.model.AppModel
import tk.zwander.rootactivitylauncher.data.model.BaseInfoModel
import tk.zwander.rootactivitylauncher.util.LocalFavoriteModel

@Composable
fun AppList(
    appListState: LazyStaggeredGridState,
    filteredApps: List<BaseInfoModel>,
    isForTasker: Boolean,
    onItemSelected: (BaseComponentInfo) -> Unit,
    extractCallback: (AppModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutDirection = LocalLayoutDirection.current
    val favoriteModel = LocalFavoriteModel.current
    val favoriteSize by favoriteModel.totalInitialSize.collectAsState()

    val actualFilteredApps = remember(favoriteSize, filteredApps.toList()) {
        if (favoriteSize == 0) filteredApps.filterNot { it == favoriteModel }
        else filteredApps
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(400.dp),
        modifier = modifier,
        contentPadding = WindowInsets.statusBars.asPaddingValues().run {
            PaddingValues(
                start = 8.dp + this.calculateStartPadding(layoutDirection),
                top = 8.dp + this.calculateTopPadding(),
                end = 8.dp + this.calculateEndPadding(layoutDirection),
                bottom = 8.dp + this.calculateBottomPadding()
            )
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        state = appListState,
    ) {
        items(items = actualFilteredApps, key = { if (it is AppModel) it.info.packageName else "favorite_item" }) { info ->
            AppItem(
                info = info,
                isForTasker = isForTasker,
                selectionCallback = {
                    onItemSelected(it)
                },
                extractCallback = extractCallback,
                modifier = Modifier.fillMaxWidth()
                    .animateItem(),
            )
        }
    }
}
