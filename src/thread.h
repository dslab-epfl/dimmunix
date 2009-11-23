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

extern int (*real_pthread_mutex_lock) (pthread_mutex_t *lock);
extern int (*real_pthread_mutex_trylock) (pthread_mutex_t *lock);
extern int (*real_pthread_mutex_unlock) (pthread_mutex_t *lock);

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
