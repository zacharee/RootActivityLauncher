package tk.zwander.rootactivitylauncher.views.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import tk.zwander.rootactivitylauncher.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchComponent(
    expanded: Boolean,
    query: String,
    onExpandChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(visible = expanded) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (query.isNotBlank()) {
                                onQueryChange("")
                            } else {
                                onExpandChange(false)
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_close_24),
                            contentDescription = stringResource(
                                id = if (query.isNotBlank()) {
                                    R.string.clear
                                } else {
                                    R.string.close
                                }
                            )
                        )
                    }
                }
            )
        }
        
        AnimatedVisibility(visible = !expanded) {
            IconButton(onClick = { onExpandChange(true) }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(id = R.string.search)
                )
            }
        }
    }
}
