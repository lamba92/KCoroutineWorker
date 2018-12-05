package it.lamba.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * An implementation of [AbstractCoroutineWorker] that adds a maintenance function and
 * keeps track of files created in particular paths.
 * @param allowedExtensions Extensions tracked by this worker.
 * @param paths The paths in which to watch for new files.
 * @param comparator When multiple files put in queue while executing they are ordered with this comparator.
 * @param context The coroutine context where to execute the job of the worker.
 */
abstract class FilesQueueWorker(
    allowedExtensions: Iterable<String>,
    paths: Iterable<Path>,
    context: CoroutineContext = Dispatchers.IO,
    comparator: Comparator<Path> = compareBy { it.toFile().lastModified() }
) : AbstractCoroutineWorker(context) {

    private val filesQueue = PriorityQueue(comparator)

    /**
     * Minimum time between executions of [executeMaintenance].
     */
    var timeBetweenMaintenance = 500L

    private var lastMaintenance: Long = 0
    private val lapTime: Long
        get() = System.currentTimeMillis() - lastMaintenance

    /**
     * Alternative setter method for [timeBetweenMaintenance].
     */
    fun setTimeBetweenMaintenance(time: Long, unit: TimeUnit){
        timeBetweenMaintenance = TimeUnit.MILLISECONDS.convert(time, unit)
    }

    private val watcher = KDirectoryWatcher {
        addPaths(paths)
        preExistingAsCreated = true
        addFilter { path ->
            path.toFile().let { file ->
                file.isFile && allowedExtensions.any { it == file.extension }
            }
        }
    }

    override fun preStartExecution() {
        watcher.start()
        lastMaintenance = System.currentTimeMillis()
    }

    override fun postCancellationExecution() {
        watcher.stop()
    }

    override suspend fun execute() {
        val path = filesQueue.poll()
        if (path != null) {
            execute(path)
        }
        if (lapTime >= timeBetweenMaintenance)
            executeMaintenance()
        if (path == null) {
            delay(5000)
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
     * @param nextPath The next [Path] from the queue.
     */
    abstract suspend fun execute(nextPath: Path)
}