package tk.zwander.rootactivitylauncher.util

import androidx.compose.runtime.compositionLocalOf
import tk.zwander.rootactivitylauncher.data.model.FavoriteModel
import tk.zwander.rootactivitylauncher.data.model.MainModel

val LocalMainModel = compositionLocalOf<MainModel> { error("No MainModel specified!") }
val LocalFavoriteModel = compositionLocalOf<FavoriteModel> { error("No FavoriteModel specified!") }
