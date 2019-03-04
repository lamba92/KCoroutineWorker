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

    protected fun isActive() = if (::currentJob.isInitialized) currentJob.isActive else false

    val status: WorkerStatus
        get() = WorkerStatus(isActive())

    data class WorkerStatus(val isActive: Boolean)

    protected val logger = KotlinLogging.logger(this.javaClass.simpleName)
    private var forceNextMaintenance = false

    fun triggerMaintenance() {
        forceNextMaintenance = true
    }

    private var timeBetweenMaintenances = 60000L
    private var timeBetweenExecutions = 1000L

    var shouldExecuteMaintenance = false

    private var lastMaintenance: Long = 0
    private val lapTime: Long
        get() = System.currentTimeMillis() - lastMaintenance

    /**
     * Setter method for [timeBetweenMaintenances].
     */
    fun setTimeBetweenMaintenances(time: Long, unit: TimeUnit) {
        timeBetweenMaintenances = TimeUnit.MILLISECONDS.convert(time, unit)
    }

    /**
     * Setter method for [timeBetweenExecutions].
     */
    fun setTimeBetweenExecutions(time: Long, unit: TimeUnit) {
        timeBetweenExecutions = TimeUnit.MILLISECONDS.convert(time, unit)
    }


    /**
     * Starts the worker.
     * @param await Blocks the current execution until the worker stops.
     */
    suspend fun start(await: Boolean = false) {
        if(isActive())
            return
        logger.debug { "Starting..." }
        currentJob = GlobalScope.launch(context) {
            onStart()
            lastMaintenance = System.currentTimeMillis()
            while (isActive) {
                logger.debug { "Starting execution..." }
                try {
                    execute()
                    checkMaintenance()
                    delay(timeBetweenExecutions)
                } catch (e: CancellationException) {
                }
            }
            logger.debug { "Exiting main cycle" }
            checkMaintenance()
            onPostStop()
        }
        if (await) {
            currentJob.join()
        }
    }

    private suspend fun checkMaintenance() {
        logger.debug { "Checking if maintenance is needed..." }
        logger.debug { "forceNextMaintenance = $forceNextMaintenance | shouldExecuteMaintenance = $shouldExecuteMaintenance | lapTime >= timeBetweenMaintenances = ${lapTime >= timeBetweenMaintenances}" }
        if (forceNextMaintenance || (shouldExecuteMaintenance && lapTime >= timeBetweenMaintenances)) {
            logger.debug { "Maintenance needed. | Executing maintenance..." }
            try {
                executeMaintenance()
            } catch (e: Throwable) {
                logger.error(e) { "Maintenance interrupted due to error" }
            }
            lastMaintenance = System.currentTimeMillis()
            forceNextMaintenance = false
        } else logger.debug { "Maintenance not needed" }
    }

    /**
     * Called during the maintenance of this worker. This method is called
     * every [timeBetweenMaintenances] seconds. Use [setTimeBetweenMaintenances]
     * to modify the interval.
     */
    protected open suspend fun executeMaintenance() {}

    /**
     * The cyclically called method where the workers job is executes.
     */
    protected abstract suspend fun execute()

    /**
     * Signals the worker to stop.
     * @param wait Blocks the current execution until the worker stops.
     */
    suspend fun stop(wait: Boolean = false) {
        if (isActive()) {
            onStop()
            logger.debug { "Stopping..." }
            currentJob.cancel()
            if (wait)
                currentJob.join()
        }
    }

    /**
     * Resets the worker callin [onReset].
     * @param wait Blocks the current execution until the worker restarts.
     */
    suspend fun reset(wait: Boolean = false) {
        logger.debug { "Resetting..." }
        if (!wait)
            GlobalScope.launch {
                stop(true)
                onReset()
                start()
            }
        else {
            if (isActive())
                stop(true)
            onReset()
            start()
        }
    }

    /**
     * Restart the worker.
     * @param wait Blocks the current execution until the worker restarts.
     */
    suspend fun restart(wait: Boolean = false) {
        logger.debug { "Resetting..." }
        if (!wait)
            GlobalScope.launch {
                stop(true)
                start()
            }
        else {
            stop(true)
            start()
        }
    }

    /**
     * Waits until the worker finishes without interrupting it.
     */
    suspend fun join() {
        if (::currentJob.isInitialized)
            currentJob.join()
    }

    /**
     * Called when restarting the worker in [reset].
     */
    protected open fun onReset() {}

    /**
     * Called as soon as the the worker's job is offloaded into a coroutine. It is executed once.
     */
    protected open suspend fun onStart() {}

    /**
     * Called before the worker's job receives the cancellation signal.
     */
    protected open fun onStop() {}

    protected open suspend fun onPostStop() {}
}
