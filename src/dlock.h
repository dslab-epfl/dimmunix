/*
     Created by Horatiu Jula, George Candea, Daniel Tralamazza, Cristian Zamfir
     Copyright (C) 2009 EPFL (Ecole Polytechnique Federale de Lausanne)

     This file is part of Dimmunix.

     Dimmunix is free software: you can redistribute it and/or modify it
     under the terms of the GNU General Public License as published by the
     Free Software Foundation, either version 3 of the License, or (at
     your option) any later version.

     Dimmunix is distributed in the hope that it will be useful, but
     WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
     General Public License for more details.

     You should have received a copy of the GNU General Public
     License along with Dimmunix. If not, see http://www.gnu.org/licenses/.

     EPFL
     Dependable Systems Lab (DSLAB)
     Room 330, Station 14
     1015 Lausanne
     Switzerland
*/

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
typedef dlock::Mutex dlock_mutex_t;
typedef dlock::Thread dlock_thread_t;
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* helpers (dlock.c) */
extern dlock_mutex_t* dlock_mutex(pthread_mutex_t* /*pmtx*/);
extern dlock_thread_t* dlock_thread(pthread_t* /*pthr*/);
extern void dlock_set_thread(pthread_t* /*pthr*/, dlock_thread_t* /*thr*/);

/* thread functions (thread.cc) */
extern dlock_thread_t* dlock_thread_create();
/*extern void dlock_thread_destroy(dlock_thread_t* thr);*/

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
