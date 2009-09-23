#include "dlock.h"
/* thr_private.h contains pthread_mutex and pthread structs */
//#include "thr_private.h"

/* These functions must be specific for each OS/libpthread */

/*
dlock_mutex_t* dlock_mutex(pthread_mutex_t* pmtx) {
//	return ((struct pthread_mutex*)*pmtx)->dlock_mtx;
	return NULL;
}

dlock_thread_t* dlock_thread(pthread_t* pthr) {
//	return ((struct pthread*)*pthr)->dlock_thr;
	return NULL;
}
*/
