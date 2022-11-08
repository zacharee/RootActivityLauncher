package tk.zwander.rootactivitylauncher.views.components

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.parcelize.Parcelize
import tk.zwander.rootactivitylauncher.R
import tk.zwander.rootactivitylauncher.util.launchUrl

@Parcelize
private data class MenuItem(
    val url: String,
    @StringRes val nameRes: Int,
) : Parcelable

@Composable
fun Menu(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items = remember {
        listOf(
            MenuItem(
                "https://twitter.com/Wander1236",
                R.string.twitter
            ),
            MenuItem(
                "https://zwander.dev",
                R.string.website
            ),
            MenuItem(
                "https://github.com/zacharee",
                R.string.github
            ),
            MenuItem(
                "mailto:zachary@zwander.dev?subject=${
                    context.resources.getString(R.string.app_name)
                        .replace(" ", "%20")
                }",
                R.string.email
            ),
            MenuItem(
                "https://bit.ly/ZachareeTG",
                R.string.telegram
            ),
            MenuItem(
                "https://bit.ly/zwanderDiscord",
                R.string.discord
            ),
            MenuItem(
                "https://bit.ly/zwanderPatreon",
                R.string.patreon
            )
        )
    }

    var showingMenu by remember {
        mutableStateOf(false)
    }
    var showingSupportersDialog by remember {
        mutableStateOf(false)
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = {
                showingMenu = true
            }
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(id = R.string.menu)
            )
        }

        DropdownMenu(
            expanded = showingMenu,
            onDismissRequest = { showingMenu = false },
            offset = DpOffset(16.dp, 0.dp),
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(id = item.nameRes))
                    },
                    onClick = {
                        context.launchUrl(item.url)
                    }
                )
            }

            DropdownMenuItem(
                text = {
                    Text(text = stringResource(id = R.string.supporters))
                },
                onClick = {
                    showingSupportersDialog = true
                }
            )
        }
    }

    PatreonSupportersDialog(showing = showingSupportersDialog) {
        showingSupportersDialog = false
    }
}
