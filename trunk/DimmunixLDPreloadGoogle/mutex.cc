#include <pthread.h>
#include "mutex.h"
#include "dlock.h"


using namespace dlock;


Mutex* dlock_mutex_create(pthread_mutex_t* pmtx) {
	return new Mutex(pmtx);
}

void dlock_mutex_destroy(Mutex* mtx) {
	if (mtx) delete mtx;
}

void dlock_mutex_contention(Mutex* mtx) {
//	if (mtx) mtx->inc_contentions();
}

void dlock_mutex_enable_avoidance(pthread_mutex_t* pmtx) {
	if (Mutex* m = dlock_mutex(pmtx))
		m->avoid_toggle(true);
}

void dlock_mutex_disable_avoidance(pthread_mutex_t* pmtx) {
	if (Mutex* m = dlock_mutex(pmtx))
		m->avoid_toggle(false);
}

