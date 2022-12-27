package tk.zwander.rootactivitylauncher.views.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.themeadapter.material3.Mdc3Theme

@Composable
fun Theme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MaterialTheme(
            colorScheme = if (dark) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            },
            content = content
        )
    } else {
        Mdc3Theme(
            content = content
        )
    }
}
