package it.lamba.utils

import kotlin.system.getTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

actual fun currentTimeMillis() = getTimeMillis()

actual fun <T> runBlocking(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): T = kotlinx.coroutines.runBlocking(context, block)