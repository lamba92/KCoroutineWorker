package it.lamba.utils

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * A worker that [execute] always the same task until [stop]ped. Multiple methods
 * called during the worker's lifecicle are available for overriding.
 * @param context The coroutine context where to execute the job of the worker
 */
abstract class AbstractCoroutineWorker(private val context: CoroutineContext = Dispatchers.Default) {

    private lateinit var currentJob: Job
    val isActive = if (::currentJob.isInitialized) currentJob.isActive else false

    /**
     * Starts the worker.
     * @param await Blocks the current execution until the worker stops.
     */
    fun start(await: Boolean = false) {
        currentJob = GlobalScope.launch(context) {
            preStartExecution()
            while (isActive)
                execute()
        }
        if (await) {
            runBlocking { currentJob.join() }
        }
    }

    /**
     * The cyclically called method where the workers job is executes.
     */
    protected abstract suspend fun execute()

    /**
     * Signals the worker to stop.
     * @param await Blocks the current execution until the worker stops.
     */
    fun stop(await: Boolean = false) = runBlocking {
        if (isActive) {
            preCancellationExecution()
            currentJob.cancel()
            postCancellationExecution()
            if (await) {
                runBlocking { currentJob.join() }
            }
        }
    }

    /**
     * Resets the worker.
     * @param await Blocks the current execution until the worker restart.
     */
    fun reset(await: Boolean = false){
        if(await)
            GlobalScope.launch {
                stop(true)
                onReset()
                start()
            }
        else {
            stop(true)
            onReset()
            start()
        }
    }

    /**
     * Waits until the worker finishes without interrupting it.
     */
    fun join(){
        if (::currentJob.isInitialized)
            runBlocking { currentJob.join() }
    }

    /**
     * Called when restarting the worker in [reset].
     */
    protected open fun onReset() {}

    /**
     * Called as soon as the the worker's job is offloaded into a coroutine. It is executed once.
     */
    open fun preStartExecution() {}

    /**
     * Called after the worker's job received the cancellation signal.
     */
    open fun postCancellationExecution() {}

    /**
     * Called before the worker's job receives the cancellation signal.
     */
    open fun preCancellationExecution() {}
}