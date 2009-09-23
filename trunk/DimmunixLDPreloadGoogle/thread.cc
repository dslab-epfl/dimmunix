#include "thread.h"
#include "dlock.h"


namespace dlock {

Thread::Thread() : zombie(false), bypass_avoidance(false), fLastLockPos(0) {
	pthread_cond_init(&fYieldCond, 0);
	pthread_mutex_init(&fYieldMtx, 0);
	/* disable avoidance for the yield lock */
	dlock_mutex_disable_avoidance(&fYieldMtx);
//	pthread_cleanup_push(Thread::dlock_thread_cleanup, this);
	positions_granted.reserve(1000);
}

Thread::~Thread() {
	pthread_mutex_destroy(&fYieldMtx);
	pthread_cond_destroy(&fYieldCond);
}

/* thread waits (blocks) if it has something in its yield_cause */
void Thread::yield_wait() {
	clock_gettime(CLOCK_REALTIME, &ts_wait);
	pthread_mutex_lock(&fYieldMtx);				/* native_lock(yieldLock[t]) */
	while (!yield_cause.empty() && !bypass_avoidance)	/* if yieldCause[t] != null */
		pthread_cond_wait(&fYieldCond, &fYieldMtx);	/* yieldLock[t].wait */
	pthread_mutex_unlock(&fYieldMtx);			/* native_unlock(yieldLock[t]) */
}

/* if yield_cause is not empty wakes up this thread */
void Thread::yield_notify() {
	if (!yield_cause.empty()) {
		pthread_mutex_lock(&fYieldMtx);		/* native_lock(yieldLock[t']) */
		yield_cause.clear();			/* yieldCause[t'] = null */
		pthread_cond_signal(&fYieldCond);	/* yieldLock[t'].notify */
		DLOCK_DEBUGF("%p notifies\n", pthread_self());
		pthread_mutex_unlock(&fYieldMtx);	/* native_unlock(yieldLock[t']) */
	}
}


}; // namespace dlock

