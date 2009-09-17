/*
 *  thread.h
 *
 *
 *  Created by Daniel Tralamazza on 4/12/08.
 *  Copyright 2008. All rights reserved.
 *
 */

#ifndef __DLOCK_THR_H
#define __DLOCK_THR_H

#include <vector>
#include <pthread.h>
#include <stdint.h>
#include <time.h>
#include "position.h"
#include "util.h"
#include "hash.h"
#include "yieldcause.h"
#include "event.h"
#include "mutex.h"

namespace dlock {

class Mutex;

class Thread {
public:
	Thread();
	~Thread();

	void enqueue_event(EventQueue& q, Position* _p, Mutex* _m, Event::Type _type) {
//		_eq.enqueue(Event(_p, this, _m, _type));
		q.enqueue(Event(_p, this, _m, _type));
	}

	/* thread waits (blocks) if it has something in its yield_cause */
	void yield_wait();

	/* if yield_cause is not empty wakes up this thread */
	void yield_notify();

	vector<YieldCause> yield_cause;	/* yieldCause[t] */

	Position* lockPos; /* small hack to pass the acquire position ahead */

	volatile bool zombie; /* dead thread */

	volatile bool bypass_avoidance; /* flag set to true when this thread was forcefuly woke up */

	EventQueue _eq; /* per thread event queue */
private:
	/* copy ctor */
	Thread(const Thread& t) {}
	/* assign operator */
	Thread& operator = (const Thread&t) { return *this; }

	pthread_cond_t fYieldCond;		/* yieldLock[t] */
	pthread_mutex_t fYieldMtx;		/* yieldLock[t] */

#ifdef __FreeBSD__
	static void dlock_thread_cleanup(void* arg) {
		if (arg)
			((Thread*)arg)->zombie = true; /* mark as zombie  */
	}
#endif
};

}; // namespace dlock

#endif
