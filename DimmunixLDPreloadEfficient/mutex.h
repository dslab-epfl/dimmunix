/*
 *  mutex.h
 *
 *
 *  Created by Daniel Tralamazza on 4/12/08.
 *  Copyright 2008. All rights reserved.
 *
 */

#ifndef __DLOCK_MTX_H
#define __DLOCK_MTX_H

#include <pthread.h>
#include <stdint.h>
#include "position.h"
#include "util.h"
#include "thread.h"

namespace dlock {

class Thread;

class Mutex {
public:
	Mutex(pthread_mutex_t* _pmtx) : pos(0), owner(0), count(0), fAvoiding(true) {}

	void avoid_toggle(const bool v) { fAvoiding = v; }
	bool avoiding() const { return fAvoiding; }

	Position* pos;			/* acqPos[l] */
	Thread* owner;	/* current owner otherwise null */
	volatile int count;
private:
	/* copy ctor */
	Mutex(Mutex &m) {}
	/* assign operator */
	Mutex& operator = (Mutex &m) { return *this; }

	volatile bool fAvoiding;	/* flag */
};


}; // namespace dlock

typedef dlock::Mutex dlock_mutex_t;

#endif
