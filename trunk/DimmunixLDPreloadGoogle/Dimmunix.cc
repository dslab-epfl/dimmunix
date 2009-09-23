/*
 * Dimmunix.cc
 *
 *  Created on: Sep 18, 2009
 *      Author: horatiu
 */

#include "Dimmunix.h"

int (*real_pthread_mutex_lock) (pthread_mutex_t *lock) = NULL;
int (*real_pthread_mutex_trylock) (pthread_mutex_t *lock) = NULL;
int (*real_pthread_mutex_unlock) (pthread_mutex_t *lock) = NULL;

ULock globalLock;

volatile bool exiting = false;
volatile bool avoidanceInitialized = false;
volatile bool detectionInitialized = false;

Dimmunix dimmunix;

Dimmunix::Dimmunix() {
	globalLock.lock();
	init();
	globalLock.unlock();
}

Dimmunix::~Dimmunix() {
	exiting = true;
	delete[] threadsInfo;
}

void Dimmunix::init() {
	real_pthread_mutex_lock = (int (*)(pthread_mutex_t*))dlsym(RTLD_NEXT, "pthread_mutex_lock");
	real_pthread_mutex_trylock = (int (*)(pthread_mutex_t*))dlsym(RTLD_NEXT, "pthread_mutex_trylock");
	real_pthread_mutex_unlock = (int (*)(pthread_mutex_t*))dlsym(RTLD_NEXT, "pthread_mutex_unlock");

	threadsInfo = new ThreadInfo[NTHREADS];
	nthreads = NTHREADS;

	initialized = true;
}

inline int gettid() {
	return syscall(__NR_gettid);
}

/* return current thread dlock_thread_t pointer */
static dlock_thread_t* current_dlock_thread() {
	dlock_thread_t* &dt = dimmunix.threadsInfo[gettid()].dt;
	if (dt == NULL) {
		dt = dlock_thread_create();
	}
	return dt;
}

/* #################################################################### */

int pthread_mutex_lock(pthread_mutex_t *m) {
	if (!dimmunix.initialized) {
		globalLock.lock();
		if (!dimmunix.initialized)
			dimmunix.init();
		globalLock.unlock();
		return real_pthread_mutex_lock(m);
	}
	if (exiting || !detectionInitialized || !avoidanceInitialized)
		return real_pthread_mutex_lock(m);

	if (dimmunix.threadsInfo[gettid()].inDimmunix)
		return real_pthread_mutex_lock(m);
	dimmunix.threadsInfo[gettid()].inDimmunix = true;

	dlock_thread_t* dt = current_dlock_thread();
	dlock_mutex_t* dm = dlock_mutex(m);
	dlock_acquire(dt, dm, 0);
	int r = real_pthread_mutex_lock(m);
	if (r == 0)
		dlock_acquired(dt, dm);

	dimmunix.threadsInfo[gettid()].inDimmunix = false;
	return r;
}

int pthread_mutex_trylock(pthread_mutex_t *m) {
	if (!dimmunix.initialized) {
		globalLock.lock();
		if (!dimmunix.initialized)
			dimmunix.init();
		globalLock.unlock();
		return real_pthread_mutex_trylock(m);
	}
	if (exiting || !detectionInitialized || !avoidanceInitialized)
		return real_pthread_mutex_trylock(m);

	if (dimmunix.threadsInfo[gettid()].inDimmunix)
		return real_pthread_mutex_trylock(m);
	dimmunix.threadsInfo[gettid()].inDimmunix = true;

	dlock_thread_t* dt = current_dlock_thread();
	dlock_mutex_t* dm = dlock_mutex(m);
	dlock_acquire(dt, dm, 1);
	int r = real_pthread_mutex_trylock(m);
	if (r == 0)
		dlock_acquired(dt, dm);
	else
		dlock_mutex_contention(dm);

	dimmunix.threadsInfo[gettid()].inDimmunix = false;
	return r;
}

int pthread_mutex_unlock(pthread_mutex_t *m) {
	if (!dimmunix.initialized) {
		globalLock.lock();
		if (!dimmunix.initialized)
			dimmunix.init();
		globalLock.unlock();
		return real_pthread_mutex_unlock(m);
	}
	if (exiting || !detectionInitialized || !avoidanceInitialized)
		return real_pthread_mutex_unlock(m);

	if (dimmunix.threadsInfo[gettid()].inDimmunix)
		return real_pthread_mutex_unlock(m);
	dimmunix.threadsInfo[gettid()].inDimmunix = true;

	dlock_thread_t* dt = current_dlock_thread();
	dlock_mutex_t* dm = dlock_mutex(m);
	int r = real_pthread_mutex_unlock(m);
	if (r == 0)
		dlock_release(dt, dm);

	dimmunix.threadsInfo[gettid()].inDimmunix = false;
	return r;
}


/* #################################################################### */

dlock_mutex_t* dlock_mutex(pthread_mutex_t* pmtx) {
	globalLock.lock();

	dlock_mutex_t* &m = dimmunix.locksMap[pmtx];
	if (m == NULL)
		m = dlock_mutex_create(pmtx);

	globalLock.unlock();

	return m;
}

