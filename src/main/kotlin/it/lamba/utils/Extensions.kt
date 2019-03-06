package it.lamba.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

suspend fun Iterable<AbstractCoroutineWorker>.stopAll(wait: Boolean = false) =
    map { GlobalScope.launch { it.stop(wait) } }.takeIf { wait }?.forEach { it.join() } ?: Unit

suspend fun Iterable<AbstractCoroutineWorker>.startAll(wait: Boolean = false) =
    map { GlobalScope.launch { it.start(wait) } }.takeIf { wait }?.forEach { it.join() } ?: Unit

suspend fun Iterable<AbstractCoroutineWorker>.resetAll(wait: Boolean = false) =
    map { GlobalScope.launch { it.reset(wait) } }.takeIf { wait }?.forEach { it.join() } ?: Unit

suspend fun Iterable<AbstractCoroutineWorker>.restartAll(wait: Boolean = false) =
    map { GlobalScope.launch { it.restart(wait) } }.takeIf { wait }?.forEach { it.join() } ?: Unit

suspend fun Array<AbstractCoroutineWorker>.stopAll(wait: Boolean = false) =
    map { GlobalScope.launch { it.stop(wait) } }.takeIf { wait }?.forEach { it.join() } ?: Unit

suspend fun Array<AbstractCoroutineWorker>.startAll(wait: Boolean = false) =
    map { GlobalScope.launch { it.start(wait) } }.takeIf { wait }?.forEach { it.join() } ?: Unit

suspend fun Array<AbstractCoroutineWorker>.resetAll(wait: Boolean = false) =
    map { GlobalScope.launch { it.reset(wait) } }.takeIf { wait }?.forEach { it.join() } ?: Unit

suspend fun Array<AbstractCoroutineWorker>.restartAll(wait: Boolean = false) =
    map { GlobalScope.launch { it.restart(wait) } }.takeIf { wait }?.forEach { it.join() } ?: Unit