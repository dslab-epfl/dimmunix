#include <dlfcn.h>
#include <pthread.h>
#include "dlock.h"
#include "uthash.h"
#include "util.h"
#include <linux/unistd.h>
#include "Dimmunix.h"
#include "ulock.h"

typedef struct {
		int key; /* pthread_mutex_t* */
		dlock_mutex_t* dm;
		UT_hash_handle hh;
} aux_mutex_t;


pthread_key_t pkey;
pthread_rwlock_t hlock;
aux_mutex_t* aux_mutexes = NULL;

static bool inDimmunix[100000];
static Dimmunix dimmunix;
volatile bool exiting;
ULock initLock;

int (*real_pthread_mutex_lock) (pthread_mutex_t *__mutex) = NULL;
int (*real_pthread_mutex_trylock) (pthread_mutex_t *__mutex) = NULL;
int (*real_pthread_mutex_unlock) (pthread_mutex_t *__mutex) = NULL;

inline int gettid() {
	return syscall(__NR_gettid);
}

/* return current thread dlock_thread_t pointer */
static dlock_thread_t* current_dlock_thread() {
	dlock_thread_t* dt = (dlock_thread_t*)pthread_getspecific(pkey);
	if (dt == NULL) {
		dt = dlock_thread_create();
		pthread_setspecific(pkey, dt);
	}
	return dt;
}

/* #################################################################### */

int pthread_mutex_lock(pthread_mutex_t *m) {
	if (!dimmunix.initialized) {
		initLock.lock();
		if (!dimmunix.initialized)
			dimmunix.init();
		initLock.unlock();
		return real_pthread_mutex_lock(m);
	}
	if (exiting)
		return real_pthread_mutex_lock(m);

	if (inDimmunix[gettid()])
		return real_pthread_mutex_lock(m);
	inDimmunix[gettid()] = true;

	printf_nonblocking("lock\n");
	dlock_thread_t* dt = current_dlock_thread();
	dlock_mutex_t* dm = dlock_mutex(m);
	dlock_acquire(dt, dm, 0);
	int r = real_pthread_mutex_lock(m);
	if (r == 0)
		dlock_acquired(dt, dm);

	inDimmunix[gettid()] = false;
	return r;
}

int pthread_mutex_trylock(pthread_mutex_t *m) {
	if (!dimmunix.initialized) {
		initLock.lock();
		if (!dimmunix.initialized)
			dimmunix.init();
		initLock.unlock();
		return real_pthread_mutex_trylock(m);
	}
	if (exiting)
		return real_pthread_mutex_trylock(m);

	if (inDimmunix[gettid()])
		return real_pthread_mutex_trylock(m);
	inDimmunix[gettid()] = true;

	dlock_thread_t* dt = current_dlock_thread();
	dlock_mutex_t* dm = dlock_mutex(m);
	dlock_acquire(dt, dm, 1);
	int r = real_pthread_mutex_trylock(m);
	if (r == 0)
		dlock_acquired(dt, dm);
	else
		dlock_mutex_contention(dm);

	inDimmunix[gettid()] = false;
	return r;
}

int pthread_mutex_unlock(pthread_mutex_t *m) {
	if (!dimmunix.initialized) {
		initLock.lock();
		if (!dimmunix.initialized)
			dimmunix.init();
		initLock.unlock();
		return real_pthread_mutex_unlock(m);
	}
	if (exiting)
		return real_pthread_mutex_unlock(m);

	if (inDimmunix[gettid()])
		return real_pthread_mutex_unlock(m);
	inDimmunix[gettid()] = true;

	dlock_thread_t* dt = current_dlock_thread();
	dlock_mutex_t* dm = dlock_mutex(m);
	int r = real_pthread_mutex_trylock(m);
	if (r == 0)
		dlock_release(dt, dm);

	inDimmunix[gettid()] = false;
	return r;
}

/* #################################################################### */

dlock_mutex_t* dlock_mutex(pthread_mutex_t* pmtx) {
	int mkey = (int) pmtx;
	aux_mutex_t* am = NULL;
	pthread_rwlock_rdlock(&hlock);
	HASH_FIND_INT(aux_mutexes, &mkey, am);
	pthread_rwlock_unlock(&hlock);
	if (am == NULL) {
		/* not found, insert it */
		pthread_rwlock_wrlock(&hlock);
		am = (aux_mutex_t*)malloc(sizeof(aux_mutex_t));
		am->key = mkey;
		am->dm = dlock_mutex_create(pmtx);
		HASH_ADD_INT(aux_mutexes, key, am);
		pthread_rwlock_unlock(&hlock);
	}
	return am->dm;
}

//dlock_thread_t* dlock_thread(pthread_t* pthr) { }

//void dlock_set_thread(pthread_t* pthr, dlock_thread_t* thr) { }

