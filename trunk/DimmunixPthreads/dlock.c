#include "dlock.h"

/* These functions must be specific for each OS/libpthread */

#ifdef __FreeBSD__
/* thr_private.h contains pthread_mutex and pthread structs */
#include "thr_private.h"

dlock_mutex_t* dlock_mutex(pthread_mutex_t* pmtx) {
	return ((struct pthread_mutex*)*pmtx)->dlock_mtx;
}

dlock_thread_t* dlock_thread(pthread_t* pthr) {
	return ((struct pthread*)*pthr)->dlock_thr;
}

void dlock_set_thread(pthread_t* pthr, dlock_thread_t* thr) {
	((struct pthread*)*pthr)->dlock_thr = thr;
}
#endif


