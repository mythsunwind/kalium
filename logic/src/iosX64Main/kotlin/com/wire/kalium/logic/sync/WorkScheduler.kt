package com.wire.kalium.logic.sync

import com.wire.kalium.logic.feature.UserSessionScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import platform.UIKit.UIApplication

class WorkerFactory(private val userSessionScope: UserSessionScope) {

    fun createWorker(work: KClass<out UserSessionWorker>): UserSessionWorker {
        return when (work.qualifiedName) {
            "com.wire.kalium.logic.sync.SlowSyncWorker" -> SlowSyncWorker(userSessionScope)
            else -> throw RuntimeException("Unknown worker: ${work.qualifiedName}")
        }
    }

}

actual class WorkScheduler(private val userSessionScope: UserSessionScope) {

    val workerFactory = WorkerFactory(userSessionScope)

    actual fun schedule(work: KClass<out UserSessionWorker>, name: String) {
        val worker = workerFactory.createWorker(work)

        GlobalScope.launch {
            val taskID = UIApplication.sharedApplication.beginBackgroundTaskWithName(name) {}
            worker.doWork()
            UIApplication.sharedApplication.endBackgroundTask(taskID)
        }
    }

}
