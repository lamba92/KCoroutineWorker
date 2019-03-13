package it.lamba.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

fun Iterable<AbstractCoroutineWorker>.stopAll() = forEach { it.stop() }

suspend fun Iterable<AbstractCoroutineWorker>.stopAndJoinAll()
        = asyncMap { it.stopAndJoin() }.forEach { it.join() }

fun Iterable<AbstractCoroutineWorker>.startAll() = forEach { it.start() }

suspend fun Iterable<AbstractCoroutineWorker>.startAndJoinAll()
        = asyncMap { it.startAndJoin() }.forEach { it.join() }

fun Iterable<AbstractCoroutineWorker>.resetAll() = forEach { it.reset() }

suspend fun Iterable<AbstractCoroutineWorker>.joinAllAndReset()
        = asyncMap { it.joinAndReset() }.forEach { it.join() }

fun Iterable<AbstractCoroutineWorker>.restartAll() = forEach { it.restart() }

suspend fun Iterable<AbstractCoroutineWorker>.joinAllAndResart()
        = asyncMap { it.joinAndRestart() }.forEach { it.join() }

fun <T, R> Iterable<T>.asyncMap(func: suspend (T) -> R)
        = map { GlobalScope.async { func(it) } }