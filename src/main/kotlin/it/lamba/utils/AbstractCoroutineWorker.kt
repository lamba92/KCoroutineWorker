package it.lamba.utils

import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * A worker that [execute] always the same task until [stop]ped. Multiple methods
 * called during the worker's lifecicle are available for overriding.
 * @param context The coroutine context where to execute the job of the worker
 */
abstract class AbstractCoroutineWorker(private val context: CoroutineContext = Dispatchers.Default) {

    private lateinit var currentJob: Job

    val isActive = if (::currentJob.isInitialized) currentJob.isActive else false

    protected val logger = KotlinLogging.logger(this.javaClass.simpleName)

    /**
     * Minimum time between executions of [executeMaintenance].
     */
    private var timeBetweenMaintenance = 500L

    private var lastMaintenance: Long = 0
    private val lapTime: Long
        get() = System.currentTimeMillis() - lastMaintenance

    /**
     * Alternative setter method for [timeBetweenMaintenance].
     */
    fun setTimeBetweenMaintenance(time: Long, unit: TimeUnit){
        timeBetweenMaintenance = TimeUnit.MILLISECONDS.convert(time, unit)
    }


    /**
     * Starts the worker.
     * @param await Blocks the current execution until the worker stops.
     */
    fun start(await: Boolean = false) {
        logger.debug { "Starting..." }
        currentJob = GlobalScope.launch(context) {
            preStartExecution()
            lastMaintenance = System.currentTimeMillis()
            while (isActive) {
                execute()
                if (lapTime >= timeBetweenMaintenance) {
                    logger.debug { "lapTime ($lapTime) >= timeBetweenMaintenance ($timeBetweenMaintenance) | Executing maintenance..." }
                    executeMaintenance()
                    lastMaintenance = System.currentTimeMillis()
                }
            }
        }
        if (await) {
            runBlocking { currentJob.join() }
        }
    }

    /**
     * Called during the maintenance of this worker. This method is called
     * every [timeBetweenMaintenance] seconds. Use [setTimeBetweenMaintenance]
     * to modify the interval.
     */
    open suspend fun executeMaintenance() {}

    /**
     * The cyclically called method where the workers job is executes.
     */
    protected abstract suspend fun execute()

    /**
     * Signals the worker to stop.
     * @param await Blocks the current execution until the worker stops.
     */
    fun stop(await: Boolean = false) = runBlocking {
        logger.debug { "Stopping..." }
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
        logger.debug { "Resetting..." }
        if(!await)
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