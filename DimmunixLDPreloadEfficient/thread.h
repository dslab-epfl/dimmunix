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
#include <sys/time.h>
#include "position.h"
#include "util.h"
#include "hash.h"
#include "yieldcause.h"
#include "event.h"
#include "mutex.h"
#include "template_instance.h"
#include <tr1/unordered_map>

using namespace std::tr1;

namespace dlock {

class Mutex;

class Thread {
public:
	Thread();
	~Thread();

	void enqueue_event(Position* _p, Mutex* _m, Event::Type _type) {
		vector<YieldCause> v;
		_eq.enqueue(Event(_p, this, _m, _type, 0, v));
	}

	void enqueue_yield_event(Position* _p, Mutex* _m, const vector<YieldCause>& yc) {
		_eq.enqueue(Event(_p, this, _m, Event::YIELD, 0, yc));
	}

	/* thread waits (blocks) if it has something in its yield_cause */
	void yield_wait();

	/* if yield_cause is not empty wakes up this thread */
	void yield_notify();

	vector<YieldCause> yield_cause;	/* yieldCause[t] */
	TemplateInstance currentInstance;

	Position* lockPos; /* small hack to pass the acquire position ahead */

	volatile bool zombie; /* dead thread */

	volatile bool bypass_avoidance; /* flag set to true when this thread was forcefuly woke up */

	EventQueue _eq; /* per thread event queue */

	bool in_dimmunix;

	typedef unordered_map<Mutex*, vector<Mutex*>, hash<Mutex*> > InnerLocks;
	InnerLocks innerLocks;

	typedef unordered_map<Mutex*, vector<TemplateInstance>, hash<Mutex*> > Instances;
	Instances instances;

	vector<Mutex*> locksHeld;

	void acquire(Mutex* m);

	void release(Mutex* m, unsigned maxFPs);

private:
	/* copy ctor */
	Thread(const Thread& t) {}
	/* assign operator */
	Thread& operator = (const Thread&t) { return *this; }

	pthread_cond_t fYieldCond;		/* yieldLock[t] */
	pthread_mutex_t fYieldMtx;		/* yieldLock[t] */

	bool lockInversion(Mutex* m1, vector<Mutex*>& v1, Mutex* m2, vector<Mutex*>& v2);

#ifdef __FreeBSD__
	static void dlock_thread_cleanup(void* arg) {
		if (arg)
			((Thread*)arg)->zombie = true; /* mark as zombie  */
	}
#endif
};

}; // namespace dlock

#endif
