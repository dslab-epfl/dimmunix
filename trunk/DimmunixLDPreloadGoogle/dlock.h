#ifndef __DLOCK_H
#define __DLOCK_H

#include <pthread.h>

#ifndef __cplusplus
/* opaque types */
typedef struct dlock_mutex_t dlock_mutex_t;
typedef struct dlock_thread_t dlock_thread_t;
#else
#include "thread.h"
#include "mutex.h"
using namespace dlock;
typedef Mutex dlock_mutex_t;
typedef Thread dlock_thread_t;
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* helpers (dlock.c) */
extern dlock_mutex_t* dlock_mutex(pthread_mutex_t* /*pmtx*/);
//extern dlock_thread_t* dlock_thread(pthread_t* /*pthr*/);

/* thread functions (thread.cc) */
extern dlock_thread_t* dlock_thread_create();

/* mutex functions (mutex.cc) */
extern dlock_mutex_t* dlock_mutex_create(pthread_mutex_t* /*pmtx*/);
extern void dlock_mutex_destroy(dlock_mutex_t* /*mtx*/);
extern void dlock_mutex_contention(dlock_mutex_t* /*mtx*/);
extern void dlock_mutex_enable_avoidance(pthread_mutex_t* /*mtx*/);
extern void dlock_mutex_disable_avoidance(pthread_mutex_t* /*mtx*/);

/* dimmunix main API (avoidance.cc) */
extern void dlock_acquire(dlock_thread_t* /*thr*/, dlock_mutex_t* /*mtx*/, int /*non_blocking*/);
extern void dlock_acquired(dlock_thread_t* /*thr*/, dlock_mutex_t* /*mtx*/);
extern void dlock_release(dlock_thread_t* /*thr*/, dlock_mutex_t* /*mtx*/);
extern void dlock_cancel(dlock_thread_t* /*thr*/, dlock_mutex_t* /*mtx*/);

#ifdef __cplusplus
}
#endif

#endif
