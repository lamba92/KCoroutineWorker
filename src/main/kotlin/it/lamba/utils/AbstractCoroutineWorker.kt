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
abstract class AbstractCoroutineWorker(
    private val context: CoroutineContext = Dispatchers.Default,
    var shouldExecuteMaintenance: Boolean = false,
    var stopOnError: Boolean = false,
    var executeMaintenanceOnStop: Boolean = true
) {

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
    fun start() {
        if (isActive())
            return
        logger.debug { "Starting..." }
        currentJob = GlobalScope.launch(context) {
            onStart()
            lastMaintenance = System.currentTimeMillis()
            while (isActive) {
                logger.debug { "Starting execution..." }
                try {
                    withContext(NonCancellable) {
                        execute()
                        checkMaintenance()
                    }
                    delay(timeBetweenExecutions)
                } catch (e: CancellationException) {
                    logger.info { "Worker interrupted" }
                } catch (e: Throwable) {
                    logger.error(e) { "Worker execution encountered and error" }
                    if (stopOnError)
                        break
                }
            }
            logger.debug { "Exiting main cycle" }
            if (executeMaintenanceOnStop) withContext(NonCancellable) {
                executeMaintenance()
            }
            onPostStop()
        }
    }

    suspend fun startAndJoin() {
        start()
        join()
    }

    private suspend fun checkMaintenance() {
        logger.debug { "Checking if maintenance is needed..." }
        logger.debug { "forceNextMaintenance = $forceNextMaintenance | shouldExecuteMaintenance = $shouldExecuteMaintenance | lapTime >= timeBetweenMaintenances = ${lapTime >= timeBetweenMaintenances}" }
        if (forceNextMaintenance || (shouldExecuteMaintenance && lapTime >= timeBetweenMaintenances)) {
            logger.debug { "Maintenance needed. | Executing maintenance..." }
            executeMaintenance()
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
    fun stop() {
        if (isActive()) {
            GlobalScope.launch {
                onStop()
                logger.debug { "Stopping..." }
                currentJob.cancel()
            }
        }
    }

    suspend fun stopAndJoin() {
        stop()
        join()
    }

    /**
     * Resets the worker callin [onReset].
     * @param wait Blocks the current execution until the worker restarts.
     */
    fun reset() {
        GlobalScope.launch {
            logger.debug { "Restarting..." }
            stopAndJoin()
            onReset()
            start()
        }
    }

    suspend fun joinAndReset() {
        stopAndJoin()
        onReset()
        start()
    }

    /**
     * Restart the worker.
     * @param wait Blocks the current execution until the worker restarts.
     */
    fun restart() {
        GlobalScope.launch {
            logger.debug { "Resetting..." }
            stopAndJoin()
            start()
        }
    }

    suspend fun joinAndRestart() {
        stopAndJoin()
        start()
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
    protected open suspend fun onReset() {}

    /**
     * Called as soon as the the worker's job is offloaded into a coroutine. It is executed once.
     */
    protected open suspend fun onStart() {}

    /**
     * Called before the worker's job receives the cancellation signal.
     */
    protected open suspend fun onStop() {}

    protected open suspend fun onPostStop() {}
}
