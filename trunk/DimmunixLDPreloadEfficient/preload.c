#include <dlfcn.h>
#include <pthread.h>
#include "dlock.h"
#include "uthash.h"
#include "util.h"
#include <linux/unistd.h>


// uthash struct
typedef struct {
		int key; /* pthread_mutex_t* */
		dlock_mutex_t* dm;
		UT_hash_handle hh;
} aux_mutex_t;


static pthread_once_t initializer = PTHREAD_ONCE_INIT;
static pthread_key_t pkey;
static pthread_rwlock_t hlock;
static aux_mutex_t* aux_mutexes = NULL;

// pthread func pointers
int (*real_pthread_mutex_lock) (pthread_mutex_t *__mutex) = NULL;
int (*real_pthread_mutex_trylock) (pthread_mutex_t *__mutex) = NULL;
int (*real_pthread_mutex_unlock) (pthread_mutex_t *__mutex) = NULL;

// called when the library exits
void preload_done() __attribute__((destructor));


/* pthread_once function */
void preload_init() {
	real_pthread_mutex_lock = dlsym(RTLD_NEXT, "pthread_mutex_lock");
	real_pthread_mutex_trylock = dlsym(RTLD_NEXT, "pthread_mutex_trylock");
	real_pthread_mutex_unlock = dlsym(RTLD_NEXT, "pthread_mutex_unlock");
	pthread_key_create(&pkey, NULL);
	pthread_rwlock_init(&hlock, NULL);
}

/* destructor function */
void preload_done() {
	pthread_rwlock_destroy(&hlock);
	pthread_key_delete(pkey);
}

/* return current thread dlock_thread_t pointer */
static dlock_thread_t* current_dlock_thread() {
	dlock_thread_t* dt = pthread_getspecific(pkey);
	if (dt == NULL) {
		dt = dlock_thread_create();
		pthread_setspecific(pkey, dt);
	}
	return dt;
}

/* return dlock_mutex associated with a pthread_mutex */
dlock_mutex_t* dlock_mutex(pthread_mutex_t* pmtx) {
	int mkey = (int) pmtx;
	aux_mutex_t* am = NULL;
	pthread_rwlock_rdlock(&hlock);
	HASH_FIND_INT(aux_mutexes, &mkey, am);
	pthread_rwlock_unlock(&hlock);
	if (am == NULL) {
		/* not found, insert it */
		pthread_rwlock_wrlock(&hlock);
		am = malloc(sizeof(aux_mutex_t));
		am->key = mkey;
		am->dm = dlock_mutex_create(pmtx);
		HASH_ADD_INT(aux_mutexes, key, am);
		pthread_rwlock_unlock(&hlock);
	}
	return am->dm;
}

// dummy dlock.h functions
dlock_thread_t* dlock_thread(pthread_t* pthr) { return NULL; }
void dlock_set_thread(pthread_t* pthr, dlock_thread_t* thr) {}

static inline int isInvalidLockOp(int tid, pthread_mutex_t *m) {
	return m == NULL;
}

static inline int isInvalidUnlockOp(int tid, pthread_mutex_t *m) {
	return m == NULL;
}

inline int gettid() {
	return syscall(__NR_gettid);
}

/* ########################## PTHREAD API ################################ */

int pthread_mutex_lock(pthread_mutex_t *m) {
	pthread_once(&initializer, preload_init);

	if (isInvalidLockOp(gettid(), m))
		return real_pthread_mutex_lock(m);

	dlock_thread_t* dt = current_dlock_thread();
	dlock_mutex_t* dm = dlock_mutex(m);
	dlock_acquire(dt, dm, 0);
	int r = real_pthread_mutex_lock(m);
	if (r == 0)
		dlock_acquired(dt, dm);
	return r;
}

int pthread_mutex_trylock(pthread_mutex_t *m) {
	pthread_once(&initializer, preload_init);

	if (isInvalidLockOp(gettid(), m))
		return real_pthread_mutex_trylock(m);

	dlock_thread_t* dt = current_dlock_thread();
	dlock_mutex_t* dm = dlock_mutex(m);
	dlock_acquire(dt, dm, 1);
	int r = real_pthread_mutex_trylock(m);
	if (r == 0)
		dlock_acquired(dt, dm);
	else
		dlock_mutex_contention(dm);
	return r;
}

int pthread_mutex_unlock(pthread_mutex_t *m) {
	pthread_once(&initializer, preload_init);

	if (isInvalidUnlockOp(gettid(), m))
		return real_pthread_mutex_unlock(m);

	dlock_thread_t* dt = current_dlock_thread();
	dlock_mutex_t* dm = dlock_mutex(m);

	dlock_release(dt, dm);
	int r = real_pthread_mutex_unlock(m);
	return r;
}
