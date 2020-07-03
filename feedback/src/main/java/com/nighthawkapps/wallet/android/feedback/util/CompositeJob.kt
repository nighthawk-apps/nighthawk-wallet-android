package com.nighthawkapps.wallet.android.feedback.util

import kotlinx.coroutines.Job

class CompositeJob {

    private val activeJobs = mutableListOf<Job>()
    val size: Int get() = activeJobs.size

    fun add(job: Job) {
        activeJobs.add(job)
        job.invokeOnCompletion {
            remove(job)
        }
    }

    fun remove(job: Job): Boolean {
        return activeJobs.remove(job)
    }

    fun isActive(): Boolean {
        return activeJobs.any { isActive() }
    }

    suspend fun await() {
        // allow for concurrent modification since the list isn't coroutine or thread safe
        do {
            val job = activeJobs.firstOrNull()
            if (job?.isActive == true) {
                job.join()
            } else {
                // prevents an infinite loop in the extreme edge case where the list has a null item
                try {
                    activeJobs.remove(job)
                } catch (t: Throwable) {
                }
            }
        } while (size > 0)
    }

    fun cancel() {
        activeJobs.filter { isActive() }.forEach { it.cancel() }
    }

    operator fun plusAssign(also: Job) {
        add(also)
    }
}