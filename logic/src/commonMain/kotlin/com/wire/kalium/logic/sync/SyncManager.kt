package com.wire.kalium.logic.sync

import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.sync.SyncRepository
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.kaliumLogger
import com.wire.kalium.logic.sync.event.EventProcessor
import com.wire.kalium.util.KaliumDispatcher
import com.wire.kalium.util.KaliumDispatcherImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface SyncManager {
    fun onSlowSyncComplete()

    /**
     * Suspends the caller until all pending events are processed,
     * and the client has finished processing all pending events.
     *
     * Suitable for operations where the user is required to be online
     * and without any pending events to be processed, for maximum sync.
     * @see startSyncIfIdle
     * @see waitUntilSlowSyncCompletion
     */
    @Deprecated(
        message = "SyncManager won't serve as a Sync Utils anymore",
        ReplaceWith(
            expression = "SyncRepository.syncState.first { it is SyncState.Live }",
            imports = arrayOf(
                "com.wire.kalium.logic.data.sync.SyncRepository",
                "com.wire.kalium.logic.data.sync.SyncState"
            ),
        )
    )
    suspend fun waitUntilLive()

    /**
     * Suspends the caller until at least basic data is processed,
     * even though Sync will run on a Job of its own.
     *
     * Suitable for operations where the user can be offline, but at least some basic post-login sync is done.
     * @see startSyncIfIdle
     * @see waitUntilLive
     */
    @Deprecated(
        message = "SyncManager won't serve as a Sync Utils anymore",
        ReplaceWith(
            expression = "SyncRepository.syncState.first { it in setOf(SyncState.GatheringPendingEvents, SyncState.Live) }",
            imports = arrayOf(
                "com.wire.kalium.logic.data.sync.SyncRepository",
                "com.wire.kalium.logic.data.sync.SyncState"
            ),
        )
    )
    suspend fun waitUntilSlowSyncCompletion()

    /**
     * ### Deprecated
     * NO-OP. Doesn't do anything. Sync will start automatically once conditions are met:
     * - Client is Registered
     * - There's internet connection
     *
     * ### Original docs
     * Will run in a parallel job without waiting for completion.
     *
     * Suitable for operations that the user can perform even while offline.
     * @see waitUntilLive
     * @see waitUntilSlowSyncCompletion
     */
    @Deprecated("Sync can't be forced to start. It will be started automatically once conditions are met")
    fun startSyncIfIdle()
    suspend fun isSlowSyncOngoing(): Boolean
    suspend fun isSlowSyncCompleted(): Boolean
    fun onSlowSyncFailure(cause: CoreFailure): SyncState
}

@Suppress("LongParameterList") //Can't take them out right now. Maybe we can extract an `EventProcessor` on a future PR
internal class SyncManagerImpl(
    private val userSessionWorkScheduler: UserSessionWorkScheduler,
    private val syncRepository: SyncRepository,
    private val eventProcessor: EventProcessor,
    private val eventGatherer: EventGatherer,
    private val syncCriteriaObserver: SyncCriteriaObserver,
    kaliumDispatcher: KaliumDispatcher = KaliumDispatcherImpl
) : SyncManager {

    /**
     * A dispatcher with limited parallelism of 1.
     * This means using this dispatcher only a single coroutine will be processed at a time.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val eventProcessingDispatcher = kaliumDispatcher.default.limitedParallelism(1)

    /**
     * A [SupervisorJob] that will serve as parent to the [processingJob].
     * This way, [processingJob] can fail or be cancelled and another can be put in its place.
     */
    private val processingSupervisorJob = SupervisorJob()

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        when (throwable) {
            is CancellationException -> {
                kaliumLogger.i("Sync job was cancelled")
                syncRepository.updateSyncState { SyncState.Waiting }
            }

            is KaliumSyncException -> {
                kaliumLogger.i("SyncException during events processing", throwable)
                syncRepository.updateSyncState { SyncState.Failed(throwable.coreFailureCause) }
            }

            else -> {
                kaliumLogger.i("Sync job failed due to unknown reason", throwable)
                syncRepository.updateSyncState { SyncState.Failed(CoreFailure.Unknown(throwable)) }
            }
        }
    }

    private val syncScope = CoroutineScope(processingSupervisorJob + kaliumDispatcher.io)

    init {
        syncScope.launch {
            syncCriteriaObserver.observeStartCriteria().collect {
                if(it is SyncStartCriteriaResolution.Ready){
                    // START SYNC
                    startSyncIfNotYetStarted()
                }else{
                    // STOP SYNC
                    TODO("Stop?!")
                }
            }
        }
    }

    /**
     * The scope in which the processing of events run.
     * All coroutines will have limited parallelism, as this scope uses [eventProcessingDispatcher].
     * All coroutines will have [processingSupervisorJob] as their parent.
     * @see eventProcessingDispatcher
     * @see processingSupervisorJob
     */
    private val eventProcessingScope = CoroutineScope(processingSupervisorJob + eventProcessingDispatcher + coroutineExceptionHandler)
    private var processingJob: Job? = null

    override fun onSlowSyncComplete() {
        // Processing already running, don't launch another
        kaliumLogger.d("SyncManager.onSlowSyncComplete called")
        val isRunning = processingJob?.isActive ?: false
        if (isRunning) {
            kaliumLogger.d("SyncManager.processingJob still active. Sync won't keep going")
            return
        }
        processingJob?.cancel(null)

        syncRepository.updateSyncState { SyncState.GatheringPendingEvents }

        processingJob = eventProcessingScope.launch { gatherAndProcessEvents() }
    }

    private suspend fun gatherAndProcessEvents() = eventGatherer.gatherEvents().collect {
        eventProcessor.processEvent(it)
    }

    override fun onSlowSyncFailure(cause: CoreFailure) = syncRepository.updateSyncState { SyncState.Failed(cause) }

    @Deprecated(
        "SyncManager won't serve as a Sync Utils anymore",
        replaceWith = ReplaceWith(
            "SyncRepository.syncState.first { it is SyncState.Live }",
            "com.wire.kalium.logic.data.sync.SyncRepository",
            "com.wire.kalium.logic.data.sync.SyncState"
        )
    )
    override suspend fun waitUntilLive() {
        syncRepository.syncState.first { it == SyncState.Live }
    }

    @Deprecated(
        "SyncManager won't serve as a Sync Utils anymore",
        replaceWith = ReplaceWith(
            "SyncRepository.syncState.first { it in setOf(SyncState.GatheringPendingEvents, SyncState.Live) }",
            "com.wire.kalium.logic.data.sync.SyncRepository",
            "com.wire.kalium.logic.data.sync.SyncState"
        )
    )
    override suspend fun waitUntilSlowSyncCompletion() {
        syncRepository.syncState.first { it in setOf(SyncState.GatheringPendingEvents, SyncState.Live) }
    }

    @Deprecated("Sync can't be forced to start. It will be started automatically once conditions are met")
    override fun startSyncIfIdle() {
        /** NO-OP **/
    }

    private suspend fun startSyncIfNotYetStarted() {
        syncRepository.updateSyncState {
            when (it) {
                SyncState.Waiting, is SyncState.Failed -> {
                    userSessionWorkScheduler.enqueueSlowSyncIfNeeded()
                    SyncState.SlowSync
                }

                else -> it
            }
        }
    }

    override suspend fun isSlowSyncOngoing(): Boolean = syncRepository.syncState.first() == SyncState.SlowSync
    override suspend fun isSlowSyncCompleted(): Boolean =
        syncRepository.syncState.first() in setOf(SyncState.GatheringPendingEvents, SyncState.Live)
}
