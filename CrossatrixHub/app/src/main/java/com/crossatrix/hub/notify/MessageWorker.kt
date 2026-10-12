package com.crossatrix.hub.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.crossatrix.hub.data.MessageApi
import com.crossatrix.hub.data.SessionStore
import java.util.concurrent.TimeUnit

class MessageWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val session = SessionStore(ctx).load() ?: return Result.success()

        // If notifications are blocked, don't consume messages: try again next time.
        if (!MessageNotifier.canNotify(ctx)) return Result.success()

        val messages = MessageApi.fetch(session.user.id) ?: return Result.success()

        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet(KEY_LAST, emptySet()).orEmpty()
        val base = System.nanoTime().toInt()

        // One notification per message object that wasn't in the previous fetch.
        messages.filter { it.key !in previous }.forEachIndexed { i, msg ->
            MessageNotifier.show(ctx, msg, base + i)
        }

        prefs.edit().putStringSet(KEY_LAST, messages.map { it.key }.toSet()).apply()
        return Result.success()
    }

    companion object {
        private const val PREFS = "message_state"
        private const val KEY_LAST = "last_keys"

        fun clearState(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }
}

object MessageScheduler {
    private const val PERIODIC = "message_poll"
    private const val NOW = "message_check_now"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Checks immediately, and makes sure the 15-minute check is scheduled. */
    fun start(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MessageWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build(),
        )
        wm.enqueueUniqueWork(
            NOW,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MessageWorker>()
                .setConstraints(constraints)
                .build(),
        )
    }

    fun stop(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(PERIODIC)
        wm.cancelUniqueWork(NOW)
        MessageWorker.clearState(context)
    }
}
