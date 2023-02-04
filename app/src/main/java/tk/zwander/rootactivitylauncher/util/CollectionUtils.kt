package tk.zwander.rootactivitylauncher.util

import android.util.SparseArray
import androidx.core.util.forEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import tk.zwander.rootactivitylauncher.data.model.AppModel
import tk.zwander.rootactivitylauncher.data.model.BaseInfoModel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T, R> SparseArray<out T>.map(transform: (T) -> R): List<R> {
    return mapTo(ArrayList(), transform)
}

fun <T, R, C : MutableCollection<in R>> SparseArray<out T>.mapTo(destination: C, transform: (T) -> R): C {
    forEach { _, any ->
        destination.add(transform(any))
    }

    return destination
}

suspend fun <T> Collection<T>.forEachParallel(context: CoroutineContext = EmptyCoroutineContext, scope: CoroutineScope, block: suspend CoroutineScope.(T) -> Unit) {
    val jobs = ArrayList<Deferred<*>>(size)
    forEach {
        jobs.add(
            scope.async(context) {
                block(it)
            }
        )
    }
    jobs.awaitAll()
}

suspend fun <T> Array<T>.forEachParallel(context: CoroutineContext = EmptyCoroutineContext, scope: CoroutineScope, block: suspend CoroutineScope.(T) -> Unit) {
    val jobs = ArrayList<Deferred<*>>(size)
    forEach {
        jobs.add(
            scope.async(context) {
                block(it)
            }
        )
    }
    jobs.awaitAll()
}

fun Collection<BaseInfoModel>.distinctByPackageName(): List<BaseInfoModel> {
    return distinctBy {
        if (it is AppModel) it.info.packageName else "favorite"
    }
}
