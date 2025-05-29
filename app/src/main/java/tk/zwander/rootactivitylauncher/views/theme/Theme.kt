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

@Composable
fun Theme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isAndroid12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    MaterialTheme(
        colorScheme = if (dark) {
            if (isAndroid12) dynamicDarkColorScheme(context) else darkColorScheme()
        } else {
            if (isAndroid12) dynamicLightColorScheme(context) else lightColorScheme()
        },
        content = content,
    )
}
