package tk.zwander.rootactivitylauncher.views.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import tk.zwander.rootactivitylauncher.R

@Composable
fun Theme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isAndroid12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorPrimary = colorResource(R.color.colorPrimary)
    val colorPrimaryDark = colorResource(R.color.colorPrimaryDark)
    val colorAccent = colorResource(R.color.colorAccent)

    MaterialTheme(
        colorScheme = if (dark) {
            if (isAndroid12) dynamicDarkColorScheme(context) else darkColorScheme(
                primary = colorPrimary,
                secondary = colorAccent,
                tertiary = colorPrimaryDark,
            )
        } else {
            if (isAndroid12) dynamicLightColorScheme(context) else lightColorScheme(
                primary = colorPrimary,
                secondary = colorAccent,
                tertiary = colorPrimaryDark,
            )
        },
        content = content,
    )
}
