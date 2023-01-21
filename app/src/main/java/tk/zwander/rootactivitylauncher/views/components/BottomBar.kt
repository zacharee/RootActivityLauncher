package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.data.model.BaseInfoModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomBar(
    isSearching: Boolean,
    useRegex: Boolean,
    includeComponents: Boolean,
    query: String?,
    progress: Float?,
    apps: List<BaseInfoModel>,
    appListState: LazyStaggeredGridState,
    onShowFilterDialog: () -> Unit,
    onUseRegexChanged: (Boolean) -> Unit,
    onIncludeComponentsChanged: (Boolean) -> Unit,
    onIsSearchingChanged: (Boolean) -> Unit,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.0.dp,
        tonalElevation = 3.0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = isSearching,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.widthIn(max = 600.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectableCard(
                                modifier = Modifier.weight(1f),
                                selected = useRegex,
                                onClick = { onUseRegexChanged(!useRegex) },
                                unselectedColor = Color.Transparent
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = stringResource(id = R.string.regex))
                                }
                            }

                            SelectableCard(
                                modifier = Modifier.weight(1f),
                                selected = includeComponents,
                                onClick = { onIncludeComponentsChanged(!includeComponents) },
                                unselectedColor = Color.Transparent
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = stringResource(id = R.string.include_components))
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchComponent(
                    expanded = isSearching,
                    query = query!!,
                    onExpandChange = onIsSearchingChanged,
                    onQueryChange = onQueryChanged,
                    modifier = Modifier.weight(1f)
                )

                AnimatedVisibility(visible = progress == null) {
                    IconButton(
                        onClick = {
                            onShowFilterDialog()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_filter_list_24),
                            contentDescription = stringResource(id = R.string.filter)
                        )
                    }
                }

                val firstIndex by remember {
                    derivedStateOf { appListState.firstVisibleItemIndex }
                }
                val lastIndex by remember {
                    derivedStateOf {
                        firstIndex + appListState.layoutInfo.visibleItemsInfo.size - 1
                    }
                }

                AnimatedVisibility(
                    visible = progress == null && firstIndex > 0
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (firstIndex > 20) {
                                    appListState.scrollToItem(0)
                                } else {
                                    appListState.animateScrollToItem(0)
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.scroll_to_top),
                            contentDescription = stringResource(id = R.string.scroll_top)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = progress == null && lastIndex < apps.size - 1
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (apps.size - 1 - lastIndex > 20) {
                                    appListState.scrollToItem(apps.size - 1)
                                } else {
                                    appListState.animateScrollToItem(apps.size - 1)
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.scroll_to_bottom),
                            contentDescription = stringResource(id = R.string.scroll_bottom)
                        )
                    }
                }

                Menu()
            }
        }
    }
}
