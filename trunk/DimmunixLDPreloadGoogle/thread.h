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

#include <map>
#include <vector>
#include <pthread.h>
#include <stdint.h>
#include <time.h>
#include "position.h"
#include "util.h"
#include "hash.h"
#include "mutex.h"


namespace dlock {

class Thread;

class YieldCause {
public:
	YieldCause(Position* p, Thread* t, Mutex *m) : pos(p), thread(t), mutex(m)  {}

	/* copy ctor */
	YieldCause(const YieldCause &yc) : pos(yc.pos), thread(yc.thread), mutex(yc.mutex) {}

	/* assign operator */
	YieldCause& operator = (const YieldCause &yc) {
		if (this != &yc) {
			pos = yc.pos;
			thread = yc.thread;
			mutex = yc.mutex;
		}
		return *this;
	}

	bool operator == (const YieldCause &yc) const {
		return (pos->hashcode == yc.pos->hashcode) && (thread == yc.thread) && (mutex == yc.mutex);
	}

	bool operator < (const YieldCause &yc) const {
		if (pos->hashcode < yc.pos->hashcode)
			return true;
		else if (pos->hashcode > yc.pos->hashcode)
			return false;
		else {
			if (mutex < yc.mutex)
				return true;
			else if (mutex > yc.mutex)
				return false;
			else
				return thread < yc.thread;
		}
	}

	Position* pos;
	Thread* thread;
	Mutex* mutex;
};

inline size_t hash_value(YieldCause const &yc) {
	size_t seed = 0;
	hash_combine(seed, yc.pos->hashcode);
	hash_combine(seed, yc.thread);
	hash_combine(seed, yc.mutex);
	return seed;
}


class Thread {
public:
	typedef pair<Position*, Mutex*> PositionGrant;

	Thread();
	~Thread();

	/* thread waits (blocks) if it has something in its yield_cause */
	void yield_wait();

	/* if yield_cause is not empty wakes up this thread */
	void yield_notify();

	void add_granted(Position* p, Mutex* mtx) {
		positions_granted.push_back(PositionGrant(p, mtx));
	}

	void remove_granted(Position* p, Mutex* mtx) {
		 foreach(vector<PositionGrant>::iterator, it, positions_granted)
			if ( ((*it).first->hashcode == p->hashcode) && (*it).second == mtx ) {
				positions_granted.erase(it);
				break;
			}
	}

	bool contains_granted(Position* p, Mutex* mtx) {
		 foreach(vector<PositionGrant>::iterator, it, positions_granted)
			if ( ((*it).first->hashcode == p->hashcode) && (*it).second == mtx )
				return true;
		 return false;
	}

	volatile bool zombie; /* dead thread */
	volatile bool bypass_avoidance; /* flag set to true when this thread was forcefuly woke up */
	Position* lockPos; /* small hack to pass the acquire position ahead */

	vector<YieldCause> yield_cause;	/* yieldCause[t] */

	vector<PositionGrant> positions_granted;

	struct timespec ts_wait; /* waiting since */
private:
	/* copy ctor */
	Thread(const Thread& t) {}
	/* assign operator */
	Thread& operator = (const Thread&t) { return *this; }

	pthread_cond_t fYieldCond;		/* yieldLock[t] */
	pthread_mutex_t fYieldMtx;		/* yieldLock[t] */
	Position* fLastLockPos;

	static void dlock_thread_cleanup(void* arg) {
		Thread* thr = (Thread*)arg;
		if (thr)
			thr->zombie = true; /* mark as zombie  */
	}
};

}; // namespace dlock

#endif
